# LinuxSMP

Roll Linux distributions as gacha characters, collect them, and fight with them. Each distro is a real character with its own stats, its own ability, and its own way of breaking on you if you are careless. Arch will hand you a shockwave when it is patched and hurt you when it is not. NixOS lets you rewind a bad moment. Gentoo pins you in place for an eight-second compile that anyone can interrupt, and pays out only if you survive it.

There are 14 distros across five rarities. You start with nothing and roll your way up.

## The loop

Roll with `/distroroll`. You get a distro token. Right-click it to install, or keep it to fuse later. Your active distro gives you a passive buff all the time, and its ability fires when you sneak and swap hands (Shift+F), or when you run its own command like `/arch` or `/nixos`.

Higher rarity distros are stronger but riskier. They brick more often, they demand upkeep, and losing one hurts. Common distros just work.

## Rarities

- Common: safe, low upkeep, never permanently bricks.
- Rare: better stats, a small brick chance.
- Epic: strong abilities, real risk.
- Legendary: source-based monsters. Perma-brick if you fail the check.
- Mythic: one distro. Linux From Scratch. Build it yourself and pray.

## The distros

Every distro has its own signature ability. No two do the same kind of thing.

<details>
<summary>Click to see all 14</summary>

| Icon | Distro | Rarity | What its ability does |
|:---:|---|---|---|
| <img src="https://files.catbox.moe/3rjsyc.png" width="32" height="32" alt="Ubuntu"> | Ubuntu | Common | Heals and shields the allies around you |
| <img src="https://files.catbox.moe/0k5yru.png" width="32" height="32" alt="Linux Mint"> | Linux Mint | Common | Wipes your debuffs and heals you clean |
| <img src="https://files.catbox.moe/w3fp68.png" width="32" height="32" alt="Pop!_OS"> | Pop!_OS | Common | Merges and sorts your whole inventory, then a short dash |
| <img src="https://files.catbox.moe/416sf2.png" width="32" height="32" alt="Debian"> | Debian | Rare | Six seconds where you shrug off most damage and bounce attackers back |
| <img src="https://files.catbox.moe/ki29e5.png" width="32" height="32" alt="Fedora"> | Fedora | Rare | An offensive aura that cuts everything standing next to you |
| <img src="https://files.catbox.moe/rvltxy.png" width="32" height="32" alt="Alpine"> | Alpine | Rare | Shrinks you into a tiny, fast, hard-to-hit target |
| <img src="https://files.catbox.moe/jy6nta.png" width="32" height="32" alt="Manjaro"> | Manjaro | Rare | Pulls a random useful item straight out of the AUR |
| <img src="https://files.catbox.moe/ppushf.png" width="32" height="32" alt="Tails"> | Tails | Rare | You vanish, and every hostile nearby forgets you exist |
| <img src="https://files.catbox.moe/6l8199.png" width="32" height="32" alt="Kali"> | Kali Linux | Epic | Steals enemy buffs and lights up every mob through walls |
| <img src="https://files.catbox.moe/wn2zww.png" width="32" height="32" alt="Arch"> | Arch Linux | Epic | A shockwave if you kept it updated, self-damage if you did not |
| <img src="https://files.catbox.moe/qylsdl.png" width="32" height="32" alt="NixOS"> | NixOS | Epic | Saves a generation, then rolls you back to it |
| <img src="https://files.catbox.moe/zpstp6.png" width="32" height="32" alt="Gentoo"> | Gentoo | Legendary | Rooted for an 8s emerge that any hit cancels; survive it for a detonation and 20s of -march=native |
| <img src="https://files.catbox.moe/vikbaj.png" width="32" height="32" alt="Slackware"> | Slackware | Legendary | One precise lightning bolt on your target, with a chance to backfire |
| <img src="https://files.catbox.moe/hk3b8c.png" width="32" height="32" alt="Linux From Scratch"> | Linux From Scratch | Mythic | Rebuilds you from nothing: massive heal and an area blast |

</details>

## Fusion

Two tokens can fuse into a third distro. Ubuntu plus Debian gives you Mint. Arch plus Mint gives you Manjaro. Fusion runs a skill check, and on a failure one of your tokens is gone. Open `/distrofuse` to drag two tokens in and see your real success chance before you commit, or `/distrofuse recipes` for the fusion map, where clicking any distro shows what it is made from and what it fuses into.

## Bricking and upkeep

Risky distros can brick when an action fails. A temp brick locks the distro for a few minutes. A perma brick (Arch, Gentoo, Slackware, LFS) locks it until you pay to repair it with `/distrorepair`. High-maintenance distros also drift out of tune over time, so run `/pacman-syu` to keep them stable. Swap your active distro mid-combat and the build turns "mixed": it works, but it needs constant upkeep after that.

## The rest of it

- Collection sets: own a full family (all the Commons, the whole Debian line, and so on) for a permanent passive.
- Synergies: your active distro gets an extra buff when it is in its element. Alpine is faster in the mountains, Tails is invisible at night, Debian regenerates near a village.
- Duels: challenge another player with `/distroduel`.
- Leaderboard: `/distrotop` ranks players by how rare their collection is.
- Pet: a small floating logo of your active distro follows you around.

## Commands

- `/distroroll` roll a distro
- `/distros` open your roster and profile
- `/distroswap <distro>` change your active distro
- `/arch`, `/nixos`, `/gentoo`, ... boot that distro and fire its ability
- `/distrofuse [recipes]` fusion screen and fusion map
- `/distrotop` leaderboard
- `/distroduel <player>` challenge someone
- `/pacman-syu` maintenance for your active distro
- `/distrorepair <distro>` fix a bricked distro
- `/distrotoken <distro>` eject a distro as a fusable token

## Requirements

- Paper 26.x (api-version 26.1.2)
- Java 25
- Vault is optional, only for the economy side
- An optional resource pack gives every distro the custom item art you see above
