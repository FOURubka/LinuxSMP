set -u
cd /home/_4rubka_/Projects/plugins/LinuxSMP/resourcepack/work
mkdir -p out32
base="https://commons.wikimedia.org/wiki/Special:FilePath"
UA="LinuxSMP-assets/1.0 (personal minecraft plugin; contact v.barskiy2012@gmail.com)"
get() {
  enc=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$2")
  code=$(curl -sL -m 40 -A "$UA" --retry 5 --retry-delay 5 --retry-all-errors -o "src_$1.bin" -w "%{http_code}" "$base/$enc")
  sz=$(stat -c%s "src_$1.bin" 2>/dev/null || echo 0)
  [ "$code" = "200" ] && [ "$sz" -gt 500 ] && echo "OK   $1 [$sz]" || { echo "FAIL $1 code=$code sz=$sz"; rm -f "src_$1.bin"; }
}
get ubuntu    'Former UbuntuCoF.svg'
get mint      'Linux Mint logo without wordmark.svg'
get popos     'Pop! OS Icon.svg'
get debian    'Openlogo-debianV2.svg'
get fedora    'Fedora icon (2021).svg'
get alpine    'Alpine Linux Logo.svg'
get manjaro   'Icon manjaro rounded 2022.svg'
get kali      'Kali-dragon-icon.svg'
get arch      'Arch Linux "Crystal" icon.svg'
get gentoo    'Gentoo Linux logo matte.svg'
get nixos     'Nix Snowflake Logo.svg'
get slackware 'Slackware logo.svg'
get tux       'Tux.svg'

echo "=== render 32px ==="
for id in ubuntu mint popos debian fedora alpine manjaro kali arch gentoo nixos slackware tux; do
  [ -f "src_$id.bin" ] || { echo "skip $id (no source)"; continue; }
  rsvg-convert -w 256 -h 256 --keep-aspect-ratio "src_$id.bin" -o "big_$id.png" 2>/dev/null
  magick "big_$id.png" -trim +repage -background none \
    -resize 30x30 -gravity center -extent 32x32 -posterize 6 "out32/$id.png"
done

# lfs + iso: hand-drawn 16px from pixelart.py, nearest-scaled 200%
magick out/distro_lfs.png  -filter point -resize 200% out32/lfs.png
magick out/recovery_iso.png -filter point -resize 200% out32/iso.png

# tails: procedural pixel onion
python3 - <<'PY'
from PIL import Image
import math
S=32
img=Image.new('RGBA',(S,S),(0,0,0,0)); px=img.load()
def c(h):h=h.lstrip('#');return(int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),255)
purple=c('7d3ca8'); lite=c('a86fd6'); dark=c('5a2580')
cx,cy=16,18
for x in range(S):
    for y in range(S):
        dx=(x-cx)/11.0; dy=(y-cy)/12.0
        d=dx*dx+dy*dy
        if d<=1.0:
            r=math.sqrt(d)
            if r>0.82: col=dark
            elif r>0.55: col=purple
            elif r>0.3: col=lite
            else: col=purple
            px[x,y]=col
for x in range(S):
    for y in range(S):
        if px[x,y]!=(0,0,0,0):
            if (x-cx) in (-6,-2,2,6) and y>cy-8:
                px[x,y]=dark
for (x,y) in [(16,5),(16,4),(15,5),(17,5),(14,3),(18,3),(15,4),(17,4)]:
    px[x,y]=lite
img.save('out32/tails.png')
PY
echo "=== out32 ==="
for f in out32/*.png; do printf "%-22s %-7s alpha=%s\n" "$(basename $f)" "$(magick identify -format '%wx%h' $f)" "$(magick $f -alpha extract -format '%[fx:mean]' info:)"; done
