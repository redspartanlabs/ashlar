# How Ashlar's CI runs the tests

CI builds and tests everything on every pull request and every push to `master`. The Showcase tests (96 classes, about 900 test executions, most of them driving a real Chrome) are split across four runners. This page explains why, how a class ends up in a shard, what the final `build` check proves, and what to do when it fails.

Files involved:

| File | Role |
|---|---|
| `.github/workflows/ci.yml` | The jobs: `library`, `showcase (shard 1..4)`, `build` |
| `.github/ci/showcase-shards.json` | The shard count and the pinned (hand-placed) classes |
| `.github/scripts/shard-tests.mjs` | Discovers the test classes and assigns each to a shard |
| `.github/scripts/coverage-gate.mjs` | The gate run by the `build` job |
| `.github/scripts/test-manifest.mjs` | Reads JUnit XML and writes the executed-test inventory and class timings |

## Why four shards

The unsharded build took 35 to 46 minutes, 97% of it in the Showcase browser tests, which run one class after another in a single JVM. Measured on the same runner image (`ubuntu-26.04`): 44.6 minutes unsharded against 13.0 to 13.7 minutes sharded, for about the same total machine time (roughly 47 runner minutes against 45). Four is close to the useful limit: the single largest class (`BadgeParityTest`, about 440 seconds) is a floor of roughly 9 minutes, and more shards would repeat the one-minute build step without shortening the longest one.

Sharding changes where a test runs, never whether or how it runs. No test, assertion, tolerance or browser setup is different in a shard, and every browser test still gets its own full runner.

## The jobs

- **`library`**: everything `./gradlew build` does except the Showcase tests, including the fast Ashlar library tests.
- **`showcase (shard 1..4)`**: each runs `:showcase:test` with one `--tests` filter per class assigned to it.
- **`build`**: the final gate, and the only check branch rules require. It is named `build` on purpose. It needs `library` and `showcase`, and it runs even when they fail, are cancelled or are skipped, because a skipped required check counts as passing. It fails unless the coverage gate passes.

## How a class is assigned to a shard

A *test class* is any class under `showcase/src/test/java` whose source has a JUnit test annotation (`@Test`, `@ParameterizedTest`, and so on). `shard-tests.mjs` assigns each one to exactly one shard by two rules:

1. **Pinned**: if the class is listed under `pinned` in `.github/ci/showcase-shards.json`, it runs in that shard.
2. **Otherwise**: it runs in shard `(sha256(fully qualified class name) mod shards) + 1`. There is no salt and no table.

To see the whole assignment, or one shard's list:

```
node .github/scripts/shard-tests.mjs plan
node .github/scripts/shard-tests.mjs select --shard 2
```

## How pinned classes are maintained

Pins exist only to balance the load: the slow classes are placed by hand so that no shard gets several of them. The current rule is to pin every class that took 20 seconds or more, longest first into the least-loaded shard (31 classes). The rest, which are fast, are spread by the hash.

- A pin must name a class that still exists, in a shard that exists. A stale pin is an error in the shard jobs and in the gate, so a renamed or deleted class has to be fixed in `showcase-shards.json`.
- Do not tune pins from one run. Per-class times are stable between runs (about 4% median difference), but the same shard can vary by more than a minute from run to run on identical code, which is runner noise, not imbalance.
- Reassess the pins after a major component-family migration adds or reshapes many test classes, or when several runs in a row show the same shard clearly slowest (see "Refreshing the pins").
- The `shards` count and the `shard: [1, 2, 3, 4]` matrix in `ci.yml` must agree. If they do not, the gate fails (a missing shard artifact) or a shard job fails (a shard number out of range). Changing the count also changes where hashed classes land, so re-pin after changing it.

## How new test classes are included automatically

Nothing is listed by hand except the pins. A new test class is found by its annotation, has no pin, and so is assigned by its hash. Adding a class never moves another class. Each shard job then checks that the classes the sources declare are exactly the classes Gradle compiled, and the gate checks that the new class actually ran. If a class could not be found or run, the `build` check fails rather than passing silently.

## What the `build` gate validates

The gate (`coverage-gate.mjs`) needs no committed snapshot of the tests. It derives everything it compares against, in the same run, from the sources and from what Gradle compiled and executed. It fails closed: anything it cannot read, find or account for is a failure. It fails when:

1. a required job did not finish as `success` (failed, cancelled, skipped, or missing), or the job results are unreadable;
2. a job's artifact, JUnit XML, class list or compiled-class list is missing, empty or malformed;
3. the classes Gradle compiled differ from the test classes the sources declare;
4. a compiled Showcase class is not assigned to exactly one shard;
5. a shard was told to run a list that differs from its assignment;
6. a shard did not execute an assigned class, or executed one it was not assigned;
7. a class ran in no shard, in more than one shard, or more than once within a shard;
8. the library job executed a different set of classes from the ones its sources declare;
9. any test failed, errored or was skipped (there is no allowance for skips);
10. a plain `@Test` method declared in the sources has no executed test case of that name.

Its report is in the `build` job's summary, and the `coverage-gate` artifact holds `coverage-gate.md`, `coverage-gate.json` (every violation) and the combined inventory of executed tests (`combined-manifest.txt`).

### Limitation: parameterized tests

Gradle's JUnit XML names a parameterized invocation by its display name only, not by the method that produced it. Two methods of one class whose invocations share a display name (for example `[1] INFO`) are indistinguishable. For `@ParameterizedTest` methods the gate can only require at least one invocation per method and compare the number of executions per display name and per class; it cannot say which method ran an invocation. Every plain `@Test` method is checked by name. Method-level equivalence for parameterized tests would need the JUnit Platform's own unique IDs (its open-test-reporting XML, or a `TestExecutionListener`); that is a separate, deferred infrastructure task.

A stronger periodic check, a scheduled run that compares the sharded inventory with an unsharded run of the same commit, has been approved as future work and is not part of this setup.

## Investigating a failed or incomplete build

1. **Which job failed?** Open the pull request's checks. A red `showcase (shard N)` is a test or infrastructure failure in that shard; a red `build` with green shards means the gate found a coverage problem.
2. **Read the gate.** Open the `build` job and read its summary (the same text is in the `coverage-gate` artifact). The first violation is usually the cause. Typical messages:
   - `required job "showcase" finished as "failure"`: a shard failed; look at that shard's log.
   - `shard N: its artifact is missing` or `holds no JUnit XML`: the shard never produced results (it failed early, was cancelled or timed out). Re-run the shard.
   - `test class X exists but was executed in no shard`: the class was not run. If a shard also failed, that explains it; otherwise check `shard-classes.txt` in that shard's artifact.
   - `X was compiled as a test class but the sources do not declare it` (or the reverse): a class that Gradle sees and the discovery does not, or the opposite. Check that its annotations are ordinary JUnit annotations and that it is a top-level class.
   - `the @Test method M has no executed test case`: a method did not run. Look for a filter, a rename, or a class that failed to initialize.
   - `... pins N class(es) that are not test classes any more`: fix `showcase-shards.json`.
3. **Look at the evidence.** Each shard uploads an artifact (`showcase-shard-N`, kept 14 days) with the JUnit XML, the list it was told to run (`shard-classes.txt`), the compiled classes, a timing and inventory summary (`manifest/summary.md`, `manifest/timings.json`) and Gradle's profile.
4. **Reproduce locally.** Run one shard's classes exactly as CI does:

   ```
   node .github/scripts/shard-tests.mjs select --shard 2 > shard-classes.txt
   args=(); while IFS= read -r c; do args+=(--tests "$c"); done < shard-classes.txt
   ./gradlew :showcase:test "${args[@]}"
   ```

   A single class: `./gradlew :showcase:test --tests dev.redspartanlabs.ashlar.showcase.cascade.TabsParityTest`.

## Refreshing the pins

Do this only when the guidance above says to. It needs the timings of several recent sharded CI runs.

1. Pick three or more recent successful runs on `master` and download their artifacts into separate folders:

   ```
   gh run download <run id> -D timings/run1
   gh run download <run id> -D timings/run2
   gh run download <run id> -D timings/run3
   ```

2. Save this script outside the repository (it only reads) and run it from the repository root. It averages each class's time over the runs, keeps the hashed (fast) classes where the hash puts them, and places every class of 20 seconds or more, longest first, into the shard with the least expected time:

   ```js
   // refresh-pins.mjs   usage: node refresh-pins.mjs timings/run1 timings/run2 timings/run3
   import { readdirSync, readFileSync } from "node:fs";
   import { join, resolve } from "node:path";
   import { pathToFileURL } from "node:url";
   const { discover, loadConfig, hashShard } = await import(pathToFileURL(resolve(".github/scripts/shard-tests.mjs")).href);
   const config = loadConfig();
   const THRESHOLD_SECONDS = 20;
   const seconds = new Map();
   for (const run of process.argv.slice(2)) {
     for (const shard of readdirSync(run).filter((d) => d.startsWith("showcase-shard-"))) {
       const timings = JSON.parse(readFileSync(join(run, shard, "manifest", "timings.json"), "utf8"));
       for (const c of timings.classes) if (c.module === "showcase") (seconds.get(c.class) ?? seconds.set(c.class, []).get(c.class)).push(c.suiteSeconds);
     }
   }
   const mean = (name) => { const v = seconds.get(name) ?? [0]; return v.reduce((a, b) => a + b, 0) / v.length; };
   const names = discover("showcase/src/test/java").map((c) => c.fqcn);
   const heavy = names.filter((n) => mean(n) >= THRESHOLD_SECONDS);
   const load = Array(config.shards).fill(0);
   for (const n of names) if (!heavy.includes(n)) load[hashShard(n, config.shards) - 1] += mean(n);
   const pinned = {};
   for (const n of heavy.sort((a, b) => mean(b) - mean(a) || a.localeCompare(b))) {
     const i = load.indexOf(Math.min(...load));
     load[i] += mean(n);
     pinned[n] = i + 1;
   }
   console.log(JSON.stringify(Object.fromEntries(Object.entries(pinned).sort(([a, x], [b, y]) => x - y || a.localeCompare(b))), null, 2));
   console.error("expected class seconds per shard:", load.map((x) => x.toFixed(0)).join(" / "));
   ```

3. Replace the `pinned` object in `showcase-shards.json` with the printed one and update its `_basis` note with the run ids. Check the result with `node .github/scripts/shard-tests.mjs plan`.
4. Open a pull request and look at the new run before merging: the gate must pass, and the slowest shard should not be slower than before. If the improvement is only a few percent, leave the pins alone.
