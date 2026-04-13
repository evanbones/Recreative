# Recreative

<a href='https://files.minecraftforge.net'><img alt="forge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg"></a>
<a href='https://fabricmc.net'><img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg"></a>
<a href='https://neoforged.net/'><img alt="neoforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"></a>

Recreative is a powerful, data-driven mod that gives modpack developers and players complete control over Creative Mode tabs! Using simple JSON configuration files, you can completely reorganize, modify, hide, or create entirely new Creative Tabs without writing a single line of code.

## Features

* Completely hide unwanted vanilla or modded tabs from the Creative Menu.
* Rename tabs, change their icons, or add and remove specific items.
* Build brand new tabs from scratch with your own custom icons and item lists.
* Reorder tabs and force them to appear in a specific order from left to right.
* Instead of just appending items to the end of a tab, you can inject them exactly where you want using `after` or `before` placement rules.
* Add or remove entire groups of items at once using Minecraft tags (e.g., `#minecraft:logs`, `#c:ores`).
* Tweak your UI on the fly with hot reloading, so you can see your changes instantly without restarting the game.

## Usage & Configuration

Recreative watches the `./config/recreative/` folder in your Minecraft instance. You can create as many `.json` files inside this folder as you want to organize your rules.

### Example Rule File
```json
[
  {
    "action": "remove_tab",
    "tabs": ["minecraft:building_blocks"]
  },
  {
    "action": "custom_tab",
    "tabs": ["my_pack:magic"],
    "name": "Magical Items",
    "icon": "minecraft:amethyst_shard",
    "add_items": [
      "minecraft:enchanted_book",
      "#minecraft:potions"
    ]
  }
]
```

*Check out the [wiki](https://moddedmc.wiki/en/project/recreative/latest/docs) for documentation on all actions, advanced item placement, and syntax.*

## Commands

Recreative provides a couple useful commands to help you manage your configuration and easily find IDs without leaving the game.

* `/recreative reload` - Hot-reloads your `recreative_settings.json` and all rule files in the `config/recreative/` folder and instantly rebuilds the creative menu.
* `/recreative dump <tabs|items|blocks|templates|all>` - Exports registered IDs into nicely formatted JSON files located in `config/recreative_exports/`. Perfect for finding the exact namespace IDs needed for your scripts.

The `templates` dump converts all your currently registered tabs into the Recreative format in `config/recreative/tabs`.

## In-Game Settings

General config options can be accessed in-game (when **[Cloth Config](https://modrinth.com/mod/cloth-config)** is installed). This includes toggling the mod on/off globally, and a debug option to show internal tab IDs, which replaces all tab names with their registry ID to help you write your config files faster.

## License

[![Code license (MIT)](https://img.shields.io/badge/code%20license-MIT-green.svg?style=flat-square)](https://github.com/evanbones/Recreative/blob/1.20.1/LICENSE)

## Credits

Icon made by Nekomaster!

---

[![discord-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_vector.svg)](https://discord.com/invite/JcGRdT6Pbx) [![github-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/github-plural_vector.svg)](https://github.com/evanbones/Recreative)
