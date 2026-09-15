# Git workflow

## Branches

- Day-to-day work: **`review/<feature_name>`**
  - Example: `review/arena_submit_flow`
  - Allowed characters in `<feature_name>`: `a-z`, `0-9`, `_`, `-`
- **`main` / `master`**: only the **initial commit** may be created/pushed there. After that, commits and pushes to `main` are blocked by hooks.

## Commit messages

```text
CONTEXT | imperative summary
```

Allowed `CONTEXT` values:

`IDENTITY` · `STRATEGY` · `MARKETDATA` · `ARENA` · `LEADERBOARD` · `SHARED` · `DOCS` · `BUILD` · `ARCH`

Examples:

```text
ARENA | add backtest job port
DOCS | document submit sequence
BUILD | wire osv scanner into qualityCheck
```

## Hooks

Install (or reinstall) with:

```bash
./gradlew installGitHooks
# or: ./scripts/install-git-hooks.sh
```

| Hook | Enforces |
| --- | --- |
| `pre-commit` | Branch naming + `./gradlew qualityCheck` |
| `commit-msg` | `CONTEXT \| message` |
| `pre-push` | No push to `main`/`master` after the initial commit |

Do not bypass with `--no-verify`.

## Flow docs

When building a flow, document it under `docs/flows/` (Mermaid sequence + text). See the agent rule `flow-documentation`.
