"""Premium Heathland overhead SVG visual prototype.

Self-contained deterministic VECTOR ART SOURCE, not a game renderer.
The curves are illustrative; production integration must replace them with
backend-authoritative hole geometry and hazard data.

Requires: numpy, scipy, shapely (generation only). No raster images in SVG.
"""
from __future__ import annotations
# Decorative micro-contours use fewer quadratic segments without touching golf surfaces.
# Runtime/DOM optimization: the same fine texture marks are batched into
# compound SVG paths; authoritative golf surfaces, hazards, scenery corridors,
# routing, and lighting remain unchanged.
import math, random
from pathlib import Path
import numpy as np
from scipy.interpolate import CubicSpline
from shapely.geometry import Point, Polygon, LineString
from shapely.ops import unary_union

OUT = Path(__file__).with_name('premium_heathland_vector.svg')
W,H=1600,900
rng=random.Random(281026)
S=[]
F=lambda x:f'{x:.2f}'
def add(s): S.append(s)
def path(d,fill='none',stroke=None,sw=1,opacity=None,extra=''):
    attrs=[f'd="{d}"',f'fill="{fill}"']
    if stroke:attrs += [f'stroke="{stroke}"',f'stroke-width="{F(sw)}"', 'stroke-linecap="round"','stroke-linejoin="round"']
    if opacity is not None: attrs.append(f'opacity="{F(float(opacity))}"')
    if extra: attrs.append(extra)
    add('<path '+' '.join(attrs)+'/>')
class FineStrokeBatch:
    """Combine many independent, low-contrast texture strokes into compound paths.

    This is restricted to decorative strokes in one isolated SVG layer; it never
    touches golf geometry, vegetation silhouettes, shadows, paths or hazards.
    """
    def __init__(self, opacity_step=.07, width_step=.35):
        self.opacity_step=opacity_step
        self.width_step=width_step
        self.groups={}

    def stroke(self, d, color, width, opacity):
        width=max(self.width_step,round(width/self.width_step)*self.width_step)
        opacity=min(1.,max(self.opacity_step,round(opacity/self.opacity_step)*self.opacity_step))
        self.groups.setdefault((color,width,opacity),[]).append(d)

    def emit(self):
        for (color,width,opacity),segments in sorted(self.groups.items()):
            path(' '.join(segments),stroke=color,sw=width,opacity=opacity)

def ellipse(x,y,rx,ry,fill,op=1,extra=''):
    add(f'<ellipse cx="{F(x)}" cy="{F(y)}" rx="{F(rx)}" ry="{F(ry)}" fill="{fill}" opacity="{F(op)}" {extra}/>')
def circle(x,y,r,fill,op=1,extra=''):
    add(f'<circle cx="{F(x)}" cy="{F(y)}" r="{F(r)}" fill="{fill}" opacity="{F(op)}" {extra}/>')
def contour(cx,cy,rx,ry,seed,n=44,wig=.10):
    ps=[]
    for i in range(n):
        t=2*math.pi*i/n
        rr=1+wig*math.sin(3*t+seed*.75)+wig*.43*math.sin(7*t+seed*1.2)+wig*.27*math.cos(5*t+seed*.53)
        ps.append((cx+rx*rr*math.cos(t),cy+ry*rr*math.sin(t)))
    return 'M '+' L '.join(f'{F(x)} {F(y)}' for x,y in ps)+' Z'
def soft_contour(cx,cy,rx,ry,seed,n=22,wig=.13):
    """Smooth vector plant silhouettes (quadratic midpoint curve, not faceted polygons)."""
    pts=[]
    for i in range(n):
        t=2*math.pi*i/n
        r=1+wig*math.sin(3*t+seed*.75)+wig*.38*math.sin(7*t+seed*1.2)+wig*.20*math.cos(5*t+seed*.53)
        pts.append((cx+rx*r*math.cos(t),cy+ry*r*math.sin(t)))
    mx=(pts[-1][0]+pts[0][0])*.5
    my=(pts[-1][1]+pts[0][1])*.5
    d=f'M {F(mx)} {F(my)}'
    for i in range(n):
        x,y=pts[i]; xx,yy=pts[(i+1)%n]
        d+=f' Q {F(x)} {F(y)} {F((x+xx)*.5)} {F((y+yy)*.5)}'
    return d+' Z'
def pspline(points):
    pp=np.array(sorted(points,key=lambda p:p[1]),dtype=float)
    sx=CubicSpline(pp[:,1],pp[:,0], bc_type='natural')
    return lambda y:float(sx(y))
def corridor(xfn, y_bottom, y_top, width_fn, cap_depth=None, count=108):
    """Variable-width continuous nested corridor, with true rounded end cap.

    Ordered from bottom to green end, then around a semicircular back of green,
    then returns down opposite edge. The end is not an added decorative blob.
    """
    ys=np.linspace(y_bottom,y_top,count)
    left=[];right=[]
    for y in ys:
        x=xfn(float(y))
        tangent=(xfn(float(y)+.5)-xfn(float(y)-.5))
        q=math.hypot(1,tangent)
        radius=width_fn(float(y)) if callable(width_fn) else width_fn
        left.append((x-radius/q,y+tangent*radius/q))
        right.append((x+radius/q,y-tangent*radius/q))
    top=float(y_top); cx=xfn(top);r=(width_fn(top) if callable(width_fn) else width_fn)
    depth=cap_depth or r*.73
    # left point -> round top -> right point using basis along corridor normal
    tangent=xfn(top+.5)-xfn(top-.5)
    norm=math.hypot(1,tangent)
    nx,ny=1/norm,-tangent/norm
    arc=[(cx+r*math.cos(t)*nx, top+r*math.cos(t)*ny-depth*math.sin(t))
         for t in np.linspace(math.pi,0,35)]
    pts=left+arc+list(reversed(right))
    return 'M '+' L '.join(f'{F(x)} {F(y)}' for x,y in pts)+' Z', Polygon(pts).buffer(0)

def make_hole(name,pts,bottom,top,widths,depths,op=1):
    fn=pspline(pts)
    dlist=[];polys=[]
    for width,dep in zip(widths,depths):
        d,poly=corridor(fn,bottom,top,width,dep)
        dlist.append(d);polys.append(poly)
    return {'name':name,'fn':fn,'paths':dlist,'polys':polys,'top':top,'bottom':bottom,'op':op}

# Layout is based on the user's straight, par 3, and dogleg Heathland screenshots.
main=make_hole('active',[(821,958),(818,847),(786,735),(771,638),(810,515),(873,408),(896,298),(892,205),(893,170)],950,178,
 [lambda y:106+8*math.sin(y/124),lambda y:94+7*math.sin(y/124),lambda y:83+6*math.sin(y/124),lambda y:62+4*math.sin(y/124)],
 [88,82,78,76])
# Partial flanking holes have equally complete green ends where in view.
right=make_hole('right',[(1336,1005),(1330,850),(1332,710),(1312,557),(1271,420),(1253,275),(1248,207)],980,210,
 [86,76,65,49],[70,67,63,65],.66)

add('''<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900" role="img" aria-labelledby="htitle hdesc">
<title id="htitle">Premium Heathland — overhead SVG golf course presentation prototype</title>
<desc id="hdesc">An orthographic SVG scene with a vibrant active fairway, genuine-looking surrounding holes, open sandy heath, purple heather, yellow gorse, scattered pines, continuous rounded green-end landforms, clear golf access and side lighting. Static illustrative geometry.</desc>
<defs>
 <linearGradient id="ground" x1="0" y1="0" x2=".85" y2="1"><stop stop-color="#4f6948"/><stop offset=".32" stop-color="#7b8259"/><stop offset=".57" stop-color="#98915b"/><stop offset=".79" stop-color="#728257"/><stop offset="1" stop-color="#4b6549"/></linearGradient>
 <linearGradient id="grassland" x1="0" y1="0" x2="1" y2=".35"><stop stop-color="#657d51"/><stop offset=".45" stop-color="#89975f"/><stop offset="1" stop-color="#566e48"/></linearGradient>
 <linearGradient id="fairway" x1="0" y1="0" x2="1" y2=".15"><stop stop-color="#7fa85a"/><stop offset=".27" stop-color="#a6ce74"/><stop offset=".58" stop-color="#b4db7d"/><stop offset=".78" stop-color="#97c668"/><stop offset="1" stop-color="#74a052"/></linearGradient>
 <linearGradient id="green" x1="0" y1="0" x2="1" y2=".12"><stop stop-color="#83bd65"/><stop offset=".48" stop-color="#bce18b"/><stop offset="1" stop-color="#85b964"/></linearGradient>
 <linearGradient id="sand" x1="0" y1="0" x2=".7" y2="1"><stop stop-color="#eee0a8"/><stop offset=".5" stop-color="#dac78f"/><stop offset="1" stop-color="#bca579"/></linearGradient>
 <linearGradient id="heather" x1="0" y1="0" x2=".8" y2="1"><stop stop-color="#3f4639"/><stop offset=".25" stop-color="#625966"/><stop offset=".59" stop-color="#887184"/><stop offset="1" stop-color="#514e52"/></linearGradient>
 <linearGradient id="pine" x1="0" y1="0" x2=".75" y2="1"><stop stop-color="#66845a"/><stop offset=".28" stop-color="#476c45"/><stop offset=".67" stop-color="#274d37"/><stop offset="1" stop-color="#152f2a"/></linearGradient>
 <linearGradient id="lightBand" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#07100d" stop-opacity=".95"/><stop offset=".10" stop-color="#09140f" stop-opacity=".77"/><stop offset=".23" stop-color="#101913" stop-opacity=".46"/><stop offset=".36" stop-color="#132016" stop-opacity=".14"/><stop offset=".47" stop-color="#000000" stop-opacity="0"/><stop offset=".60" stop-color="#000000" stop-opacity="0"/><stop offset=".74" stop-color="#111c17" stop-opacity=".36"/><stop offset=".90" stop-color="#06120e" stop-opacity=".82"/><stop offset="1" stop-color="#030b0a" stop-opacity=".96"/></linearGradient>
 <linearGradient id="verticalLight" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#122518" stop-opacity=".21"/><stop offset=".17" stop-color="#000" stop-opacity="0"/><stop offset=".82" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#05100c" stop-opacity=".10"/></linearGradient>
</defs>''')

# Layer 1: unbroken property, low meadow undulation and sandy openings.
add('<g id="heath-ground">')
add('<rect width="1600" height="900" fill="url(#ground)"/>')
for i,(x,y,rx,ry) in enumerate([(80,150,280,205),(375,155,300,220),(1100,140,340,255),(1460,400,280,280),(440,630,300,270),(1130,730,350,300),(850,470,340,265)]):
    path(contour(x,y,rx,ry,i+5,54,.09),fill=['#9d9b67','#b6a773','#4c7150'][i%3],opacity=[.23,.20,.16][i%3])
    path(contour(x-10,y-12,rx*.78,ry*.74,i+8,53,.085),fill=['#b7a978','#73915b','#cab887'][i%3],opacity=.18)
# Irregular exposed sandy heath flushes, not desert dunes.
for i,(x,y,rx,ry) in enumerate([(515,186,140,94),(1050,360,185,150),(580,682,150,130),(1045,772,178,110),(1180,70,180,100),(185,470,172,120)]):
    path(contour(x,y,rx,ry,17+i,52,.15),fill='#bbaa7a',opacity='.29')
    path(contour(x+6,y-9,rx*.67,ry*.62,25+i,50,.11),fill='#dac38e',opacity='.24')
# Contours: calm rolling inland heath rather than exposed coastal dune ridges.
for idx,(d,w) in enumerate([
 ('M -110 220 C 180 135 280 320 495 230 S 850 70 1160 188',28),
 ('M 40 700 C 200 525 403 560 580 449 S 845 264 1045 310',24),
 ('M 480 950 C 645 752 680 658 580 490 S 485 225 645 35',30),
 ('M 1020 -55 C 1110 180 1000 300 1200 490 S 1470 700 1420 930',36),
 ('M 1520 -30 C 1300 180 1465 345 1270 530 S 1210 830 1510 920',25),
]):
    path(d,stroke='#485c44',sw=w+16,opacity=.10)
    path(d,stroke='#d6c48c',sw=w,opacity=.12)
    path(d,stroke='#f0dcaa',sw=1.8,opacity=.18)
for i in range(130):
    x=rng.uniform(-150,1750);y=rng.uniform(-70,970)
    leng=rng.uniform(40,185)
    path(f'M {F(x)} {F(y)} C {F(x+leng*.3)} {F(y-14)} {F(x+leng*.67)} {F(y+19)} {F(x+leng)} {F(y+rng.uniform(-16,10))}',stroke=rng.choice(['#c5b883','#354b39','#ede1ac']),sw=rng.uniform(.7,1.5),opacity=rng.uniform(.08,.19))
# Fine soil/leaf litter and scattered straw fibres break up broad flat tonal patches.
# Low-contrast vector strokes rather than bitmap/noise filters; all behind golf bands.
add('<g id="heath-soil-grain" aria-label="Fine heath soil and dry rough texture">')
soil_marks=FineStrokeBatch(opacity_step=.07,width_step=.35)
for i in range(1180):
    x=rng.uniform(80,1510);y=rng.uniform(0,900)
    span=rng.uniform(1.6,6.6)
    angle=rng.uniform(-.95,.38)
    soil_marks.stroke(f'M {F(x)} {F(y)} l {F(span*math.cos(angle))} {F(span*math.sin(angle))}',
         rng.choice(['#d9c496','#e6d5a6','#4b6248','#536046','#827b55','#b3a078']),
         rng.uniform(.6,1.1),rng.uniform(.10,.26))
soil_marks.emit()
add('</g>')
add('</g>')

# Draw subordinate holes before scenery; the same nested rounded surface approach is used for all holes.
color_bands=[('#ad9b6c',.64),('#58734c',.87),('#728e58',.94),('url(#grassland)',.84)]
def draw_corridor(hole,active=False):
    op=hole['op']
    add(f'<g id="{hole["name"]}-surface-bands" data-rounded-bands="true">')
    for i,d in enumerate(hole['paths']):
        fill,alpha=color_bands[i]
        if active:
            fill=['#ab9d70','#4b633f','#68884c','url(#fairway)'][i]
            alpha=[.88,.98,.99,1][i]
        path(d,fill=fill,opacity=alpha*op)
    add('</g>')

# Left neighbour is reflected vertically to create a reversed routing with a lower green end.
# Its four bands have proper rounded ends, not a separate oval cap.
# Generate native bottom-end orientation directly by transforming a top-cap tube.
# reflected xfn symmetrical visual, y original 900-top => cap at y=~680.
leftbase=make_hole('left-neighbor',[(321,1010),(310,900),(320,700),(333,500),(325,340),(318,230)],1000,230,
 [80,71,60,44],[67,64,62,60],.68)
# Reflect about the scene center so the nested bands end smoothly at the green near y=670.
add('<g id="left-neighbour-reflection" transform="translate(0 900) scale(1 -1)">')
draw_corridor(leftbase)
add('</g>')
draw_corridor(right)

# Under-story vegetation is clustered on the SIDE property and clipped/cleared around any playable corridor.
# We deliberately exclude all constructed bands, rather than relying on painting vegetation beneath the fairway.
from shapely.affinity import scale as scale_shape
pond_geometry=scale_shape(Point(652,366).buffer(1,resolution=48),34,61,origin=(652,366))
geometry_corridors=unary_union([main['polys'][0],right['polys'][0],
    # reflected left-neighbor polygon - apply y transform
    Polygon([(x,900-y) for x,y in leftbase['polys'][0].exterior.coords]).buffer(0),
    pond_geometry])

# Physical, shape-wide exclusion: even the underlying heather silhouettes must not
# overlap an active or decorative golf-hole band (sampling flower centres alone fails).
# A mask covers all three OUTER bands plus a small safety margin, the pond, and bunkers.
# Drawing source geometry remains intact; no hiding of paths or playable surfaces.
add('<defs><mask id="no-heather-on-golf" maskUnits="userSpaceOnUse" x="0" y="0" width="1600" height="900">')
add('<rect width="1600" height="900" fill="white"/>')
for hole in (main,right):
    path(hole['paths'][0],fill="black",stroke="black",sw=18)
add('<g transform="translate(0 900) scale(1 -1)">')
path(leftbase['paths'][0],fill="black",stroke="black",sw=18)
add('</g>')
# Canonical-style bunker exclusions for all prototype hazards.
for bx,by,brx,bry in [(812,258,18,29),(975,226,21,30),(757,493,21,33),
                      (916,615,20,29),(685,770,23,36),(425,563,18,26),
                      (248,565,17,28),(1150,256,19,29),(1352,340,16,29),
                      (1358,697,21,25)]:
    ellipse(bx,by,brx+15,bry+15,'black')
ellipse(652,366,48,75,'black')
add('</mask></defs>')

def clear(x,y,r=0):
    return not geometry_corridors.buffer(7+r).intersects(Point(x,y))

# Signature HEATHER: many small connected tufts and irregular islands on sandy
# shoulders, never huge lavender sheets. Each island is tested as a complete polygon,
# not merely by the position of a decorative dot. The SVG mask is a backstop.
add('<g id="heather-moorland" aria-label="Clumped heath vegetation" mask="url(#no-heather-on-golf)">')
heather_banks=[(480,145,140,70),(530,315,145,115),(414,570,150,137),(544,805,155,100),
 (1075,120,126,75),(1118,304,135,142),(1120,607,180,127),(1050,820,130,90),
 (175,735,150,120),(1440,600,150,160),(1550,220,145,125),(600,58,100,48)]
# Flowering heather in real heathland appears as many low, unevenly shaded
# woody twig-crowns within broken scrub banks, never featureless lavender disks.
# The palette is deliberately desaturated; tiny blossoms supply the purple read.
heather_colors=['#786378','#826986','#8b7191','#8f748e','#726171','#9b8098','#79697d']
clumps_drawn=0
for j,(cx,cy,rx,ry) in enumerate(heather_banks):
    # Same approved banks/clearances, but finer clustered silhouette texture.
    for k in range(78):
        theta=rng.random()*math.tau
        radial=math.sqrt(rng.random())
        x=cx+rx*.87*radial*math.cos(theta)
        y=cy+ry*.82*radial*math.sin(theta)
        rad=rng.uniform(5.5,16.0)
        shy=rad*rng.uniform(.40,.78)
        seed=j*109+k*11
        if not clear(x,y,rad+11):
            continue
        # Grounded, directional shadow: small, low vegetation on sandy soil.
        path(soft_contour(x+2.7,y+3.5,rad*1.18,shy*1.15,seed,14,.15),
             fill='#253b31',opacity=.26)
        path(soft_contour(x,y,rad,shy,seed+2,14,.19),
             fill='#4e4b47',opacity=.61)
        path(soft_contour(x-.8,y-1.3,rad*.91,shy*.87,seed+4,14,.20),
             fill=heather_colors[(j+k)%len(heather_colors)],opacity=rng.uniform(.72,.91))
        # Batch tiny floral accents per crown while preserving their silhouettes.
        # Their original independently randomized tints are too subtle to justify
        # three separately composited DOM elements on every tiny heather tuft.
        sprigs=[]
        for t in range(3):
            offset_x=rng.uniform(-rad*.68,rad*.60)
            offset_y=rng.uniform(-shy*.61,shy*.50)
            rr=rng.uniform(rad*.22,rad*.43)
            shape=soft_contour(x+offset_x,y+offset_y,rr,rr*.52,seed+7+t*3,10,.20)
            tint=rng.choice(['#ab8da9','#b19bb2','#77716d','#a28ba4','#77846d'])
            alpha=rng.uniform(.37,.59)
            sprigs.append((shape,tint,alpha))
        path(' '.join(s[0] for s in sprigs),fill=sprigs[0][1],
             opacity=sum(s[2] for s in sprigs)/len(sprigs))
        # Fine twig/fescue edge texture gives a flowering shrub rather than pebble silhouettes.
        # Group the fine marks in one compound SVG path per crown for efficiency.
        if k%3!=0:
            twigs=[]
            for t in range(5):
                angle=rng.uniform(0,math.tau)
                rr=rng.uniform(.30,.83)
                sx=x+math.cos(angle)*rad*rr
                sy=y+math.sin(angle)*shy*rr
                tx=sx+math.cos(angle)*rng.uniform(1.6,4.4)
                ty=sy+math.sin(angle)*rng.uniform(.7,3.3)-.6
                twigs.append(f'M {F(sx)} {F(sy)} L {F(tx)} {F(ty)}')
            path(' '.join(twigs),stroke=rng.choice(['#b9a9ad','#a59aa2','#5b674b','#a18b9a']),
                 sw=.68,opacity=.43)
        if k%2==0:
            # One compound fine-stem path per tuft, retaining both positions.
            stems=[]
            for t in range(2):
                sx=x+rng.uniform(-rad*.65,rad*.60)
                sy=y+rng.uniform(-shy*.49,shy*.55)
                if clear(sx,sy,1):
                    shape=f'M {F(sx)} {F(sy+2)} l {F(rng.uniform(-2,2))} {F(-rng.uniform(2,6))}'
                    tint=rng.choice(['#b8a2b4','#a18b9d','#8e7b90'])
                    width=rng.uniform(.7,1.25)
                    alpha=rng.uniform(.36,.61)
                    stems.append((shape,tint,width,alpha))
            if stems:
                path(' '.join(s[0] for s in stems),stroke=stems[0][1],
                     sw=sum(s[2] for s in stems)/len(stems),
                     opacity=sum(s[3] for s in stems)/len(stems))
        clumps_drawn+=1
add('</g>')
# One restrained inland heath pond (illustrative; never an unrecorded runtime hazard).
# Organic edges are illustrative and must use canonical water geometry in the game.
add('<g id="illustrative-heath-pond">')
path(contour(652,366,37,65,31,49,.09),fill='#a6a07a',opacity='.49')
path(contour(652,366,32,59,31,49,.08),fill='#3c5147',opacity='.86')
path(contour(652,365,27,53,31,49,.07),fill='#335e69',opacity='.86')
path(contour(649,364,21,46,32,45,.07),fill='#558796',opacity='.38')
path('M 645 344 Q 651 339 659 343 M 643 374 Q 654 368 663 370',stroke='#add0c7',sw=1.3,opacity=.27)
add('</g>')

# Sandy patches: small dry islands with stratified edges (distinct from Links coastal dunes).
add('<g id="sandy-heath-bare-earth">')
for i,(x,y,rx,ry) in enumerate([(604,260,45,26),(1080,450,50,33),(570,520,41,27),(1190,750,37,28),(495,755,42,28),(650,90,38,17)]):
    if not clear(x,y,max(rx,ry)): continue
    path(contour(x,y,rx+7,ry+6,14+i,40,.16),fill='#8e8a5b',opacity=.36)
    path(contour(x,y,rx,ry,21+i,42,.13),fill='url(#sand)',opacity=.86)
    path(contour(x-3,y-2,rx*.65,ry*.57,31+i,40,.12),fill='#efe0ae',opacity=.18)
    # Sandy soil is granular and feathered, not a flat tan decal.
    for fleck in range(29):
        dx=rng.uniform(-rx*.84,rx*.84)
        dy=rng.uniform(-ry*.8,ry*.8)
        if (dx/(rx*.86))**2+(dy/(ry*.83))**2>1: continue
        qx=x+dx;qy=y+dy
        if clear(qx,qy,3):
            path(f'M {F(qx)} {F(qy)} l {F(rng.uniform(1.5,5))} {F(rng.uniform(-1.5,1.5))}',
                 stroke=rng.choice(['#f4e3b4','#ad9466','#867957']),sw=rng.uniform(.7,1.2),opacity=rng.uniform(.17,.33))
add('</g>')

# Heathland tracks must be geometrically clear of ALL corridors, hazards and green complexes.
# Path network goes between outside-access lines; its branches join at actual points.
# Neighbouring holes are not decorative disconnected islands.
route_defs={
'active-right': [(955,936),(951,842),(965,718),(978,611),(989,510),(1008,411),(1028,312),(1035,216),(1020,110),(1000,-22)],
'right-neighbour':[(1020,110),(1061,115),(1102,135),(1093,250),(1106,351),(1132,446),(1152,513),(1161,650),(1172,803),(1193,934)],
'left-neighbour':[(449,-34),(451,110),(453,225),(459,351),(474,478),(484,601),(495,735),(508,841),(535,958)],
'upper-left-link':[(451,110),(566,64),(700,48),(825,47),(939,61),(1020,110)],
}
# Distances checked with real geometric tubes; if a line crosses a protected band the generator must FAIL.
# Validate paths against playable and rough corridors, green outer surrounds, bunker circles, and water.
# Routes are authored in outside spaces and may be excluded individually if their clearance is invalid.
# Their appearance is procedural stroked SVG paths, not visually hidden intersections.
add('<g id="connected-golf-access" fill="none" stroke-linecap="round" stroke-linejoin="round" aria-label="Connected off-fairway walking paths">')
def smooth_route(points):
    if len(points)<3:
        return 'M '+' L '.join(f'{F(x)} {F(y)}' for x,y in points)
    # Catmull-Rom as cubic Beziers; routes genuinely pass through every listed junction.
    d=f'M {F(points[0][0])} {F(points[0][1])}'
    for i in range(len(points)-1):
        p0=points[max(0,i-1)];p1=points[i];p2=points[i+1];p3=points[min(len(points)-1,i+2)]
        c1=(p1[0]+(p2[0]-p0[0])/6,p1[1]+(p2[1]-p0[1])/6)
        c2=(p2[0]-(p3[0]-p1[0])/6,p2[1]-(p3[1]-p1[1])/6)
        d+=f' C {F(c1[0])} {F(c1[1])} {F(c2[0])} {F(c2[1])} {F(p2[0])} {F(p2[1])}'
    return d
for name,points in route_defs.items():
    d=smooth_route(points)
    add(f'<g id="route-{name}" data-functional="true">')
    path(d,stroke='#3d4935',sw=11,opacity=.30)
    path(d,stroke='#cdbb90',sw=6.2,opacity=.88)
    path(d,stroke='#f3dfab',sw=1.8,opacity=.40)
    add('</g>')
add('</g>')

# Trees, not a woodland corridor: widely spaced Scots pine silhouettes clustered in natural islands.
add('<g id="scattered-scots-pines" aria-label="Sparse Scots pine woodland and small groups">')
def pine(cx,cy,r,alpha=1):
    """Vector Scots pine canopy: broken lobes, needle fans and cast shadow."""
    seed=int(cx*4+cy)%29
    # All shadows point southeast: consistent with bright upper-left ambience.
    path(soft_contour(cx+r*.40,cy+r*.53,r*1.02,r*.70,seed,20,.18),
         fill='#132a24',opacity=.39*alpha)
    path(soft_contour(cx,cy,r,r*.83,seed+1,23,.22),
         fill='#193b31',opacity=.96*alpha)
    path(soft_contour(cx-r*.12,cy-r*.14,r*.81,r*.66,seed+2,21,.21),
         fill='url(#pine)',opacity=.98*alpha)
    for k in range(6):
        ang=(k/6)*math.tau+.26
        xx=cx+math.cos(ang)*r*.48
        yy=cy+math.sin(ang)*r*.42
        rr=r*(.27+.07*((k+seed)%3)/2)
        path(soft_contour(xx,yy,rr,rr*.78,seed+k+9,15,.25),
             fill=['#35563d','#294f37','#5a7747','#345a3b','#446a44','#1b4533'][k],
             opacity=(.58 if k%2 else .72)*alpha)
    # Compound strokes by canopy highlight colour (three rather than eight nodes).
    needle_groups={}
    for k in range(8):
        ang=rng.uniform(0,math.tau)
        inner=rng.uniform(.15,.52)*r
        xx=cx+math.cos(ang)*inner
        yy=cy+math.sin(ang)*inner
        shape=f'M {F(xx)} {F(yy)} l {F(math.cos(ang)*r*.17)} {F(math.sin(ang)*r*.11)}'
        tint=['#7d995f','#203f31','#a0aa6d'][k%3]
        width=rng.uniform(.65,1.2)
        opacity=(.27 if k%3 else .38)*alpha
        needle_groups.setdefault(tint,[]).append((shape,width,opacity))
    for tint,needles in needle_groups.items():
        path(' '.join(s[0] for s in needles),stroke=tint,
             sw=sum(s[1] for s in needles)/len(needles),
             opacity=sum(s[2] for s in needles)/len(needles))
# Grouped loose stands, no continuous tree wall near playable core
stands=[(270,148,12),(328,315,16),(315,595,14),(380,815,13),(595,130,11),(650,390,14),(624,680,14),(1120,220,12),(1150,535,15),(1135,820,14),(1395,250,16),(1380,690,13),(710,60,11),(1063,71,11)]
for x,y,r in stands:
    for j in range(rng.randint(3,7)):
        px=x+rng.uniform(-53,53);py=y+rng.uniform(-61,61)
        sz=rng.uniform(r*.55,r*1.30)
        if clear(px,py,sz+6): pine(px,py,sz,rng.uniform(.78,1.0))
for i in range(58):
    x=rng.uniform(45,1550);y=rng.uniform(5,900);r=rng.uniform(5,13)
    if clear(x,y,r+10):pine(x,y,r,.82)
add('</g>')

# Gorse is characteristic: dense low gold-flowering bramble islands, NOT uniform sand specks.
add('<g id="flowering-gorse">')
def gorse(x,y,r,alpha=1):
    """Low prickly gorse: irregular dark twigs with restrained yellow flowers."""
    seed=int(x*2+y)%23
    path(soft_contour(x+2,y+3,r*1.19,r*.75,seed,15,.22),
         fill='#2f3c29',opacity=.48*alpha)
    path(soft_contour(x,y,r*1.06,r*.73,seed+2,15,.20),
         fill='#43553a',opacity=.91*alpha)
    for k in range(5):
        ang=k*math.tau/5+.32
        px=x+math.cos(ang)*r*.48;py=y+math.sin(ang)*r*.29
        path(soft_contour(px,py,r*.31,r*.23,seed+k+4,11,.23),
             fill=['#687547','#879052','#647a47','#415a3c','#7f8150'][k],
             opacity=rng.uniform(.62,.83)*alpha)
    # Flowers retain their individual vector outlines but share one of two
    # compound fills. This reduces hundreds of sub-pixel shapes to small batches.
    blossom_groups={}
    for k in range(rng.randint(5,10)):
        px=x+rng.uniform(-r*.73,r*.66);py=y+rng.uniform(-r*.45,r*.34)
        rr=rng.uniform(.85,2.3)
        shape=soft_contour(px,py,rr*1.18,rr*.71,seed+k*7,9,.17)
        tint=rng.choice(['#b5a84d','#d6be53','#dfc75b','#b9ad57'])
        op=rng.uniform(.45,.78)*alpha
        blossom_groups.setdefault('warm' if tint in ('#d6be53','#dfc75b') else 'olive',[]).append((shape,op))
    for tone,blossoms in blossom_groups.items():
        path(' '.join(s[0] for s in blossoms),
             fill='#d6be53' if tone=='warm' else '#b9ad57',
             opacity=sum(s[1] for s in blossoms)/len(blossoms))
gorse_centres=[(490,230),(570,435),(525,600),(596,822),(1100,280),(1080,610),(1108,767),(1205,110),(448,735),(1428,525)]
for x,y in gorse_centres:
    for i in range(13):
        px=x+rng.uniform(-55,55);py=y+rng.uniform(-50,50);r=rng.uniform(4,11)
        if clear(px,py,r+7):gorse(px,py,r,rng.uniform(.75,1))
add('</g>')

# Dry rough texture and sparse individual heather stems in negative space.
add('<g id="heath-grass-marks" fill="none" stroke-linecap="round">')
rough_marks=FineStrokeBatch(opacity_step=.07,width_step=.35)
for i in range(1350):
    x=rng.uniform(30,1570);y=rng.uniform(0,900)
    if not clear(x,y,1):continue
    l=rng.uniform(3,12);dx=rng.uniform(-4.5,5)
    rough_marks.stroke(f'M {F(x)} {F(y)} q {F(dx*.2)} {F(-l*.48)} {F(dx)} {F(-l)}',
         rng.choice(['#b7ab7f','#a69a6d','#51694a','#586b46','#938368','#b09b85','#cab789']),
         rng.uniform(.6,1.45),rng.uniform(.19,.42))
rough_marks.emit()
add('</g>')

# Greens and bunkers for the decorative neighbours are part of their geometry, not floating blobs.
add('<g id="neighbour-grounds-and-hazards">')
def green(x,y,rx,ry,alpha=1,seed=10):
    # Quiet, nested green fringe blended into the SAME fairway/approach corridor.
    # No thick dark sticker-outline or overhanging green footprint.
    path(contour(x,y,rx+8,ry+9,seed,45,.045),fill='#70924f',opacity=.47*alpha)
    path(contour(x,y,rx+4,ry+5,seed+1,46,.047),fill='#8dae62',opacity=.72*alpha)
    path(contour(x,y,rx,ry,seed+2,46,.048),fill='url(#green)',opacity=.96*alpha)
    path(contour(x-2,y-2,rx*.73,ry*.76,seed+4,44,.04),fill='#d3e99a',opacity=.11*alpha)
def bunker(x,y,rx,ry,angle=0,alpha=1):
    attr=f'transform="rotate({angle} {F(x)} {F(y)})"'
    ellipse(x+2,y+4,rx+6,ry+7,'#364636',.32*alpha,attr)
    ellipse(x,y,rx+4,ry+5,'#77764d',.85*alpha,attr)
    ellipse(x-1,y-2,rx+2,ry+3,'url(#sand)',.95*alpha,attr)
    ellipse(x-3,y-6,rx*.63,ry*.52,'#fff0c6',.35*alpha,attr)
# Left neighbour green is toward the visible bottom of the frame.
green(325,636,30,43,.60,16)
for b in [(425,563,18,26,-14,.60),(248,565,17,28,11,.55)]:bunker(*b)
green(1247,227,31,41,.72,15)
for b in [(1150,256,19,29,13,.61),(1352,340,16,29,-7,.65),(1358,697,21,25,18,.53)]:bunker(*b)
add('</g>')

# Active surfaces late in the scene: protected, true playable focus. Four nested CAPPED bands.
draw_corridor(main,True)
add('<g id="active-fairway-stripes">')
# Restrict gently tilted mow stripes to the fairway polygon using a native SVG clipPath created from SAME geometry.
add(f'<clipPath id="activeFairwayClip"><path d="{main["paths"][-1]}"/></clipPath>')
add('<g clip-path="url(#activeFairwayClip)">')
for y in range(210,960,50):
    add(f'<rect x="680" y="{y}" width="350" height="25" fill="#d3ec9e" opacity=".085"/>')
for y in range(245,925,74):
    x=main['fn'](float(y));path(f'M {F(x-61)} {F(y)} Q {F(x)} {F(y-12)} {F(x+61)} {F(y-2)}',stroke='#e4efa9',sw=1.4,opacity=.12)
add('</g></g>')

add('<g id="active-green-complex">')
green(892,190,35,45,1,24)
# nuanced green contours
path('M 859 190 Q 893 198 922 185',stroke='#e5f3b5',sw=1.5,opacity=.20)
path('M 869 162 Q 890 167 911 161',stroke='#f0f6c1',sw=1,opacity=.15)
# tee pad near bottom
path(contour(819,864,24,17,22,34,.065),fill='#688c4e')
add('<rect x="802" y="854" width="34" height="16" rx="5" fill="#d7e3b2" stroke="#577442" stroke-width="2"/>')
add('<rect x="806" y="857" width="26" height="10" rx="3" fill="#f6f4d7"/>')
add('</g>')

# Hazards are illustrative placeholders and must be populated by authoritative generator in production.
add('<g id="active-bunkers">')
for b in [(812,258,18,29,-15,1),(975,226,21,30,10,1),(757,493,21,33,-5,1),(916,615,20,29,12,1),(685,770,23,36,-17,1)]:bunker(*b)
add('</g>')

# Directional golf UI is decorative; pure SVG, deliberately noninteractive.
add('<g id="reference-aim-overlay" pointer-events="none">')
path('M 819 864 Q 795 594 892 190',stroke='#65c4ff',sw=2,opacity=.55,extra='stroke-dasharray="6 7"')
add('<circle cx="850" cy="459" r="16" stroke="#57b9fa" stroke-width="2.4" fill="none" opacity=".95"/>')
path('M 826 459 H 874 M 850 435 V 483',stroke='#57b9fa',sw=2.0,opacity=.93)
circle(892,190,4.1,'#141f14')
path('M 892 190 L 892 160 L 915 168 L 892 177 Z',fill='#ea5555')
add('<rect x="807" y="858" width="23" height="9" rx="4" fill="#fffef2"/>')
add('</g>')
# The focus effect is full-height LINEAR side falloff, never a radial spotlight.
add('<g id="premium-heathland-lighting" pointer-events="none">')
add('<rect width="1600" height="900" fill="url(#lightBand)"/>')
add('<rect width="1600" height="900" fill="url(#verticalLight)"/>')
add('</g>')
add('</svg>')

# Check the actual route geometry before writing the file; no visual hiding of illegal routes.
# Smooth Catmull curves used above may overshoot polyline; high-res dense cubic sample for clearance tests.
def sample_bezier(points):
    result=[]
    for i in range(len(points)-1):
        p0=points[max(0,i-1)]; p1=points[i];p2=points[i+1];p3=points[min(len(points)-1,i+2)]
        c1=(p1[0]+(p2[0]-p0[0])/6,p1[1]+(p2[1]-p0[1])/6)
        c2=(p2[0]-(p3[0]-p1[0])/6,p2[1]-(p3[1]-p1[1])/6)
        for t in np.linspace(0,1,16,endpoint=False):
            pt=((1-t)**3*np.array(p1)+3*(1-t)**2*t*np.array(c1)+3*(1-t)*t*t*np.array(c2)+t**3*np.array(p2))
            result.append(tuple(pt))
    result.append(points[-1]);return LineString(result)
# Convert strip reflection to test geometry correctly.
restricted=[main['polys'][0],right['polys'][0],
 Polygon([(x,900-y) for x,y in leftbase['polys'][0].exterior.coords]).buffer(0),pond_geometry]
for name,pts in route_defs.items():
    testline=sample_bezier(pts).buffer(5)
    conflicts=[]
    for h,poly in zip(['main','right','left','pond'],restricted):
        area=testline.intersection(poly.buffer(3)).area
        if area>1: conflicts.append((h,round(area)))
    # Exact visual bunker footprints, conservatively buffered. Every route is checked.
    from shapely.affinity import scale, rotate
    all_bunkers=[(812,258,18,29,-15),(975,226,21,30,10),(757,493,21,33,-5),
                 (916,615,20,29,12),(685,770,23,36,-17),
                 (425,563,18,26,-14),(248,565,17,28,11),
                 (1150,256,19,29,13),(1352,340,16,29,-7),(1358,697,21,25,18)]
    for j,(x,y,rx,ry,ang) in enumerate(all_bunkers):
        obj=rotate(scale(Point(x,y).buffer(1, resolution=30),rx+7,ry+7,origin=(x,y)),ang,origin=(x,y))
        area=testline.intersection(obj).area
        if area>1:conflicts.append((f'bunker-{j}',round(area)))
    print('route',name,'clear' if not conflicts else conflicts)
    if conflicts:raise ValueError(f'Illegal route {name}: {conflicts}')
# Full-shape clump clearance is tested above; mask also prevents overlapping
# neighbouring playable areas regardless of SVG draw order.
print('heather clumps',clumps_drawn)
OUT.write_text('\n'.join(S),encoding='utf-8')
print('SVG',OUT,'size',OUT.stat().st_size,'elements',len(S))
