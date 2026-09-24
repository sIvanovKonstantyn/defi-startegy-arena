# DeFi Arena design system

## Product personality

Calm financial infrastructure + competitive simulation. Precise, professional, data-oriented, trustworthy. Preserve the green primary brand; do not decorate with neon, glow, casino, or cyberpunk treatments.

## Design tokens (CSS)

Defined on `:root` in `ui/src/index.css`.

### Color (semantic)

| Token | Role |
| --- | --- |
| `--color-bg` | Page background |
| `--color-surface` | Panels / cards |
| `--color-surface-subtle` | Nested / muted surfaces |
| `--color-text` | Primary text |
| `--color-text-muted` | Secondary text |
| `--color-border` | Default borders |
| `--color-border-strong` | Emphasized borders |
| `--color-primary` | Primary actions / active |
| `--color-primary-hover` | Primary hover |
| `--color-primary-ink` | Text on filled primary |
| `--color-danger` | Destructive |
| `--color-danger-hover` | Destructive hover |
| `--color-success` | Positive / ready |
| `--color-warning` | Attention |
| `--color-info` | Informational |
| `--color-focus` | Focus ring |

### Spacing scale

`4 · 8 · 12 · 16 · 24 · 32 · 48 · 64` (CSS: `--space-1` … `--space-8`)

### Radius

| Token | Value | Use |
| --- | --- | --- |
| `--radius-sm` | 6px | small controls |
| `--radius-md` | 10px | inputs / buttons |
| `--radius-lg` | 14px | panels / cards |
| `--radius-pill` | 999px | badges |

### Typography

Family: **IBM Plex Sans** only.

| Role | Size / line / weight | Token |
| --- | --- | --- |
| H1 | 28 / 36 / 700 | `.text-h1` |
| H2 | 22 / 28 / 650 | `.text-h2` |
| H3 | 18 / 24 / 600 | `.text-h3` |
| Body | 16 / 24 / 400 | default |
| Small | 14 / 20 / 400 | `.text-small` |
| Caption | 12 / 16 / 500 | `.text-caption` |

## Layout

- Shell max-width: `1440px`, horizontal padding `--space-6` (32px), reduce on small screens.
- Prefer page → header → toolbar → sections → pagination over one giant card wrapping everything.

## Button hierarchy

| Level | Class | Use |
| --- | --- | --- |
| Primary | `.btn.btn-primary` | New strategy, Save, Sign in |
| Secondary | `.btn.btn-secondary` | Edit, Cancel, mode switches |
| Tertiary | `.btn.btn-tertiary` | Back, View details |
| Danger | `.btn.btn-danger` | Delete (after confirmation) |
| Icon | `.btn.btn-icon` | Close / chevrons only + `aria-label` |

## Primitives (`ui/src/ui/`)

`Button`, `IconButton`, `InputField`, `Badge`, `PageHeader`, `EmptyState`, `ConfirmDialog`

Extend this set rather than inlining bespoke controls on pages.

## Accessibility

Visible focus (`:focus-visible`), labeled controls, keyboard operable dialogs, color never the sole signal.
