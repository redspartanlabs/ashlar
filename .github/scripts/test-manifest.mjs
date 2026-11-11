#!/usr/bin/env node
// Summarizes the JUnit XML reports a Gradle build wrote, so that CI can publish what a test run actually executed and how long each test class took.
//
//   node .github/scripts/test-manifest.mjs --out <dir> --module <name>=<reports dir> [--module ...]
//
// It reads reports only. It never changes a test, a build file or the build's outcome.
//
// Outputs in <dir>:
//   manifest.json  deterministic: identity of every executed test case, invocation counts and outcomes, and two hashes. No times, no dates, so two runs of the
//                  same tests produce the same bytes and a sharded run can be compared with an unsharded one by merging its manifests.
//   manifest.txt   the same identities, one per line and sorted, for diff.
//   timings.json   per-class suite time (Gradle counts @BeforeAll/@AfterAll work in it), per-case time, and where the data came from.
//   summary.md     the figures people read first; also appended to the job summary when GITHUB_STEP_SUMMARY is set.
//
// Test identity is module | test class | test case name, exactly as the XML reports them. Parameterized invocations are separate cases (their name is the invocation's
// display name). Two executions with the same identity are counted, not collapsed: the count is part of the manifest.

import { createHash } from "node:crypto";
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const args = process.argv.slice(2);
let out = "e2b-out";
const modules = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--out") out = args[++i];
  else if (args[i] === "--module") {
    const [name, dir] = args[++i].split(/=(.*)/s);
    modules.push({ name, dir });
  } else {
    console.error("unknown argument: " + args[i]);
    process.exit(2);
  }
}
if (modules.length === 0) {
  console.error("at least one --module <name>=<reports dir> is required");
  process.exit(2);
}

const ENTITIES = { lt: "<", gt: ">", amp: "&", quot: '"', apos: "'" };
const decode = (s) => s.replace(/&(#x[0-9a-fA-F]+|#\d+|lt|gt|amp|quot|apos);/g, (m, e) => (e[0] === "#" ? String.fromCodePoint(e[1] === "x" ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10)) : ENTITIES[e]));
const attributes = (text) => {
  const found = {};
  for (const m of text.matchAll(/([\w:.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g)) found[m[1]] = decode(m[2] ?? m[3]);
  return found;
};
const TAG_ATTRIBUTES = String.raw`((?:[^>"']|"[^"]*"|'[^']*')*?)`;

const suites = []; // one per XML file / testsuite element
const problems = [];
for (const { name: module, dir } of modules) {
  if (!existsSync(dir)) {
    problems.push(`${module}: no reports directory at ${dir}`);
    continue;
  }
  const files = readdirSync(dir).filter((f) => f.startsWith("TEST-") && f.endsWith(".xml")).sort();
  if (files.length === 0) problems.push(`${module}: ${dir} holds no TEST-*.xml reports`);
  for (const file of files) {
    // Console output and failure bodies are removed first: they are free text and can contain anything, including something that looks like a tag.
    const xml = readFileSync(join(dir, file), "utf8")
      .replace(/<system-(out|err)>[\s\S]*?<\/system-\1>/g, "")
      .replace(/<!\[CDATA\[[\s\S]*?\]\]>/g, "");
    const suiteTag = new RegExp(String.raw`<testsuite\b${TAG_ATTRIBUTES}>`).exec(xml);
    if (!suiteTag) {
      problems.push(`${module}/${file}: no <testsuite> element`);
      continue;
    }
    const suite = attributes(suiteTag[1]);
    const cases = [];
    const caseTag = new RegExp(String.raw`<testcase\b${TAG_ATTRIBUTES}(/?)>`, "g");
    for (let m; (m = caseTag.exec(xml)); ) {
      const a = attributes(m[1]);
      let body = "";
      if (m[2] !== "/") {
        const end = xml.indexOf("</testcase>", caseTag.lastIndex);
        body = end < 0 ? "" : xml.slice(caseTag.lastIndex, end);
      }
      const status = /<skipped\b/.test(body) ? "skipped" : /<failure\b/.test(body) ? "failed" : /<error\b/.test(body) ? "error" : "passed";
      const reason = status === "skipped" ? decode((/<skipped\b[^>]*\bmessage="([^"]*)"/.exec(body) ?? [])[1] ?? "") : undefined;
      cases.push({ classname: a.classname ?? suite.name, name: a.name ?? "", seconds: Number(a.time ?? 0), status, reason });
    }
    if (Number(suite.tests) !== cases.length) problems.push(`${module}/${file}: the suite says ${suite.tests} tests but ${cases.length} <testcase> elements were read`);
    suites.push({ module, file, name: suite.name, declared: Number(suite.tests), seconds: Number(suite.time ?? 0), timestamp: suite.timestamp, cases });
  }
}

// ---- per class ----
const classes = new Map(); // "module|class" -> aggregate
for (const s of suites) {
  for (const c of s.cases) {
    const key = `${s.module}|${c.classname}`;
    const k = classes.get(key) ?? { module: s.module, classname: c.classname, tests: 0, passed: 0, failed: 0, error: 0, skipped: 0, caseSeconds: 0, suites: new Set() };
    k.tests++;
    k[c.status]++;
    k.caseSeconds += c.seconds;
    k.suites.add(s.file);
    classes.set(key, k);
  }
}
const suiteSecondsOf = new Map();
for (const s of suites) {
  const key = `${s.module}|${s.name}`;
  suiteSecondsOf.set(key, (suiteSecondsOf.get(key) ?? 0) + s.seconds);
}
for (const [key, k] of classes) {
  k.suiteSeconds = suiteSecondsOf.get(key) ?? k.caseSeconds;
  k.outsideCasesSeconds = Math.max(0, k.suiteSeconds - k.caseSeconds); // @BeforeAll / @AfterAll and any other time not inside a test case
}

// ---- identities ----
const identities = new Map(); // "module|class|name" -> { count, statuses[] }
for (const s of suites) {
  for (const c of s.cases) {
    const id = `${s.module}|${c.classname}|${c.name}`;
    const e = identities.get(id) ?? { count: 0, statuses: [] };
    e.count++;
    e.statuses.push(c.status);
    identities.set(id, e);
  }
}
const ids = [...identities.keys()].sort();
const identityLines = ids.map((id) => `${id}\t${identities.get(id).count}`);
const statusLines = ids.map((id) => `${id}\t${identities.get(id).count}\t${identities.get(id).statuses.slice().sort().join(",")}`);
const sha = (lines) => createHash("sha256").update(lines.join("\n") + "\n").digest("hex");
const duplicates = ids.filter((id) => identities.get(id).count > 1).map((id) => ({ id, count: identities.get(id).count }));

const total = (pick) => [...classes.values()].reduce((a, k) => a + pick(k), 0);
const totals = { classes: classes.size, tests: total((k) => k.tests), passed: total((k) => k.passed), failed: total((k) => k.failed), errors: total((k) => k.error), skipped: total((k) => k.skipped), distinctIdentities: ids.length, duplicateIdentities: duplicates.length };
const perModule = {};
for (const k of classes.values()) {
  const m = (perModule[k.module] ??= { classes: 0, tests: 0, passed: 0, failed: 0, errors: 0, skipped: 0 });
  m.classes++;
  m.tests += k.tests;
  m.passed += k.passed;
  m.failed += k.failed;
  m.errors += k.error;
  m.skipped += k.skipped;
}
const skippedCases = suites.flatMap((s) => s.cases.filter((c) => c.status === "skipped").map((c) => ({ id: `${s.module}|${c.classname}|${c.name}`, reason: c.reason })));

const manifest = {
  schema: 1,
  note: "Identity is module|class|test case name as reported in the JUnit XML. Counts are executions of that identity. Contains no times or dates.",
  hashes: { identity: sha(identityLines), identityAndOutcome: sha(statusLines) },
  totals,
  modules: Object.fromEntries(Object.entries(perModule).sort(([a], [b]) => a.localeCompare(b))),
  classes: Object.fromEntries([...classes.values()].sort((a, b) => `${a.module}|${a.classname}`.localeCompare(`${b.module}|${b.classname}`))
    .map((k) => [`${k.module}|${k.classname}`, { tests: k.tests, passed: k.passed, failed: k.failed, errors: k.error, skipped: k.skipped }])),
  skipped: skippedCases.sort((a, b) => a.id.localeCompare(b.id)),
  duplicates,
};
const timings = {
  schema: 1,
  source: { sha: process.env.GITHUB_SHA ?? null, runId: process.env.GITHUB_RUN_ID ?? null, runnerOs: process.env.RUNNER_OS ?? null, runnerName: process.env.RUNNER_NAME ?? null },
  note: "suiteSeconds is Gradle's time for the whole class and includes @BeforeAll/@AfterAll; caseSeconds is the sum of the test cases only; outsideCasesSeconds is the difference.",
  classes: [...classes.values()].sort((a, b) => b.suiteSeconds - a.suiteSeconds).map((k) => ({ module: k.module, class: k.classname, tests: k.tests, suiteSeconds: round(k.suiteSeconds), caseSeconds: round(k.caseSeconds), outsideCasesSeconds: round(k.outsideCasesSeconds) })),
  cases: suites.flatMap((s) => s.cases.map((c) => ({ id: `${s.module}|${c.classname}|${c.name}`, seconds: round(c.seconds), status: c.status }))).sort((a, b) => b.seconds - a.seconds),
  suiteTimestamps: suites.map((s) => ({ module: s.module, class: s.name, start: s.timestamp, seconds: round(s.seconds) })).sort((a, b) => String(a.start).localeCompare(String(b.start))),
};
function round(x) { return Math.round(x * 1000) / 1000; }

// ---- summary ----
const sorted = timings.classes;
const sumSuite = sorted.reduce((a, k) => a + k.suiteSeconds, 0);
const buckets = [["< 1 s", 0, 1], ["1 to 10 s", 1, 10], ["10 to 60 s", 10, 60], ["60 to 200 s", 60, 200], ["200 s and over", 200, Infinity]]
  .map(([label, lo, hi]) => { const inB = sorted.filter((k) => k.suiteSeconds >= lo && k.suiteSeconds < hi); return `| ${label} | ${inB.length} | ${inB.reduce((a, k) => a + k.suiteSeconds, 0).toFixed(0)} s |`; });
const fmt = (s) => s.toFixed(1);
const lines = [
  "## Test timings and executed-test manifest",
  "",
  `| module | classes | tests | passed | failed | errors | skipped |`,
  `|---|---|---|---|---|---|---|`,
  ...Object.entries(manifest.modules).map(([m, v]) => `| ${m} | ${v.classes} | ${v.tests} | ${v.passed} | ${v.failed} | ${v.errors} | ${v.skipped} |`),
  `| **all** | **${totals.classes}** | **${totals.tests}** | **${totals.passed}** | **${totals.failed}** | **${totals.errors}** | **${totals.skipped}** |`,
  "",
  `Distinct test identities: ${totals.distinctIdentities}; identities executed more than once: ${totals.duplicateIdentities}.`,
  `Manifest hashes: identity \`${manifest.hashes.identity.slice(0, 16)}\`, identity and outcome \`${manifest.hashes.identityAndOutcome.slice(0, 16)}\`.`,
  `Sum of class times: ${sumSuite.toFixed(0)} s.`,
  "",
  "### Class time distribution",
  "",
  "| class time | classes | total |",
  "|---|---|---|",
  ...buckets,
  "",
  "### Slowest 25 classes",
  "",
  "| # | module | class | tests | class s | in cases s | outside cases s (setup) |",
  "|---|---|---|---|---|---|---|",
  ...sorted.slice(0, 25).map((k, i) => `| ${i + 1} | ${k.module} | ${k.class.split(".").pop()} | ${k.tests} | ${fmt(k.suiteSeconds)} | ${fmt(k.caseSeconds)} | ${fmt(k.outsideCasesSeconds)} |`),
  "",
  ...(skippedCases.length ? ["### Skipped", "", ...skippedCases.map((c) => `- ${c.id}${c.reason ? " - " + c.reason : ""}`), ""] : ["No test was skipped.", ""]),
  ...(duplicates.length ? ["### Identities executed more than once", "", ...duplicates.map((d) => `- ${d.id} x${d.count}`), ""] : []),
  ...(problems.length ? ["### Problems reading the reports", "", ...problems.map((p) => "- " + p), ""] : []),
];

mkdirSync(out, { recursive: true });
writeFileSync(join(out, "manifest.json"), JSON.stringify(manifest, null, 1) + "\n");
writeFileSync(join(out, "manifest.txt"), statusLines.join("\n") + "\n");
writeFileSync(join(out, "timings.json"), JSON.stringify(timings, null, 1) + "\n");
writeFileSync(join(out, "summary.md"), lines.join("\n"));
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, lines.join("\n") + "\n");
console.log(lines.slice(0, 12).join("\n"));
if (problems.length) {
  console.error(problems.join("\n"));
  process.exit(suites.length === 0 ? 1 : 0);
}
