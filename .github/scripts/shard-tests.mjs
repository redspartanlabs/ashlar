#!/usr/bin/env node
// Decides which CI shard runs which Showcase test class, and checks that decision against what Gradle will actually find.
//
//   node .github/scripts/shard-tests.mjs select --shard <1..N>      the test classes (one fully qualified name per line) shard <n> must run
//   node .github/scripts/shard-tests.mjs plan                       the whole assignment, class by class, with the pinned or hashed reason
//   node .github/scripts/shard-tests.mjs compiled <dir>             the compiled test classes in <dir>, one per line
//   node .github/scripts/shard-tests.mjs verify-compiled <dir>      the compiled test classes in <dir> are exactly the classes this tool discovered
//
// The rule, in full:
//   1. A class named in .github/ci/showcase-shards.json ("pinned") runs in the shard given there. These are the heavy classes, placed by hand so the shards are
//      balanced.
//   2. Every other class runs in shard (sha256(fully qualified name) mod N) + 1. No salt, no table: a class added tomorrow is assigned the same way today, so it
//      cannot be left out of every shard.
// A class is a test class if its source file has a JUnit test annotation. The whole split is therefore a partition by construction: each discovered class gets
// exactly one shard. Nothing here excludes a class, and nothing here changes how a test runs.

import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { pathToFileURL } from "node:url";

export const CONFIG_PATH = ".github/ci/showcase-shards.json";
export const SHOWCASE_TESTS = "showcase/src/test/java";
export const ASHLAR_TESTS = "ashlar/src/test/java";
const TEST_ANNOTATION = /^[ \t]*@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b/m;
const COMPILED_ANNOTATION = /Lorg\/junit\/jupiter\/api\/(Test|RepeatedTest|TestFactory|TestTemplate);|Lorg\/junit\/jupiter\/params\/ParameterizedTest;/;

function walk(dir, found = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) walk(path, found);
    else found.push(path);
  }
  return found;
}

/** Every test class under <root>: { fqcn, file }, sorted by name. A class is a test class if its source has a JUnit test annotation. */
export function discover(root) {
  if (!existsSync(root)) throw new Error(`no test source directory at ${root}`);
  const classes = [];
  for (const file of walk(root).filter((f) => f.endsWith(".java")).sort()) {
    const text = readFileSync(file, "utf8");
    if (!TEST_ANNOTATION.test(text)) continue;
    const pkg = (/^package\s+([\w.]+)\s*;/m.exec(text) ?? [])[1];
    const simple = file.split(sep).pop().replace(/\.java$/, "");
    classes.push({ fqcn: pkg ? `${pkg}.${simple}` : simple, file });
  }
  return classes.sort((a, b) => a.fqcn.localeCompare(b.fqcn));
}

export function loadConfig(path = CONFIG_PATH) {
  const config = JSON.parse(readFileSync(path, "utf8"));
  if (!Number.isInteger(config.shards) || config.shards < 1) throw new Error(`${path}: "shards" must be a positive integer`);
  for (const [name, shard] of Object.entries(config.pinned ?? {})) {
    if (!Number.isInteger(shard) || shard < 1 || shard > config.shards) throw new Error(`${path}: ${name} is pinned to shard ${shard}, but there are ${config.shards} shards`);
  }
  return config;
}

export function hashShard(fqcn, shards) {
  return (createHash("sha256").update(fqcn).digest().readUInt32BE(0) % shards) + 1;
}

/** Assigns every discovered class to one shard. Throws if the pins name a class that no longer exists (a stale pin must be fixed, not ignored). */
export function assign(classes, config) {
  const names = new Set(classes.map((c) => c.fqcn));
  const stale = Object.keys(config.pinned ?? {}).filter((n) => !names.has(n));
  if (stale.length) throw new Error(`${CONFIG_PATH} pins ${stale.length} class(es) that are not test classes any more: ${stale.join(", ")}`);
  return new Map(classes.map((c) => [c.fqcn, config.pinned?.[c.fqcn] !== undefined ? { shard: config.pinned[c.fqcn], reason: "pinned" } : { shard: hashShard(c.fqcn, config.shards), reason: "hash" }]));
}

/** The compiled top-level classes under <dir> that carry a JUnit test annotation, as fully qualified names. */
export function compiledTestClasses(dir) {
  if (!existsSync(dir) || !statSync(dir).isDirectory()) throw new Error(`no compiled test classes at ${dir}`);
  return walk(dir)
    .filter((f) => f.endsWith(".class") && !f.split(sep).pop().includes("$"))
    .filter((f) => COMPILED_ANNOTATION.test(readFileSync(f).toString("latin1")))
    .map((f) => relative(dir, f).replace(/\.class$/, "").split(sep).join("."))
    .sort();
}

function main() {
  const [command, ...rest] = process.argv.slice(2);
  const option = (name) => { const i = rest.indexOf(name); return i < 0 ? undefined : rest[i + 1]; };
  const classes = discover(SHOWCASE_TESTS);
  const config = loadConfig();
  const plan = assign(classes, config);
  if (command === "select") {
    const shard = Number(option("--shard"));
    if (!Number.isInteger(shard) || shard < 1 || shard > config.shards) throw new Error(`--shard must be 1..${config.shards}`);
    const mine = [...plan].filter(([, v]) => v.shard === shard).map(([name]) => name).sort();
    if (mine.length === 0) throw new Error(`shard ${shard} has no test classes`);
    console.log(mine.join("\n"));
  } else if (command === "plan") {
    console.log(`${classes.length} test classes, ${config.shards} shards, ${Object.keys(config.pinned ?? {}).length} pinned`);
    for (let s = 1; s <= config.shards; s++) {
      const mine = [...plan].filter(([, v]) => v.shard === s);
      console.log(`\nshard ${s}: ${mine.length} classes (${mine.filter(([, v]) => v.reason === "pinned").length} pinned, ${mine.filter(([, v]) => v.reason === "hash").length} hashed)`);
      for (const [name, v] of mine.sort()) console.log(`  ${v.reason === "pinned" ? "P" : "h"} ${name}`);
    }
  } else if (command === "compiled") {
    // The compiled test classes under <dir>, one fully qualified name per line. CI records this list per job so the gate can compare it with the source discovery.
    console.log(compiledTestClasses(rest[0]).join("\n"));
  } else if (command === "verify-compiled") {
    const compiled = compiledTestClasses(rest[0]);
    const discovered = classes.map((c) => c.fqcn);
    const onlyCompiled = compiled.filter((n) => !discovered.includes(n));
    const onlyDiscovered = discovered.filter((n) => !compiled.includes(n));
    if (onlyCompiled.length || onlyDiscovered.length) {
      console.error(`The compiled test classes and the discovered test classes differ.\n  compiled only: ${onlyCompiled.join(", ") || "none"}\n  discovered only: ${onlyDiscovered.join(", ") || "none"}`);
      process.exit(1);
    }
    console.log(`compiled and discovered test classes agree: ${compiled.length} classes`);
  } else {
    console.error("usage: shard-tests.mjs select --shard <n> | plan | compiled <dir> | verify-compiled <dir>");
    process.exit(2);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { main(); } catch (e) { console.error(String(e.message ?? e)); process.exit(1); }
}
