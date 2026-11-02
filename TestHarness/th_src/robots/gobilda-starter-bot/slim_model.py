"""
Makes the goBILDA Starter Bot model the Unity replay draws, from the OBJ in the art originals (C:\Dev\VI_Arena-art, see VI_Arena/ART_INBOX.md).

The OBJ came from goBILDA's STEP file (3200-2627-0003.step), converted to OBJ by imagetostl.com:
3.4 million triangles, one object per color. This script
  1. splits each color into connected parts (vertices closer than 0.02 mm count as one),
  2. drops parts smaller than 25 mm (screws, nuts, standoffs), except inside the wheels,
  3. gathers the six wheels into their own named objects so Unity can spin them,
  4. simplifies everything by vertex clustering (5 mm grid, 3.5 mm for the wheels),
and writes about 140,000 triangles (about 4 MB). Plain Python 3, no extra packages.

Run from Harness/:
  python th_src/robots/gobilda-starter-bot/slim_model.py C:/Dev/VI_Arena-art/GoBuilda_StarterBot.obj VI_Arena/src/Assets/VI_Arena/Resources/Robots/gobilda-starter-bot/gobilda-starter-bot.obj

The model is in millimeters, z up, and the robot's front (where the mechanism sticks out) is -y.
The .mtl next to the output holds the colors from the converter's .mtl.
"""
import sys
path=sys.argv[1]; out=sys.argv[2]; cell=5.0; wheelcell=3.5
# Splits each material group of the OBJ into connected parts (vertices welded by position) and
# saves every part's material, triangle count and bounds, plus the parts' triangles, for later steps.
verts=[]; groups=[]; cur=None
for line in open(path,encoding='utf-8'):
    c=line[:2]
    if c=='v ':
        a=line.split(); verts.append((float(a[1]),float(a[2]),float(a[3])))
    elif c=='f ':
        a=line.split(); cur[1].append((int(a[1])-1,int(a[2])-1,int(a[3])-1))
    elif c=='o ':
        cur=[None,[]]; groups.append(cur)
    elif line.startswith('usemtl'):
        cur[0]=line.split()[1]
key={}; weld=[0]*len(verts)
for i,(x,y,z) in enumerate(verts):
    k=(round(x*50),round(y*50),round(z*50))
    weld[i]=key.setdefault(k,len(key))
parent=list(range(len(key)))
def find(a):
    while parent[a]!=a:
        parent[a]=parent[parent[a]]; a=parent[a]
    return a
parts=[]
for mtl,tris in groups:
    for t in tris:
        a,b,c=find(weld[t[0]]),find(weld[t[1]]),find(weld[t[2]])
        if a!=b: parent[b]=a
        if a!=c: parent[find(c)]=find(a)
for gi,(mtl,tris) in enumerate(groups):
    byroot={}
    for t in tris:
        byroot.setdefault(find(weld[t[0]]),[]).append(t)
    for r,ts in byroot.items():
        vs=set(i for t in ts for i in t)
        mn=[min(verts[i][k] for i in vs) for k in range(3)]; mx=[max(verts[i][k] for i in vs) for k in range(3)]
        parts.append({'g':gi,'mtl':mtl,'tris':ts,'min':mn,'max':mx})
# Makes the small Unity model from parts.pkl: drops tiny parts (screws, nuts), gathers each wheel's
# parts into one named object, and simplifies by vertex clustering (each part on its own grid).
V=verts; P=parts
# wheel centers (x, y), radius 48, z center 0; names say which side and which end (-y is the front)
wheels={'wheelLeftFront':(201,-144),'wheelLeftMiddle':(201,0),'wheelLeftRear':(201,144),
        'wheelRightFront':(-201,-144),'wheelRightMiddle':(-201,0),'wheelRightRear':(-201,144)}
def wheel_of(p):
    for n,(x,y) in wheels.items():
        if p['min'][0]>=x-14 and p['max'][0]<=x+14 and p['min'][1]>=y-50 and p['max'][1]<=y+50 and p['min'][2]>=-50 and p['max'][2]<=50:
            return n
    return None
objs={}
for p in P:
    size=max(p['max'][k]-p['min'][k] for k in range(3))
    w=wheel_of(p)
    if w is None and size<25: continue
    objs.setdefault(w or 'chassis',[]).append(p)
vout=[]; lines=[]; total=0
for name in ['chassis']+list(wheels):
    lines.append('g '+name)
    bymtl={}
    for p in objs.get(name,[]):
        c=wheelcell if name!='chassis' else cell
        cellmap={}; sums=[]
        def vid(i):
            x,y,z=V[i]; k=(int(x//c),int(y//c),int(z//c))
            j=cellmap.get(k)
            if j is None:
                j=len(sums); cellmap[k]=j; sums.append([0.0,0.0,0.0,0])
            s=sums[j]; s[0]+=x; s[1]+=y; s[2]+=z; s[3]+=1
            return j
        tris=set(); local=[]
        for t in p['tris']:
            a,b,cc=vid(t[0]),vid(t[1]),vid(t[2])
            if a==b or b==cc or a==cc: continue
            key=tuple(sorted((a,b,cc)))
            if key in tris: continue
            tris.add(key); local.append((a,b,cc))
        base=len(vout)
        for s in sums: vout.append((s[0]/s[3],s[1]/s[3],s[2]/s[3]))
        bymtl.setdefault(p['mtl'],[]).extend((a+base,b+base,cc+base) for a,b,cc in local)
    for m,ts in sorted(bymtl.items()):
        lines.append('usemtl '+m)
        lines.extend('f %d %d %d'%(a+1,b+1,c+1) for a,b,c in ts); total+=len(ts)
with open(out,'w',newline='\n') as f:
    f.write('# goBILDA FTC Starter Bot (2026-2027), simplified for the PVI harness replay. Millimeters, z up, front -y.\n')
    f.write('mtllib gobilda-starter-bot.mtl\n')
    f.writelines('v %.2f %.2f %.2f\n'%v for v in vout)
    f.write('\n'.join(lines)+'\n')
print('triangles',total,'vertices',len(vout),{k:len(v) for k,v in objs.items()})
