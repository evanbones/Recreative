# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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