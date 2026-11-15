"""Fasst Styles einer Kategorie zusammen, die exakt dieselben Dateien haben (z. B. derselbe Himmel in zwei Packs).

Der erste Style bleibt, bekommt beide Namen ("A / B"), die anderen werden geloescht.
Aufruf: python -I dedupe_styles.py <styles-ordner> [kategorie ...]   (Standard: alle Kategorien)
"""
import hashlib
import json
import shutil
import sys
from pathlib import Path

IGNORE = {"style.json", "preview.png"}


def fingerprint(style_dir):
    digest = hashlib.sha256()
    for f in sorted(p for p in style_dir.rglob("*") if p.is_file() and p.relative_to(style_dir).as_posix() not in IGNORE):
        digest.update(f.relative_to(style_dir).as_posix().encode())
        digest.update(hashlib.sha256(f.read_bytes()).digest())
    return digest.hexdigest()


def name_of(style_dir):
    try:
        return json.loads((style_dir / "style.json").read_text(encoding="utf-8")).get("name", style_dir.name)
    except Exception:
        return style_dir.name


def main():
    root = Path(sys.argv[1])
    categories = sys.argv[2:] or sorted(p.name for p in root.iterdir() if p.is_dir())
    for cat in categories:
        groups = {}
        for style in sorted((root / cat).iterdir()) if (root / cat).is_dir() else []:
            if style.is_dir():
                groups.setdefault(fingerprint(style), []).append(style)
        for styles in groups.values():
            if len(styles) < 2:
                continue
            keep = styles[0]
            names = [name_of(s) for s in styles]
            # Zweitnamen: eine gespeicherte Auswahl mit einer der weggefallenen IDs zeigt dann auf diesen Style.
            data = {"name": " / ".join(names), "aliases": [s.name for s in styles[1:]]}
            (keep / "style.json").write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
            for other in styles[1:]:
                shutil.rmtree(other)
            print(f"{cat}: {' + '.join(s.name for s in styles)} -> {keep.name}")


if __name__ == "__main__":
    main()
