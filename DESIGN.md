---
name: Mimir
description: A warm violet Material 3 workshop for safely maintaining retro ROM libraries across Android and desktop.
colors:
  primary: "#7B1FA2"
  primary-dark: "#E0B0FF"
  secondary: "#6A1B9A"
  secondary-dark: "#F5B7FF"
  tertiary: "#5F5663"
  background: "#FAF7FC"
  background-dark: "#100B14"
  surface: "#FFFFFF"
  surface-dark: "#19121F"
  surface-variant: "#EEE5F2"
  surface-variant-dark: "#30243A"
  outline: "#76677A"
  outline-dark: "#A99AAF"
  text: "#1A111F"
  text-dark: "#F5EDF8"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "28sp"
    fontWeight: 600
    lineHeight: "36sp"
  headline:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 600
    lineHeight: "32sp"
  title:
    fontFamily: "sans-serif"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "28sp"
  body:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "monospace"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.8sp"
rounded:
  icon: "10dp"
  card: "12dp"
  action-surface: "16dp"
  chip: "999dp"
spacing:
  compact: "8dp"
  card: "12dp"
  comfortable: "16dp"
  page: "20dp"
components:
  tool-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.card}"
    padding: "14dp 16dp"
  tool-card-selected:
    backgroundColor: "#7B1FA21F"
    textColor: "{colors.text}"
    rounded: "{rounded.card}"
    padding: "14dp 16dp"
  floating-action-surface:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.action-surface}"
    padding: "8dp"
---

# Design System: Mimir

## Overview

**Creative North Star: "The Violet Workshop"**

Mimir is a warm, approachable cross-platform workshop for careful library maintenance. Violet connects the app's tools and decisions; generous, gently lifted surfaces make the operational work feel manageable rather than clinical.

The interface is Material 3 first. It uses clear task labels, platform-appropriate controls, and deliberate confirmation points to make local file work feel trustworthy. Dark mode is a first-class violet-night scheme, never an inverted light screen.

**Key Characteristics:**

- Warm violet accents on pale lilac or near-black plum surfaces.
- Landscape-first operation with a persistent navigation rail on desktop and expanded Android layouts, with a readable vertical task list on every target.
- Gently lifted cards, plain-language task copy, and technical mono labels for statuses and small metadata.

## Colors

The palette pairs a welcoming workshop violet with quiet lilac neutrals; the accent identifies action and selection rather than filling the screen.

### Primary

- **Workshop Violet:** primary actions, selected tools, section labels, and icon emphasis. Use its dark-mode counterpart on the violet-night scheme.

### Secondary

- **Orchid Signal:** supporting status, output, and positive-progress emphasis; reserve it for secondary emphasis.

### Tertiary

- **Muted Plum:** restrained supporting hierarchy where neither action nor status should dominate.

### Neutral

- **Lilac Paper:** the light background and soft gradient base.
- **Clean Surface:** cards and temporary elevated surfaces.
- **Violet Night:** the dark background, with deep-plum surfaces layered above it.
- **Soft Outline:** card boundaries and inactive selection affordances.

**The Accent-Has-A-Job Rule.** Violet is for action, selection, and navigation context. Let the neutral surface do most of the visual work.

## Typography

**Display Font:** platform sans-serif.
**Body Font:** platform sans-serif.
**Label/Mono Font:** platform monospace.

**Character:** Semibold sans-serif headings make tools easy to scan; mono labels give filenames, status, and operation language a useful workshop character without compromising body readability.

### Hierarchy

- **Display:** used for key page-level states and large summaries.
- **Headline:** section titles and operational summaries.
- **Title:** cards and secondary headings.
- **Body:** descriptions, folder names, and explanatory copy.
- **Label:** compact all-caps-like technical labels, metadata, and status markers.

**The Plain-Task Rule.** Use normal language for decisions and consequences; reserve mono treatment for concise labels and technical detail.

## Layout

Landscape is the primary operating context. At expanded sizes, Mimir keeps a navigation rail on the left and increases the content inset beside it; the main content remains a single readable vertical list rather than a dense dashboard grid. This preserves predictable scanning for sequential ROM-library work.

Compact portrait screens retain the same vertical task order with page padding and a bottom navigation treatment. Use 12 logical pixels between stacked content blocks, 16 for card interiors, and 20 for top-bar horizontal padding. The navigation rail appears from 840 logical pixels width and 480 logical pixels height, covering landscape handhelds as well as larger devices. Support system font scaling, insets, keyboard focus, and 48 logical-pixel minimum touch targets.

## Elevation & Depth

Mimir uses soft lift, not heavy shadows: translucent white or deep-plum cards sit above a subtle vertical background gradient, with Material tonal elevation providing the main separation. The persistent bottom action surface is the clearest elevated element because it carries the next irreversible step.

**The Gentle-Lift Rule.** Elevation should clarify a control's readiness or action priority; it must not make every surface look floating.

## Shapes

The form language is softly rectangular: 12dp for task cards, 16dp for action surfaces, 10dp for icon tiles, and pill shapes only for compact chips. Outlines are thin and low-contrast at rest, becoming more present for selection.

## Components

### Buttons

- **Character:** warm, direct Material 3 actions.
- **Primary:** standard filled Material 3 button for scanning, applying, installing, and confirming.
- **Secondary:** outlined Material 3 button for cancellation, alternative folder selection, and reversible actions.
- **Placement:** a bottom action surface may hold the primary operation when a tool page needs a persistent next step.

### Chips

- **Style:** standard Material 3 filter chips in a wrapping row with compact gaps.
- **State:** selection is explicit through the Material selected treatment; chips choose options rather than trigger destructive actions.

### Cards / Containers

- **Character:** gently lifted, full-width workbench cards.
- **Tool cards:** a thin outline, 12dp corners, and a 44dp icon tile; selected cards receive a low-opacity primary wash.
- **Internal spacing:** 16dp horizontally and 14dp vertically for tool rows.

### Navigation

- **Expanded:** Material navigation rail at the left for home and each tool.
- **Compact:** use the existing compact navigation treatment, while keeping system Back behavior intact.
- **Active state:** primary violet indicates the current context.

### Dialogs

- **Style:** standard Material 3 alert dialogs for confirmation only.
- **Content:** state the file-operation consequence plainly, including when a change cannot be undone.

## Do's and Don'ts

### Do:

- **Do** make landscape the reference layout: rail plus one vertical task sequence.
- **Do** treat dark mode as a complete violet-night palette with readable surface contrast.
- **Do** use primary violet to show action, navigation context, and selected work.
- **Do** keep destructive file operations behind clear Material confirmation dialogs.

### Don't:

- **Don't** turn the landscape workspace into a multi-column tool grid.
- **Don't** use violet as a full-screen fill or add decorative neon effects.
- **Don't** replace native Material 3 controls with iOS-like controls or custom equivalents.
- **Don't** use mono for paragraphs or instructional copy.
