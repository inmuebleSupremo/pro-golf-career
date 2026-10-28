#!/usr/bin/env python3
"""Three environment surrounds using the exact approved Parkland putting core.

Run: python create_putting_environment_variants.py

Outputs six environment SVGs (Natural / Read Green) and two original Parkland SVGs.
All golf geometry, ball/cup positions, aiming, turf marks and guidance overlays are
extracted verbatim from the original V1 builder; this module owns scenery only.
Design-time artwork: no physics, real green contour or authoritative hazards.
Python standard library only.
"""
from __future__ import annotations
import math
import random
import re
from pathlib import Path
import create_premium_putting_svg as base

OUT=Path(__file__).resolve().parent
SEED=8821028
W,H=base.W,base.H

CONFIG={
    'links': {
        'ground': ['#695f40','#97835b','#b29a6c','#846e4b'],
        'wash': ['#ccb47e','#d2c18e','#897b53','#f0db9a','#8d875f'],
        'path': ['#635b46','#a99772','#d3bd8e','#e5d2ac'],
        'bunker': ['#ded0aa','#f4e6bd','#dfcc9b','#a78b6b'],
        'side': '#161a14',
    },
    'heathland': {
        'ground': ['#19281e','#42472d','#605c3b','#333c28'],
        'wash': ['#827d50','#91845a','#676f43','#c1aa75','#515337'],
        'path': ['#1e2921','#746e54','#c0ad82','#d6c59e'],
        'bunker': ['#a99d75','#e9dbb3','#ddc9a5','#9d8b69'],
        'side': '#091711',
    },
    'desert': {
        'ground': ['#694d36','#a57851','#c39b68','#926744'],
        'wash': ['#d6b580','#d7ad73','#e7c995','#895a42','#a67b58'],
        'path': ['#5a4539','#b29370','#d4bc91','#f0d5ab'],
        'bunker': ['#cba66f','#f3d9a5','#dfbd8b','#9c7550'],
        'side': '#211915',
    },
}


def polygon_clear(x,y,r,pts):
    """Reject vegetation/stone silhouettes crossing an outlined area, incl. margins."""
    return base.inside(x,y,pts) or min(base.distseg(x,y,a,b) for a,b in zip(pts,pts[1:]+pts[:1])) < r


def free(x,y,r):
    if polygon_clear(x,y,r+8,base.scaled(base.GREEN,1.185,1.2)):
        return False
    if min(base.distseg(x,y,a,b) for a,b in zip(base.WALK,base.WALK[1:])) < r+23:
        return False
    for b in base.BUNKERS:
        if polygon_clear(x,y,r+13,b):return False
    return True


def stroke_path(points,stroke,width,opacity=1):
    return base.path(base.smooth(points,False),stroke=stroke,sw=width,op=opacity)


def rock(x,y,rx,ry,rng,palette):
    """Irregular flat overhead rock with two material faces and a cast shadow."""
    n=8
    angles=[j*math.tau/n for j in range(n)]
    wob=[rng.uniform(.78,1.16) for _ in range(n)]
    pts=[(x+rx*w*math.cos(a),y+ry*w*math.sin(a)) for a,w in zip(angles,wob)]
    shadow=[(px+7,py+10) for px,py in pts]
    top=[(x+(px-x)*.79-2,y+(py-y)*.76-3) for px,py in pts]
    return ''.join((base.path(base.smooth(shadow),fill='#172217',op=.24),
                    base.path(base.smooth(pts),fill=palette[0]),
                    base.path(base.smooth(top),fill=palette[1],op=.83),
                    base.path(f'M {x-rx*.52:.1f} {y-ry*.17:.1f} q {rx*.62:.1f} {-ry*.46:.1f} {rx*1.08:.1f} {-ry*.04:.1f}',stroke=palette[2],sw=1.6,op=.22)))


def scrub(x,y,r,rng,palette,kind='shrub'):
    e=[]
    if kind=='fescue':
        for j in range(6):
            a=rng.uniform(-2.6,-.4)
            ln=r*rng.uniform(.7,1.7)
            dx=math.cos(a)*ln;dy=math.sin(a)*ln*.53
            e.append(base.path(f'M {x+rng.uniform(-3,3):.1f} {y+2:.1f} q {dx*.1:.1f} {dy*.4:.1f} {dx:.1f} {dy:.1f}',stroke=rng.choice(palette),sw=rng.uniform(.7,1.55),op=rng.uniform(.23,.67)))
    elif kind=='cactus':
        e.append(base.ell(x+3,y+4,r*.75,r*.49,'#291e16',.22))
        e.append(base.ell(x,y,r*.56,r*.8,palette[0],.92))
        e.append(base.ell(x-r*.16,y-r*.15,r*.24,r*.50,palette[1],.61))
        e.append(base.path(f'M {x:.1f} {y-r*.7:.1f} l 0 {r*1.32:.1f}',stroke=palette[2],sw=.65,op=.4))
    else:
        e.append(base.ell(x+3,y+5,r*1.15,r*.76,'#0e1410',.18))
        for k in range(6):
            a=k*math.tau/6+rng.uniform(-.28,.28)
            ox=math.cos(a)*r*.45;oy=math.sin(a)*r*.30
            col=palette[(k+int(x+y))%len(palette)]
            e.append(base.ell(x+ox,y+oy,r*rng.uniform(.32,.52),r*rng.uniform(.18,.38),col,rng.uniform(.68,.94)))
        e.append(base.ell(x-r*.22,y-r*.20,r*.38,r*.15,palette[-1],.40))
    return ''.join(e)


def heather(x,y,r,rng):
    # Layered shrub clusters, deliberately NOT large uniformly coloured circles.
    greens=['#4b5035','#5b5c3e','#373e31','#4d6140','#746a51']
    flowers=['#aa88b5','#b594c4','#92709c','#d1afd0','#805c99']
    e=[base.ell(x+5,y+8,r*1.4,r*.72,'#111b17',.23)]
    for j in range(10):
        ang=rng.uniform(0,math.tau); rad=rng.uniform(.06,.83)*r
        xx=x+math.cos(ang)*rad;yy=y+math.sin(ang)*rad*.48
        rr=r*rng.uniform(.19,.42)
        e.append(base.ell(xx,yy,rr,rr*.57,rng.choice(greens),.94))
        if rng.random()<.83:
            e.append(base.ell(xx-rr*.15,yy-rr*.28,rr*.65,rr*.29,rng.choice(flowers),rng.uniform(.63,.89)))
    return ''.join(e)


def gorse(x,y,r,rng):
    e=[base.ell(x+3,y+6,r*1.22,r*.75,'#101912',.18)]
    for j in range(9):
        xx=x+rng.uniform(-.85,.85)*r;yy=y+rng.uniform(-.5,.48)*r
        rr=r*rng.uniform(.18,.36)
        e.append(base.ell(xx,yy,rr,rr*.71,rng.choice(('#36482b','#47603a','#5a6a3c','#6c7440')), .86))
        if rng.random()<.62:
            e.append(base.ell(xx-rr*.24,yy-rr*.24,rr*.30,rr*.17,rng.choice(('#dac261','#e5d77a','#bb9f46')), .85))
    return ''.join(e)


def pine(x,y,r,rng):
    # Matches accepted Parkland/Heathland layered-canopy visual technique.
    return base.canopy(x,y,r,'pine',rng)


def raised_land(x,y,rx,ry,rng,palette):
    # Wide, understated rocky terrace, wholly off the golf corridor and path.
    points=[(x+rx*math.cos(a)*(1+rng.uniform(-.13,.10)),y+ry*math.sin(a)*(1+rng.uniform(-.15,.10))) for a in [j*math.tau/12 for j in range(12)]]
    inset=[(x+(px-x)*.86-2,y+(py-y)*.83-4) for px,py in points]
    return (base.path(base.smooth([(a+9,b+10) for a,b in points]),fill='#4c3628',op=.23)
            +base.path(base.smooth(points),fill=palette[0],op=.50)
            +base.path(base.smooth(inset),fill=palette[1],op=.42))


def scenery(env):
    cfg=CONFIG[env]
    rng=random.Random(SEED+{'links':11,'heathland':29,'desert':53}[env])
    s=['<g id="environment-scenery">', '<rect width="1600" height="900" fill="url(#ground)"/>']
    if env=='links':
        # Top-down exposed sandy grassland with long restrained dune transitions.
        for k in range(135):
            x=rng.uniform(-180,1780);y=rng.uniform(-110,1050)
            rx=rng.uniform(25,180);ry=rng.uniform(12,80)
            c=rng.choice(cfg['wash'])
            s.append(base.ell(x,y,rx,ry,c,rng.uniform(.018,.11),f'transform="rotate({rng.uniform(-27,27):.1f} {x:.1f} {y:.1f})"'))
        for i in range(22):
            y=rng.uniform(-30,950);shift=rng.uniform(-80,80)
            s.append(base.path(f'M -70 {y:.1f} C 290 {y+55:.1f} 570 {y-65+shift:.1f} 820 {y+10:.1f} S 1350 {y+36:.1f} 1690 {y-30:.1f}',stroke=rng.choice(('#e4d2a7','#6c6f50','#f1dea8')),sw=rng.uniform(1,4),op=rng.uniform(.05,.16)))
        # Soft berms and broad sandy dune edges, not a coastal landscape sticker.
        # Banded contours help this read as exposed links terrain at gameplay scale.
        for pts in [
            [(-65,95),(115,126),(254,210),(336,324),(408,386)],
            [(1200,-75),(1260,78),(1420,150),(1532,236),(1690,287)],
            [(-55,750),(146,700),(300,729),(426,818),(640,940)],
            [(1310,660),(1440,608),(1575,665),(1695,772)],
        ]:
            s.append(stroke_path(pts,'#b29c6d',74,.16))
            s.append(stroke_path(pts,'#eee0b2',3.5,.20))
            s.append(stroke_path([(x,y+14) for x,y in pts],'#5e674b',4,.15))
        # Sparse cropped neighbouring green corridor hints at consistent scale.
        s.append(stroke_path([(1540,-130),(1528,55),(1499,194),(1532,353),(1638,470)],'#70854d',102,.20))
        s.append(stroke_path([(-95,95),(10,225),(44,345),(-45,495)],'#8c9c5c',78,.15))
    elif env=='heathland':
        for k in range(168):
            x=rng.uniform(-150,1750);y=rng.uniform(-110,1000)
            rx=rng.uniform(25,139);ry=rng.uniform(11,75)
            s.append(base.ell(x,y,rx,ry,rng.choice(cfg['wash']),rng.uniform(.025,.12),f'transform="rotate({rng.uniform(-55,55):.1f} {x:.1f} {y:.1f})"'))
        for k in range(33):
            y=rng.uniform(-10,970);x=rng.uniform(-100,1550)
            s.append(base.path(f'M {x:.1f} {y:.1f} q 90 -24 203 3 t 140 28',stroke='#cfbb86',sw=rng.uniform(1,3.4),op=.12))
        s.append(stroke_path([(1580,-110),(1513,80),(1518,202),(1620,412)],'#73905d',84,.15))
    else:
        for k in range(134):
            x=rng.uniform(-170,1790);y=rng.uniform(-120,1020)
            rx=rng.uniform(18,155);ry=rng.uniform(10,88)
            s.append(base.ell(x,y,rx,ry,rng.choice(cfg['wash']),rng.uniform(.03,.14),f'transform="rotate({rng.uniform(-30,30):.1f} {x:.1f} {y:.1f})"'))
        # Dry-wash etched channels (not water/hazards) in the background.
        for pts in [[(-70,650),(95,640),(250,745),(475,815),(660,978)],[(1270,-75),(1390,60),(1475,275),(1660,456)],[(45,-85),(164,120),(210,275),(275,403)]]:
            s.append(stroke_path(pts,'#946d4b',37,.37));s.append(stroke_path(pts,'#e0ba85',21,.31))
        for x,y,rx,ry in [(1560,530,116,65),(110,180,94,51),(1450,817,106,69)]:
            if free(x,y,max(rx,ry)*.98):s.append(raised_land(x,y,rx,ry,rng,('#835c43','#d7aa76')))
        s.append(stroke_path([(1558,-105),(1515,80),(1482,194),(1573,420)],'#6d9650',94,.11))
    # Compact terrain textures, farther from green and hazards, never on putting surfaces.
    for i in range({'links':730,'heathland':540,'desert':520}[env]):
        x=rng.uniform(-30,1630);y=rng.uniform(-24,930)
        if not free(x,y,5):continue
        if env=='desert':
            s.append(base.path(f'M {x:.1f} {y:.1f} l {rng.uniform(-3,4):.1f} {rng.uniform(-1,3):.1f}',stroke=rng.choice(('#f1c997','#704b35','#e4bf8a')),sw=rng.uniform(.6,1.4),op=rng.uniform(.11,.31)))
        else:
            s.append(base.path(f'M {x:.1f} {y:.1f} q {rng.uniform(-4,1):.1f} {-rng.uniform(1,6):.1f} {rng.uniform(-6,6):.1f} {-rng.uniform(3,11):.1f}',stroke=rng.choice(('#b4b189','#c8ae82','#536441') if env=='links' else ('#93936e','#8b7656','#4c6844')),sw=rng.uniform(.5,1.25),op=rng.uniform(.15,.41)))
    # Compact access path in its approved location; constructed as worn-in trail with shoulders.
    width=(20,16,10,4) if env!='desert' else (27,21,12,5)
    for col,wd,opacity in zip(cfg['path'],width,(.55,.81,.75,.48)):
        s.append(stroke_path(base.WALK,col,wd,opacity))
    # Bunker shapes/positions and surface clipping preserved. Bunker materials only vary.
    # Draw after background, before vegetation/green, following original V1 layer order.
    for bi,b in enumerate(base.BUNKERS):
        cx=sum(p[0] for p in b)/len(b);cy=sum(p[1] for p in b)/len(b)
        ex=cfg['bunker']
        s.append(base.path(base.smooth(base.scaled(b,1.12,1.13,cx,cy)),fill=ex[0],op=.75))
        s.append(base.path(base.smooth(base.scaled(b,1.065,1.07,cx,cy)),fill=ex[3],op=.79))
        s.append(base.path(base.smooth(b),fill=ex[2],stroke=ex[3],sw=2))
        s.append(base.path(base.smooth(base.scaled(b,.965,.964,cx,cy)),fill='url(#bunker)'))
        s.append(base.path(base.smooth(base.scaled(b,.89,.87,cx,cy)),stroke=ex[1],sw=2,op=.34))
        s.append(f'<clipPath id="sand{bi}"><path d="{base.smooth(b)}"/></clipPath>')
        s.append(f'<g clip-path="url(#sand{bi})">')
        for k in range(35):
            xx=cx+rng.uniform(-82,82);yy=cy+rng.uniform(-75,75)
            s.append(base.path(f'M {xx-23:.1f} {yy-3:.1f} q 19 -9 40 -2',stroke=rng.choice((ex[3],ex[1])),sw=rng.uniform(.45,1.2),op=.15))
        s.append('</g>')
    # Region-specific material detail; deterministic, with silhouette-aware clearance.
    if env=='links':
        for j in range(600):
            x=rng.uniform(-20,1620);y=rng.uniform(-20,920);r=rng.uniform(6,16)
            if not free(x,y,r+4):continue
            s.append(scrub(x,y,r,rng,('#73754b','#b9b584','#d4c69b','#5a7050'),kind='fescue'))
        for j in range(85):
            x=rng.uniform(-20,1620);y=rng.uniform(0,900);r=rng.uniform(2.2,7)
            if free(x,y,r+4):s.append(rock(x,y,r,r*.6,rng,('#77765a','#c3b78a','#eee3bb')))
    elif env=='heathland':
        # Refined flowering heather, gorse and occasional pine groups; not purple stamps.
        # Heather/flower clusters occupy open acidic sandy heath in soft uneven beds.
        # Each bed is many small leaf+flower crowns, never a single purple patch.
        heath_centres=[(190,185,210,210),(1410,380,235,235),(1490,695,180,180),
                      (565,855,225,130),(90,580,160,240),(840,18,310,100)]
        count=0
        for j in range(980):
            if count>=315:break
            mx,my,sx,sy=rng.choice(heath_centres)
            x=rng.gauss(mx,sx*.55);y=rng.gauss(my,sy*.57)
            r=rng.uniform(9,26)
            if not (-25<=x<=1630 and -25<=y<=940) or not free(x,y,r+12):continue
            if rng.random()<.81:s.append(heather(x,y,r,rng))
            else:s.append(gorse(x,y,r*.83,rng))
            count+=1
        trees=[]
        for j in range(1000):
            if len(trees)>=85:break
            x=rng.uniform(-40,1660);y=rng.uniform(-10,930);r=rng.uniform(12,27)
            if not free(x,y,r*1.45+9):continue
            if min((math.hypot(x-xx,y-yy) for xx,yy,_ in trees),default=1e9)<r*1.8:continue
            if not(x<410 or x>1300 or y<55 or y>780):continue
            trees.append((x,y,r))
        for x,y,r in sorted(trees,key=lambda p:p[1]):s.append(pine(x,y,r,rng))
        for j in range(110):
            x=rng.uniform(-10,1610);y=rng.uniform(0,900);r=rng.uniform(3,12)
            if free(x,y,r+5):s.append(rock(x,y,r,r*.5,rng,('#6d654b','#af9568','#d1be83')))
    else:
        for j in range(260):
            x=rng.uniform(-20,1620);y=rng.uniform(-10,920);r=rng.uniform(4,15)
            if not free(x,y,r+10):continue
            if rng.random()<.17:
                s.append(scrub(x,y,r*.68,rng,('#3e623d','#6f9556','#a0ad77'),kind='cactus'))
            else:
                s.append(scrub(x,y,r,rng,('#394a36','#657548','#756d48','#8d8855'),kind='shrub'))
        for j in range(180):
            x=rng.uniform(-20,1620);y=rng.uniform(-10,920);r=rng.uniform(3,14)
            if free(x,y,r+6):s.append(rock(x,y,r,r*.62,rng,('#936c4c','#d2aa7a','#f5d1a2')))
        for j in range(48):
            x=rng.uniform(-20,1620);y=rng.uniform(-10,920);r=rng.uniform(16,29)
            if free(x,y,r*1.35) and (x<420 or x>1280 or y<100 or y>745):
                s.append(scrub(x,y,r,rng,('#4b6340','#60794c','#839568','#374d37'),kind='shrub'))
    s.append('</g>')
    return '\n'.join(s)


def shared_source(read:bool):
    """Return unaltered V1 green segment, rendering overlays and finishing filters."""
    original=base.build(read)
    title_start=original.index('<title id="title">')
    header=original[:original.index('</defs>')+len('</defs>')]
    start=base.path(base.smooth(base.scaled(base.GREEN,1.165,1.17)),fill='#183d23',op=.77)
    a=original.index(start)
    b=original.index('<rect width="1600" height="900" fill="url(#sideShade)" pointer-events="none"/>',a)
    core=original[a:b]
    finale=original[b:]
    assert 'clip-path="url(#greenClip)"' in core and 'stroke-dasharray' in core
    return header,core,finale


def svg(env,read):
    cfg=CONFIG[env]
    header,core,finale=shared_source(read)
    header=header.replace('Premium Parkland Putting Green — vector design prototype',f'Premium {env.title()} Putting Green — vector design prototype')
    header=header.replace('Strictly overhead putting green in mature woodland,',f'Strictly overhead putting green in {env} environment,')
    # Only environment background, bunker sand, and peripheral shadows change.
    colors=cfg['ground']
    ground=f'<linearGradient id="ground" x1="0" y1="0" x2=".68" y2="1"><stop offset="0" stop-color="{colors[0]}"/><stop offset=".35" stop-color="{colors[1]}"/><stop offset=".7" stop-color="{colors[2]}"/><stop offset="1" stop-color="{colors[3]}"/></linearGradient>'
    header=re.sub(r'<linearGradient id="ground".*?</linearGradient>',ground,header,count=1)
    bc=cfg['bunker'];bk=f'<linearGradient id="bunker" x1=".1" y1="0" x2=".8" y2="1"><stop offset="0" stop-color="{bc[1]}"/><stop offset=".57" stop-color="{bc[2]}"/><stop offset="1" stop-color="{bc[3]}"/></linearGradient>'
    header=re.sub(r'<linearGradient id="bunker".*?</linearGradient>',bk,header,count=1)
    # Identical falloff geometry and stop alpha values, warm hue where appropriate.
    # Avoid changing the accepted green illumination / green gradient.
    side=f'<linearGradient id="sideShade" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="{cfg["side"]}" stop-opacity=".82"/><stop offset=".19" stop-color="{cfg["side"]}" stop-opacity=".39"/><stop offset=".37" stop-color="{cfg["side"]}" stop-opacity="0"/><stop offset=".65" stop-color="{cfg["side"]}" stop-opacity="0"/><stop offset=".84" stop-color="{cfg["side"]}" stop-opacity=".32"/><stop offset="1" stop-color="{cfg["side"]}" stop-opacity=".86"/></linearGradient>'
    header=re.sub(r'<linearGradient id="sideShade".*?</linearGradient>',side,header,count=1)
    return header+'\n'+scenery(env)+'\n<g id="shared-putting-green">\n'+core+'</g>\n'+finale


def main():
    for read,mode in [(False,'natural'),(True,'read_green')]:
        # Exact approved Parkland V1, including its original source formatting.
        (OUT/f'premium_putting_parkland_{mode}.svg').write_text(base.build(read),encoding='utf-8')
        for env in CONFIG:
            txt=svg(env,read)
            p=OUT/f'premium_putting_{env}_{mode}.svg'
            p.write_text(txt,encoding='utf-8')
            print(p.name, len(txt),'bytes')

if __name__=='__main__':main()
