#!/usr/bin/env python3
"""Premium putting green visual prototype, shared turf with optional reading overlay.

Runs using Python standard library only. The putting contours/arrows here are
illustrative, NOT authoritative physics data: production must substitute the
existing game geometry/elevation/putting data. Geometry and aiming are static.

Usage: python create_premium_putting_svg.py
Creates premium_putting_natural.svg and premium_putting_read_green.svg next to this script.
"""
from __future__ import annotations
from pathlib import Path
import math
import random
import html

W,H = 1600,900
SEED = 281026
OUT=Path(__file__).resolve().parent

# Shared green anchors: outer surface, fringe and collar are derived from the
# SAME continuous outline. Do not add free-floating caps or disconnected blobs.
GREEN=[(810,115),(916,107),(1006,139),(1090,197),(1151,288),
       (1182,392),(1166,500),(1122,587),(1030,662),(933,707),
       (804,722),(695,698),(610,634),(565,551),(548,449),
       (569,346),(620,252),(702,181)]
CX,CY=865,420
BALL=(813,628)
CUP=(876,246)
BUNKERS=[
  [(366,575),(396,542),(438,546),(478,579),(488,622),(462,674),
   (411,687),(365,659),(345,618)],
  [(1172,188),(1207,153),(1252,141),(1293,165),(1303,204),(1292,244),
   (1247,266),(1198,256),(1165,226)]
]
# Reserve a realistic outside-the-fringe buffer for the upper-right bunker.
BUNKERS[1]=[(x+42,y-18) for x,y in BUNKERS[1]]
# Scenic walking path, outside entire maintained green-and-collar complex.
WALK=[(213,956),(246,812),(281,704),(304,605),(324,505),(342,401),
      (350,303),(371,220),(389,150),(422,62),(464,-48)]


def fmt(x):return f'{x:.2f}'.rstrip('0').rstrip('.') if isinstance(x,float) else str(x)
def smooth(points, closed=True):
    """Catmull--Rom to cubic Bezier; a single smooth SVG boundary."""
    p=[tuple(v) for v in points]; n=len(p)
    if not n:return ''
    out=[f'M {fmt(p[0][0])} {fmt(p[0][1])}']
    if closed:
        for i in range(n):
            a,b,c,d=p[(i-1)%n],p[i],p[(i+1)%n],p[(i+2)%n]
            c1=(b[0]+(c[0]-a[0])/6,b[1]+(c[1]-a[1])/6)
            c2=(c[0]-(d[0]-b[0])/6,c[1]-(d[1]-b[1])/6)
            out.append(f'C {fmt(c1[0])} {fmt(c1[1])} {fmt(c2[0])} {fmt(c2[1])} {fmt(c[0])} {fmt(c[1])}')
        return ' '.join(out)+' Z'
    for i in range(n-1):
        a=p[i-1] if i>0 else p[i]
        b=p[i];c=p[i+1];d=p[i+2] if i+2<n else p[i+1]
        c1=(b[0]+(c[0]-a[0])/6,b[1]+(c[1]-a[1])/6)
        c2=(c[0]-(d[0]-b[0])/6,c[1]-(d[1]-b[1])/6)
        out.append(f'C {fmt(c1[0])} {fmt(c1[1])} {fmt(c2[0])} {fmt(c2[1])} {fmt(c[0])} {fmt(c[1])}')
    return ' '.join(out)

def scaled(points,sx=1,sy=None,ox=CX,oy=CY):
    sy=sx if sy is None else sy
    return [(ox+(x-ox)*sx,oy+(y-oy)*sy) for x,y in points]

def path(d,fill='none',stroke=None,sw=1,op=None,extra=''):
    att=[f'd="{d}"',f'fill="{fill}"']
    if stroke:att += [f'stroke="{stroke}"',f'stroke-width="{fmt(sw)}"', 'stroke-linecap="round"','stroke-linejoin="round"']
    if op is not None:att.append(f'opacity="{fmt(op)}"')
    if extra:att.append(extra)
    return '<path '+' '.join(att)+'/>'

def ell(x,y,rx,ry,fill,op=None,other=''):
    return f'<ellipse cx="{fmt(x)}" cy="{fmt(y)}" rx="{fmt(rx)}" ry="{fmt(ry)}" fill="{fill}"'+(f' opacity="{fmt(op)}"' if op is not None else '')+(' '+other if other else '')+'/>'

def circ(x,y,r,fill,op=None,other=''):
    return f'<circle cx="{fmt(x)}" cy="{fmt(y)}" r="{fmt(r)}" fill="{fill}"'+(f' opacity="{fmt(op)}"' if op is not None else '')+(' '+other if other else '')+'/>'

def distseg(x,y,a,b):
    ax,ay=a;bx,by=b
    dx,dy=bx-ax,by-ay
    t=max(0,min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy or 1)))
    return math.hypot(x-ax-t*dx,y-ay-t*dy)

def inside(x,y,points):
    yes=False
    for i,(ax,ay) in enumerate(points):
        bx,by=points[i-1]
        if (ay>y)!=(by>y) and x<(bx-ax)*(y-ay)/(by-ay)+ax:
            yes=not yes
    return yes

def exclude(x,y,clear=0):
    if inside(x,y,scaled(GREEN,1.20,1.22)) or min(distseg(x,y,a,b) for a,b in zip(WALK[:-1],WALK[1:]))<clear+19:
        return True
    for b in BUNKERS:
        if inside(x,y,scaled(b,1.38,1.38,ox=sum(q[0] for q in b)/len(b),oy=sum(q[1] for q in b)/len(b))):
            return True
    return False


def canopy(x,y,r,kind,rng):
    # Leaf/canopy masses: soft cast shade + darker structural mass + irregular sunlit clusters.
    e=[]
    e.append(ell(x+8,y+12,r*1.16,r*.96,'#010b08',.28))
    e.append(circ(x,y,r,'#162a17' if kind=='decid' else '#1b2d20'))
    leaves=11 if kind=='decid' else 12
    palette=('#38572a','#466b32','#507638','#638345','#73934b','#294921') if kind=='decid' else ('#203b28','#2b5130','#345e36','#446f3c','#527847','#1a3826')
    for j in range(leaves):
        ang=(j/leaves)*math.tau+rng.uniform(-.16,.16)
        rr=r*rng.uniform(.31,.66)
        px=x+math.cos(ang)*rr;py=y+math.sin(ang)*rr*.78
        rad=r*rng.uniform(.37,.58)
        e.append(ell(px,py,rad,rad*rng.uniform(.70,1.03),palette[(j+int(x+y))%len(palette)]))
    for j in range(4):
        px=x+rng.uniform(-.52,.22)*r;py=y+rng.uniform(-.55,.03)*r
        e.append(ell(px,py,r*rng.uniform(.18,.31),r*.16,rng.choice(('#7da458','#6f934c','#587a41')), .48))
    return '<g>'+''.join(e)+'</g>'


def build(read:bool=False)->str:
    rng=random.Random(SEED)
    s=[];s.append('''<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900" role="img" aria-labelledby="title description">
<title id="title">Premium Parkland Putting Green — vector design prototype</title>
<desc id="description">Strictly overhead putting green in mature woodland, with continuous fringe, two integrated bunkers, shaded trees, cup and ball. All putting slope graphics are illustrative, not real gameplay data.</desc>
<defs>
 <linearGradient id="ground" x1="0" y1="0" x2=".68" y2="1"><stop offset="0" stop-color="#162918"/><stop offset=".35" stop-color="#24451e"/><stop offset=".7" stop-color="#234523"/><stop offset="1" stop-color="#10251a"/></linearGradient>
 <radialGradient id="light" cx="52%" cy="42%" r="67%"><stop offset="0" stop-color="#96c66d" stop-opacity=".30"/><stop offset=".65" stop-color="#517a43" stop-opacity=".10"/><stop offset="1" stop-color="#0b1a0e" stop-opacity="0"/></radialGradient>
 <linearGradient id="green" x1=".22" y1="0" x2=".85" y2="1"><stop offset="0" stop-color="#88bf62"/><stop offset=".42" stop-color="#75ae55"/><stop offset=".79" stop-color="#6aa34d"/><stop offset="1" stop-color="#679c49"/></linearGradient>
 <linearGradient id="collar" x1=".1" y1="0" x2=".95" y2="1"><stop offset="0" stop-color="#4e783a"/><stop offset=".58" stop-color="#436d35"/><stop offset="1" stop-color="#315c2d"/></linearGradient>
 <linearGradient id="fringe" x1="0" y1=".1" x2="1" y2="1"><stop offset="0" stop-color="#5c8e46"/><stop offset=".6" stop-color="#568540"/><stop offset="1" stop-color="#3c7235"/></linearGradient>
 <radialGradient id="greenShade" cx="38%" cy="31%" r="86%"><stop offset="0" stop-color="#d8ee9a" stop-opacity=".17"/><stop offset=".60" stop-color="#8cbd68" stop-opacity=".04"/><stop offset="1" stop-color="#112e22" stop-opacity=".23"/></radialGradient>
 <linearGradient id="bunker" x1=".1" y1="0" x2=".8" y2="1"><stop offset="0" stop-color="#f5e7c7"/><stop offset=".57" stop-color="#dbc9aa"/><stop offset="1" stop-color="#bfa989"/></linearGradient>
 <radialGradient id="edgeShade" cx="50%" cy="50%" r="72%"><stop offset=".55" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#06120d" stop-opacity=".86"/></radialGradient>
 <linearGradient id="sideShade" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#06110d" stop-opacity=".82"/><stop offset=".19" stop-color="#06110d" stop-opacity=".39"/><stop offset=".37" stop-color="#06110d" stop-opacity="0"/><stop offset=".65" stop-color="#06110d" stop-opacity="0"/><stop offset=".84" stop-color="#06110d" stop-opacity=".32"/><stop offset="1" stop-color="#06110d" stop-opacity=".86"/></linearGradient>
 <radialGradient id="ball" cx="31%" cy="25%" r="78%"><stop offset="0" stop-color="#ffffff"/><stop offset=".53" stop-color="#f7f8f0"/><stop offset="1" stop-color="#9aa49c"/></radialGradient>
 <clipPath id="greenClip"><path d="__GREEN_D__"/></clipPath>
 <clipPath id="collarClip"><path d="__COLLAR_D__"/></clipPath>
</defs>'''.replace('__GREEN_D__',smooth(GREEN)).replace('__COLLAR_D__',smooth(scaled(GREEN,1.106,1.115))))
    s.append('<rect width="1600" height="900" fill="url(#ground)"/>')
    # scenic land patches. Uneven but not a repeating stamped texture.
    for j in range(165):
        x=rng.uniform(-130,1730);y=rng.uniform(-120,1000);rx=rng.uniform(23,128);ry=rng.uniform(11,84)
        if inside(x,y,scaled(GREEN,1.1,1.1)):continue
        c=rng.choice(['#36582c','#2e502a','#55713a','#456638','#1b3926','#658545','#1c3b2b'])
        s.append(ell(x,y,rx,ry,c,rng.uniform(.035,.11),f'transform="rotate({rng.uniform(-45,45):.1f} {x:.1f} {y:.1f})"'))
    # Secondary landscape hints: subtle, partial maintained corridor fragments, not fake complete holes.
    s.append(path(smooth([(76,-65),(117,45),(143,170),(122,294),(74,384),(31,510),(-52,565)],False),stroke='#719a53',sw=145,op=.10))
    s.append(path(smooth([(1523,-40),(1450,76),(1425,211),(1484,355),(1594,467),(1660,571)],False),stroke='#7aab55',sw=146,op=.09))
    # pathway follows outside of the green and bunker: no sharp crossing, no switchbacks.
    s.append(path(smooth(WALK,False),stroke='#101e15',sw=24,op=.52))
    s.append(path(smooth(WALK,False),stroke='#5e6850',sw=17,op=.90))
    s.append(path(smooth(WALK,False),stroke='#a5a98c',sw=10,op=.66))
    s.append(path(smooth(WALK,False),stroke='#c1be97',sw=5,op=.49))
    # Bunkers physically recessed in the surrounding rough. Controlled geometry, not a separate shadow sticker.
    for bi,b in enumerate(BUNKERS):
        bx=sum(x for x,y in b)/len(b);by=sum(y for x,y in b)/len(b)
        s.append(path(smooth(scaled(b,1.12,1.13,bx,by)),fill='#203720',op=.74))
        s.append(path(smooth(scaled(b,1.065,1.07,bx,by)),fill='#7c8e55'))
        s.append(path(smooth(b),fill='#b6a07c',stroke='#d0bf94',sw=2))
        s.append(path(smooth(scaled(b,.965,.964,bx,by)),fill='url(#bunker)'))
        s.append(path(smooth(scaled(b,.89,.87,bx,by)),stroke='#f9e9c8',sw=2.5,op=.38))
        # raking lines are clipped via bunker shape, without contaminating nearby turf.
        s.append(f'<clipPath id="sand{bi}"><path d="{smooth(b)}"/></clipPath>')
        s.append(f'<g clip-path="url(#sand{bi})">')
        for q in range(35):
            xx=bx+rng.uniform(-82,82);yy=by+rng.uniform(-75,75)
            s.append(path(f'M {xx-23:.1f} {yy-3:.1f} q 19 -9 40 -2',stroke=rng.choice(['#987d60','#fff6e3']),sw=rng.uniform(.45,1.2),op=.15))
        s.append('</g>')
    # Trees around the perimeter. Canopy shadows respect putting and bunker clearance.
    coords=[]
    for t in range(2400):
        if len(coords)>=295: break
        x=rng.uniform(-85,1695);y=rng.uniform(-90,990)
        r=rng.uniform(14,36)
        # favor clusters at stage edges and in the green's surrounding woodland
        if not (x<460 or x>1270 or y<100 or y>745): continue
        if exclude(x,y,r+9):continue
        if min((math.hypot(x-tx,y-ty) for tx,ty,tr in coords),default=1e9)<r*.86:continue
        coords.append((x,y,r))
    # Draw farther canopies first for natural overlap.
    coords.sort(key=lambda p:p[1])
    for x,y,r in coords:
        s.append(canopy(x,y,r,'pine' if rng.random()<.35 else 'decid',rng))
    # Smaller undergrowth between trees and green.
    for q in range(410):
        x=rng.uniform(0,W);y=rng.uniform(0,H)
        if exclude(x,y,12):continue
        rad=rng.uniform(1.4,6.0)
        s.append(ell(x,y,rad,rad*.7,rng.choice(['#668b4a','#365a37','#7b8d49','#1b442b']),rng.uniform(.12,.36)))
    # Golf green: shared continuous organic bands, not an overlaid green sticker.
    s.append(path(smooth(scaled(GREEN,1.165,1.17)),fill='#183d23',op=.77))
    s.append(path(smooth(scaled(GREEN,1.11,1.117)),fill='url(#collar)'))
    s.append(path(smooth(scaled(GREEN,1.055,1.062)),fill='url(#fringe)'))
    s.append(path(smooth(scaled(GREEN,1.025,1.025)),fill='#55873f'))
    s.append(path(smooth(GREEN),fill='url(#green)'))
    s.append(path(smooth(GREEN),fill='url(#greenShade)'))
    # Subtle turf highlights, natural directional mowing width, clipped to green.
    s.append('<g clip-path="url(#greenClip)">')
    for t in range(13):
        xpos=420+t*80
        s.append(path(f'M {xpos} -30 C {xpos-88} 205 {xpos+90} 470 {xpos+13} 980',stroke='#d1e8a0' if t%2 else '#2e6534',sw=rng.randint(30,76),op=.047 if t%2 else .043))
    # Tiny overlapping cut-grass tonal patches, never isolated decorative marks on turf.
    for t in range(430):
        x=rng.uniform(530,1210);y=rng.uniform(110,740)
        if not inside(x,y,GREEN):continue
        s.append(ell(x,y,rng.uniform(7,28),rng.uniform(2,8),rng.choice(['#cde2a2','#37763b','#a5cb7c']),rng.uniform(.011,.023)))
    for t in range(280):
        x=rng.uniform(530,1210);y=rng.uniform(110,740)
        if not inside(x,y,GREEN):continue
        s.append(path(f'M {x:.1f} {y:.1f} l {rng.uniform(-1.8,2.1):.2f} {rng.uniform(3,8):.2f}',stroke=rng.choice(['#def2b4','#2a6735','#f0f6c8']),sw=rng.uniform(.4,1),op=rng.uniform(.045,.095)))
    # Contours illustrative only; actual production contour positions must use backend slope data.
    for i in range(10):
        y=170+61*i
        pts=[(365+x*51,y+47*math.sin((365+x*51)/175+(i*.34))+8*math.sin((365+x*51)/83+i*.3)) for x in range(22)]
        s.append(path(smooth(pts,False),stroke='#e5f3ba' if i%2==0 else '#b1d997',sw=1.6 if read else 1.0,op=.32 if read else .11))
    s.append('</g>')
    # Slight aerial specular illumination toward cup. Good readability without perspective tilt.
    s.append(ell(826,240,215,115,'#e4f7ae',.040))
    # Read Green optional overlay: additional soft contours and subdued slope arrows.
    if read:
        s.append('<g clip-path="url(#greenClip)">')
        for row in range(5):
            for col in range(5):
                x=620+col*125+(row%2)*21; y=230+row*94
                if not inside(x,y,GREEN) or math.hypot(x-CUP[0],y-CUP[1])<50 or math.hypot(x-BALL[0],y-BALL[1])<46:continue
                ang=-.25+0.3*math.sin(x/170+y/110)
                s.append(f'<g transform="translate({x} {y}) rotate({math.degrees(ang):.1f})" opacity=".47">')
                s.append(path('M -9 -2 L 5 -2 L 5 -7 L 16 1 L 5 9 L 5 4 L -9 4 Z',fill='#b9dd9d'))
                s.append('</g>')
        s.append('</g>')
    # Aim line remains controlled by game in production, not a precomputed roll trajectory.
    aim=f'M {BALL[0]} {BALL[1]} L {CUP[0]} {CUP[1]}'
    s.append(path(aim,stroke='#132c1b',sw=6.0,op=.33))
    s.append(path(aim,stroke='#eef6e5',sw=2.9,op=.92,extra='stroke-dasharray="8 12"'))
    # Cup and subtle surrounding shadow, then flag pole.
    s.append(ell(CUP[0]+2,CUP[1]+4,15,10,'#223e23',.38))
    s.append(circ(CUP[0],CUP[1],12,'#c8d9b0',.67))
    s.append(circ(CUP[0],CUP[1],8,'#0c1811'))
    s.append(circ(CUP[0]-2,CUP[1]-2,3,'#384c35',.74))
    s.append(path(f'M {CUP[0]+1} {CUP[1]-3} v -48',stroke='#f3eee1',sw=2.6,op=.94))
    s.append(path(f'M {CUP[0]+3} {CUP[1]-51} L {CUP[0]+28} {CUP[1]-43} L {CUP[0]+3} {CUP[1]-35} Z',fill='#e45d48'))
    # Ball, shadow and dimples.
    s.append(ell(BALL[0]+4,BALL[1]+7,17,8,'#142d1d',.48))
    s.append(circ(BALL[0],BALL[1],12,'url(#ball)'))
    for a,b in [(-5,-4),(-1,-6),(4,-4),(-6,1),(1,0),(6,3),(-3,5)]:
        s.append(circ(BALL[0]+a,BALL[1]+b,1.1,'#b6bdad',.42))
    # Linear side falloff matches accepted course-family scene, with soft corner vignette.
    s.append('<rect width="1600" height="900" fill="url(#sideShade)" pointer-events="none"/>')
    s.append('<rect width="1600" height="900" fill="url(#edgeShade)" pointer-events="none" opacity=".58"/>')
    # keep ball and cup legible atop lighting, no raster photos.
    s.append('</svg>')
    return '\n'.join(s)


def main():
    for read,name in [(False,'premium_putting_natural.svg'),(True,'premium_putting_read_green.svg')]:
        data=build(read)
        p=OUT/name;p.write_text(data,encoding='utf-8')
        print(f'{name}: {len(data):,} bytes, {data.count("<path "):,} paths, {data.count("<ellipse "):,} ellipses')
if __name__=='__main__':main()
