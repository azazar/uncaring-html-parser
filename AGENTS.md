# Development principles

- KISS first, across code, tests, CI, and documentation. Make the smallest
  correct change that satisfies the requested task; avoid unrelated cleanup.
- Preserve readable formatting and the surrounding style. Minimal changes
  do not mean compressed code or unnecessary reformatting.
- Prefer platform facilities and established libraries over custom
  infrastructure. Check existing solutions first and keep dependencies minimal.
- Use the project's language, toolchain, and test framework. Avoid introducing
  extra languages, scripts, or frameworks for work the existing tools can do.
- Do not create unsolicited documents, reports, or README sections or links.
  Keep documentation edits within the requested scope.
- Establish relevant regression checks before changing behavior. Test observable
  requirements rather than duplicating implementation details.
- Use reproducible measurements that cover the work relevant to the claim,
  including dependencies. State exclusions and distinguish proxies from actual
  outcomes.
- Measure optimizations individually and together. Do not let an improvement
  in one metric or scenario conceal a regression in another.
- Do not weaken or silently skip checks to make changes pass. Tighten regression
  expectations when accepting improvements.
- Run checks appropriate to the change and report verified results and material
  limitations without claiming unperformed validation.
- Respect the user's branch and publication constraints when committing,
  pushing, or merging.
- Keep CI minimal. When adding or updating tooling, verify and use the latest
  compatible stable releases while preserving reproducible test environments.
