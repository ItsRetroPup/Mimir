---
name: Mimir
description: A console memory-card manager for retro ROM libraries; tools are save blocks, planned changes are file entries, every write is confirmed.
colors:
  night-field-top: "#0B1024"
  night-field-bottom: "#05070D"
  night-grid: "#1B2650"
  night-panel: "#111934"
  night-panel-raised: "#18224A"
  night-edge-light: "#7FA7FF"
  night-text: "#E8EEFF"
  night-text-muted: "#9AA7CB"
  night-caution: "#FFB347"
  night-on-caution: "#2A1600"
  night-error: "#FF7A7A"
  day-field-top: "#E6ECF8"
  day-field-bottom: "#D3DBEC"
  day-grid: "#BCC8E2"
  day-panel: "#F7F9FD"
  day-panel-raised: "#FFFFFF"
  day-edge-light: "#3550C8"
  day-text: "#0C1430"
  day-text-muted: "#475478"
  day-caution: "#A85200"
  day-on-caution: "#FFFFFF"
  day-error: "#C62828"
  ink-multidisc-night: "#FF8A3D"
  ink-multidisc-day: "#C2520F"
  ink-vita-night: "#5B95FF"
  ink-vita-day: "#2558D0"
  ink-esde-night: "#34D399"
  ink-esde-day: "#087A53"
  ink-nsz-night: "#FF5C93"
  ink-nsz-day: "#C81E5E"
  ink-romzipper-night: "#FFC93C"
  ink-romzipper-day: "#9A6B00"
  ink-chdman-night: "#2DD4E6"
  ink-chdman-day: "#0B7F8E"
  ink-rvz-night: "#B197FC"
  ink-rvz-day: "#6A47D6"
  ink-zcci-night: "#B5E853"
  ink-zcci-day: "#55800B"
  ink-xiso-night: "#4ADE80"
  ink-xiso-day: "#13803B"
typography:
  display:
    fontFamily: "Rubik"
    fontSize: "32sp"
    fontWeight: 800
    lineHeight: "36sp"
    letterSpacing: "-0.015em"
  headline-large:
    fontFamily: "Rubik"
    fontSize: "28sp"
    fontWeight: 700
    lineHeight: "32sp"
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "Rubik"
    fontSize: "21sp"
    fontWeight: 700
    lineHeight: "26sp"
  title-large:
    fontFamily: "Rubik"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "24sp"
  title:
    fontFamily: "Rubik"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "21sp"
  title-small:
    fontFamily: "Rubik"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "18sp"
  body-large:
    fontFamily: "Rubik"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "23sp"
  body:
    fontFamily: "Rubik"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  body-small:
    fontFamily: "Rubik"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "17sp"
  label:
    fontFamily: "Rubik"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "0.01em"
  label-small:
    fontFamily: "Rubik"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "16sp"
    letterSpacing: "0.02em"
  figures:
    fontFamily: "Rubik"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "0.01em"
    fontFeature: "tnum"
rounded:
  segment: "2dp"
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  dialog: "24dp"
  pill: "9999dp"
spacing:
  hair: "2dp"
  xxs: "4dp"
  xs: "6dp"
  sm: "8dp"
  sm-plus: "10dp"
  md: "12dp"
  slot-gap: "14dp"
  lg: "16dp"
  info: "18dp"
  gutter: "20dp"
  xl: "24dp"
  field-grid: "48dp"
components:
  console-panel:
    backgroundColor: "{colors.night-panel}"
    textColor: "{colors.night-text}"
    rounded: "{rounded.md}"
    padding: "16dp"
  console-panel-day:
    backgroundColor: "{colors.day-panel}"
    textColor: "{colors.day-text}"
    rounded: "{rounded.md}"
    padding: "16dp"
  save-block:
    backgroundColor: "{colors.night-panel}"
    textColor: "{colors.night-text-muted}"
    typography: "{typography.label-small}"
    rounded: "{rounded.md}"
    padding: "6dp"
  save-block-focused:
    backgroundColor: "{colors.ink-xiso-night}"
    textColor: "{colors.night-text}"
    rounded: "{rounded.md}"
  button-ink:
    backgroundColor: "{colors.night-edge-light}"
    textColor: "{colors.night-field-bottom}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10dp 24dp"
  button-ink-day:
    backgroundColor: "{colors.day-edge-light}"
    textColor: "{colors.day-panel-raised}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10dp 24dp"
  button-caution:
    backgroundColor: "{colors.night-caution}"
    textColor: "{colors.night-on-caution}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10dp 24dp"
  button-caution-day:
    backgroundColor: "{colors.day-caution}"
    textColor: "{colors.day-on-caution}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10dp 24dp"
  button-outlined:
    textColor: "{colors.night-edge-light}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10dp 24dp"
  legend-bar:
    backgroundColor: "{colors.night-panel}"
    textColor: "{colors.night-text}"
    typography: "{typography.label}"
    height: "48dp"
    padding: "2dp 16dp"
  legend-action:
    textColor: "{colors.night-text}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    height: "48dp"
    padding: "0 12dp"
  button-glyph:
    backgroundColor: "{colors.night-text}"
    textColor: "{colors.night-field-bottom}"
    rounded: "{rounded.pill}"
    size: "22dp"
  data-row:
    backgroundColor: "{colors.night-panel}"
    textColor: "{colors.night-text}"
    typography: "{typography.title}"
    rounded: "{rounded.sm}"
    padding: "8dp 12dp"
    height: "56dp"
  choice-chip:
    backgroundColor: "{colors.night-panel-raised}"
    textColor: "{colors.night-text}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
  choice-chip-selected:
    backgroundColor: "{colors.night-edge-light}"
    textColor: "{colors.night-field-bottom}"
    rounded: "{rounded.pill}"
  format-badge:
    textColor: "{colors.night-edge-light}"
    typography: "{typography.label-small}"
    rounded: "{rounded.xs}"
    padding: "3dp 8dp"
  status-line:
    textColor: "{colors.night-text}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "10dp 12dp"
  block-progress:
    backgroundColor: "{colors.night-grid}"
    rounded: "{rounded.segment}"
    height: "12dp"
  size-bar:
    backgroundColor: "{colors.night-grid}"
    rounded: "{rounded.segment}"
    height: "5dp"
    width: "160dp"
---

# Design System: Mimir

## Overview

**Creative North Star: "The Memory Card Manager"**

Mimir is drawn as a console's own system data browser. The tools are save blocks sitting in two memory-card slots on a night-blue field ruled with faint slot gridlines; a tool page is the card's data view, with what to read on the left and the files Mimir found, as file entries with block-meter size bars, on the right. Writing anything is a ceremony the console would recognise: a plain dialog, a named count, an amber button only when the write cannot be taken back.

The world is controller-first on a 1920×1080 handheld (AYN Thor) and touch-second. Every screen carries a header strip on top and a face-button legend bar at the foot; the legend is not decoration but the literal map of what A, B, X and Y do here, and its entries are touch targets too. Density is moderate and landscape: two panes, no scrolling column of stacked cards. Each of the nine tools owns one ink and speaks in it everywhere on its page (block, header crumb, buttons, chips, checkboxes, focus ring), so the page tells you where you are without a label.

There are two lighting states of the same world, night (default, deep navy) and daylight (cool paper-blue), swapped by the Y button on home or the header toggle. Every token below comes in both.

**Key Characteristics:**
- Night-blue field with a 48dp slot grid drawn over a vertical fade; solid panels with a 1dp cold edge light.
- One ink per tool, with a night value and a darker daylight value.
- Tools are drawn objects (discs, cartridges, game cards, folders) rendered in their ink, never icon-font tiles.
- Drawn A/B/X/Y face-button glyphs: a filled disc with its letter knocked out.
- One turning object at a time: the focused save block rocks about its vertical axis.
- Rubik throughout; rank by weight and reversal more than by size jumps; tabular figures for every number.

## Colors

A cold, two-state console palette: navy and steel at rest, nine saturated inks for identity, amber held back for the one moment that matters.

### Primary
- **Cold Edge Light** (night-edge-light / day-edge-light): the system's own accent when no tool is open. It is Material's primary on home, the edge of every panel (28% alpha at rest, 55% for outlines), the About block's ink, and the selected-chip fill outside a tool.
- **Tool Inks** (ink-*-night / ink-*-day): one per tool. Multi-disc Organizer orange, Vita Shortcuts cobalt, ES-DE Systems mint, NSZ to NSP magenta, RomZipper gold, CHDMan cyan, Dolphin RVZ violet, Azahar ZCCI lime, Xbox XISO green. Opening a tool wraps its page in an ink theme that replaces Material's primary and secondary with the ink, so filled buttons, chips, checkboxes, switches, focus rings, the header crumb, the "Writes" value and the panel accent all take it. Night inks are bright on navy (5.9:1 or better on the panel); daylight inks are darkened so a white label on an ink fill stays at 4.6:1 or better.

### Tertiary
- **Caution Amber** (night-caution / day-caution, with its on-colour): reserved for writes that cannot be taken back: the confirm button of the apply/convert dialog, "Replace output", "Remove shortcut", "Stop now", the delete icon on a Vita shortcut, and the "existing output will be replaced" lines in queue rows. It is never an error colour and never a highlight.
- **Error** (night-error / day-error): failures and low-storage warnings, shown in the status line at 12% tint with an outlined error icon.

### Neutral
- **Night Field** (night-field-top to night-field-bottom): the vertical fade every screen sits on. The bottom value is also the dark label colour on night inks and the knocked-out letter in face-button glyphs.
- **Slot Grid** (night-grid / day-grid): the 48dp field gridlines (55% alpha night, 70% daylight), panel rules, empty-cell dashes, and the unlit segments of block meters.
- **Panel** (night-panel / day-panel): every solid surface: console panels, save-block cells, data rows, the legend bar (at 94%).
- **Raised Panel** (night-panel-raised / day-panel-raised): unselected choice chips and Material's high containers.
- **Ice Text / Steel Muted** (night-text, night-text-muted and their day pair): primary text, and secondary text such as labels, descriptions, sizes and the free-space readout (both 7:1 or better on panel).

### Named Rules
**The Amber Is A Warning Rule.** Amber appears only on a control that destroys or replaces data, or on the line warning that it will. Errors use the error role. If a screen shows amber and nothing on it can be lost, it is wrong.

**The One Ink Per Page Rule.** A tool page speaks only in its own ink, plus the cold edge light for the system chrome. Never mix two tools' inks on one page; home shows all nine only because it shows all nine blocks.

**The Paired Lighting Rule.** Every colour exists as a night and a daylight value. Never introduce a colour that only works in one.

## Typography

**Display Font:** Rubik (bundled variable font `res/font/rubik.ttf`, OFL; weights 400 to 800 via variation axis)
**Body Font:** Rubik
**Label/Mono Font:** Rubik with tabular figures (`tnum`) for numbers

**Character:** One soft-cornered geometric sans for everything, so hierarchy has to come from weight (400 body, 600 titles and labels, 700 headlines, 800 display) and from reversal (ink text, knocked-out glyph letters), not from a second family.

### Hierarchy
- **Display** (800, 32sp/36sp, -0.015em): the "Mimir" name in the About panel only.
- **Headline Large** (700, 28sp/32sp, -0.01em): the focused tool's name in the home info panel, the scale-contrast moment beside the small blocks.
- **Headline** (700, 21sp/26sp): data-pane headings (plan header, Vita database, ROM folders) and empty-pane titles.
- **Title Large** (600, 19sp/24sp): header strip name and tool crumb, slot titles ("Slot 1 • Utilities"), report title.
- **Title** (600, 16sp/21sp): row titles in data rows, the Reads/Writes values, the A enter prompt, progress labels.
- **Title Small** (600, 14sp/18sp): option labels and the selected folder name in the setup pane.
- **Body Large / Body / Body Small** (400; 16/23, 14/20, 12/17sp): home prompt; descriptions and status text; file paths, option details, target names and the amber/error inline lines.
- **Label** (600, 14sp/18sp, 0.01em): buttons, chips, legend labels, the "Reads/Writes" captions.
- **Label Small** (600, 12sp/16sp, 0.02em): save-block names under each cell, format badges, slot field captions.
- **Figures** (Label or Body Small with `tnum`): free storage, file sizes, counts, title IDs, queue positions.

### Named Rules
**The Sentence Case Rule.** Buttons, legend labels, chips and titles are sentence case ("Choose folder", "Scan folder", "Shrink Xbox games"). The only uppercase text is a file-extension badge (".ISO" shown as "ISO"), which is data, not styling.

**The Tabular Figures Rule.** Any number that can change or be compared (sizes, counts, free space, IDs, progress) is set with `tnum`.

**The No Kicker Rule.** No eyebrow or kicker text above headings. A section is introduced by its title alone; context comes from the slot, the ink and the header crumb.

## Layout

The screen is a fixed console frame: header strip (minimum 52dp, 12dp horizontal padding) on top, legend bar (minimum 48dp, 16dp horizontal padding) at the foot, content between on the gridded field.

- **Home, wide (600dp and up):** two columns with a 20dp gutter and 20dp/16dp outer padding. Left 60%: two memory-card slots, each a title plus a row of five square cells with 10dp gaps; slots sit 14dp apart and size their cells from the available height so both fit without scrolling. Unused cells are drawn as dashed empty frames (1.5dp, 10/8 dash, grid colour). Right 40%: the info panel for the focused block, filling the height, with the A prompt pinned to its foot.
- **Tool page, wide (700dp and up):** setup pane 42% and data pane 58%, 20dp gutter, each independently scrollable with 12dp vertical padding; setup stacks at 12dp, data at 10dp, data rows at 6dp.
- **Narrow:** both stack into one scrolling column with 16dp padding.
- **Rhythm:** a near-linear 2/4/6/8/10/12/16/18/20/24dp scale; panels pad 16dp (info 18dp, About 20dp). The 48dp field grid is decorative and does not snap content.
- **Locale and scale headroom:** labels allow two lines (block names reserve two), legend labels ellipsize to one, and panes scroll rather than clip, so German/French strings and system font scale hold.

**The Two Pane Rule.** A tool page is always setup on the left, data on the right. Never collapse it into a single column of cards on a wide screen.

## Elevation & Depth

Flat and tonal. No surface casts a shadow. Depth comes from three things only: the field fading from top to bottom behind a grid; solid panels lifted off it by colour and a 1dp edge light (cold edge at 28% alpha, or the page ink at 55% when the panel belongs to a tool); and motion, where the focused element rises.

The drawn save-block objects carry their own internal depth (a darker side offset beneath the face, a light-to-shade gradient, a specular arc on discs). That is illustration, not UI elevation.

### Named Rules
**The Lift Not Shadow Rule.** Focus raises an element: scale to 1.06 with a small upward shift (3.6px), settling on a bouncy spring (damping 0.45, medium-low stiffness). Nothing ever gets a drop shadow to show state.

## Shapes

Soft, consistent rounding on a geometric world. Panels and save-block cells use the 16dp corner; data rows, status lines and change rows 12dp; format badges 8dp; dialogs 24dp. Every interactive pill (buttons, chips, legend actions, the default focus ring) is fully rounded, and face-button glyphs are circles. Block-meter segments are tiny 2dp-rounded bars with 3dp gaps. The logo tile rounds at 22% of its size and sits on a night tile in daylight, since the artwork is white.

Game-card objects share one silhouette: a rounded rectangle with the top-right corner cut.

## Components

### Buttons
- **Shape:** fully rounded pill.
- **Ink (filled):** the page ink with the dark field colour as label on night, white label in daylight. Used for the primary forward action on a pane (Choose folder, Install systems, Create shortcut).
- **Caution (filled):** caution amber with its on-colour; only for the irreversible confirmations listed under Colors.
- **Outlined:** ink text and a 60% ink outline; Cancel, Select all, Stop after current, per-row Add/Pick.
- **Focus:** a 3dp ink ring plus the focus lift. On night only, a filled button's fill also brightens 22% toward white; in daylight it does not, because the white label would lose contrast. Material's focus overlay is switched off on filled buttons because it darkens them and reads as disabled. Pressing still ripples.

### Chips
- **Style:** choice chips in a wrapping row, 8dp gaps, pill shape. Unselected: raised panel and text colour. Selected: ink fill with the on-ink label.
- **State:** a controller moving into the row lands on the current choice, not the nearest chip.

### Cards / Containers
- **Console panel:** panel colour, 16dp corners, 1dp edge light (cold edge at 28%, or ink at 55% when it belongs to a tool), 16dp padding, 12dp stack. Hosts the setup pane, info panel, About panel, and progress and report panels.
- **Panel rule:** a 1dp line in the grid colour separating groups inside a panel.
- **Shadow Strategy:** none (see Elevation & Depth).

### Inputs / Fields
- **Search field:** Material outlined text field in the page ink. Read-only until activated with A, Enter or a tap, so a controller can pass over it without summoning the keyboard; it returns to read-only when focus leaves, and Up/Down always move focus out of it.

### Navigation
- **Header strip:** Home button (tool pages only), Mimir mark (34dp), "Mimir", then a chevron and the tool name in its ink. At the right, an SD icon, a 12-segment block meter of used storage in steel, the free space in tabular figures, and the theme toggle. Icon buttons carry a circular focus ring.
- **Legend bar:** panel at 94% along the foot. Each action is a glyph plus a sentence-case label at least 48dp tall. Disabled actions drop to 38% alpha; the screen's primary action is emphasised with an ink tint at 18%, a 60% ink outline and ink-coloured glyph and label.
- **Controller model:** A selects or opens; B goes back (home); X scans (or downloads on ES-DE); Y applies or converts with the selected count, installs on ES-DE, and toggles the theme on home. The first directional press on a screen lands in content, not the header. Returning home refocuses the block you opened. Touch input never shows focus.

### Save Block (signature)
A square cell on the slot grid holding the tool's drawn object, with its name beneath in two reserved lines. At rest: panel fill, 1dp edge at 22%, steel name. Focused: ink fill at 16%, a 3dp ink border, the name brightens to full text, the cell lifts, and the object turns: it rocks between -18° and 18° about its vertical axis on an 1800ms ease-in-out reversing loop. The last block opened keeps a 60% ink border as "selected". The info panel beside it mirrors the focused block: the object at 52dp, the name in Headline Large, a description, Reads and Writes rows (Writes in ink), and the A prompt in ink.

### Block Meter (signature)
Progress is drawn as a row of segments, never a smooth Material bar: 24 segments at 12dp for progress, 16 segments at 5dp and 160dp wide as a per-row size bar (relative to the largest item), 12 segments at 8dp in the header for storage. Unlit segments use the grid colour; lit segments use the ink, partially lit at the leading edge. When the amount of work is unknown, a group of lit segments sweeps back and forth (1400ms linear).

### Data Row
A file-list entry: panel fill (ink at 10% when selected), 12dp corners, minimum 56dp tall, 12dp/8dp padding. A checkbox, the title, a size bar, the target name in steel, and an amber line if an output will be replaced; sizes sit at the right in tabular figures. Change rows add an ink format badge (ink text on 14% ink, 8dp corners) and a sources → targets file list with an ink arrow.

### Empty Pane
Centred in the data pane: the tool's object drawn at 96dp in its ink at 55%, a headline and one line of plain guidance. No illustration beyond the tool's own object.

### Status Line
Full-width, 12dp corners, 12% tint of the ink (info) or error role (failure) behind an outlined icon and body text.

## Do's and Don'ts

### Do:
- **Do** give every new tool exactly one ink with a night value and a darker daylight value, and its own drawn object.
- **Do** show focus with an ink ring (3dp) and the focus lift; on night, filled buttons may also brighten.
- **Do** route every face-button action through the legend so the bar always shows exactly what A, B, X and Y do.
- **Do** confirm every write in a dialog that names the count, with the destructive confirm in caution amber and Cancel outlined.
- **Do** park all looping motion (block turning, meter sweep) when the system animation scale is 0.
- **Do** keep exactly one object turning at a time: the focused save block.

### Don't:
- **Don't** show focus by dimming or darkening an element; on a handheld that reads as disabled.
- **Don't** use amber for errors, highlights or anything that can be undone; errors use the error role.
- **Don't** represent a tool with an icon-font glyph or a Material icon tile; tools are drawn objects.
- **Don't** add eyebrow or kicker text above headings, or uppercase labels.
- **Don't** stack Material cards in one scrolling column on a wide screen.
- **Don't** draw progress as a continuous bar; use the block meter.
- **Don't** let a text field open the keyboard just because a controller passed over it.
