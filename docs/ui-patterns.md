# DeFi Arena UI patterns

## Authentication

Minimal focused form. Explicit mode switch (`Sign in` / `Create account`). Labeled fields. Primary submit with text label — never icon-only submit.

## Strategies list

Page question: *What strategies do I have?*  
Primary action: **New strategy**.

- Page header + toolbar
- Table for comparison (current prototype); cards when browsing becomes the primary task
- Empty state explaining next step; primary CTA stays on the page header
- Row actions: labeled **Edit**; **Delete** behind confirmation
- Pagination with labeled prev/next (icons allowed with labels/aria)

## Strategy form (create / edit)

Side panel or drawer. Clear title. Labeled inputs. Primary **Save** / **Create**. Icon-only **Close** is OK.

## Errors

User-facing modal with title, plain-language description (no HTTP codes), dismiss action. See `ui/src/errors/`.

## Loading

Disable submitting controls; avoid blank screens. Prefer skeletons for longer waits (future).

## Anti-patterns

Random spacing/colors/radii · ambiguous icon-only CRUD · giant page cards · color-only status · placeholder-only labels · delete without confirmation · page-specific duplicates of shared primitives.
