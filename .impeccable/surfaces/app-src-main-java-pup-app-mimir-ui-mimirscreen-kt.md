---
version: 1
slug: "app-src-main-java-pup-app-mimir-ui-mimirscreen-kt"
primary_target: "app/src/main/java/pup/app/mimir/ui/MimirScreen.kt"
related_targets: ["app/src/main/java/pup/app/mimir/ui/HomeScreen.kt"]
---

# Surface brief: Mimir app shell (all screens)

Scope: every screen of the Android app (home tool picker, nine tool pages, queue/preview, progress, dialogs). Mode: Operate. Device: AYN Thor, 1920×1080 landscape, controller-first, touch secondary. Must hold 11 locales (long German/French strings) and system font scale.

Audience and job: retro handheld owners preparing ROM libraries on the couch; pick a tool, point it at a folder, review every planned change, apply. Constraints: preview-and-confirm safety, controller focus always obvious, Back always works, no data leaves the device.

Chosen direction: Memory Card Manager (user's pick). Memorable moment: the focused tool's save-block icon lifts and slowly turns, exactly like a save file on a console memory card, while its info panel updates beside it.

## Direction contract

THESIS: Mimir is the console's own system data browser: your tools and planned file changes shown as save blocks on a memory card, every write confirmed with the console's calm ceremony. It refuses the stacked-Material-cards-in-one-scrolling-column app.

OWN-WORLD: Night-blue field (#05070D to #0B1024) with faint slot gridlines; panels are solid deep navy (#121A33) with a 1dp cold edge light (#7FA7FF at 35%); ice-white text (#E8EEFF), muted steel secondary (#93A0C4). Each tool owns one ink (orange, cobalt, mint, magenta, gold, cyan, violet, lime, green) used for its block, its page accent and its focus. Amber #FFB347 is reserved for destructive confirmation. Rubik throughout; save blocks are drawn objects (discs, cartridges, cards), never icon-font tiles. Drawn A/B/X/Y glyphs in the legend bar.

STORY: The visitor sees all tools at once as two memory-card slots, understands each from its drawn object and info panel, opens one, picks a folder, watches the data pane fill with the files Mimir found, and confirms knowing exactly what will be written.

FIRST VIEWPORT: Home at 1920×1080: header strip (logo, "Mimir", free storage readout, theme toggle) top; left 62%: Slot 1 Utilities and Slot 2 Space savers as rows of large save blocks on a visible 5-cell grid, empty cells drawn faint; right 38%: info panel for the focused tool at scale (tool name huge, what it reads → writes, formats, enter prompt). Bottom: button legend bar (A Open, B Back, Y Theme). Tool pages: left setup pane (folder slot, options), right data pane (planned changes as save rows with size bars), legend bar mapping X Scan, Y Apply.

FORM: Memory Card Manager, my top-ranked grounded candidate (position 1 of 7), presented as IMPECCABLE’S PICK and chosen by the user; seed key 9b76e5f4. Raises kept from the round: visible slot grid with empty cells; one ink per tool, focused block takes its ink and settles with a small bounce; tight type scale with rank by weight and reversal; scale contrast in the info panel.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
