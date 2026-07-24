# LinuxSMP Resource Pack

Ресурспак для плагіна LinuxSMP (Minecraft **26.1.2**, pack_format **84**).
Дає кожному дистрибутиву власну іконку через `custom_model_data`.

## Структура (готова)

```
pack.mcmeta                                  # min_format/max_format = 84
pack.png                                     # ІКОНКА ПАКА — треба намалювати (128x128)
assets/minecraft/
  items/paper.json                           # диспетч PAPER -> моделі дистрибутивів
  items/music_disc_11.json                   # диспетч диску -> модель Recovery ISO
  models/item/distro_*.json                  # 11 моделей (parent: item/generated)
  models/item/recovery_iso.json
  textures/item/                             # <-- СЮДИ КЛАСТИ PNG-ТЕКСТУРИ
```

## Текстури (вже намальовані)

Усі іконки — **16×16 піксель-арт у стилі Minecraft** (прозорий фон), лежать у
`assets/minecraft/textures/item/`. Готовий пак — `../LinuxSMP-RP.zip`.
Щоб замінити будь-яку — перезапиши PNG тим самим іменем.

| Файл | Дистрибутив | custom_model_data | Рідкість |
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
| `recovery_iso.png`   | Recovery ISO (предмет) | 1010 | — |
| `pack.png`           | Іконка пака (в корені, не в textures/) | — | — |

> Іконка ISO та pack.png — окремі, не дистрибутиви.

## Встановлення

1. Намалюй PNG, поклади у `textures/item/` (і `pack.png` у корінь пака).
2. Заархівуй **вміст** папки `LinuxSMP-RP` у zip (щоб `pack.mcmeta` був у корені zip, а не в підпапці).
3. Постав як серверний resource pack (`server.properties` → `resource-pack`) або локально в `.minecraft/resourcepacks/`.

## Як це працює

Плагін кладе на предмет `Material.PAPER` компонент `custom_model_data` = ID дистрибутива
(2001–2011). `items/paper.json` через `range_dispatch` підміняє модель на `distro_<id>`,
яка тягне текстуру `textures/item/distro_<id>.png`. Recovery ISO — те саме на диску 11.
Якщо текстури нема — предмет виглядає як звичайний папір (fallback), помилок не буде.
