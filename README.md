# LinuxSMP

A gacha plugin for Paper: players roll Linux distributions as characters, with rarities,
stats, abilities, a brick mechanic, fusion and duels.

Compiled against the Paper API 1.21.11 and runs on 1.21.3 - 26.2. Requires Java 21 or newer.

## Commands

| Command | Description |
|---|---|
| `/distroroll [premium]` | Roll, with a GUI animation. The first one is free, after that 100 Packages; premium costs 1 RepoToken and raises the Legendary/Mythic odds |
| `/distroswap <distro>` | Swap the active distro. Skill check, 5s of Slowness, 15s cooldown. Swapping mid-combat leaves a mixed build, which needs permanent upkeep |
| `/pacman-syu` | Maintenance (aliases: `/syu`, `/distroupdate`). Without it, high-maintenance and mixed builds take debuffs every 5 minutes |
| `/distros` | Roster GUI: distros, bricks, currencies, skill level |
| `/distrotoken <distro>` | Drop a distro as a token item, for fusion. Right-click a token to install it back |
| `/distrorepair <distro>` | Repair: a temp brick costs Packages, a perma brick needs a Recovery ISO plus Packages |
| `/distroduel <player\|accept\|deny>` | Duel. Pays out Packages for a win or a loss; `same-rarity-only` is a config.yml option |
| `/linuxsmp` | Admin: reload, give, iso, token, and exchange (Vault) |

**Ability:** Shift+F, sneak plus swap-hands, fires the active distro's ability. Skill
check, 30s cooldown.

## Mechanics

- **Skill checks** on maintenance, swap, fusion and ability. Failure chance is
  `base + learning-curve x modifier x 0.01 - skill x 0.001`, clamped to 1%-60%.
  Every success gives +1 skill, up to 200.
- **Brick**: TEMP, on the lighter distros, disables one for N seconds. PERMA, on Arch,
  Gentoo and LFS, loses it until you find a Recovery ISO. Comes with a kernel-panic title
  and sound.
- **Piloting penalty**: a learning curve of 6 or more with a skill below `curve x 8` gives
  you periodic Slowness and Mining Fatigue.
- **Fusion** is a ShapedRecipe from two tokens placed side by side, left to right in the
  order they appear in distros.yml. Failing the skill check burns one of the input tokens.
  Recipes: ubuntu+debian to mint, arch+mint to manjaro, debian+alpine to kali,
  ubuntu+gentoo to popos.
- **Mob loot**: Packages (30%), RepoToken (2%), Dotfile (5%), Recovery ISO (0.5%).
- **Vault** (softdepend): `/linuxsmp exchange buy|sell ...` trades Packages and RepoTokens
  against the server economy.

## Data

- `distros.yml` holds the distros, their stats, brick risk, rarity weights and fusion
  recipes. It can be extended without recompiling.
- `config.yml` holds costs, cooldowns, chances and custom-model-data (1001-1005 by rarity,
  1010 for the ISO).
- `playerdata/<uuid>.yml` holds per-player data, saved automatically every 5 minutes.

## Deviations from the original spec

- The Manjaro recipe is arch+mint rather than a dedicated "Manjaro-simplicity-token".
- The ability trigger is Shift+F, because the client never sends a right-click with an
  empty hand.
