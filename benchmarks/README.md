# Instruction-count regression gate

The required gate counts executed Java bytecode instructions using JVMTI
`SingleStep` events, under `-Xint`. It does not measure CPU or wall-clock time.
The small native agent is only a benchmark tool, not a library dependency.

Requirements: Linux, a C compiler, Python 3.9+, and the exact JDK recorded in
`instruction-baseline.json` (currently Temurin 25.0.4.1+1). Run `mvn test` once
to download dependencies, then:

```sh
python3 benchmarks/compare.py --java-home "$JAVA_HOME"
```

The agent counts only the calling thread between `start()` and `stop()`.
The measured region includes the query, Java library calls and fixed harness
bytecodes. Initialization, fixture construction, warmup and output are outside
it. Native method bodies, GC and other threads are not counted. The harness is
single-threaded and checks query results as well as count stability.

Each scenario must produce identical counts across four measured calls in
each of three fresh JVMs. There is no averaging or noise tolerance. A fixed
heap, Serial GC and one reported processor reduce environment-dependent paths.
JDK/compiler version, dependency hashes, harness/agent hashes and VM options
are pinned. A mismatch or unstable count fails instead of accepting a baseline.
Different hardware still must execute the same Java paths; an architecture or
JDK change can require an explicit baseline migration.

Exit statuses:

- **0:** exact match, or explicitly accepted improvement.
- **1:** any count increased; baseline updates are refused even if other
  scenarios improved. Measurement/build errors also fail.
- **2:** any count decreased without increases elsewhere; update is required.

To accept an improvement, remeasure with:

```sh
python3 benchmarks/compare.py --java-home "$JAVA_HOME" --update-baseline
```

Commit `instruction-baseline.json` and `instruction-results.json`. The update
only succeeds with equal or lower counts for every scenario. Baseline writes
are atomic. `--initialize-baseline` creates a missing baseline and refuses to
overwrite an existing one; use it only for an explicitly reviewed migration.

Policy tests:

```sh
python3 -m unittest discover -s benchmarks -p 'test_*.py'
```

The fixture contains 50 repeated elements. Eight scenarios cover matching and
missing tags, case-insensitive names, classes, attribute presence/absence,
attribute equality and descendants. This measures executed bytecodes rather
than the speed of JIT-generated machine code. `ParserBenchmark.java` remains
available for optional timing investigation; timing is not an acceptance gate.
