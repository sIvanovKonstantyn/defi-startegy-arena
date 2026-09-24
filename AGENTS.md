# DeFi Arena — agent guidance

## UI/UX (mandatory for `ui/`)

Follow the product design system — not one-off screen redesigns.

| Doc | Role |
| --- | --- |
| [`docs/design-system.md`](docs/design-system.md) | Tokens, typography, color, radius, primitives |
| [`docs/ui-patterns.md`](docs/ui-patterns.md) | Buttons, forms, tables, dialogs, empty/error/loading |
| [`docs/information-architecture.md`](docs/information-architecture.md) | Navigation, page questions, Strategy as core object |
| [`.cursor/rules/ui-ux-design-system.mdc`](.cursor/rules/ui-ux-design-system.mdc) | Cursor rule enforcing the above under `ui/**` |

**Workflow:** system → primitives (`ui/src/ui/`) → application shell → migrate screens → new features.

Personality: calm financial infrastructure + competitive simulation. Preserve green brand for primary/success; avoid crypto-casino, cyberpunk, and generic admin chrome.

## Quality gates

- Backend / Java: `./gradlew qualityCheck`
- Frontend (`ui/`): `cd ui && npm run qualityCheck`

See `docs/code-quality-and-architecture.md` and `docs/frontend-quality.md`.
