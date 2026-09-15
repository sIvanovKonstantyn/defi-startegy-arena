# Code quality & architecture tooling (Java)

This document defines the static-analysis stack that agents and CI use to enforce code quality and architecture on the Java backend.

**Platform (locked):** Gradle, Java 25, package-based modulith (runtime/framework TBD — see [system-architecture.md](./system-architecture.md)).

Stack overview:

| Concern | Tool | Why |
| --- | --- | --- |
| Architecture (layers, packages, dependency direction) | **ArchUnit** | Executable architecture tests; no framework coupling |
| Strict code rules (complexity, size, comments, literals, …) | **PMD** (`config/pmd/main-ruleset.xml`) | Built-in metrics + custom XPath rules |
| Test coding rules | **PMD** (`config/pmd/test-ruleset.xml`) | Literals relaxed; structure still strict |
| Code coverage | **JaCoCo** at **100%** line / branch / instruction | Also the gate for **untested / unused** public code (uncovered lines fail the build) |
| Copy-paste duplicates | **PMD CPD** (`cpdCheck`, min 40 tokens) | Fails the build on duplicated blocks in `src/main/java` |
| Unsafe / security-relevant code | **SpotBugs** + **FindSecBugs**, **PMD security** | Bytecode + source security patterns; fails the build |
| Vulnerable dependencies | **CycloneDX SBOM** + **OSV-Scanner** (OSV/GHSA) | Fast classpath/SBOM scan via `dependencyVulnCheck`; fails the build |
| Pre-commit | `./gradlew qualityCheck` via git hook | No commit without green gates |
| Agent enforcement | `.cursor/rules/quality-gates.mdc` | Agents must read failures and fix until green |

**Lombok is forbidden** (Gradle exclude + ArchUnit + PMD). Prefer **Java records** for immutable data carriers. Do not introduce anemic getter/setter beans; if rare mutable state is required, model behavior explicitly (not Lombok, not bean boilerplate).

Frontend (React) tooling is out of scope here and will be documented separately.

### Locked PMD thresholds (main)

| Rule | Threshold / policy |
| --- | --- |
| Cyclomatic complexity | method ≤ 5, class ≤ 40 — see plain-language notes below |
| Cognitive complexity | ≤ 8 — see plain-language notes below |
| NCSS | method ≤ 20, class ≤ 150 — see plain-language notes below |
| Parameters | **at most one**, and it must be a **DTO** (not primitive/`String`); use records as request/command objects |
| Methods / fields per type | ≤ 12 / ≤ 8 |
| Comments | **none** (line, block, Javadoc) |
| Magic numbers | **always banned** — every number in executable code is a named constant (no `-1`/`0`/`1` free pass) |
| String literals | non-empty strings must be named fields (annotations exempt) |
| Unused code | PMD: locals, private fields/methods, assignments, formal parameters; **JaCoCo 100%** fails on untested public code |
| Duplicates | PMD `AvoidDuplicateLiterals` + **CPD** (`minimumTokenCount = 40`) on main sources |
| Security (unsafe code) | **SpotBugs** (effort max, report level low) + **FindSecBugs**; PMD `HardCodedCryptoKey`, `InsecureCryptoIv` |
| Vulnerable dependencies | CycloneDX BOM (`runtime`/`compile`/`testRuntime`) scanned by **OSV-Scanner** v2.6.x |
| Lombok | banned |
| Getter/setter carriers | ≥ 4 get/set/is methods → prefer records |

#### What the size/complexity limits mean (plain language)

**Cyclomatic complexity ≤ 5 (method)**  
Counts how many independent paths you can take through a method (`if`, `else`, loops, `&&`, `||`, `?:`, `catch`, etc.). At most ~4 branches beyond the straight-line path. If you need more, extract smaller methods or replace nested conditionals with clearer structure (strategy/rules, early returns into separate units, etc.).

**Cognitive complexity ≤ 8**  
Similar idea, but weighted for how hard the code is for a human to read: nesting hurts more than a flat list of checks. A method can sometimes pass cyclomatic and still fail cognitive if it is deeply nested. Keep nesting shallow.

**NCSS ≤ 20 (method) / ≤ 150 (class)**  
Non-Commenting Source Statements — roughly “how many real statements,” not raw lines and not comments (we ban comments anyway). A method longer than ~20 statements is doing too much; a class over ~150 is becoming a grab-bag — split by responsibility.

**One DTO parameter**  
Do not write `foo(String a, int b, boolean c)`. Write `foo(SomeCommand dto)` (usually a record). Zero-arg methods are fine. Record/canonical constructors may still list multiple components; the one-DTO rule targets ordinary methods.

Test ruleset: higher complexity caps; literals allowed for fixtures; comments still banned; `Thread.sleep` banned.

Changing these values is an explicit ruleset PR.

---

## ArchUnit (architecture)

**Decision: we use ArchUnit.**

ArchUnit encodes architectural constraints as unit tests (typically under `src/test/java/.../architecture`). Examples of rules we expect to grow over time:

- Layer / package dependency rules (e.g. `domain` must not depend on `web` or `infrastructure`)
- Bounded-context package isolation (see [system-architecture.md](./system-architecture.md))
- Naming and placement conventions (controllers only in `..adapter.web..`, repositories only in `..adapter.persistence..`)
- Forbidden dependencies (no frameworks in domain, no JDBC outside adapters, etc.)
- **HTTP infra isolation:** Jetty/Helidon only in `shared.infra..`; `HttpServerBootstrap` implementations in `shared.infra..`; hexagonal layers must not depend on `shared.infra`; composition root selects bootstrap, never server libraries directly (see [http-server-bootstrap flow](./flows/http-server-bootstrap.md))

ArchUnit complements PMD: PMD judges *local* code shape; ArchUnit judges *global* structure.

Convention:

- Keep architecture tests in a dedicated package, e.g. `…architecture`
- Prefer one focused test class per concern (`ArchitectureRulesTest`, …)
- Fail the build on any ArchUnit violation (same bar as unit tests)

---

## Delivery: context e2e first (TDD)

Aligned with [system-architecture.md §9](./system-architecture.md):

1. New context / slice starts with **context-level e2e scenarios**
2. User **approves** scenarios before implementation
3. Approved e2e tests are written and then **frozen** (no edits to green the build)
4. Production code is driven to satisfy those e2e tests
5. **Unit tests** cover edge cases only; they do not replace the e2e contract

Agents must not weaken, skip, or rewrite approved context e2e tests without an explicit user-approved scenario change.

---

## Recommended strict linter: PMD

**Recommendation: use PMD as the primary strict Java linter / rule engine.**

### Why PMD (not Checkstyle-only, not Sonar-only)

| Requirement | PMD fit |
| --- | --- |
| Code complexity | Built-in `CyclomaticComplexity`, `CognitiveComplexity`, `NPathComplexity` |
| Method / class size | `NcssCount`, `ExcessiveParameterList`, `TooManyMethods`, `TooManyFields`, related design rules |
| No comments in code | No stock “ban all comments” rule → add a **custom XPath rule** (see below) |
| No magic numbers / hardcoded literals | `AvoidLiteralsInIfCondition`, `AvoidDuplicateLiterals`, plus custom XPath for string literals where needed |
| Easy to extend | **XPath 3.1 rules live in the ruleset XML** — no Java plugin for most new rules; escalate to a Java `AbstractJavaRule` only when XPath is insufficient |

**Checkstyle** is strong for token-level style and has a solid `MagicNumber` check, but custom checks usually mean writing Java. Prefer Checkstyle only if we later want formatting/import order as a separate concern — not as the main extensible rules engine.

**SonarQube / SonarLint** can aggregate quality gates later; it should not replace a versioned, repo-owned PMD ruleset that agents can edit and review in PRs.

**SpotBugs** is useful for bug patterns (NPEs, resource leaks); optional later, orthogonal to our “strict shape” rules.

### Baseline rule categories (initial profile)

Tighten thresholds early; loosen only with an explicit ADR.

1. **Complexity** — low cyclomatic / cognitive caps per method; fail on God classes.
2. **Size** — max NCSS (or lines) per method and class; max parameters; max methods/fields per type.
3. **Comments** — forbid `//` and `/* … */` in production sources (see custom rule). Javadoc may be banned or limited to public API only — decide in the first ruleset PR.
4. **Literals** — forbid magic numbers and ad-hoc string literals in business logic; allow narrow exceptions (e.g. `0`, `1`, `−1`, empty string, test sources, generated code).
5. **Design / clarity** — unused code, empty catch, overly broad exceptions, etc., from PMD’s `design` / `errorprone` / `bestpractices` categories as we harden the profile.

### Extending rules (agent-friendly workflow)

1. Prefer a new **XPath rule** in `config/pmd/main-ruleset.xml` or `config/pmd/test-ruleset.xml`.
2. Document intent in the rule `<description>` (agents and humans read this).
3. Add a tiny **fixture** under `config/pmd/fixtures/` (bad snippet that must fail, good snippet that must pass) when the rule is non-obvious.
4. Only if XPath cannot express the check: add a small Java custom rule module and reference it from the ruleset.

Example sketch — ban block/line comments in `src/main/java`:

```xml
<rule name="NoCommentsInCode"
      language="java"
      message="Comments are forbidden; rename identifiers and extract methods instead."
      class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
  <description>
    Production code must not contain line or block comments.
  </description>
  <priority>1</priority>
  <properties>
    <property name="xpath">
      <value><![CDATA[
        //Comment
      ]]></value>
    </property>
  </properties>
</rule>
```

(Exact node names / exclusions for license headers or generated sources will be fixed when the Gradle/Maven PMD task is wired.)

### Where configuration lives

Planned layout (to be created when the Java module is scaffolded):

```text
config/
  pmd/
    main-ruleset.xml     # strict rules for src/main/java
    test-ruleset.xml     # defined coding rules for src/test/java
    fixtures/            # optional bad/good snippets for new rules
  jacoco/
    # thresholds documented in build file and/or here
  archunit/              # optional shared constants / package names
```

Build integration (**Gradle**, Java 25):

- `./gradlew check` runs unit tests **including ArchUnit**, context **e2e** tests, **PMD** (main + test rulesets), and **JaCoCo** coverage verification
- PMD / JaCoCo violations fail the build (`ignoreFailures = false` / minimum coverage gates)
- Do **not** run `main-ruleset.xml` unchanged on tests — use `test-ruleset.xml` instead

---

## Code coverage: JaCoCo (extra tool)

**ArchUnit and PMD do not check coverage.** Coverage needs a dedicated instrumenting tool.

**Decision: use JaCoCo.**

- Collects line, branch, and instruction coverage from the test run
- **Minimum covered ratio is 1.0 (100%)** for the counted class set
- Exclusions (only): `package-info`
- Prefer **records** so component accessors are exercised by real domain/application tests — do not write getter/setter-only types to chase coverage
- Reports under `build/reports/jacoco/`

Optional later (not required for v1): **PIT mutation testing** — measures whether tests actually catch faults; complementary to JaCoCo’s “lines executed” metric.

---

## Coding rules for tests

**Yes — define a separate PMD ruleset for tests**, plus ArchUnit where structure matters.

Production and test code share the same *kind* of tooling, but not the same thresholds or bans.

| Area | Production (`main-ruleset.xml`) | Tests (`test-ruleset.xml`) |
| --- | --- | --- |
| Literals / magic numbers | Strict | Usually relaxed (fixture data is expected) |
| Comments | Forbidden (or nearly) | Same policy unless we explicitly allow arrange/act/assert notes — prefer clear names over comments |
| Complexity / method size | Strict | Still capped, but slightly higher limits are OK for table-driven setups |
| Test-specific rules | N/A | Enforce naming (`*Test`, `*IT`), forbid Thread.sleep, limit Mockito `any()`, require assertions, no ignored tests without reason, etc. |
| Architecture | Domain/app layering | Tests may depend on production; production must never depend on test; optional “test slice” rules via ArchUnit |

How to extend test rules (same workflow as main):

1. Add XPath (or Java) rules to `config/pmd/test-ruleset.xml`
2. Wire the build so `src/test/java` uses only that ruleset
3. Keep agent-facing descriptions on each rule

Examples of test conventions we can encode early:

- Context e2e tests live under `…/<context>/e2e/` and are treated as a frozen contract after user approval
- Unit tests live under `…/<context>/unit/` for edge cases
- Test class naming and package placement mirroring production packages
- Every test method must contain an assertion (or use a framework that implies one)
- No `System.out` / print debugging left in tests
- No empty or `@Disabled` tests without a linked reason (custom rule or convention)
- Do not rename/change approved e2e scenarios to match a buggy implementation

---

## Agent responsibilities

Quality/architecture agents (CI jobs or Cursor agents) should:

1. Treat `config/pmd/main-ruleset.xml`, `config/pmd/test-ruleset.xml`, ArchUnit tests, JaCoCo thresholds, and **approved context e2e tests** as the contract — do not “fix” violations by weakening rules or rewriting e2e without an explicit change request.
2. Prefer fixing code (smaller methods, named constants, clearer structure, better tests) over suppressions.
3. If a suppression is unavoidable, require a one-line justification next to `@SuppressWarnings` / PMD suppression, and keep the scope minimal.
4. When adding a new convention, encode it as a PMD XPath rule or an ArchUnit test in the same change set as the docs update.
5. Treat coverage gaps as missing or weak tests — do not exclude production packages from JaCoCo to green the build without an explicit decision.
6. For new context work: scenarios → user approval → frozen e2e → implementation → unit tests for edges (see system architecture §9).

---

## Non-goals (for now)

- Replacing type-checking or compilation with lint rules
- Full SonarQube server as a hard dependency for local/CI green builds
- Mutation testing (PIT) as a merge blocker
- Gradle multi-module splits (package-based modulith first)
- Choosing a runtime/DI/web framework (architecture stays agnostic until then)
- Enforcing React/TS rules in this document

---

## Pre-commit hook

```bash
./gradlew installGitHooks
# or: ./scripts/install-git-hooks.sh
```

Installs `.git/hooks/pre-commit` which runs `./gradlew qualityCheck`. Commits are blocked until PMD, ArchUnit tests, and JaCoCo 100% all pass. Do not use `--no-verify`.

---

## Next steps

1. Keep extending ArchUnit as contexts gain `domain` / `application` / `adapter` code.
2. Add context e2e scenarios (user-approved, frozen) before feature implementation.
3. Document React/TS quality tooling when the web app is created.
