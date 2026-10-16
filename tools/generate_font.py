"""Génère une police bitmap (format BMFont texte, lu par libGDX) à partir d'une police vectorielle.

Usage : python3 generate_font.py <police.otf|ttf> <taille_px> <dossier_sortie> <nom>

La police est rendue en grand (taille_px), en blanc sur fond transparent : libGDX la colore
et la réduit avec des mipmaps, ce qui garde un texte net à toutes les tailles.
"""
import sys
from PIL import Image, ImageDraw, ImageFont

src, size, out_dir, name = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
PAD = 4          # marge transparente autour de chaque glyphe (évite les bavures avec le filtrage)
ATLAS_W = 2048

chars = list(range(32, 127)) + list(range(160, 256)) + [0x152, 0x153, 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x20AC, 0x2022, 0x2192, 0x2190, 0x2191, 0x2193]
font = ImageFont.truetype(src, size)
ascent, descent = font.getmetrics()

glyphs = []
for code in chars:
    ch = chr(code)
    left, top, right, bottom = font.getbbox(ch, anchor="ls")
    w, h = max(0, right - left), max(0, bottom - top)
    glyphs.append((code, ch, left, top, w, h, round(font.getlength(ch))))

# Rangement en lignes, du plus haut au plus bas pour tasser l'atlas.
x = y = row_h = 0
placed = []
for g in sorted(glyphs, key=lambda g: -g[5]):
    code, ch, left, top, w, h, adv = g
    cw, chh = (w + 2 * PAD, h + 2 * PAD) if w and h else (0, 0)
    if x + cw > ATLAS_W:
        x, y, row_h = 0, y + row_h, 0
    placed.append((g, x, y, cw, chh))
    x += cw
    row_h = max(row_h, chh)
height = 1
while height < y + row_h:
    height *= 2

atlas = Image.new("RGBA", (ATLAS_W, height), (255, 255, 255, 0))
mask = Image.new("L", (ATLAS_W, height), 0)
draw = ImageDraw.Draw(mask)
lines = []
for (code, ch, left, top, w, h, adv), gx, gy, cw, chh in placed:
    if cw:
        draw.text((gx + PAD - left, gy + PAD - top), ch, font=font, fill=255, anchor="ls")
    xoff = left - PAD if cw else 0
    yoff = ascent + top - PAD if cw else 0
    lines.append(f"char id={code} x={gx} y={gy} width={cw} height={chh} xoffset={xoff} yoffset={yoff} xadvance={adv} page=0 chnl=15")
atlas.putalpha(mask)
atlas.save(f"{out_dir}/{name}.png", optimize=True)

# Interligne proportionnel à la hauteur des capitales, comme la police par défaut de libGDX
# (capitales de 11 px, lignes de 18 px) : les textes sur plusieurs lignes gardent leur mise en page.
xb = font.getbbox("X", anchor="ls")
cap = xb[3] - xb[1]
line_height = round(cap * 18 / 11)

with open(f"{out_dir}/{name}.fnt", "w", encoding="utf-8") as f:
    f.write(f'info face="{font.getname()[0]}" size={size} bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding={PAD},{PAD},{PAD},{PAD} spacing=0,0\n')
    f.write(f"common lineHeight={line_height} base={ascent} scaleW={ATLAS_W} scaleH={height} pages=1 packed=0\n")
    f.write(f'page id=0 file="{name}.png"\n')
    f.write(f"chars count={len(lines)}\n")
    f.write("\n".join(sorted(lines, key=lambda l: int(l.split()[1][3:]))) + "\n")

print(f"atlas {ATLAS_W}x{height}, {len(lines)} glyphes, hauteur des capitales = {cap} px")
