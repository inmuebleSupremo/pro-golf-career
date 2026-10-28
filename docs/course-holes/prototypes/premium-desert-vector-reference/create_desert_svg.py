"""Premium Desert -- editable, deterministic, native SVG visual prototype.

This is ARTWORK, not an authoritative golf engine. Fairway, greens and hazards
are illustrative. The rendering methods and geometry protection are intended
for later integration with canonical generated Desert course geometry.

Generation dependencies: numpy, scipy, shapely. No raster images, bitmap
textures or foreign objects are embedded in the SVG.
"""
from __future__ import annotations
import math, random
from pathlib import Path
from collections import defaultdict
import numpy as np
from scipy.interpolate import CubicSpline
from shapely.geometry import Point, Polygon, LineString
from shapely.ops import unary_union
from shapely.affinity import scale as geom_scale, affine_transform, rotate as geom_rotate, translate as geom_translate

W,H=1600,900
OUT=Path(__file__).with_name('premium_desert_vector.svg')
rng=random.Random(281028)
S=[]
stat=defaultdict(int)
def F(v): return f'{v:.2f}'
def add(v): S.append(v)
def path(d, fill='none', stroke=None, sw=1, opacity=None, extra=''):
    props=[f'd="{d}"',f'fill="{fill}"']
    if stroke: props.extend([f'stroke="{stroke}"', f'stroke-width="{F(sw)}"', 'stroke-linecap="round"', 'stroke-linejoin="round"'])
    if opacity is not None:props.append(f'opacity="{F(opacity)}"')
    if extra:props.append(extra)
    add('<path '+' '.join(props)+'/>')
    stat['paths']+=1

def ell(x,y,rx,ry,fill,op=1, extra=''):
    add(f'<ellipse cx="{F(x)}" cy="{F(y)}" rx="{F(rx)}" ry="{F(ry)}" fill="{fill}" opacity="{F(op)}" {extra}/>')
    stat['ellipses']+=1

def circ(x,y,r,fill,op=1,extra=''):ell(x,y,r,r,fill,op,extra)

def organic(cx,cy,rx,ry,seed=1,wig=.12,n=25,rotation=0):
    pts=[]
    for i in range(n):
        t=math.tau*i/n
        rad=1+wig*math.sin(3*t+seed*.63)+wig*.45*math.sin(7*t+seed*1.12)+wig*.22*math.cos(11*t+seed*.77)
        xx=rx*rad*math.cos(t); yy=ry*rad*math.sin(t)
        rot=math.radians(rotation)
        pts.append((cx+xx*math.cos(rot)-yy*math.sin(rot),cy+xx*math.sin(rot)+yy*math.cos(rot)))
    sx=(pts[0][0]+pts[-1][0])/2;sy=(pts[0][1]+pts[-1][1])/2
    d=f'M {F(sx)} {F(sy)}'
    for i,p in enumerate(pts):
        npt=pts[(i+1)%len(pts)]
        d+=f' Q {F(p[0])} {F(p[1])} {F((p[0]+npt[0])/2)} {F((p[1]+npt[1])/2)}'
    return d+' Z'

def x_spline(points):
    points=sorted(points,key=lambda p:p[1]); ar=np.array(points,float)
    spline=CubicSpline(ar[:,1],ar[:,0],bc_type='natural')
    return lambda y:float(spline(y))

def bands(xs,bottom,top,widths,cap_depths,count=136):
    """Construct nested corridors using a SINGLE continuous polygon per band.

    Every band turns smoothly around behind the green (not a detached cap).
    Rectangular closure is allowed only below the visible tee at viewport edge.
    """
    ys=np.linspace(bottom,top,count)
    defs=[]
    for width,depth in zip(widths,cap_depths):
        left=[];right=[]
        for yf in ys:
            y=float(yf); x=xs(y)
            tangent=xs(y+.6)-xs(y-.6)
            norm=math.hypot(1,tangent)
            r=width(y) if callable(width) else width
            left.append((x-r/norm,y+tangent*r/norm))
            right.append((x+r/norm,y-tangent*r/norm))
        xt=xs(top);r=width(top) if callable(width) else width
        tangent=xs(top+.6)-xs(top-.6);norm=math.hypot(1,tangent)
        nx=1/norm;ny=-tangent/norm
        cap=[(xt+r*math.cos(t)*nx,top+r*math.cos(t)*ny-depth*math.sin(t)) for t in np.linspace(math.pi,0,34)]
        polypts=left+cap+list(reversed(right))
        d='M '+' L '.join(f'{F(x)} {F(y)}' for x,y in polypts)+' Z'
        p=Polygon(polypts).buffer(0)
        defs.append((d,p))
    return defs

# Active dogleg par 4 adapted from supplied Desert Hole 3 proportions.
main_x=x_spline([(811,1000),(803,900),(776,796),(748,672),(751,560),(803,447),(886,329),(910,245),(893,183),(885,174)])
main=bands(main_x,965,190,
 [lambda y:107+7*math.cos(y/160),lambda y:92+6*math.cos(y/160),lambda y:76+5*math.cos(y/160),lambda y:59+4*math.cos(y/160),lambda y:48+3*math.cos(y/160)],
 [94,88,80,72,65])
# Left flanking straight par 5: upper tee outside stage, green near lower edge.
# For real rounded green-end, reflect a top-capped corridor in Y.
left_x=x_spline([(320,1110),(315,975),(310,822),(332,650),(332,500),(316,321),(318,145)])
left=bands(left_x,1030,155,[87,77,66,54,42],[76,73,69,64,55])
left_round=[(d,affine_transform(poly,[1,0,0,-1,0,900])) for d,poly in left]
# Right flank par 3, green on stage, tee off bottom (narrow visible section).
right_x=x_spline([(1338,1010),(1322,854),(1310,681),(1280,500),(1276,366),(1300,235)])
right=bands(right_x,997,240,[86,77,65,50,41],[70,66,61,56,50])

# Hazards are illustrative; all are kept separate from routes and decoration.
bunkers=[(815,332,23,33,11),(971,236,25,32,-14),(720,510,18,33,5),(888,597,22,34,-19),(737,748,25,37,9),
         (245,565,20,29,0),(397,500,22,29,-7),(240,774,20,27,13),
         (1211,385,23,32,-10),(1378,310,24,28,13)]
bunker_polys=[geom_scale(Point(x,y).buffer(1,resolution=50),rx+6,ry+6,origin=(x,y)) for x,y,rx,ry,_ in bunkers]
# outer-bands cover all five for every course + hazard margins
holes=[main[0][1],left_round[0][1],right[0][1]]
protected=unary_union(holes+bunker_polys)
# 4. Access routes: no crossing through maintained outer band, hazards, any neighbor.
# Curves are *not* purely cosmetic connecting strokes; the geometry is validated below.
# routing near right edge of central hole, out to the green, then continues offstage.
route_vertices={
 'main-access':[(1000,940),(1008,840),(991,744),(965,655),(973,569),(982,462),(1055,348),(1064,238),(1056,130),(1051,-40)],
 # passes to left of far-right hole, up to shared access on green side
 'right-access':[(1134,-40),(1133,70),(1135,160),(1149,281),(1157,400),(1171,520),(1154,679),(1146,815),(1154,955)],
 # passes to right of far-left playing corridor, with green-end access toward bottom
 'left-access':[(435,-40),(434,103),(435,250),(441,380),(446,510),(433,634),(447,769),(449,950)],
 # links all three access tracks behind the active green, via traversable bare ground
 'property-link':[(433,49),(570,38),(720,36),(863,36),(1051,48),(1133,57)],
}
# Build C1-continuous cubic spline strokes and validate line buffers against hazards.
def route_xy(anchors,n=160):
    P=np.array(anchors,float); lengths=np.sqrt(np.sum(np.diff(P,axis=0)**2,axis=1))
    t=np.r_[0,np.cumsum(lengths)]
    xs=CubicSpline(t,P[:,0],bc_type='natural');ys=CubicSpline(t,P[:,1],bc_type='natural')
    return [(float(xs(z)),float(ys(z))) for z in np.linspace(0,t[-1],n)]
route_sample={k:route_xy(v) for k,v in route_vertices.items()}
for key,pts in route_sample.items():
    line=LineString(pts)
    intrusion=line.buffer(3.6).intersection(protected.buffer(4))
    if not intrusion.is_empty and intrusion.area>1.0:
        print('ROUTE OVERLAP',key,'area=',round(intrusion.area,1))
        # don't silently conceal impossible routes behind golf surfaces
        raise RuntimeError(f'{key} crosses the protected golf architecture')
# Reserve the full width and shoulders of each path from raised bedrock and shrubs.
# The outer shoulders are deliberately kept open so the path follows natural bare terrain.
route_protected=unary_union([LineString(points).buffer(15) for points in route_sample.values()])
# Only decorative terrain and vegetation are protected here; the canonical hole regions
# continue to use the original hazard and surface geometry unchanged.
decor_protected=unary_union([protected,route_protected])
def clear_geom(shape,padding=0):return not decor_protected.buffer(padding).intersects(shape)
def clear_disc(x,y,r,margin=0):return clear_geom(Point(x,y).buffer(r),margin)

add('''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900" role="img" aria-labelledby="title desc">
<title id="title">Premium Desert — overhead procedural SVG visual prototype</title>
<desc id="desc">Strictly overhead, stylised vector desert golf property with bright central dogleg, continuous nested green surrounds, complete neighbouring golf holes, rocky terrain, desert scrub and linked access routes. Geometry illustrative, not gameplay authoritative.</desc>
<defs>
 <linearGradient id="desert" x1="0" y1="0" x2="1" y2=".65"><stop stop-color="#aa8357"/><stop offset=".38" stop-color="#ca9d62"/><stop offset=".65" stop-color="#bc8957"/><stop offset="1" stop-color="#9a714e"/></linearGradient>
 <linearGradient id="gravel" x1="0" y1="0" x2=".65" y2="1"><stop stop-color="#d4ac72"/><stop offset=".4" stop-color="#b99160"/><stop offset="1" stop-color="#94724b"/></linearGradient>
 <linearGradient id="redrock" x1="0" y1="0" x2=".7" y2="1"><stop stop-color="#c88e60"/><stop offset=".4" stop-color="#aa6d49"/><stop offset="1" stop-color="#654636"/></linearGradient>
 <linearGradient id="turf" x1="0" y1="0" x2="1" y2=".24"><stop stop-color="#659946"/><stop offset=".23" stop-color="#89bf5a"/><stop offset=".51" stop-color="#a9d66d"/><stop offset=".78" stop-color="#92c460"/><stop offset="1" stop-color="#649849"/></linearGradient>
 <linearGradient id="green" x1="0" y1="0" x2="1" y2=".18"><stop stop-color="#8fca70"/><stop offset=".52" stop-color="#b9e790"/><stop offset="1" stop-color="#85ba61"/></linearGradient>
 <linearGradient id="sand" x1="0" y1="0" x2=".65" y2="1"><stop stop-color="#fae8b9"/><stop offset=".56" stop-color="#e7cb94"/><stop offset="1" stop-color="#b99a66"/></linearGradient>
 <linearGradient id="shrub" x1="0" y1="0" x2=".8" y2="1"><stop stop-color="#75965e"/><stop offset=".38" stop-color="#58794a"/><stop offset="1" stop-color="#294d36"/></linearGradient>
 <linearGradient id="light" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#0a100c" stop-opacity=".96"/><stop offset=".11" stop-color="#1a160f" stop-opacity=".80"/><stop offset=".25" stop-color="#241d12" stop-opacity=".44"/><stop offset=".39" stop-color="#392a18" stop-opacity=".08"/><stop offset=".49" stop-color="#000000" stop-opacity="0"/><stop offset=".59" stop-color="#000000" stop-opacity="0"/><stop offset=".75" stop-color="#1b170e" stop-opacity=".44"/><stop offset=".89" stop-color="#0f130e" stop-opacity=".83"/><stop offset="1" stop-color="#060b09" stop-opacity=".97"/></linearGradient>
 <linearGradient id="toplight" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#0a1410" stop-opacity=".18"/><stop offset=".18" stop-color="#000000" stop-opacity="0"/><stop offset=".84" stop-color="#000000" stop-opacity="0"/><stop offset="1" stop-color="#07110d" stop-opacity=".1"/></linearGradient>
</defs>''')

# 1. Continuous desert terrain (no rectangular panel), rocky washes, hills.
add('<g id="desert-ground">')
add('<rect width="1600" height="900" fill="url(#desert)"/>')
# Depositional gravel fans, non-hazard geology.
for i,(x,y,rx,ry) in enumerate([(115,120,290,225),(590,90,350,215),(1130,160,370,240),(1480,530,275,295),(390,677,365,265),(1040,680,335,320),(810,425,275,230)]):
    path(organic(x,y,rx,ry,i*11+3,.10,35,rotation=(i*13)%30),fill=['#d4ad75','#e0b778','#956a48','#b78a58'][i%4],opacity=[.34,.26,.26,.18][i%4])
    path(organic(x+10,y-14,rx*.72,ry*.73,i*10+7,.10,32),fill=['#dabb83','#8f6548','#e3c388'][i%3],opacity=.24)
# Large eroded rock benches/shadowed shelves, strategically outside corridors.
mesa_centers=[(95,205,220,165,-17),(490,175,140,88,14),(1060,164,122,100,-11),(1545,275,230,210,-12),
              (185,730,185,170,-8),(531,675,142,110,19),(1153,685,173,125,-18),(1475,790,240,185,-12),
              (1090,470,92,75,13),(555,390,81,65,-13)]
def rock_bench_footprint(x,y,rx,ry,rot):
    # Conservatively envelopes the natural (irregular) outline, including cast-shadow margin.
    unit=Point(0,0).buffer(1,resolution=40)
    return geom_translate(geom_rotate(geom_scale(unit,rx*1.42,ry*1.46),rot,origin=(0,0)),xoff=x+11,yoff=y+7)

def terrain_fitted_bench(x,y,rx,ry,rot):
    # A raised outcrop may never be drawn across a golf-access route or a golf hole.
    # Locate it nearby on clear desert, shrinking only where the corridor is constrained.
    if not route_protected.buffer(5).intersects(rock_bench_footprint(x,y,rx,ry,rot)):
        return x,y,rx,ry
    for scale in (.86,.74,.62,.52,.42,.34):
        for shift in (0,.55,.95,1.3,1.75,2.2):
            for ux,uy in ((-1,0),(1,0),(0,-1),(0,1),(-.71,-.71),(.71,-.71),(-.71,.71),(.71,.71)):
                px=x+ux*rx*scale*shift
                py=y+uy*ry*scale*shift
                if not route_protected.buffer(5).intersects(rock_bench_footprint(px,py,rx*scale,ry*scale,rot)):
                    return px,py,rx*scale,ry*scale
    return None

repositioned_benches=0
retained_benches=0
for j,(x,y,rx,ry,rot) in enumerate(mesa_centers):
    fitted=terrain_fitted_bench(x,y,rx,ry,rot)
    if fitted is None:continue
    if fitted != (x,y,rx,ry):repositioned_benches+=1
    x,y,rx,ry=fitted
    retained_benches+=1
    path(organic(x+16,y+21,rx*1.18,ry*1.2,21+j,.17,30,rot),fill='#523f32',opacity=.24)
    path(organic(x,y,rx,ry,7+j,.18,36,rot),fill='url(#redrock)',opacity=.63)
    path(organic(x-12,y-19,rx*.78,ry*.64,30+j,.11,30,rot),fill='#d6a371',opacity=.40)
    path(organic(x-17,y-23,rx*.62,ry*.49,42+j,.10,28,rot),fill='#ecc28a',opacity=.22)
    # A few natural erosion seams. Avoid parallel contour striping.
    for k in range(2):
        sx=x-rx*.42+k*rx*.31;sy=y+ry*.30+k*ry*.16
        path(f'M {F(sx)} {F(sy)} q {F(rx*.18)} {F(ry*.14)} {F(rx*.48)} {F(-ry*.12)}',stroke='#503a30',sw=1.6,opacity=.27)
# Dry arroyo occupies clear ground and is NOT blue nor depicted as gameplay water.
path('M 550 -70 C 578 64 594 163 559 279 C 540 371 580 473 547 581 C 532 655 565 796 608 969',stroke='#624d39',sw=69,opacity=.12)
path('M 550 -70 C 578 64 594 163 559 279 C 540 371 580 473 547 581 C 532 655 565 796 608 969',stroke='#e4bf8c',sw=48,opacity=.31)
path('M 550 -70 C 578 64 594 163 559 279 C 540 371 580 473 547 581 C 532 655 565 796 608 969',stroke='#f1d5a0',sw=16,opacity=.22)
# Ground gravel / gritty soil in batched paths rather than expensive filters.
for style in [('#594b37',.14,1.4,320),('#f0d1a0',.24,1.2,350),('#88684a',.22,1.0,460)]:
    color,op,sw,count=style
    pieces=[]
    for i in range(count):
        x=rng.uniform(-25,1625);y=rng.uniform(-20,920);long=rng.uniform(1.3,5.7)
        pieces.append(f'M {F(x)} {F(y)} l {F(long*.8)} {F(-long*.32)}')
    path(' '.join(pieces),stroke=color,sw=sw,opacity=op)
add('</g>')

# 2. Subordinate holes and active hole: same five nested continuous band contours.
# Widest to narrowest: waste/outer transition -> rough -> first cut -> turf -> close-mown fairway.
# Bunkers are real voids in the band artwork: excavate their footprints once before
# drawing the sand floors. This avoids floating "stickers" on uninterrupted fairway.
add('<defs><mask id="course-bunker-cutouts" maskUnits="userSpaceOnUse" x="0" y="0" width="1600" height="900">')
add('<rect x="0" y="0" width="1600" height="900" fill="white"/>')
for bx,by,brx,bry,angle in bunkers:
    add(f'<g transform="translate({F(bx)} {F(by)}) rotate({angle})">')
    path(organic(0,0,brx*1.035,bry*1.025,12+abs(angle),.095,32),fill='#000000')
    add('</g>')
add('</mask></defs>')
def draw_bands(defs,ident,active=False,op=1):
    add(f'<g id="{ident}" data-rounded-five-bands="true">')
    if active:
        styles=[('#c39b69',.90),('#5c7c42',.99),('#6f9c47',1),('#80b250',1),('url(#turf)',1)]
    else:
        styles=[('#b79768',.72),('#4a7040',.94),('#648a47',.95),('#70a04b',.97),('#7eb156',.99)]
    for (d,_),(fill,a) in zip(defs,styles):path(d,fill=fill,opacity=op*a)
    add('</g>')

# Flanking holes painted first, with green ends inside the same smooth layered contours.
add('<g mask="url(#course-bunker-cutouts)">')
add('<g id="left-neighbour-reflected" transform="translate(0 900) scale(1 -1)">')
draw_bands(left,'left-hole-bands',op=.75)
add('</g>')
draw_bands(right,'right-hole-bands',op=.79)
draw_bands(main,'active-hole-bands',active=True)
add('</g>')

# Subtle fairway stripe treatment clipped to each real band rather than rectangle.
add('<defs>')
for name,defs in [('active',main),('left',left),('right',right)]:
    add(f'<clipPath id="{name}-fairway"><path d="{defs[4][0]}"/></clipPath>')
add('</defs>')
add('<g id="mowing" opacity=".13" clip-path="url(#active-fairway)">')
for y in range(-50,960,39):
    path(f'M 575 {y} Q 850 {y+12} 1070 {y-4}',stroke='#ebf8b9',sw=21,opacity=.40)
add('</g>')
# before fringe/greens fine bunker / green surroundings

def green(x,y,rx,ry,seed=1,op=1,rotation=0):
    add(f'<g opacity="{F(op)}">')
    path(organic(x,y,rx*1.24,ry*1.30,seed+2,.065,26,rotation),fill='#557b41',opacity=.49)
    path(organic(x,y,rx*1.10,ry*1.16,seed+4,.065,26,rotation),fill='#7cad57',opacity=.83)
    path(organic(x,y,rx,ry,seed+1,.065,28,rotation),fill='url(#green)')
    path(f'M {F(x-rx*.64)} {F(y-ry*.16)} Q {F(x)} {F(y-ry*.73)} {F(x+rx*.67)} {F(y-ry*.20)}',stroke='#e0f4aa',sw=1.5,opacity=.30)
    add('</g>')

# Active green lies WITHIN all continuous bands, not dangling beyond a blobbed cap.
green(904,211,36,46,3)
# left green is reflected and below within left hole, right green near top
green(321,742,34,43,8,.78)
green(1300,252,32,45,10,.79)

# Canonical-style bunker footprints (illustrative) are shaded, with irregular compact edges.
def bunker(x,y,rx,ry,rot,op=1):
    # This is a turf excavation, not a floating sand oval. Its rough/turf shoulder
    # adopts the material it interrupts and the sand is seated inside a shaded lip.
    center=Point(x,y)
    band_index=-1
    for hole in (main,left_round,right):
        for i in range(5):
            if hole[i][1].contains(center):band_index=max(band_index,i)
    if band_index>=4:shoulder='#84b758'; transition='#7a9e51'
    elif band_index>=3:shoulder='#79a551';transition='#678d48'
    elif band_index>=1:shoulder='#80904c';transition='#6a7541'
    elif band_index==0:shoulder='#b79a67';transition='#aa8758'
    else:shoulder='#bb9362';transition='#a77d52'
    add(f'<g transform="translate({F(x)} {F(y)}) rotate({rot})" opacity="{F(op)}">')
    # The broad, irregular shoulder blends INTO neighbouring turf rather than casting
    # a big external oval shadow. A thin inward shadow implies a recessed sand floor.
    path(organic(0,0,rx*1.17,ry*1.145,12+abs(rot),.09,32),fill=shoulder,opacity=.54)
    path(organic(0,0,rx*1.095,ry*1.08,12+abs(rot),.09,32),fill=transition,opacity=.58)
    path(organic(0,0,rx*1.045,ry*1.035,12+abs(rot),.095,32),fill='#705a3f',opacity=.43)
    path(organic(0,0,rx,ry,12+abs(rot),.095,32),fill='url(#sand)')
    # Shallow cut face is INSIDE the bunker, not a shadow laid over the fairway.
    path(f'M {F(-rx*.74)} {F(ry*.32)} Q {F(-rx*.10)} {F(ry*.86)} {F(rx*.72)} {F(ry*.32)}',stroke='#fff0c0',sw=1.15,opacity=.19)
    path(f'M {F(-rx*.79)} {F(-ry*.22)} Q {F(-rx*.11)} {F(-ry*.88)} {F(rx*.73)} {F(-ry*.28)}',stroke='#876d4a',sw=2.2,opacity=.24)
    add('</g>')
for idx,b in enumerate(bunkers):bunker(*b,op=1 if idx<5 else .76)

# 3. Decor only outside every hole, grass band, bunker. No purple or intrusive desert props.
# Varied rock clusters, mesquite, creosote, prickly pear/saguaro silhouette. Position geometry checked.
add('<g id="desert-vegetation-and-rocks">')
# Sedimentary stones clustered near mesa outskirts, with coherent shadow and highlight.
rock_areas=[(110,210,195,180),(520,185,112,105),(1090,180,150,120),(1470,280,185,200),
            (240,690,190,170),(545,705,135,105),(1150,685,165,145),(1470,750,210,150),
            (510,415,95,98),(1110,445,96,82)]
rock_count=0
for j,(cx,cy,rx,ry) in enumerate(rock_areas):
    for k in range(45):
        angle=rng.random()*math.tau;rr=math.sqrt(rng.random())
        x=cx+rx*rr*math.cos(angle);y=cy+ry*rr*math.sin(angle)
        size=rng.uniform(2.5,11.5)
        if not clear_disc(x,y,size+3,2):continue
        shape=organic(x,y,size*1.24,size*rng.uniform(.51,.82),12+j*13+k,.25,12,rotation=rng.uniform(-30,30))
        path(organic(x+2,y+3,size*1.20,size*.81,5+j+k,.20,12),fill='#3f362c',opacity=.28)
        path(shape,fill=rng.choice(['#97724e','#ad8255','#c49864','#cda270','#876449']),opacity=rng.uniform(.72,.95))
        path(f'M {F(x-size*.65)} {F(y-size*.22)} q {F(size*.50)} {F(-size*.62)} {F(size*1.10)} {F(-size*.1)}',stroke='#f2c99a',sw=1.05,opacity=.35)
        rock_count+=1
# Shrubs are sparse but material: mesquite larger, creosote smaller; include canopy depth and cast shadow.
shrub_count=0
for i in range(430):
    x=rng.uniform(50,1550);y=rng.uniform(-5,905)
    r=rng.uniform(3.5,13) if i%7 else rng.uniform(12,19)
    if not clear_disc(x,y,r+6,4):continue
    # Avoid excessive vegetation in central illuminated desert; enough for believable scrub.
    if rng.random()>.76:continue
    path(organic(x+3.0,y+3.8,r*.99,r*.72,i,.20,14),fill='#423b2d',opacity=.32)
    path(organic(x,y,r,r*.77,i+9,.22,15),fill='#294c39',opacity=.92)
    path(organic(x-1.7,y-2.2,r*.83,r*.61,i+31,.18,15),fill='url(#shrub)',opacity=.90)
    for j in range(2):
        ell(x+r*rng.uniform(-.46,.28),y-r*rng.uniform(.08,.4),r*.29,r*.15,rng.choice(['#839961','#9fa46c','#668b56']),.50)
    shrub_count+=1
# Desert landmarks: rare vertical saguaro seen strictly from above as crown and branching pads.
# Avoid cartoon-green cactus icons, keep them subtle and appropriately sparse.
cactus_count=0
for i in range(85):
    x=rng.uniform(60,1550);y=rng.uniform(30,850)
    if not clear_disc(x,y,11,10):continue
    if rng.random()>.69:continue
    s=rng.uniform(4,9)
    ell(x+4,y+5,s*.92,s*.69,'#433e2d',.36)
    ell(x,y,s*.76,s*.71,'#52764b',.94)
    ell(x-1.5,y-1.9,s*.31,s*.29,'#8ca16c',.64)
    if i%3==0:
        ell(x+s*.65,y+s*.18,s*.42,s*.37,'#486c45',.87)
    cactus_count+=1
# Batched grit and dry-straw details placed only outside complete protected polygons.
for color,sw,op,count in [('#e8cb9c',1,.30,310),('#5e5940',1,.23,250),('#d4ac77',1.35,.29,340)]:
    chunks=[]
    for i in range(count):
        x=rng.uniform(10,1590);y=rng.uniform(5,890)
        if not clear_disc(x,y,1.8,3):continue
        s=rng.uniform(2.5,8)
        chunks.append(f'M {F(x)} {F(y)} l {F(s*.55)} {F(-s*.85)}')
    path(' '.join(chunks),stroke=color,sw=sw,opacity=op)
add('</g>')

add('<g id="connected-desert-access-network">')
for k,pts in route_sample.items():
    d='M '+' L '.join(f'{F(x)} {F(y)}' for x,y in pts)
    # Compacted desert approach track, gently graded into the existing ochre earth.
    path(d,stroke='#87694b',sw=14.4,opacity=.18)
    path(d,stroke='#bd986a',sw=11.2,opacity=.55)
    path(d,stroke='#d9b789',sw=7.1,opacity=.87)
    path(d,stroke='#e4caa0',sw=3.0,opacity=.67)
    # Sparse twin wheel/foot-worn shading is subtle and does not add new route geometry.
add('</g>')

# 5. Game-like overlay (decorative only; gameplay from app and backend).
add('<g id="prototype-targeting" pointer-events="none">')
path('M 802 854 Q 770 730 763 590 Q 759 485 834 376 Q 885 290 904 211',stroke='#68c5ef',sw=2.1,opacity=.72,extra='stroke-dasharray="5 7"')
# tee
add('<rect x="788" y="858" width="25" height="13" rx="5" fill="#2b3f27" opacity=".64"/>')
add('<rect x="792" y="858" width="18" height="9" rx="3" fill="#fffdf1"/>')
# target
circ(789,524,17,'none',1,extra='stroke="#31b4e0" stroke-width="2"')
path('M 789 500 V 513 M 789 534 V 547 M 764 524 H 777 M 801 524 H 815',stroke='#31b4e0',sw=2.0,opacity=.9)
# pin dot
circ(904,211,4.5,'#1d2a1d')
path('M 904 206 V 182',stroke='#eee9c7',sw=2.2)
path('M 905 181 L 927 188 L 905 195 Z',fill='#e84a4e')
add('</g>')
# approved mood: full height lateral falloff, active field pop, subtle top and bottom.
add('<g id="premium-atmospheric-lighting" pointer-events="none">')
add('<rect width="1600" height="900" fill="url(#light)"/>')
add('<rect width="1600" height="900" fill="url(#toplight)"/>')
add('</g>')
add('</svg>')
# Source-level geometry checks: shared routes must truly meet at intersections,
# and greens must remain inside the natural hole surrounds.
from shapely.geometry import Point as GeoPoint
checks=[('active',main[3][1],GeoPoint(904,211).buffer(1)),
        ('left',left_round[3][1],GeoPoint(321,742).buffer(1)),
        ('right',right[3][1],GeoPoint(1300,252).buffer(1))]
for name,band,center in checks:
    assert band.contains(center),f'{name} green centre leaves the continuous maintained corridor'
for name,band,xy,xy_radius in [
    ('active',main[4][1],(904,211),(36,46)),
    ('left',left_round[4][1],(321,742),(34,43)),
    ('right',right[4][1],(1300,252),(32,45)),
]:
    shape=geom_scale(GeoPoint(*xy).buffer(1,resolution=56),*xy_radius,origin=xy)
    assert band.buffer(-2).contains(shape),f'{name} putting green overhangs the close-mown corridor'
link=LineString(route_sample['property-link'])
for name in ('main-access','right-access','left-access'):
    assert link.distance(LineString(route_sample[name]))<4.0,f'{name} access does not join the shared property track'
assert len(holes)==3 and all(h.area>0 for h in holes)
# Raised rock benches and all later decorative object footprints are excluded from
# the path corridor; rock candidates are relocated rather than paths cutting through them.
for x,y,rx,ry,rot in mesa_centers:
    fitted=terrain_fitted_bench(x,y,rx,ry,rot)
    if fitted is not None:
        assert not route_protected.buffer(5).intersects(rock_bench_footprint(*fitted,rot))

OUT.write_text('\n'.join(S),encoding='utf-8')
print('SVG:',OUT,'size MB:',round(OUT.stat().st_size/1e6,2),'svg paths:',stat['paths'],'ellipses:',stat['ellipses'])
print('Terrain benches:',retained_benches,'kept,',repositioned_benches,'repositioned or reduced')
print('Decor:',rock_count,'rocks,',shrub_count,'shrubs,',cactus_count,'cacti; protected surfaces:',len(holes))
print('Routes:',', '.join(route_sample))
