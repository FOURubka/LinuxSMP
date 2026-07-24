# LinuxSMP

Gacha-плагін для Paper 26.1.2 (Java 25): гравці ролять Linux-дистрибутиви як персонажів
з рідкостями, статами, абілками, brick-механікою, ф'южном і дуелями.

## Команди

| Команда | Опис |
|---|---|
| `/distroroll [premium]` | Рол з GUI-анімацією. Перший — безкоштовний, далі 100 Packages; premium — 1 RepoToken, підвищені шанси Legendary/Mythic |
| `/distroswap <distro>` | Свап активного дистрибутива. Skill check, Slowness 5с, кулдаун 15с. Свап у бою = mixed build → постійний upkeep |
| `/pacman-syu` | Обслуговування (аліаси: `/syu`, `/distroupdate`). Без нього high-maintenance/mixed білди отримують дебафи кожні 5 хв |
| `/distros` | GUI ростеру: дистрибутиви, брики, валюти, skill level |
| `/distrotoken <distro>` | Викинути дистрибутив як токен-предмет (для ф'южну; ПКМ — встановити назад) |
| `/distrorepair <distro>` | Ремонт: temp brick — за Packages; perma brick — Recovery ISO + Packages |
| `/distroduel <player\|accept\|deny>` | Дуель. Нагороди Packages за win/lose; опція same-rarity-only у config.yml |
| `/linuxsmp` | Адмінка: reload, give, iso, token; exchange (Vault) |

**Абілка:** Shift+F (свап рук із шифтом) — активує здібність активного дистрибутива
(skill check, кулдаун 30с).

## Механіки

- **Skill checks** — maintenance/swap/fusion/ability. Шанс фейлу:
  `base + learning-curve × modifier × 0.01 − skill × 0.001` (клемп 1%–60%).
  Успіх дає +1 skill (макс 200).
- **Brick**: TEMP (легкі дистрибутиви) — вимкнений на N секунд; PERMA (Arch, Gentoo, LFS) —
  втрачений, потрібен Recovery ISO. Kernel-panic титул + звук.
- **Piloting penalty**: learning-curve ≥ 6 і skill < curve×8 → періодичні Slowness/Mining Fatigue.
- **Fusion** — ShapedRecipe з двох токенів поряд (порядок зліва направо як у distros.yml).
  Фейл skill check спалює один вхідний токен. Рецепти: ubuntu+debian→mint, arch+mint→manjaro,
  debian+alpine→kali, ubuntu+gentoo→popos.
- **Лут з монстрів**: Packages (30%), RepoToken (2%), Dotfile (5%), Recovery ISO (0.5% дроп).
- **Vault** (softdepend): `/linuxsmp exchange buy|sell ...` — обмін Packages/RepoTokens на серверну валюту.

## Дані

- `distros.yml` — дистрибутиви, стати, brick-risk, ваги рідкостей, ф'южн-рецепти. Розширюється без перекомпіляції.
- `config.yml` — вартості, кулдауни, шанси, custom-model-data (1001–1005 за рідкістю, 1010 — ISO).
- `playerdata/<uuid>.yml` — дані гравців (автозбереження кожні 5 хв).

## Відхилення від ТЗ

- Рецепт Manjaro: замість "Manjaro-simplicity-token" — arch+mint.
- Тригер абілки — Shift+F (ПКМ порожньою рукою не надсилається клієнтом).
