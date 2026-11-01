# Instruction regression tests

Use Temurin JDK 25.0.4.1+1 (the version pinned by the baseline), then run:

```sh
mvn test
```

JUnit runs correctness and instruction-regression tests. There is no Python,
custom native agent, C compiler, bytecode rewriter or extra dependency. The
production library targets Java 11; tests target Java 25.

## Measurement

The JUnit test launches an isolated JVM through the JDK's JDI launching
connector. The target uses `-Xint`, a fixed heap, Serial GC and one reported
processor. The JDK's bundled JDWP debugger supplies `STEP_MIN` / `STEP_INTO`
events. We do not implement instrumentation or ship a native library.

Stepping is enabled only on the workload thread, between two marker methods.
**No class exclusion filters are installed.** Parser code, dependencies,
JDK Java method bodies and generated lambda adapters all contribute to the
total. A JDK-namespace subtotal is also recorded and must be nonzero. Native
method bodies and GC have no Java bytecodes and are not measured.

Fixture construction, warmup, debugger code and reporting are outside the
measurement. A small fixed amount of Java call-site/marker overhead remains
inside the boundaries and is pinned with the workload bytecode. Debugger
breakpoints delimit the region; instruction events are delivered asynchronously
without suspending the target on every instruction.

The fixture contains five repeated elements and eight selectors. Each scenario
is checked twice in each of two fresh target JVMs. All counts and query results
must match exactly. There is no averaging or timing tolerance. The timeout is
only a hang watchdog, not a performance metric. The full test took about one
minute in the development environment.

During migration, JDI was checked against the previous JVMTI counter on the
same query invocations. Their counts differed by exactly three Java instructions
introduced by the validation wrapper for all three selectors and both repeats.
ByCounter was evaluated but not retained because its default exclusion of JDK
bodies fails this project's required coverage.

## Baseline ratchet

The committed baseline is
`src/test/resources/performance/instructions.properties`. The latest measurement
is written to `target/instruction-results.properties`.

- Any increased count fails, including with `-Dperformance.update=true`.
- Any decreased count fails with `UPDATE_REQUIRED` until explicitly accepted.
- Equal counts pass without rewriting the baseline.
- Accept improvements with `mvn test -Dperformance.update=true`, then commit
  the tighter baseline. Every scenario is checked before an atomic file write.

The JDK, VM options, workload/counter bytecode and library hashes are pinned.
A mismatch fails. For an explicitly reviewed metric migration, remove the old
baseline and run `mvn test -Dperformance.initialize=true`. Initialization refuses
to overwrite an existing baseline. Do not initialize to accept a slowdown.

The workload and measurement boundaries changed during the migration from the
standalone native counter. Old and new totals are not compared as a speed gain.
