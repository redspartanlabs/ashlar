"""Extract the Table pixel-instability evidence (issue #34) from CI test reports into evidence.tsv and images/.

Usage: python -I derive-evidence.py <artifacts-root> <output-folder>

<artifacts-root> holds one folder per downloaded artifact, named <run-id>-s<shard>, each with the artifact's junit/ folder:
  43386499851-s4  showcase-shard-4 of run 43386499851 (TablePixelTest with diagnostics, PR #32 at bf75e12)
  43547695812-s4  showcase-shard-4 of run 43547695812 (TablePixelTest with diagnostics, and the repeat study, PR #33)
  43547695812-s1  showcase-shard-1 of run 43547695812 (the replay study, PR #33)
  43569091894-s1  showcase-shard-1 of run 43569091894 (the retry study, PR #35)

Only the four cases tb6, tb8, tb19 and tb20 are extracted. Nothing is inferred: a pixel is written only when a report lists it
or when a printed picture shows it (see README.md for how an enlargement is read). Uses only the standard library.
"""
import base64
import glob
import os
import re
import struct
import sys
import zlib
from collections import defaultdict

ROOT, OUT = sys.argv[1], sys.argv[2]
TARGET = {'tb6': 'sort button hover', 'tb8': 'active sort button hover', 'tb19': 'clear selection focus-visible', 'tb20': 'clear selection hover'}
KEY_OF_CASE = {v: k for k, v in TARGET.items()}
GUTTER = 8
MARGIN = 6


def png(data):
    """Minimal PNG decoder: 8-bit RGB or RGBA, not interlaced. Returns (width, height, rows of (r, g, b, a))."""
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, w, h, ctype = 8, b'', None, None, None
    while pos < len(data):
        n, = struct.unpack('>I', data[pos:pos + 4])
        kind, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + n]
        pos += 12 + n
        if kind == b'IHDR':
            w, h, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', body)
            assert depth == 8 and interlace == 0 and ctype in (2, 6), (depth, ctype, interlace)
        elif kind == b'IDAT':
            idat += body
        elif kind == b'IEND':
            break
    bpp = 3 if ctype == 2 else 4
    raw, stride, rows, prev, i = zlib.decompress(idat), w * bpp, [], bytearray(w * bpp), 0
    for _ in range(h):
        f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 0xFF
            elif f == 2:
                line[x] = (line[x] + b) & 0xFF
            elif f == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 0xFF
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 0xFF
        rows.append([tuple(line[x * bpp:x * bpp + 3]) + ((line[x * bpp + 3],) if bpp == 4 else (255,)) for x in range(w)])
        prev = line
    return w, h, rows


def hexc(p):
    return '%02X%02X%02X' % p[:3]


def channel(a, b):
    return max(abs(int(a[i:i + 2], 16) - int(b[i:i + 2], 16)) for i in (0, 2, 4))


def system_out(path):
    t = open(path, encoding='utf-8').read()
    m = re.search(r'<system-out><!\[CDATA\[(.*?)\]\]></system-out>', t, re.S)
    return m.group(1) if m else t


DESC = re.compile(r'differing=(\d+) maxChannel=(\d+).*?bounds=x(\d+)\.\.(\d+),y(\d+)\.\.(\d+)')
PIX = re.compile(r'\((\d+),(\d+)\) #FF([0-9A-F]{6}) -> #FF([0-9A-F]{6})')
os.makedirs(os.path.join(OUT, 'images'), exist_ok=True)
obs = []


def save(name, b64):
    open(os.path.join(OUT, 'images', name), 'wb').write(base64.b64decode(b64))
    return name


# ---- TablePixelTest diagnostics
for f in sorted(glob.glob(os.path.join(ROOT, '*', 'junit', '*.cascade.TablePixelTest.xml'))):
    folder = os.path.basename(os.path.dirname(os.path.dirname(f)))
    run, shard = folder.split('-s')
    out = system_out(f)
    images = defaultdict(dict)
    for m in re.finditer(r'^TABLE-PIXELS image \[(\d+)\] (reference|candidate|zoom) ([A-Za-z0-9+/=]+)$', out, re.M):
        images[int(m.group(1))][m.group(2)] = m.group(3)
    for m in re.finditer(r'^TABLE-PIXELS (REJECTED|tolerated) \[(\d+)\] (\S+)( \(Tailwind removed\))? (tb\d+) \(([^)]*)\): (.*)$', out, re.M):
        verdict, n, config, free, field, case, rest = m.groups()
        if field not in TARGET:
            continue
        d = DESC.search(rest)
        n = int(n)
        o = dict(run=run, artifact='showcase-shard-' + shard, report='TablePixelTest', mode='tailwind-free' if free else 'with-tailwind', config=config, field=field,
                 case=case, kind='rejected' if verdict == 'REJECTED' else 'tolerated', differing=int(d.group(1)), maxChannel=int(d.group(2)),
                 bounds='x%s..%s,y%s..%s' % d.group(3, 4, 5, 6), pixels=[], image='-')
        if 'reference' in images[n] and 'candidate' in images[n]:
            ref = save('%s-TablePixelTest-%d-reference.png' % (run, n), images[n]['reference'])
            cand = save('%s-TablePixelTest-%d-candidate.png' % (run, n), images[n]['candidate'])
            w1, h1, r1 = png(base64.b64decode(images[n]['reference']))
            w2, h2, r2 = png(base64.b64decode(images[n]['candidate']))
            o['pixels'] = [(x, y, hexc(r1[y][x]), hexc(r2[y][x]), 'full-picture') for y in range(min(h1, h2)) for x in range(min(w1, w2)) if r1[y][x] != r2[y][x]]
            o['image'] = ref + ';' + cand
            text = {(int(a), int(b)): (c, e) for a, b, c, e in PIX.findall(rest.split(' | ')[0])}
            for x, y, a, b, _ in o['pixels']:
                if (x, y) in text and text[(x, y)] != (a, b):
                    raise SystemExit('picture and text disagree at %s,%s' % (x, y))
        else:
            o['pixels'] = [(int(a), int(b), c, e, 'text') for a, b, c, e in PIX.findall(rest.split(' | ')[0])]
        obs.append(o)

# ---- #33 studies: deviation lines (first pixels in text) and printed enlargements
for f in sorted(glob.glob(os.path.join(ROOT, '*', 'junit', '*TablePixel*StudyTest.xml'))):
    if 'Retry' in f:
        continue
    folder = os.path.basename(os.path.dirname(os.path.dirname(f)))
    run, shard = folder.split('-s')
    study = 'replay study' if 'Replay' in f else 'repeat study'
    out = system_out(f)
    zooms = {}
    for m in re.finditer(r'^TABLE-STUDY-(?:REPEAT|REPLAY) zoom ([0-9a-f]{10})-vs-([0-9a-f]{10}) ([A-Za-z0-9+/=]+)$', out, re.M):
        zooms[(m.group(1), m.group(2))] = m.group(3)
    for m in re.finditer(r'^    process \d+ slot \d+: (.+?) (light/\w+|dark/\w+) (L|Mm|Mp): ([0-9a-f]{10}) vs ([0-9a-f]{10}): (.*)$', out, re.M):
        case, config, variant, a, b, rest = m.groups()
        if case not in KEY_OF_CASE:
            continue
        d = DESC.search(rest)
        if not d:
            continue
        minx, maxx, miny, maxy = map(int, d.group(3, 4, 5, 6))
        text = {(int(x), int(y)): (c, e) for x, y, c, e in PIX.findall(rest)}
        o = dict(run=run, artifact='showcase-shard-' + shard, report=study, mode='with-tailwind', config=config, field=KEY_OF_CASE[case], case=case,
                 kind='alternate in ' + variant, differing=int(d.group(1)), maxChannel=int(d.group(2)), bounds='x%d..%d,y%d..%d' % (minx, maxx, miny, maxy),
                 pixels=[], image='-')
        key = (a, b) if (a, b) in zooms else ((b, a) if (b, a) in zooms else None)
        if key:
            name = '%s-%s-zoom-%s-vs-%s.png' % (run, study.split()[0], key[0], key[1])
            save(name, zooms[key])
            w, h, rows = png(base64.b64decode(zooms[key]))
            x0, y0, y1 = max(0, minx - MARGIN), max(0, miny - MARGIN), maxy + MARGIN
            scale = h // (y1 - y0 + 1)
            panel = (w - 2 * GUTTER) // 3
            swapped = key != (a, b)
            pts = sorted({(x0 + (x - 2 * (panel + GUTTER)) // scale, y0 + y // scale) for y in range(h)
                          for x in range(2 * (panel + GUTTER), 2 * (panel + GUTTER) + panel) if rows[y][x][:3] == (255, 0, 255)}, key=lambda p: (p[1], p[0]))
            for pt in pts:
                cy = (pt[1] - y0) * scale + scale // 2
                ca = hexc(rows[cy][(pt[0] - x0) * scale + scale // 2])
                cb = hexc(rows[cy][panel + GUTTER + (pt[0] - x0) * scale + scale // 2])
                if swapped:
                    ca, cb = cb, ca
                src = 'enlargement'
                if pt in text:
                    if text[pt] != (ca, cb):
                        raise SystemExit('enlargement and text disagree at %s: %s vs %s' % (pt, text[pt], (ca, cb)))
                    src = 'text+enlargement'
                o['pixels'].append((pt[0], pt[1], ca, cb, src))
            o['image'] = name
        else:
            o['pixels'] = [(x, y, c, e, 'text') for (x, y), (c, e) in sorted(text.items(), key=lambda kv: (kv[0][1], kv[0][0]))]
        obs.append(o)

# ---- #35 retry study: rejected primary pairs (counts only; the report lists no pixels)
for f in sorted(glob.glob(os.path.join(ROOT, '*', 'junit', '*TablePixelRetryStudyTest.xml'))):
    folder = os.path.basename(os.path.dirname(os.path.dirname(f)))
    run, shard = folder.split('-s')
    out = system_out(f)
    for m in re.finditer(r'^  attempt \d+ \(process \d+\) block \d+ (\S+) (tb\d+) (.+?), navigations \d+-\d+: (differing=.*?) \|', out, re.M):
        config, field, case, rest = m.groups()
        if field not in TARGET:
            continue
        d = DESC.search(rest)
        obs.append(dict(run=run, artifact='showcase-shard-' + shard, report='retry study', mode='with-tailwind', config=config, field=field, case=case,
                        kind='rejected', differing=int(d.group(1)), maxChannel=int(d.group(2)), bounds='x%s..%s,y%s..%s' % d.group(3, 4, 5, 6), pixels=[], image='-'))

obs.sort(key=lambda o: (int(o['field'][2:]), o['config'], o['run'], o['report'], o['kind'], o['bounds']))
with open(os.path.join(OUT, 'evidence.tsv'), 'w', encoding='utf-8', newline='\n') as t:
    t.write('\t'.join(['observation', 'run', 'artifact', 'report', 'mode', 'configuration', 'field', 'case', 'kind', 'differing', 'max_channel', 'bounds', 'complete',
                       'x', 'y', 'color_a', 'color_b', 'channel', 'pixel_source', 'image']) + '\n')
    for i, o in enumerate(obs, 1):
        complete = 'yes' if len(o['pixels']) == o['differing'] else 'no'
        base = ['obs-%03d' % i, o['run'], o['artifact'], o['report'], o['mode'], o['config'], o['field'], o['case'], o['kind'], str(o['differing']), str(o['maxChannel']),
                o['bounds'], complete]
        if not o['pixels']:
            t.write('\t'.join(base + ['-', '-', '-', '-', '-', 'none', o['image']]) + '\n')
        for x, y, a, b, src in o['pixels']:
            t.write('\t'.join(base + [str(x), str(y), a, b, str(channel(a, b)), src, o['image']]) + '\n')
print('%d observations, %d images' % (len(obs), len(os.listdir(os.path.join(OUT, 'images')))))
