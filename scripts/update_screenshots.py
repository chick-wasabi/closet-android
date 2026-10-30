#!/usr/bin/env python3
"""Refresh public screenshots from actual Compose renders; detect stale UI documentation."""
import argparse, hashlib, json, pathlib, shutil, sys
ROOT = pathlib.Path(__file__).resolve().parents[1]
RENDERS = ROOT / 'app/build/outputs/design-review'
DEST = ROOT / 'docs/screenshots'
FILES = {'closet-populated-light.png':'closet.png', 'studio-light.png':'outfits.png', 'lookbook-dark.png':'saved.png', 'closet-populated-dark.png':'dark-mode.png', 'editor-light.png':'editor.png', 'settings-light.png':'settings.png', 'detail-light.png':'detail.png'}

LEGACY = {'closet.png':'closet-light.png','dark-mode.png':'closet-dark.png','outfits.png':'studio-light.png','saved.png':'lookbook-dark.png'}

def source_hash():
    digest = hashlib.sha256()
    paths = sorted((ROOT/'app/src/main/java/dev/closet').rglob('*.kt')) + sorted((ROOT/'app/src/main/assets').rglob('*')) + sorted((ROOT/'app/src/main/res').rglob('*')) + sorted((ROOT/'app/src/test/java/dev/closet/ui').glob('*.kt'))
    for path in paths:
        if path.is_file():
            digest.update(path.relative_to(ROOT).as_posix().encode())
            content = path.read_bytes()
            if path.suffix in {'.kt','.xml'}: content = content.replace(b'\r\n',b'\n')
            digest.update(content)
    return digest.hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--check',action='store_true'); args=parser.parse_args()
    glossary = ROOT/'GLOSSARY.md'
    bundled = ROOT/'app/src/main/assets/glossary.md'
    if args.check:
        if glossary.read_bytes() != bundled.read_bytes(): raise ValueError('Offline glossary is stale. Run scripts/update_screenshots.py.')
    else:
        shutil.copyfile(glossary, bundled)
    manifest = {'schema':1,'ui_source_sha256':source_hash(),'screenshots':list(FILES.values()),'sample_data':'Illustrated example wardrobe, rendered with the actual app components.'}
    if args.check:
        actual = json.loads((DEST/'manifest.json').read_text())
        if actual != manifest: raise ValueError('UI screenshots are stale. Run testDebugUnitTest and scripts/update_screenshots.py.')
        if any(not (DEST/name).is_file() for name in FILES.values()): raise ValueError('Public screenshot is missing.')
        for src,target in LEGACY.items():
            if (DEST/src).read_bytes() != (ROOT/'docs/design'/target).read_bytes(): raise ValueError('Legacy screenshot link is stale. Run scripts/update_screenshots.py.')
        print('Public screenshots and offline glossary match the current source.'); return
    missing = [name for name in FILES if not (RENDERS/name).is_file()]
    if missing: raise ValueError('Generate current UI renders with testDebugUnitTest first. Missing: '+', '.join(missing))
    DEST.mkdir(parents=True,exist_ok=True)
    for src,target in FILES.items(): shutil.copyfile(RENDERS/src, DEST/target)
    for src,target in LEGACY.items(): shutil.copyfile(DEST/src, ROOT/'docs/design'/target)
    (DEST/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Public app screenshots refreshed.')

if __name__=='__main__':
    try: main()
    except (ValueError,OSError) as error: print(str(error),file=sys.stderr);sys.exit(1)
