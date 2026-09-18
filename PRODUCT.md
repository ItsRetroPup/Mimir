# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive (Android, Windows, macOS, Linux)

## Users

People who use emulation on retro handhelds and want to reclaim storage or automate repetitive library work, such as creating M3U playlists for multi-disc games.

## Product Purpose

Mimir is an open-source, local-first toolkit for safely organising retro ROM libraries. It helps people prepare and maintain libraries from one central app, with a preview before any change is applied.

## Positioning

Mimir brings ROM organisation, conversion, frontend preparation, and PS Vita shortcut creation into one simple cross-platform app rather than requiring separate tools and manual file work.

## Operating Context

Users select folders on Android devices or desktop computers and review planned file changes before applying them. Mimir supports ROM organisers and converters, frontend-specific M3U output rules, PS Vita shortcuts, and ES-DE custom-system setup.

## Capabilities and Constraints

- All file operations run locally on the device; there is no cloud sync or account system.
- Safety comes from preview-and-confirm workflows, deterministic operations, and conflict skipping.
- Android targets API 29 and above with ARM64 native converter binaries; desktop targets use platform-specific converter bundles.
- Web and iOS are out of scope because Mimir requires direct local filesystem access and bundled converter executables.
- Frontend compatibility is prioritised, with current support for ES-DE and generic output presets.

## Brand Commitments

- Product name: Mimir.
- Keep the experience simple and centralised.
- Mimir is open source and is associated with the RetroPup creator channel.

## Evidence on Hand

- The repository README documents current features, supported formats, and legal constraints.
- The repository contains the Flutter application, platform adapters, shared Dart core, unit tests, converter backends, license texts, and PS Vita shortcut data.
- No testimonials, customer claims, benchmark results, or pricing information are available for use.

## Product Principles

1. Keep ROM-library maintenance simple.
2. Consolidate common handheld-emulation tasks in one place.
3. Preserve user control through clear previews and confirmation.
4. Work locally, without accounts or cloud dependency.
5. Prefer predictable, frontend-compatible output.

## Accessibility & Inclusion

- Follow each target platform’s accessibility expectations, using Material components, system font scaling, keyboard focus where available, and minimum touch-target sizes.
