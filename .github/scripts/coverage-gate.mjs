#!/usr/bin/env node
// The coverage-equivalence gate of the sharded CI build. It decides whether the shards, together, ran every test that exists, once, and passed.
//
//   NEEDS_JSON='<toJSON(needs)>' node .github/scripts/coverage-gate.mjs --in <dir of downloaded artifacts> --out <dir> [--baseline <manifest.txt>]
//
// <dir> holds one folder per artifact: showcase-shard-1 .. showcase-shard-N, each with junit/*.xml and shard-classes.txt, and library, with junit/*.xml.
//
// It fails (exit 1) when ANY of these holds:
//   - a required job did not finish as "success" (failed, cancelled, skipped, or absent from the needs context);
//   - a shard's artifact, JUnit XML or class list is missing;
//   - the classes a shard was told to run differ from the classes the repository's shard assignment gives it;
//   - the classes a shard executed differ from the classes it was assigned (a class missing, or one that should not be there);
//   - a test class that exists in the source was executed in no shard, or in more than one;
//   - the library module executed a different set of classes from the ones its sources declare;
//   - a test failed, errored or was skipped (there is no allowance for skips: the unsharded baseline had none);
//   - a plain @Test method declared in the source has no executed test case of that name, or a class executed fewer cases than it declares methods;
//   - with --baseline: the combined executed-test inventory (every test case's module, class, name, count and outcome) differs from the unsharded baseline.
//
// What it cannot prove, stated plainly: Gradle's JUnit XML names a parameterized invocation by its display name only, without the method that produced it.
// Two methods of one class whose invocations share a display name ("[1] INFO") are therefore indistinguishable in the XML. For those the gate compares
// the number of executions per display name and per class, not which method ran them. Every plain @Test method is checked by name. See the list printed in the
// summary, and docs in the workflow for the stronger identification this would need.

import { spawnSync } from "node:child_process";
import { appendFileSync, existsSync, mkdirSync, mkdtempSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { ASHLAR_TESTS, SHOWCASE_TESTS, assign, discover, loadConfig } from "./shard-tests.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const option = (name) => { const i = args.indexOf(name); return i < 0 ? undefined : args[i + 1]; };
const inDir = option("--in");
const outDir = option("--out") ?? "gate-out";
const baselinePath = option("--baseline");
if (!inDir) {
  console.error("usage: coverage-gate.mjs --in <artifacts dir> [--out <dir>] [--baseline <manifest.txt>]");
  process.exit(2);
}

const violations = [];
const notes = [];
const fail = (message) => violations.push(message);
const diff = (a, b) => [...a].filter((x) => !b.has(x)).sort();
const xmlFiles = (dir) => (existsSync(dir) ? readdirSync(dir).filter((f) => f.startsWith("TEST-") && f.endsWith(".xml")) : []);
const lines = (file) => readFileSync(file, "utf8").split(/\r?\n/).filter((l) => l.trim() !== "");

/** Runs the manifest tool over the given [module, dir] pairs and returns its manifest.json (parsed) and manifest.txt (lines). */
function manifestOf(pairs) {
  const out = mkdtempSync(join(tmpdir(), "gate-manifest-"));
  const run = spawnSync(process.execPath, [join(here, "test-manifest.mjs"), "--out", out, ...pairs.flatMap(([m, d]) => ["--module", `${m}=${d}`])], { encoding: "utf8" });
  if (!existsSync(join(out, "manifest.json"))) throw new Error(`the manifest tool produced nothing (${run.stderr || run.stdout})`);
  return { json: JSON.parse(readFileSync(join(out, "manifest.json"), "utf8")), txt: lines(join(out, "manifest.txt")), problems: run.stderr };
}

/** The @Test and @ParameterizedTest methods a source file declares: { plain: Set of names, parameterized: Set of names }. */
function declaredMethods(file) {
  const text = readFileSync(file, "utf8").replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");
  const plain = new Set();
  const parameterized = new Set();
  for (const m of text.matchAll(/@(Test|ParameterizedTest)\b/g)) {
    const next = /\bvoid\s+(\w+)\s*\(/.exec(text.slice(m.index));
    if (next) (m[1] === "Test" ? plain : parameterized).add(next[1]);
  }
  return { plain, parameterized };
}

// ---- 1. the required jobs ----
const REQUIRED_JOBS = ["library", "showcase"];
let needs = null;
try { needs = JSON.parse(process.env.NEEDS_JSON ?? ""); } catch { /* reported below */ }
if (!needs || typeof needs !== "object") fail("the needs context is missing or unreadable, so no job result can be trusted");
else {
  for (const job of REQUIRED_JOBS) if (!(job in needs)) fail(`required job "${job}" is not in the needs context`);
  for (const [job, value] of Object.entries(needs)) if (value?.result !== "success") fail(`required job "${job}" finished as "${value?.result}", not "success"`);
}

// ---- 2. what should run ----
let config;
let showcase;
let ashlar;
let plan;
try {
  config = loadConfig();
  showcase = discover(SHOWCASE_TESTS);
  ashlar = discover(ASHLAR_TESTS);
  plan = assign(showcase, config);
} catch (error) {
  // Without a valid assignment nothing else can be judged; say why and stop.
  fail(`the shard assignment cannot be computed: ${error.message}`);
  mkdirSync(outDir, { recursive: true });
  const text = ["## Coverage-equivalence gate: FAILED", "", ...violations.map((v) => "- " + v), ""].join("\n");
  writeFileSync(join(outDir, "coverage-gate.md"), text);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, text + "\n");
  console.log(text);
  process.exit(1);
}
const showcaseNames = new Set(showcase.map((c) => c.fqcn));
const ashlarNames = new Set(ashlar.map((c) => c.fqcn));

// ---- 3. each shard ----
const shardDirs = [];
const executedByShard = new Map();
for (let shard = 1; shard <= config.shards; shard++) {
  const dir = join(inDir, `showcase-shard-${shard}`);
  const junit = join(dir, "junit");
  const xmls = xmlFiles(junit);
  if (!existsSync(dir)) { fail(`shard ${shard}: its artifact is missing`); continue; }
  if (xmls.length === 0) { fail(`shard ${shard}: its artifact holds no JUnit XML`); continue; }
  const assigned = new Set([...plan].filter(([, v]) => v.shard === shard).map(([name]) => name));
  const listFile = join(dir, "shard-classes.txt");
  if (!existsSync(listFile)) fail(`shard ${shard}: shard-classes.txt (the classes it was told to run) is missing`);
  else {
    const told = new Set(lines(listFile));
    for (const n of diff(assigned, told)) fail(`shard ${shard}: ${n} is assigned to it by the repository but was not in the list it ran`);
    for (const n of diff(told, assigned)) fail(`shard ${shard}: it ran ${n}, which the repository assigns elsewhere or not at all`);
  }
  const m = manifestOf([["showcase", junit]]);
  const executed = new Set(Object.keys(m.json.classes).map((k) => k.slice(k.indexOf("|") + 1)));
  for (const n of diff(assigned, executed)) fail(`shard ${shard}: assigned class ${n} was not executed`);
  for (const n of diff(executed, assigned)) fail(`shard ${shard}: executed class ${n}, which is not assigned to it`);
  if (xmls.length !== executed.size) fail(`shard ${shard}: ${xmls.length} XML reports for ${executed.size} classes`);
  if (m.json.totals.tests === 0) fail(`shard ${shard}: no tests were executed`);
  executedByShard.set(shard, executed);
  shardDirs.push(junit);
}

// every discovered class exactly once across the shards
const counts = new Map();
for (const executed of executedByShard.values()) for (const n of executed) counts.set(n, (counts.get(n) ?? 0) + 1);
for (const n of showcaseNames) if (!counts.has(n)) fail(`test class ${n} exists but was executed in no shard`);
for (const [n, c] of counts) {
  if (c > 1) fail(`test class ${n} was executed in ${c} shards`);
  if (!showcaseNames.has(n)) fail(`test class ${n} was executed but is not a discovered Showcase test class`);
}

// ---- 4. the library module ----
const libraryJunit = join(inDir, "library", "junit");
const libraryXml = xmlFiles(libraryJunit);
if (libraryXml.length === 0) fail("library: its artifact holds no JUnit XML");
else {
  const m = manifestOf([["ashlar", libraryJunit]]);
  const executed = new Set(Object.keys(m.json.classes).map((k) => k.slice(k.indexOf("|") + 1)));
  for (const n of diff(ashlarNames, executed)) fail(`library: test class ${n} exists but was not executed`);
  for (const n of diff(executed, ashlarNames)) fail(`library: executed class ${n}, which is not a discovered Ashlar test class`);
}

// ---- 5. outcomes and the combined inventory ----
const pairs = [["ashlar", libraryJunit], ...shardDirs.map((d) => ["showcase", d])];
let combined = null;
if (libraryXml.length > 0 || shardDirs.length > 0) {
  combined = manifestOf(pairs.filter(([, d]) => xmlFiles(d).length > 0));
  const t = combined.json.totals;
  if (t.failed || t.errors) fail(`${t.failed} test(s) failed and ${t.errors} errored`);
  if (t.skipped) fail(`${t.skipped} test(s) were skipped, and none may be: ${combined.json.skipped.map((s) => s.id).join("; ")}`);
  if (combined.problems && combined.problems.trim()) fail(`the JUnit XML could not be read cleanly: ${combined.problems.trim().split("\n")[0]}`);

  // plain @Test methods, by name; parameterized ones can only be counted
  const namesByClass = new Map();
  const casesByClass = new Map();
  for (const line of combined.txt) {
    const [id, count] = line.split("\t");
    const [module, cls, ...rest] = id.split("|");
    const key = `${module}|${cls}`;
    (namesByClass.get(key) ?? namesByClass.set(key, []).get(key)).push(rest.join("|"));
    casesByClass.set(key, (casesByClass.get(key) ?? 0) + Number(count));
  }
  let plainChecked = 0;
  let parameterizedMethods = 0;
  for (const [module, list] of [["ashlar", ashlar], ["showcase", showcase]]) {
    for (const c of list) {
      const key = `${module}|${c.fqcn}`;
      const names = namesByClass.get(key);
      if (!names) continue; // already reported as an unexecuted class
      const declared = declaredMethods(c.file);
      for (const method of declared.plain) {
        plainChecked++;
        if (!names.some((n) => n.startsWith(`${method}(`))) fail(`${c.fqcn}: the @Test method ${method} has no executed test case`);
      }
      parameterizedMethods += declared.parameterized.size;
      const minimum = declared.plain.size + declared.parameterized.size;
      if ((casesByClass.get(key) ?? 0) < minimum) fail(`${c.fqcn}: ${casesByClass.get(key) ?? 0} cases executed for ${minimum} declared test methods`);
    }
  }
  notes.push(`${plainChecked} plain @Test methods were each matched to an executed test case by name.`);
  notes.push(`${parameterizedMethods} @ParameterizedTest methods can only be checked as "at least one invocation per method" (the XML does not say which method ran an invocation).`);
  if (combined.json.duplicates.length) {
    notes.push(`${combined.json.duplicates.length} test identities occur more than once (${combined.json.duplicates.reduce((a, d) => a + d.count - 1, 0)} extra executions): parameterized display names reused by different methods of one class. They are compared by name and count only: ${combined.json.duplicates.map((d) => d.id.split("|").slice(1).join("|").replace(/^dev\.redspartanlabs\.ashlar\./, "") + " x" + d.count).slice(0, 6).join("; ")}${combined.json.duplicates.length > 6 ? "; ..." : ""}`);
  }

  // against the unsharded baseline
  if (baselinePath) {
    if (!existsSync(baselinePath)) fail(`the baseline ${baselinePath} does not exist`);
    else {
      const baseline = lines(baselinePath);
      const have = new Set(combined.txt);
      const want = new Set(baseline);
      const missing = diff(want, have);
      const extra = diff(have, want);
      if (missing.length || extra.length) {
        fail(`the executed-test inventory differs from the unsharded baseline: ${missing.length} baseline entries absent, ${extra.length} entries not in the baseline`);
        for (const l of missing.slice(0, 10)) notes.push(`absent from the sharded run: ${l.replace(/\t/g, "  |  ").slice(0, 200)}`);
        for (const l of extra.slice(0, 10)) notes.push(`not in the baseline: ${l.replace(/\t/g, "  |  ").slice(0, 200)}`);
      } else notes.push(`the combined inventory equals the unsharded baseline entry for entry: ${baseline.length} identities, every count and outcome.`);
    }
  }
}

// ---- report ----
const t = combined?.json.totals;
const report = [
  `## Coverage-equivalence gate: ${violations.length === 0 ? "PASSED" : "FAILED"}`,
  "",
  ...(t ? [
    `| classes | executions | passed | failed | errors | skipped | distinct identities |`,
    `|---|---|---|---|---|---|---|`,
    `| ${t.classes} | ${t.tests} | ${t.passed} | ${t.failed} | ${t.errors} | ${t.skipped} | ${t.distinctIdentities} |`,
    "",
    `Inventory hashes (identity / identity and outcome): \`${combined.json.hashes.identity.slice(0, 16)}\` / \`${combined.json.hashes.identityAndOutcome.slice(0, 16)}\``,
    "",
  ] : []),
  ...(violations.length ? ["### Violations", "", ...violations.map((v) => "- " + v), ""] : []),
  ...(notes.length ? ["### Evidence and limits", "", ...notes.map((n) => "- " + n), ""] : []),
];
mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, "coverage-gate.md"), report.join("\n"));
writeFileSync(join(outDir, "coverage-gate.json"), JSON.stringify({ passed: violations.length === 0, violations, notes, totals: t ?? null, hashes: combined?.json.hashes ?? null }, null, 1) + "\n");
if (combined) {
  writeFileSync(join(outDir, "combined-manifest.txt"), combined.txt.join("\n") + "\n");
  writeFileSync(join(outDir, "combined-manifest.json"), JSON.stringify(combined.json, null, 1) + "\n");
}
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, report.join("\n") + "\n");
console.log(report.join("\n"));
process.exit(violations.length === 0 ? 0 : 1);
