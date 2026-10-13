# Mimir workspace

Mimir helps retro handheld and desktop users safely prepare ROM libraries.
The interface prioritises reviewing filenames and consequences before applying
changes. It retains Mimir's violet identity and uses native Material 3 controls.

## Layout

Below 840 logical pixels, tool navigation uses a drawer and each tool presents a
vertical task flow. Wider layouts use a navigation rail, expanded with labels at
1200 pixels. Reviews show source and output columns from 1000 pixels and selectable
rows on compact screens. Theme switching applies to the entire application.

Folder selection precedes options and scanning. Scanning produces a selectable
preview; applying requires confirmation. Batch progress remains visible while
users navigate among tools. Immediate stop and stop after the current item are
distinct controls. Results distinguish completed, skipped, failed, and cancelled.

## Accessibility and state

Use readable body text, standard focus handling, 48-pixel action targets, and
wrapping controls. Support portrait, landscape handhelds, desktop windows, and
system text scaling. Empty states explain the next action; errors provide the
filesystem or backend failure instead of presenting success.

## Validation status

Widget scenarios cover phone, landscape handheld, desktop, and enlarged text.
They still need a successful local test run and native visual inspection. The
current implementation has not passed a finish review. New workspace copy uses
English fallback while existing translated tool labels use shared Android XML.
