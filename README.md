# DeFi Strategy Arena

Backend: **Gradle / Java 25**, package-based modulith with **Jetty 12** HTTP and **Postgres + jOOQ + Flyway** persistence.  
UI: **Vite + React** in `ui/`, served via Compose nginx on port **3000**.  
Architecture: [`docs/system-architecture.md`](docs/system-architecture.md).  
Quality tooling: [`docs/code-quality-and-architecture.md`](docs/code-quality-and-architecture.md), [`docs/frontend-quality.md`](docs/frontend-quality.md).

## Prerequisites

- JDK **25** (Gradle toolchain can auto-provision via Foojay)
- Node **22+** (for `ui/` qualityCheck)
- Git
- **Docker** (optional; for `docker compose up` with real Postgres — qualityCheck ITs use H2 PostgreSQL mode)

## Commands

```bash
./gradlew qualityCheck          # PMD + CPD + SpotBugs + OSV + ArchUnit + JaCoCo (H2 PG-mode ITs; no Testcontainers)
cd ui && npm run qualityCheck   # Biome + tsc + Vitest + Playwright + npm audit
./gradlew installGitHooks       # pre-commit, commit-msg, pre-push
docker compose up --build       # db + app + ui (open http://localhost:3000)
./scripts/loadtest/run-capacity.sh  # combined CRUD RPS capacity → docs/load-tests/*.md
```

Config: [`src/main/resources/app.properties`](src/main/resources/app.properties) defaults; override with `DSA_*` env vars (see [phase-6](docs/phases/phase-6-postgres-persistence.md)).  
DB ITs: H2 `MODE=PostgreSQL` only — see `.cursor/rules/integration-test-db.mdc`.  
Load test: [`docs/load-tests/README.md`](docs/load-tests/README.md) (`./scripts/loadtest/run-capacity.sh`).

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
| JaCoCo 100% (also blocks untested/unused public code) | `build.gradle` (excludes `package-info`, generated jOOQ) |
| Pre-commit | `scripts/git-hooks/pre-commit` |
| Agent rule | `.cursor/rules/quality-gates.mdc` |

**Lombok is forbidden.** Prefer **records** for immutable data. Cross-context communication is **async only** via `shared.messaging`.
