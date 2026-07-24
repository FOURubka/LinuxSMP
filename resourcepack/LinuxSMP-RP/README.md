# LinuxSMP Resource Pack

Resource pack for the LinuxSMP plugin (Minecraft **26.1.2**, pack_format **84**).
Gives every distro its own icon through `custom_model_data`.

## Layout (ready)

```
pack.mcmeta                                  # min_format/max_format = 84
pack.png                                     # PACK ICON - needs drawing (128x128)
assets/minecraft/
  items/paper.json                           # dispatches PAPER -> distro models
  items/music_disc_11.json                   # dispatches the disc -> Recovery ISO model
  models/item/distro_*.json                  # 11 models (parent: item/generated)
  models/item/recovery_iso.json
  textures/item/                             # <-- PUT PNG TEXTURES HERE
```

## Textures (already drawn)

All icons are **16x16 pixel art in the Minecraft style** on a transparent background, and
live in `assets/minecraft/textures/item/`. The packed version is `../LinuxSMP-RP.zip`.
To replace any of them, overwrite the PNG under the same name.

| File | Distro | custom_model_data | Rarity |
|---|---|---|---|
| `distro_ubuntu.png`  | Ubuntu        | 2001 | COMMON |
| `distro_mint.png`    | Linux Mint    | 2002 | COMMON |
| `distro_popos.png`   | Pop!_OS       | 2003 | COMMON |
| `distro_debian.png`  | Debian        | 2004 | RARE |
| `distro_fedora.png`  | Fedora        | 2005 | RARE |
| `distro_alpine.png`  | Alpine        | 2006 | RARE |
| `distro_manjaro.png` | Manjaro       | 2007 | RARE |
| `distro_kali.png`    | Kali Linux    | 2008 | EPIC |
| `distro_arch.png`    | Arch Linux    | 2009 | EPIC |
| `distro_gentoo.png`  | Gentoo        | 2010 | LEGENDARY |
| `distro_lfs.png`     | Linux From Scratch | 2011 | MYTHIC |
| `recovery_iso.png`   | Recovery ISO (item) | 1010 | - |
| `pack.png`           | Pack icon (in the pack root, not in textures/) | - | - |

> The ISO icon and pack.png are separate; they are not distros.

## Installing

1. Draw the PNG, drop it into `textures/item/` (and `pack.png` into the pack root).
2. Zip the **contents** of the `LinuxSMP-RP` folder, so that `pack.mcmeta` sits at the root
   of the zip rather than inside a subfolder.
3. Serve it as a server resource pack (`server.properties` -> `resource-pack`), or install
   it locally in `.minecraft/resourcepacks/`.

## How it works

The plugin puts a `custom_model_data` component on a `Material.PAPER` item, set to the
distro id (2001-2011). Through `range_dispatch`, `items/paper.json` swaps the model for
`distro_<id>`, which pulls the texture from `textures/item/distro_<id>.png`. The Recovery
ISO works the same way on music disc 11. If a texture is missing the item simply renders as
ordinary paper, with no errors.
