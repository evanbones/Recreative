# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.2] - 2026-09-03

### Fixed

- Fixed crash with certain mods that modify creative tabs.

## [2.2.1] - 2026-09-02

### Fixed

- Generated rules no longer overwrite tag-based and list rules.

## [2.2.0] - 2026-09-02

### Added

- Improved generated rules to respect components.

### Changed

- The tab editor now saves each tab's rules back into the file they were loaded from, instead of just `tabs.json`.

### Fixed

- Fixed moving an item near an item with components (suspicious stews, firework rockets, potions, etc.) placing it in
  the wrong spot.

## [2.1.1] - 2026-09-01

### Fixed

- Fixed ordering in EMI (and possibly other recipe viewers).

## [2.1.0] - 2026-09-01

### Added

- Added multi-select support in the tab editor: Ctrl+click to toggle items, Shift+click to select a range, Ctrl+A to
  select all.
- You can now copy, cut, and paste items within a tab and between tabs, using Ctrl+C / Ctrl+X / Ctrl+V.

### Fixed

- Fixed tab ordering for items added to tabs.

## [2.0.0] - 2026-08-31

### Added

- Added an in-game creative tab editor.

### Fixed

- `/recreative dump templates` now dumps all modded items, not just vanilla ones.

## [1.5.3] - 2026-08-08

### Fixed

- Fixed issues with adding multiple items with components to the same tab.

## [1.5.2] - 2026-07-28

### Fixed

- Fixed crash when removing then adding the same item.

## [1.5.1] - 2026-07-27

### Fixed

- Fixed issues with Clutter No More.

## [1.5.0] - 2026-05-12

### Changed

- Template JSONs are no longer automatically loaded after being dumped.
- `/recreative dump all` now includes templates in the dump.

### Fixed

- Properly fixed Fabric crash on 26.1.x.
- Fixed vanilla tabs not updating their position when being removed.

## [1.4.0] - 2026-04-22

### Added

- Added support for raw `ResourceLocation`/`Identifier`s in tab icons.
    - For example: `"icon": "minecraft:textures/item/apple.png"`

### Fixed

- Fixed Fabric crash on 26.1.

## [1.3.0] - 2026-04-16

### Fixed

- Switched to a safer dynamic tab registration system, fixing a crash with Supplementaries.

## [1.2.0] - 2026-04-14

### Added

- Added proper support for NBT in tab additions/removals.

## [1.1.0] - 2026-04-13

### Fixed

- Fixed command feedback not sending on 1.21+.

### Added

- Added `dump templates` command.

## [1.0.1] - 2026-04-13

- Added missing mod description.

## [1.0.0] - 2026-04-13

- Initial release.