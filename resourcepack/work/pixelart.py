#!/usr/bin/env python3
"""Hand-drawn 16x16 Minecraft-style pixel-art icons for LinuxSMP distros.
Logo marks drawn on TRANSPARENT background (no filled disc/square behind)."""
from PIL import Image
import os, math

S = 16
OUT = "out"
os.makedirs(OUT, exist_ok=True)

T = (0, 0, 0, 0)
def C(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

WHITE = C('ffffff')
BLACK = C('1a1a1a')

def render(grid, palette, name):
    img = Image.new('RGBA', (S, S), T)
    px = img.load()
    assert len(grid) == S, f"{name}: {len(grid)} rows"
    for y, row in enumerate(grid):
        row = row.ljust(S, '.')[:S]
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = palette[ch]
    img.save(f"{OUT}/{name}.png")

# ---------- UBUNTU: Circle of Friends (orange ring + 3 dots + Y spokes) ----------
def ubuntu():
    img = Image.new('RGBA', (S, S), T); px = img.load()
    o = C('e95420'); cf = 7.5; R = 6.0
    angs = [-90, 30, 150]
    dots = [(cf + R*math.cos(math.radians(a)), cf + R*math.sin(math.radians(a))) for a in angs]
    def disc(cx, cy, r, col):
        for x in range(S):
            for y in range(S):
                if math.hypot(x+0.5-cx, y+0.5-cy) <= r:
                    px[x, y] = col
    def line(x0, y0, x1, y1, col):
        n = int(max(abs(x1-x0), abs(y1-y0))*3)+1
        for i in range(n+1):
            t = i/n; px[int(x0+(x1-x0)*t), int(y0+(y1-y0)*t)] = col
    for x in range(S):
        for y in range(S):
            dx, dy = x+0.5-cf, y+0.5-cf; d = math.hypot(dx, dy)
            if 4.6 <= d <= 6.4:
                ang = math.degrees(math.atan2(dy, dx))
                if all(abs((ang-a+180) % 360-180) > 26 for a in angs):
                    px[x, y] = o
    for dx, dy in dots:
        line(cf, cf, dx, dy, o)
    disc(cf, cf, 1.6, o)
    for dx, dy in dots:
        disc(dx, dy, 1.9, o)
    img.save(f"{OUT}/distro_ubuntu.png")
ubuntu()

# ---------- ARCH: blue triangle / mountain (unchanged, already good) ----------
arch = [
    ".......bb.......",
    ".......bb.......",
    "......bDDb......",
    "......bDDb......",
    ".....bDDDDb.....",
    ".....bDDDDb.....",
    "....bDDDDDDb....",
    "....bDDDDDDb....",
    "...bDDDbbDDDb...",
    "...bDDb..bDDb...",
    "..bDDDb..bDDDb.",
    "..bDDb....bDDb.",
    ".bDDb......bDDb",
    ".bDb........bDb",
    "bDb..........bD",
    "b...............",
]
render(arch, {'b': C('1c9de0'), 'D': C('1275ac')}, "distro_arch")

# ---------- DEBIAN: red swirl (unchanged, already reads) ----------
debian = [
    "................",
    ".....rrrrr......",
    "...rrr...rrr....",
    "..rr.......rr...",
    "..r...rrr...r...",
    ".rr..rr.rr..rr..",
    ".r..rr...r...r..",
    ".r..r....r...r..",
    ".r..r........r..",
    ".r..rr......r...",
    ".rr..rr....rr...",
    "..r...rrrrr.....",
    "..rr............",
    "...rr...........",
    ".....rr.........",
    "................",
]
render(debian, {'r': C('d70751')}, "distro_debian")

# ---------- FEDORA: just the blue 'f' mark, NO circle ----------
fedora = [
    "................",
    ".......bbbb.....",
    "......bbbbbb....",
    "......bb..bb....",
    "......bb........",
    "......bb........",
    "...bbbbbbb......",
    "...bbbbbbb......",
    "......bb........",
    "......bb........",
    "......bb........",
    "......bb........",
    "......bb........",
    "......bb........",
    "......bb........",
    "................",
]
render(fedora, {'b': C('3c6eb4')}, "distro_fedora")

# ---------- MINT: green mint leaf (monogram doesn't survive 16px on alpha) ----------
mint = [
    "................",
    ".........gg.....",
    "........gggg....",
    ".......gggggg...",
    "......gggglgg...",
    ".....ggggllgg...",
    "....ggggllggg...",
    "....gggllgggg...",
    "...ggglllgggg...",
    "...gggllggggg...",
    "..gggllgggggg...",
    "..gglgggggg.....",
    ".gglggggg.......",
    ".glgggg.........",
    "gglg............",
    "g...............",
]
render(mint, {'g': C('86be43'), 'l': C('b6e08a')}, "distro_mint")

# ---------- MANJARO: three green bars (unchanged) ----------
manjaro = [
    "................",
    "................",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gg..gg...",
    "...gg..gggggg...",
    "...gg..gggggg...",
    "................",
    "................",
    "................",
]
render(manjaro, {'g': C('35bf5c')}, "distro_manjaro")

# ---------- GENTOO: purple swirl g (unchanged) ----------
gentoo = [
    "................",
    ".....pppppp.....",
    "...ppppLLpppp...",
    "..pppLLLLLLppp..",
    "..ppLLpppppLp...",
    ".ppLLp....pp....",
    ".ppLp...........",
    ".ppLp...........",
    ".ppLp....ppppp..",
    ".ppLLp...ppLLp..",
    "..ppLLpppppLLp..",
    "..pppLLLLLLLLp..",
    "...pppppppLLp...",
    ".........pLLp...",
    "......ppppppp...",
    "................",
]
render(gentoo, {'p': C('54487a'), 'L': C('a499c6')}, "distro_gentoo")

# ---------- ALPINE: white/blue mountain peaks, NO disc ----------
alpine = [
    "................",
    "................",
    "................",
    "......B.........",
    ".....BBB........",
    ".....BwB....B...",
    "....BBwBB..BBB..",
    "....BwwwB.BwwB..",
    "...BBwwwBBBwwBB.",
    "..BBwwwwwBwwwwBB",
    ".BBwwwwwwwwwwwBB",
    ".BBBBBBBBBBBBBBB",
    "................",
    "................",
    "................",
    "................",
]
render(alpine, {'B': C('0d597f'), 'w': WHITE}, "distro_alpine")

# ---------- KALI: dragon swoosh in kali-blue, NO disc (approximate at 16px) ----------
kali = [
    "................",
    "......kk........",
    ".....kkk........",
    "...k.kkk........",
    "...kkkkkk.......",
    "..kkkkkkkk......",
    "..kkkwWkkkkkk...",
    ".kkkkkkkkkkkk...",
    ".kkkkkkkkk.kk...",
    ".kkkkkkkkk......",
    "..kkkkkkkkkk....",
    "...kkkk..kkk....",
    "...kk......k....",
    "..kk............",
    "................",
    "................",
]
render(kali, {'k': C('2c5f7a'), 'w': WHITE, 'W': C('1a3547')}, "distro_kali")

# ---------- POP!_OS: teal cursor (I-beam), NO square ----------
popos = [
    "................",
    "................",
    ".....tttttt.....",
    ".....tttttt.....",
    ".......tt.......",
    ".......tt.......",
    ".......tt.......",
    ".......tt....o..",
    ".......tt...ooo.",
    ".......tt....o..",
    ".......tt.......",
    ".......tt.......",
    ".....tttttt.....",
    ".....tttttt.....",
    "................",
    "................",
]
render(popos, {'t': C('48b9c7'), 'o': C('f5a05a')}, "distro_popos")

# ---------- LFS: mythic golden cog (unchanged) ----------
lfs = [
    "................",
    "......gGGg......",
    "..g...gGGg...g..",
    ".gGg.gGGGGg.gGg.",
    ".gGGgGGGGGGgGGg.",
    "..gGGGGrrGGGGg..",
    "...gGGrRRrGGg...",
    "gGGGGGrRRrGGGGGg",
    "gGGGGGrRRrGGGGGg",
    "...gGGrRRrGGg...",
    "..gGGGGrrGGGGg..",
    ".gGGgGGGGGGgGGg.",
    ".gGg.gGGGGg.gGg.",
    "..g...gGGg...g..",
    "......gGGg......",
    "................",
]
render(lfs, {'g': C('b8860b'), 'G': C('ffc336'), 'r': C('c0392b'), 'R': C('ff6b4a')}, "distro_lfs")

# ---------- RECOVERY ISO: shiny disc (unchanged) ----------
iso = [
    "................",
    "....ssssssss....",
    "..ssccccccccss..",
    ".sccccWWccccccs.",
    ".scccWWcccccccs.",
    "sccccccssccccccs",
    "sccccss00ssccccs",
    "sccccs0..0sccccs",
    "sccccs0..0sccccs",
    "sccccss00ssccccs",
    "sccccccssccccccs",
    ".sccccccccccccs.",
    ".sccccccccccccs.",
    "..ssccccccccss..",
    "....ssssssss....",
    "................",
]
render(iso, {'s': C('9fb3c0'), 'c': C('7fd4e8'), 'W': WHITE, '0': C('4a5a66')}, "recovery_iso")

# ---------- TUX (pack.png) ----------
tux = [
    ".....kkkk.......",
    "....kkkkkk......",
    "...kkkkkkkk.....",
    "...kkWWWWkk.....",
    "...kWWkkWWk.....",
    "...kWyykyWk.....",
    "...kWWyyWWk.....",
    "..kkkWWWWkkk....",
    ".kkWWWWWWWWkk...",
    ".kWWWWWWWWWWk...",
    ".kWWWWWWWWWWk...",
    ".kWWWWWWWWWWk...",
    "..kWWWWWWWWk....",
    "..yyk.WW.kyy....",
    ".yyyy....yyyy...",
    "................",
]
render(tux, {'k': BLACK, 'W': WHITE, 'y': C('f2b233')}, "pack")

print("generated", len([f for f in os.listdir(OUT) if f.endswith('.png')]), "pngs")
