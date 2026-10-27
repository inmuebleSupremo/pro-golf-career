from __future__ import annotations
import math, random, html
import numpy as np
from scipy.interpolate import CubicSpline
from pathlib import Path

OUT=Path(__file__).with_name('premium_links_true_vector.svg')
W,H=1600,900
rnd=random.Random(271027)
S=[]
def a(v):
    return str(v)
def add(s): S.append(s)
def fmt(x): return f'{x:.1f}'
def pth(d,fill='none',stroke=None,sw=None,opacity=None,extra=''):
    bits=[f'd="{d}"',f'fill="{fill}"']
    if stroke: bits.extend([f'stroke="{stroke}"',f'stroke-width="{sw or 1}"', 'stroke-linecap="round"','stroke-linejoin="round"'])
    if opacity is not None: bits.append(f'opacity="{opacity}"')
    if extra: bits.append(extra)
    add('<path '+ ' '.join(bits) + '/>')
def ellipse(cx,cy,rx,ry,fill, opacity=1,extra=''):
    add(f'<ellipse cx="{fmt(cx)}" cy="{fmt(cy)}" rx="{fmt(rx)}" ry="{fmt(ry)}" fill="{fill}" opacity="{opacity}" {extra}/>')
def circle(cx,cy,r,fill,opacity=1,extra=''):
    add(f'<circle cx="{fmt(cx)}" cy="{fmt(cy)}" r="{fmt(r)}" fill="{fill}" opacity="{opacity}" {extra}/>')
def organic(cx,cy,rx,ry,seed=0,n=36,wiggle=.10,phase=0):
    pts=[]
    for i in range(n):
        t=2*math.pi*i/n
        k=1+ wiggle*math.sin(t*3+seed*.9+phase)+wiggle*.55*math.cos(t*5+seed*.67)+wiggle*.23*math.sin(t*8+seed)
        pts.append((cx+rx*k*math.cos(t),cy+ry*k*math.sin(t)))
    return 'M '+' L '.join(f'{fmt(x)} {fmt(y)}' for x,y in pts)+' Z'

def poly(pts): return 'M '+' L '.join(f'{fmt(x)} {fmt(y)}' for x,y in pts)+' Z'

def sample_cent(points):
    # sorted ascending by y
    pp=np.array(sorted(points,key=lambda x:x[1]))
    spline=CubicSpline(pp[:,1],pp[:,0], bc_type='natural')
    return lambda y: float(spline(y))
main_x=sample_cent([(868,-45),(887,88),(886,156),(914,255),(934,349),(918,441),(876,542),(840,622),(831,718),(850,803),(855,910),(856,970)])

def ribbon(xfn,ys,halfwidth,phase=0):
    left=[];right=[]
    for y in ys:
        x=xfn(y)
        dx=(xfn(y+1)-xfn(y-1))/2
        norm=math.hypot(1,dx)
        nw=halfwidth(y) if callable(halfwidth) else halfwidth
        # use perpendicular to downwards curve (nx,ny)=(1,-dx)
        left.append((x-nw/norm,y+dx*nw/norm))
        right.append((x+nw/norm,y-dx*nw/norm))
    return poly(left+right[::-1])
def capped_ribbon(xfn, bottom_y, shoulder_y, halfwidth, cap_top, nose_x=899, samples=130):
    """One smooth, continuous course surface; no separate green-end patches.

    All bands use the same rear-cap construction with progressively inset edges.
    The cubic tangents avoid a hard terminal line or polygon corners.
    """
    left=[]; right=[]
    for y in np.linspace(bottom_y, shoulder_y, samples):
        x=xfn(float(y))
        dx=(xfn(float(y)+1)-xfn(float(y)-1))/2
        n=math.hypot(1,dx)
        width=halfwidth(float(y)) if callable(halfwidth) else halfwidth
        left.append((x-width/n,float(y)+dx*width/n))
        right.append((x+width/n,float(y)-dx*width/n))
    lx,ly=left[-1]; rx,ry=right[-1]
    # Follow the same shoulder on each side, then curl smoothly behind the green.
    # The final curve is a genuine closure of the surface, not a superimposed blob.
    nose=f'{fmt(nose_x)} {fmt(cap_top)}'
    d=f'M {fmt(left[0][0])} {fmt(left[0][1])}'
    d+=' ' + ' '.join(f'L {fmt(x)} {fmt(y)}' for x,y in left[1:])
    d+=f' C {fmt(lx-2)} {fmt(shoulder_y-48)} {fmt(nose_x-52)} {fmt(cap_top)} {nose}'
    d+=f' C {fmt(nose_x+52)} {fmt(cap_top)} {fmt(rx+2)} {fmt(shoulder_y-48)} {fmt(rx)} {fmt(ry)}'
    d+=' ' + ' '.join(f'L {fmt(x)} {fmt(y)}' for x,y in right[-2::-1])
    return d+' Z'

# All five nested bands terminate as the SAME continuous course complex.
# The inner fairway has a proper smooth apron around the back of the green.
def cap_band(halfwidth, cap_top):
    return capped_ribbon(main_x,915,142,halfwidth,cap_top)

ys=list(np.linspace(100,915,115))

def hw_base(y):
    return 74 +8*math.sin(y/130)+ 7*math.sin(y/58+.3)
def hw_fair(y):
    return 58+6*math.sin(y/129)+5*math.sin(y/73+.3) - 8 * (lambda t:t*t*(3-2*t))(max(0,min(1,(y-790)/75)))

add('''<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900" role="img" aria-labelledby="title desc">
<title id="title">Premium Links — top-down vector-only golf course presentation prototype</title>
<desc id="desc">A vector-only links golf hole, surrounded by dune landforms, rough grasses, authentic neighboring golf corridors, connected walking paths, pot bunkers, a luminous fairway and subtle lateral falloff.</desc>
<defs>
  <linearGradient id="sand" x1="0" y1="0" x2="1" y2=".24"><stop offset="0" stop-color="#9c9659"/><stop offset=".30" stop-color="#baa860"/><stop offset=".53" stop-color="#d5bd70"/><stop offset=".73" stop-color="#c4aa61"/><stop offset="1" stop-color="#959354"/></linearGradient>
  <linearGradient id="dune" x1="0" y1="0" x2=".5" y2="1"><stop offset="0" stop-color="#e1cc85"/><stop offset=".48" stop-color="#d4bd71"/><stop offset="1" stop-color="#b4a05f"/></linearGradient>
  <linearGradient id="fairway" x1="0" y1="0" x2="1" y2=".1"><stop offset="0" stop-color="#86a956"/><stop offset=".32" stop-color="#a5c96a"/><stop offset=".67" stop-color="#a3c967"/><stop offset="1" stop-color="#7c9c4d"/></linearGradient>
  <linearGradient id="green" x1="0" y1="0" x2="1" y2=".2"><stop offset="0" stop-color="#84bc64"/><stop offset=".52" stop-color="#a9db80"/><stop offset="1" stop-color="#78ac56"/></linearGradient>
  <linearGradient id="bunker" x1="0" y1="0" x2=".15" y2="1"><stop offset="0" stop-color="#fff0bc"/><stop offset=".6" stop-color="#f8dba2"/><stop offset="1" stop-color="#cfae79"/></linearGradient>
  <linearGradient id="lightBand" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#050e0b" stop-opacity=".90"/><stop offset=".10" stop-color="#07130e" stop-opacity=".72"/><stop offset=".23" stop-color="#0a170c" stop-opacity=".42"/><stop offset=".37" stop-color="#0c180c" stop-opacity=".10"/><stop offset=".47" stop-color="#000000" stop-opacity="0"/><stop offset=".60" stop-color="#070d07" stop-opacity=".05"/><stop offset=".75" stop-color="#050f0c" stop-opacity=".35"/><stop offset=".91" stop-color="#05100d" stop-opacity=".77"/><stop offset="1" stop-color="#020a09" stop-opacity=".94"/></linearGradient>
  <linearGradient id="verticalTone" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#06120d" stop-opacity=".24"/><stop offset=".19" stop-color="#000000" stop-opacity="0"/><stop offset=".77" stop-color="#000000" stop-opacity="0"/><stop offset="1" stop-color="#0b150f" stop-opacity=".13"/></linearGradient>
  <clipPath id="fairwayClip">''')
add(f'<path d="{cap_band(hw_fair,36)}"/></clipPath></defs>')
add('<g id="landscape-base"><rect width="1600" height="900" fill="url(#sand)"/>')
# mottled broad grass/sand interleavings
land=[(175,145,225,155,5),(445,75,260,165,3),(595,340,250,185,4),(345,525,235,230,2),(550,778,260,210,6),(1040,90,238,170,9),(1220,312,325,190,7),(1275,615,260,260,5),(1470,844,230,210,11)]
for i,(x,y,rx,ry,seed) in enumerate(land):
    pth(organic(x,y,rx*1.08,ry*1.07,seed,50,.115),fill=['#868b4e','#a7a35b','#7d8749'][i%3], opacity='.28')
    pth(organic(x-6,y-7,rx*.96,ry*.95,seed+2,50,.10),fill='url(#dune)',opacity='.87')
    pth(organic(x-14,y-16,rx*.75,ry*.73,seed+4,50,.085),fill='#e6ca80',opacity='.55')
    pth(organic(x-9,y-8,rx*.51,ry*.50,seed+8,50,.09),fill='#dbbf73',opacity='.34')
# broad ridges as nested curved strokes
ridges=[
 ('M 365 -40 C 260 102 345 191 265 309 S 316 506 160 652',58),
 ('M 560 -60 C 430 88 603 208 520 327 S 487 540 590 656',70),
 ('M 585 930 C 530 794 654 671 583 541 S 590 297 660 158',53),
 ('M 1090 -85 C 1220 64 1088 217 1160 360 S 1143 559 1030 702',64),
 ('M 1325 945 C 1305 767 1150 691 1200 535 S 1430 300 1275 99',75),
 ('M 1410 72 C 1480 201 1388 347 1515 510 S 1450 700 1550 862',48),
 ('M 80 930 C 190 810 285 818 295 692 S 185 511 338 408',53),
]
for i,(d,width) in enumerate(ridges):
    pth(d,stroke='#827b45',sw=width+21,opacity='.14')
    pth(d,stroke='#e7d083',sw=width,opacity='.20')
    pth(d,stroke='#ffdf8c',sw=max(12,width*.2),opacity='.25')
# topographic contour filaments
for i in range(68):
    x0=rnd.uniform(-90,1620); y0=rnd.uniform(-80,1000)
    if abs(x0-main_x(min(900,max(100,y0))))<100: continue
    length=rnd.uniform(50,200)
    dd=f'M {fmt(x0)} {fmt(y0)} C {fmt(x0+length*.24)} {fmt(y0-23)} {fmt(x0+length*.73)} {fmt(y0+39)} {fmt(x0+length)} {fmt(y0+rnd.uniform(-8,18))}'
    pth(dd,stroke=rnd.choice(['#f1d994','#876f45','#687542','#fce9a6']),sw=rnd.choice([1,1.3,2.0]),opacity=fmt(rnd.uniform(.10,.26)))
add('</g>')

# darker and low contrast partial neighboring fairway ribbons
add('<g id="neighboring-golf-holes" aria-label="Decorative scaled neighboring hole glimpses">')
nb1=sample_cent([(220,-50),(258,100),(281,235),(270,350),(304,465),(359,630),(426,850),(450,1000)])
nb2=sample_cent([(1352,-90),(1310,105),(1360,270),(1405,460),(1435,630),(1380,820),(1360,1000)])
for idx,(fx,y0,y1) in enumerate([(nb1,-80,1010),(nb2,-80,1000)]):
    ysample=list(np.linspace(y0,y1,110))
    pth(ribbon(fx,ysample,75),fill='#617b45',opacity='.60')
    pth(ribbon(fx,ysample,66),fill='#49673e',opacity='.68')
    pth(ribbon(fx,ysample,52),fill='#739752',opacity='.67')
    pth(ribbon(fx,ysample,47),fill='#92aa62',opacity='.48')
    # thin greens at different end portions (unobtrusive)
    if idx==0:
        ellipse(277,187,46,66,'#7baf5c','.78',extra='transform="rotate(8 277 187)"')
        ellipse(410,810,39,65,'#8eb869','.58',extra='transform="rotate(-24 410 810)"')
    else:
        ellipse(1375,705,43,64,'#82b05d','.74',extra='transform="rotate(18 1375 705)"')
        ellipse(1311,80,43,62,'#8fb76d','.60',extra='transform="rotate(12 1311 80)"')
# third small partial golf glimpse low left bottom
pth('M 490 961 C 499 829 480 734 548 650 C 612 630 650 680 610 751 C 560 824 563 912 585 963 Z',fill='#658a4d',opacity='.65')
pth('M 520 961 C 529 832 510 745 564 680 C 595 664 622 691 588 756 C 549 835 548 902 560 961 Z',fill='#9cc078',opacity='.60')
ellipse(563,701,37,56,'#97c576','.60',extra='transform="rotate(22 563 701)"')
add('</g>')

# Connected access routes run along OUTSIDE edges of the golf corridors.
# Walking routes stop at the outside edge of the green/tee access areas and
# connect through the dune land between holes, never across playable surfaces.
# The decorative secondary corridors are 150 units across at their rough edge.
# Both far-side paths therefore use an additional clearance of > 14 units.
add('<g id="connected-walking-routes" stroke-linecap="round" fill="none">')

def access_side(xfn,side,y0,y1,half_width,clearance=24,samples=106):
    """Sample an offset access path *outside* the maintained corridor."""
    coords=[]
    for y in np.linspace(y0,y1,samples):
        y=float(y)
        width=half_width(y) if callable(half_width) else half_width
        coords.append((xfn(y)+side*(width+clearance),y))
    return coords

def smooth_route(points):
    """Continuous SVG path made from the same sampled course coordinates."""
    return 'M ' + ' L '.join(f'{fmt(x)} {fmt(y)}' for x,y in points)

# Main cart/walking route: left of ALL active rough bands, from tee-side offstage
# to the far side of its capped green complex and offstage towards the next tee.
active_route=access_side(main_x,-1,945,-35,lambda y:hw_base(y)+15,24)
# Swing around the LEFT of the mid-hole bunker rather than drawing over its sand.
# A smooth Gaussian offset avoids any sharp corner at the bypass.
active_route=[(x-53*math.exp(-.5*((y-333)/49)**2),y) for x,y in active_route]

# The left neighbour receives one continuous outside access path along its east
# edge, then a green-to-tee connector through the clear dune saddle near y=560.
# It does not continue through the small lower-left golf glimpse (y=630..960).
left_route=access_side(nb1,+1,-45,568,75,23)

# Right-side access runs completely outside the right neighbour and its bunkers.
right_route=access_side(nb2,+1,943,-45,75,27)

# Smooth on-stage link: between the two course corridors, north of the small
# lower-left golf glimpse and clear of all active/neighbor hazards.
join_y=568
left_access_x=nb1(join_y)+75+23
active_access_x=(main_x(join_y)-(hw_base(join_y)+15+24)
                 -53*math.exp(-.5*((join_y-333)/49)**2))
connector=(f'M {fmt(left_access_x)} {join_y} '
           f'C {fmt(left_access_x+5)} {fmt(join_y+49)} '
           f'{fmt(active_access_x-85)} {fmt(join_y+20)} '
           f'{fmt(active_access_x)} {join_y}')

# Other inter-hole transfers are implied beyond the upper stage boundary.
# There is deliberately NO cross-fairway connector to the right neighbour.
tracks=[smooth_route(active_route),smooth_route(left_route),
        smooth_route(right_route),connector]
for d in tracks:
    pth(d,stroke='#696a45',sw=11,opacity='.28')
    pth(d,stroke='#ecd8a1',sw=6.3,opacity='.70')
    pth(d,stroke='#fff2ba',sw=1.7,opacity='.22')
add('</g>')

# neighboring bunker details behind active playable area
add('<g id="neighbor-bunkers">')
def bunker(cx,cy,rx,ry,angle=0,alpha=1):
    extra=f'transform="rotate({angle} {fmt(cx)} {fmt(cy)})"'
    ellipse(cx+2,cy+3,rx+5,ry+6,'#3e4c30',.36*alpha,extra)
    ellipse(cx,cy,rx+4,ry+5,'#777549',.85*alpha,extra)
    ellipse(cx,cy-1,rx+2,ry+3,'#ead09d',.84*alpha,extra)
    ellipse(cx-1,cy-2,rx*.83,ry*.84,'url(#bunker)',.9*alpha,extra)
    pth(f'M {fmt(cx-rx*.65)} {fmt(cy+ry*.5)} Q {fmt(cx)} {fmt(cy+ry*.95)} {fmt(cx+rx*.61)} {fmt(cy+ry*.48)}',stroke='#b38d66',sw=1.2,opacity='.48')
for x,y,rx,ry,a in [(330,480,21,31,-13),(375,762,21,36,23),(1270,320,26,33,-12),(1320,840,20,30,13),(1345,20,19,34,10),(572,696,19,24,35)]: bunker(x,y,rx,ry,a,.65)
add('</g>')

# dune texture grass hatching and pebbles (all geometry authored as paths; no raster images)
add('<g id="links-dune-texture" stroke-linecap="round" fill="none">')
for i in range(910):
    x=rnd.uniform(0,1600); y=rnd.uniform(-20,940)
    # avoid main corridor, but dune rough just beyond boundaries
    if abs(x-main_x(max(100,min(900,y))))<97: continue
    # avoid major neighbor corridors
    if abs(x-nb1(max(-70,min(995,y))))<63 or abs(x-nb2(max(-70,min(995,y))))<64: continue
    s=rnd.uniform(2.5,11)
    color=rnd.choice(['#536742','#6b7542','#7d844b','#f1d697','#e5c878','#847b48','#59653b'])
    alpha=fmt(rnd.uniform(.18,.46)); drift=rnd.uniform(-5,4)
    pth(f'M {fmt(x)} {fmt(y)} q {fmt(drift*.3)} {fmt(-s*.45)} {fmt(drift)} {fmt(-s)}',stroke=color,sw=fmt(rnd.uniform(.7,1.8)),opacity=alpha)
    if i%6==0:
        pth(f'M {fmt(x+4)} {fmt(y+2)} l {fmt(-rnd.uniform(1,6))} {fmt(-rnd.uniform(2,7))}',stroke='#e8d391',sw='.8',opacity='.27')
add('</g>')

# sparse links scrub, some small shrubs groups clustered in dunes, after tracks but before active
add('<g id="dune-scrub" aria-label="Low coastal vegetation">')
def shrub(cx,cy,r,alpha=1):
    ellipse(cx+2,cy+4,r*1.2,r*.72,'#485a3a',.22*alpha)
    palette=['#586f44','#687a48','#859052','#56673c','#77824a','#485f40']
    for j in range(5):
        ang=j*2.4+rnd.uniform(-.3,.3)
        rad=r*.32
        ellipse(cx+math.cos(ang)*rad,cy+math.sin(ang)*rad,r*rnd.uniform(.40,.60),r*rnd.uniform(.45,.65),palette[(j+int(cx))%len(palette)],rnd.uniform(.50,.83)*alpha)
    ellipse(cx-r*.18,cy-r*.22,r*.39,r*.28,'#a1a463',.32*alpha)
centers=[(504,177,14),(570,260,11),(610,373,11),(660,506,12),(624,626,13),(680,743,10),(540,784,10),(356,554,13),(351,389,10),(410,203,12),
(1095,146,12),(1095,215,11),(1115,380,11),(1092,500,14),(1188,622,12),(1142,738,13),(1200,820,10),(1260,580,12),(1170,292,11),(990,788,10),
(750,228,9),(785,610,9),(1015,310,10),(1020,650,9),(775,870,9),(900,32,11),(802,90,9),(1200,104,12)]
for xx,yy,r in centers:
    shrub(xx,yy,r,1)
    for j in range(rnd.randint(2,4)):
        dx=rnd.uniform(-42,42);dy=rnd.uniform(-30,30)
        shrub(xx+dx,yy+dy,r*rnd.uniform(.34,.76),rnd.uniform(.6,.92))
add('</g>')

# active course fairway: layered edge with dark drop-shade, rough, mown surfaces
add('<g id="active-hole-surfaces">')
pth(cap_band(lambda y:hw_base(y)+15,5),fill='#827f45',opacity='.63')
pth(cap_band(lambda y:hw_base(y)+7,12),fill='#bfa366',opacity='.94')
pth(cap_band(hw_base,20),fill='#697743',opacity='.95')
pth(cap_band(lambda y:hw_fair(y)+12,28),fill='#78944b',opacity='.99')
pth(cap_band(hw_fair,36),fill='url(#fairway)')
# translucent stripe bands clipped to organic fairway
add('<g clip-path="url(#fairwayClip)">')
for y in range(155,910,54):
    add(f'<rect x="710" y="{y}" width="380" height="27" fill="#d6e29a" opacity=".09"/>')
for y in range(200,900,78):
    # delicate horizontal grass contours
    cx=main_x(y)
    pth(f'M {fmt(cx-53)} {fmt(y)} Q {fmt(cx)} {fmt(y-11)} {fmt(cx+53)} {fmt(y-1)}',stroke='#e1e8a3',sw='1.1',opacity='.10')
add('</g>')
# subtle fairway curvature contour underneath the green
pth('M 844 183 Q 880 199 928 174',stroke='#d4e198',sw=2,opacity='.29')
# green surround / fringe and green, true overhead
pth(organic(899,133,55,82,11,56,.07),fill='#67834b',opacity='.87')
pth(organic(899,131,50,76,10,56,.075),fill='#a0bb65',opacity='.98')
pth(organic(899,130,42,65,9,56,.07),fill='url(#green)',opacity='1')
pth(organic(899,130,36,58,9,56,.07),fill='#b2dc87',opacity='.28')
# shallow green mowing contours
pth('M 866 143 Q 897 151 931 132',stroke='#e5f1b0',sw=1.5,opacity='.28')
pth('M 877 110 Q 904 120 927 102',stroke='#e5f1b0',sw=1,opacity='.22')
# tee box: small landform surrounded by fairway
pth(organic(856,867,21,16,2,32,.08),fill='#77924e')
add('<rect x="839" y="857" width="33" height="15" rx="4" fill="#e0e8bb" stroke="#60773f" stroke-width="2"/>')
add('<rect x="842" y="859" width="27" height="11" rx="3" fill="#f6f6d8"/>')
add('</g>')

# Bunker shapes anchored to active-hole edges (conceptual authoritative placeholders)
add('<g id="active-hole-bunkers">')
for tup in [(831,180,19,30,-10),(976,185,20,33,10),(813,333,19,33,-17),(1015,524,24,37,17),(787,660,20,30,18)]: bunker(*tup)
add('</g>')

# Sparse tufts around edge with crisp high-contrast near play center
add('<g id="rough-fescue">')
for i in range(190):
    y=rnd.uniform(170,860); x=main_x(y)+rnd.choice([-1,1])*rnd.uniform(77,135)
    sz=rnd.uniform(3,11)
    pth(f'M {fmt(x)} {fmt(y)} q {fmt(rnd.uniform(-4,4))} {fmt(-sz*.5)} {fmt(rnd.uniform(-3,3))} {fmt(-sz)}',stroke=rnd.choice(['#6d7847','#657440','#859650','#e3c773']),sw=fmt(rnd.uniform(.7,1.65)),opacity='.50')
add('</g>')

# hole UI overlay separately removable from decoration
add('<g id="play-overlay" pointer-events="none">')
pth('M 856 864 C 841 745 834 615 864 510 S 922 302 899 133',stroke='#9fdaf5',sw=2.2,opacity='.64',extra='stroke-dasharray="4 9"')
# target crosshair near landing zone
ellipse(880,470,15,15,'none',1,'stroke="#3696df" stroke-width="2.3"')
pth('M 880 449 V 491 M 859 470 H 901',stroke='#4cace8',sw=1.5,opacity='.95')
# ball at tee
circle(856,864,5.1,'#fefdf8')
# cup, flag
circle(898,130,4.9,'#1d2920')
pth('M 897 128 L 897 101',stroke='#f5e8ad',sw=2.1)
pth('M 897 101 L 919 108 L 897 115 Z',fill='#e94838')
add('</g>')
# lateral vignette covers all, known accepted Parkland style; no rectangular panel
add('<g id="lateral-lighting" pointer-events="none"><rect width="1600" height="900" fill="url(#lightBand)"/><rect width="1600" height="900" fill="url(#verticalTone)"/></g>')
add('</svg>')
OUT.write_text('\n'.join(S),encoding='utf-8')
print('SVG',OUT,OUT.stat().st_size, 'bytes', 'elements',len(S))
