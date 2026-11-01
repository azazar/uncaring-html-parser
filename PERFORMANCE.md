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
connector. The target uses `-Xint`, a fixed heap, Epsilon GC and one reported
processor. The JDK's bundled JDWP debugger supplies `STEP_MIN` / `STEP_INTO`
events. We do not implement instrumentation or ship a native library.
Epsilon never reclaims memory; the heap must fit the entire short-lived run.

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

Allocated heap bytes are measured separately around another invocation of the
same query, using `ThreadMXBean.getCurrentThreadAllocatedBytes()`. This includes
allocations by dependencies and JDK code on the workload thread. Accounting is
outside instruction stepping. The API specifies approximate values; the test
requires exact agreement across repeats and fresh JVMs on the pinned JDK.
Instructions and allocated bytes have independent budgets, with no weighted sum.

During migration, JDI was checked against the previous JVMTI counter on the
same query invocations. Their counts differed by exactly three Java instructions
introduced by the validation wrapper for all three selectors and both repeats.
ByCounter was evaluated but not retained because its default exclusion of JDK
bodies fails this project's required coverage.

## Baseline ratchet

The baselines are `static final int` constants in `ParserInstructionTest`, one
instruction count and one allocated-byte count per selector.
JUnit `assertEquals` checks every scenario, and `assertAll` reports all mismatches.
No baseline or report files are read or written.

- Any increased count fails with `REGRESSION`.
- Any decreased count fails with `UPDATE_REQUIRED` until explicitly accepted.
- Equal counts pass.
- Accept improvements by lowering the corresponding Java constants to the
  measured values reported by JUnit, then rerun `mvn test` and commit.

The JDK, VM options, workload/counter bytecode and library hashes are pinned.
A mismatch fails an assertion rather than silently disabling the test.
Changes to these conditions require an explicit review of the metric and its
Java assertions. Do not raise the constants to accept a slowdown.

The workload and measurement boundaries changed during the migration from the
standalone native counter. Old and new totals are not compared as a speed gain.
