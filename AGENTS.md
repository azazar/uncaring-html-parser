# Repository rules

## Scope and simplicity

- KISS first, across production code, tests, CI, and documentation. Make only
  the smallest changes needed for the requested task.
- Preserve readable formatting and the surrounding style. A smaller diff is
  not a reason to compress code or reformat unrelated code.
- Prefer existing JDK facilities and established libraries over custom
  infrastructure. Check available solutions before building a replacement;
  keep dependencies minimal.
- Do not create unsolicited documents, reports, or README sections or links.
  Update README when public API/ABI or documented usage changes.
- Preserve public API/ABI and Java 11 library compatibility unless the task
  explicitly requires changing them. Tests may require a newer JDK.

## Performance and tests

- Parser speed is a primary requirement. Establish performance baselines and
  add relevant correctness tests before changing parser behavior.
- Keep checks in Java/JUnit, run by `mvn test`. Do not add Python, C, custom
  native agents, or separate benchmark scripts or frameworks for these checks.
- Use the JDK's JDI instruction stepping with `-Xint`. Include parser,
  dependency, and JDK Java code; do not exclude JDK classes from the metric.
  Java bytecodes do not measure native method bodies; state the scope accurately.
- Use Epsilon GC for the measurement JVM. Check allocated heap bytes separately
  from instructions, including allocations made by dependencies and JDK code.
  Keep allocation accounting outside instruction stepping.
- Do not use CPU/wall-clock time as a regression metric or combine instructions
  and bytes with an arbitrary coefficient. Timeouts are only hang watchdogs.
- Keep expected counts as `static final int` constants in Java tests, checked
  with JUnit assertions. No external baseline files or automatic baseline writers.
- An increase in either metric must fail. A decrease must also fail until the
  corresponding constant is explicitly lowered. Never raise a baseline just
  to make a regression pass or let one scenario's gain hide another's regression.
- Require repeatable measurements across repetitions and fresh JVMs, using the
  pinned measurement environment. Fail on environment mismatches rather than
  silently skipping the check.
- Measure proposed optimizations and their combined effect before publishing.
  Run the full tests after updating constants. Distinguish instruction reductions
  from elapsed-time speedups; report which scenarios were actually measured.

## Git and CI

- Publish commits and merges only to `dev`. Do not push or merge into `master`
  unless the user explicitly changes this restriction.
- Keep CI minimal. Verify and use the latest stable action releases when adding
  or updating actions. Use the JDK version required by the tests, not an unpinned
  latest JDK that invalidates the baselines.
