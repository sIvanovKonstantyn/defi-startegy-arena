# DeFi Strategy Arena

Backend: **Gradle / Java 25**, package-based modulith (runtime/framework **TBD**).  
Architecture: [`docs/system-architecture.md`](docs/system-architecture.md).  
Quality tooling: [`docs/code-quality-and-architecture.md`](docs/code-quality-and-architecture.md).

## Prerequisites

- JDK **25** (Gradle toolchain can auto-provision via Foojay)
- Git

## Commands

```bash
./gradlew qualityCheck          # PMD + CPD + SpotBugs + OSV + ArchUnit + JaCoCo
./gradlew installGitHooks       # pre-commit, commit-msg, pre-push
```

Git conventions: [`docs/git-workflow.md`](docs/git-workflow.md) (`review/<feature>`, `CONTEXT | message`, no push to `main` after initial commit).  
Flow docs: [`docs/flows/`](docs/flows/).

## Quality gates

| Gate | Enforced by |
| --- | --- |
| ArchUnit | `src/test/java/.../architecture` |
| Strict PMD (main + test), including unused locals/privates/params | `config/pmd/*.xml` |
| CPD (duplicate code, ≥ 40 tokens) | `./gradlew cpdCheck` |
| SpotBugs + FindSecBugs | `./gradlew spotbugsMain` |
| PMD security (`HardCodedCryptoKey`, `InsecureCryptoIv`) | `config/pmd/main-ruleset.xml` |
| OSV-Scanner (dependency CVEs via CycloneDX SBOM) | `./gradlew dependencyVulnCheck` |
| JaCoCo 100% (also blocks untested/unused public code) | `build.gradle` (excludes `package-info`) |
| Pre-commit | `scripts/git-hooks/pre-commit` |
| Agent rule | `.cursor/rules/quality-gates.mdc` |

**Lombok is forbidden.** Prefer **records** for immutable data. Cross-context communication is **async only** via `shared.messaging`.
