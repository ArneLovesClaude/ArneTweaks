"""Packt bundled-styles/ in ZIPs fuer den Cloud-Download und schreibt index.json.

Aufruf:  python tools/pack_cloud.py bundled-styles build/cloud-styles

Pro Kategorie entsteht <kategorie>.zip mit allen Styles dieser Kategorie.
Ausnahme: Kategorien in PER_STYLE (Himmel, sehr gross). Dort enthaelt <kategorie>.zip nur die
kleinen Dateien (style.json, preview.png, Dateien bis SMALL_FILE Bytes) fuer die Vorschau im Menue,
und jeder Style hat zusaetzlich ein eigenes <kategorie>__<style>.zip mit allen Dateien.

Die ZIPs sind reproduzierbar (feste Reihenfolge und Zeitstempel): gleicher Inhalt = gleiche SHA-256.
Die Mod laedt eine Datei nur neu, wenn sich ihre SHA-256 in index.json geaendert hat.
"""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

PER_STYLE = {"sky"}
SMALL_FILE = 64 * 1024
FIXED_TIME = (2026, 1, 1, 0, 0, 0)


def files_of(folder: Path):
    return sorted((p for p in folder.rglob("*") if p.is_file()), key=lambda p: p.as_posix().lower())


def write_zip(target: Path, entries):
    """entries: Liste von (Pfad im ZIP, Datei auf der Platte)."""
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for name, path in entries:
            info = zipfile.ZipInfo(name, FIXED_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            zf.writestr(info, path.read_bytes())


def describe(path: Path):
    data = path.read_bytes()
    return {"file": path.name, "sha256": hashlib.sha256(data).hexdigest(), "size": len(data)}


def main():
    src = Path(sys.argv[1])
    out = Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for old in out.glob("*.zip"):
        old.unlink()
    index = {"format": 1, "categories": {}}
    for category in sorted(p for p in src.iterdir() if p.is_dir()):
        cat = category.name
        styles = sorted((p for p in category.iterdir() if p.is_dir()), key=lambda p: p.name.lower())
        entry_list = []
        per_style = {}
        for style in styles:
            for f in files_of(style):
                rel = f"{style.name}/{f.relative_to(style).as_posix()}"
                if cat in PER_STYLE and f.name not in ("style.json", "preview.png") and f.stat().st_size > SMALL_FILE:
                    continue
                entry_list.append((rel, f))
            if cat in PER_STYLE:
                full = out / f"{cat}__{style.name}.zip"
                write_zip(full, [(f.relative_to(style).as_posix(), f) for f in files_of(style)])
                per_style[style.name] = describe(full)
        bundle = out / f"{cat}.zip"
        write_zip(bundle, entry_list)
        info = describe(bundle)
        if per_style:
            info["styles"] = per_style
        index["categories"][cat] = info
    (out / "index.json").write_text(json.dumps(index, indent=1), encoding="utf-8")
    total = sum(p.stat().st_size for p in out.glob("*.zip"))
    print(f"{len(list(out.glob('*.zip')))} ZIPs, {total / 1024 / 1024:.1f} MB -> {out}")


if __name__ == "__main__":
    main()
