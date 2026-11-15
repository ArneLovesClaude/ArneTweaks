"""Sortiert Dateien aus entpackten Texture-Packs in Arne-Styles-Ordner.

Aufruf: python -I sort_packs.py <ziel-styles-ordner> <pack-ordner> <style-id> <anzeigename> [...weitere Tripel]

Der Pack-Ordner darf auch der entpackte ZIP-Ordner sein, in dem das eigentliche Pack in einem
Unterordner liegt. Optional: Umgebungsvariable ARNE_VANILLA_ITEMS = Ordner items/ der Vanilla-Assets.
"""
import json
import os
import re
import shutil
import sys
from pathlib import Path

MAT = r"(wooden|stone|iron|golden|diamond|netherite|copper)"
ARMOR_ITEM = r"\w+_(helmet|chestplate|leggings|boots)"
ORES = r"(coal|copper|iron|gold|redstone|lapis|diamond|emerald)_ore"

SNOW_BLOCKS = r"snow|snow_block|powder_snow|grass_block_snow|ice|packed_ice|blue_ice"
DEEPSLATE_BLOCKS = r"deepslate|deepslate_top|cobbled_deepslate|polished_deepslate|deepslate_bricks|deepslate_tiles|cracked_deepslate_\w+|tuff"
STONE_BLOCKS = (rf"stone|smooth_stone|smooth_stone_slab_side|stone_bricks|cracked_stone_bricks|mossy_stone_bricks|mossy_cobblestone"
                rf"|andesite|diorite|granite|polished_(andesite|diorite|granite)|calcite|gravel|sand|sandstone(_top|_bottom)?|bedrock|clay")
# Erze (normal und Deepslate) haben eine eigene Kategorie, auch mit Unterordnern wie redstone_ore/on.png
ORE_FILES = rf"(deepslate_)?{ORES}(/\w+)?"
GRASS_BLOCKS = (r"grass_block(_top|_side|_side_overlay)?|short_grass|tall_grass(_top|_bottom)?|fern|large_fern_(top|bottom)"
                r"|dirt|coarse_dirt|rooted_dirt|dirt_path(_top|_side)?|podzol(_top|_side)?|mycelium(_top|_side)?|farmland(_moist)?")

FOOD = (r"cooked_\w+|beef|porkchop|chicken|mutton|rabbit|cod|salmon|bread|golden_carrot|carrot|baked_potato|potato|apple"
        r"|cookie|melon_slice|sweet_berries|glow_berries|pumpkin_pie|mushroom_stew|rabbit_stew|beetroot_soup|suspicious_stew|dried_kelp|honey_bottle")
BUCKETS = r"bucket|\w+_bucket"
BOWS = r"bow(_pulling_\d)?|crossbow\w*|arrow|spectral_arrow|tipped_arrow\w*"
BLOCKS = r"cobblestone|\w+_planks"

# Reihenfolge zaehlt: die erste passende Regel gewinnt. Pfade relativ zu assets/minecraft/, ohne ".mcmeta".
RULES = [
    ("crystal", r"textures/entity/end_crystal/.+\.png"),
    ("totems", r"textures/item/totem_of_undying\.png"),
    ("totems", r"(models/item|items)/totem_of_undying\.json"),
    ("totems", r"textures/particle/glitter_[0-7]\.png"),
    ("totems", r"particles/totem_of_undying\.json"),
    ("totems", r"sounds/item/totem/.+\.ogg"),
    ("glowstone", r"textures/block/glowstone\.png"),
    ("gaps", r"textures/item/(enchanted_)?golden_apple\.png"),
    ("gaps", r"(models/item|items)/(enchanted_)?golden_apple\.json"),
    ("xp_bottles", r"textures/item/experience_bottle\.png"),
    ("xp_bottles", r"textures/entity/experience_orb\.png"),
    ("xp_bottles", r"(models/item|items)/experience_bottle\.json"),
    ("pearls", r"textures/item/ender_pearl\.png"),
    ("pearls", r"(models/item|items)/ender_pearl\.json"),
    ("potions", r"textures/item/(potion|splash_potion|lingering_potion|potion_overlay)\.png"),
    ("potions", r"(models/item|items)/(potion|splash_potion|lingering_potion)\.json"),
    ("wind_charges", r"textures/item/wind_charge\.png"),
    ("wind_charges", r"textures/entity/projectiles/wind_charge\.png"),
    ("wind_charges", r"(models/item|items)/wind_charge\.json"),
    ("shield", r"textures/entity/(shield/.+|shield_base\w*)\.png"),
    ("shield", r"(models/item|items)/shield(_blocking)?\.json"),
    ("elytra", r"textures/entity/equipment/wings/.+\.png"),
    ("elytra", r"textures/item/elytra(_broken)?\.png"),
    ("elytra", r"(models/item|items)/elytra(_broken)?\.json"),
    ("glint", r"textures/misc/enchanted_glint_(item|armor)\.png"),
    ("fire", r"textures/block/(soul_)?fire_[01]\.png"),
    ("obsidian", r"textures/block/(crying_)?obsidian(_e)?\.png"),
    ("anchor", r"textures/block/respawn_anchor_\w+\.png"),
    ("inventory", r"textures/gui/(container|sprites/container)/.+\.png"),
    ("crosshair", r"textures/gui/sprites/hud/crosshair\w*\.png"),
    ("hud", r"textures/gui/sprites/hud/.+\.png"),
    ("swords", rf"textures/item/{MAT}_sword\.png"),
    ("swords", rf"(models/item|items)/{MAT}_sword\.json"),
    ("axes", rf"textures/item/{MAT}_axe\.png"),
    ("axes", rf"(models/item|items)/{MAT}_axe\.json"),
    ("mace", r"textures/item/mace\.png"),
    ("mace", r"(models/item|items)/(mace|handheld_mace)\.json"),
    ("spears", r"textures/item/\w+_spear(_in_hand)?\.png"),
    ("spears", r"(models/item|items)/(\w+_spear(_in_hand)?|spear_in_hand)\.json"),
    ("bows", rf"textures/item/({BOWS})\.png"),
    ("bows", r"textures/entity/projectiles/(arrow|spectral_arrow|tipped_arrow)\.png"),
    ("bows", rf"(models/item|items)/({BOWS})\.json"),
    ("tools", rf"textures/item/({MAT}_(pickaxe|shovel|hoe)|trident)\.png"),
    ("armor", r"textures/entity/equipment/.+\.png"),
    ("armor", rf"textures/item/{ARMOR_ITEM}\.png"),
    ("sky", r"textures/environment/.+\.png"),
    ("sky", r"(optifine|mcpatcher)/sky/.+"),
    ("particles", r"textures/particle/.+\.png"),
    ("cobwebs", r"textures/block/cobweb\.png"),
    ("cobwebs", r"(blockstates|models/item|items)/cobweb\.json"),
    ("blocks", rf"textures/block/({BLOCKS})\.png"),
    ("blocks", rf"blockstates/({BLOCKS})\.json"),
    ("shulker", r"textures/entity/shulker/.+\.png"),
    ("shulker", r"textures/block/\w*shulker_box\.png"),
    ("snow", rf"textures/block/({SNOW_BLOCKS})\.png"),
    ("snow", rf"blockstates/({SNOW_BLOCKS})\.json"),
    ("ores", rf"textures/block/({ORE_FILES})\.png"),
    ("ores", rf"(blockstates|models/block)/({ORE_FILES})\.json"),
    ("deepslate", rf"textures/block/({DEEPSLATE_BLOCKS})\.png"),
    ("deepslate", rf"blockstates/({DEEPSLATE_BLOCKS})\.json"),
    ("stone", rf"textures/block/({STONE_BLOCKS})\.png"),
    ("stone", rf"blockstates/({STONE_BLOCKS})\.json"),
    ("grass", rf"textures/block/({GRASS_BLOCKS})\.png"),
    ("grass", r"textures/colormap/(grass|foliage)\.png"),
    ("grass", rf"blockstates/({GRASS_BLOCKS})\.json"),
    ("buckets", rf"textures/item/({BUCKETS})\.png"),
    ("buckets", rf"(models/item|items)/({BUCKETS})\.json"),
    ("food", rf"textures/item/({FOOD})\.png"),
    ("food", rf"(models/item|items)/({FOOD})\.json"),
    ("sounds", r"sounds/(?!.*/\._)(?!\._).+\.ogg"),
    ("sounds", r"sounds\.json"),
    ("items", r"textures/(item|custom/item)/.+\.png"),
    ("items", r"(models/item|items)/.+\.json"),
]

# Kategorien, deren Modelle/Blockstates auf weitere Dateien im Pack verweisen koennen.
WITH_REFERENCES = ("items", "totems", "gaps", "xp_bottles", "pearls", "potions", "wind_charges", "shield", "elytra", "swords",
                   "axes", "mace", "spears", "bows", "cobwebs", "blocks", "buckets", "food", "snow", "deepslate", "stone", "ores", "grass")

# Eigene Himmel-Formate (brauchen eine Himmel-Mod wie Skyboxify oder Nuit).
SKY_NAMESPACES = ("fabricskyboxes", "celestial", "nuit")

# Alte Dateinamen -> aktuelle Namen in 1.21.11.
RENAMES = {
    "textures/block/grass.png": "textures/block/short_grass.png",
    "textures/misc/enchanted_item_glint.png": "textures/misc/enchanted_glint_item.png",
    "textures/misc/enchanted_glint_entity.png": "textures/misc/enchanted_glint_armor.png",
    "textures/entity/elytra.png": "textures/entity/equipment/wings/elytra.png",
}

# Altes Ruestungsformat (bis 1.21.1) -> neues Format (ab 1.21.2).
OLD_ARMOR = re.compile(r"textures/models/armor/(\w+?)_layer_([12])(_overlay)?\.png")
ARMOR_NAMES = {"gold": "gold", "chainmail": "chainmail", "diamond": "diamond", "iron": "iron",
               "leather": "leather", "netherite": "netherite", "turtle": "turtle_scute"}

VANILLA_ITEMS = None

# Vanilla-Modelle, die anderen Modellen als Eltern dienen (z. B. item/generated, block/cube_all).
# Aendert ein Pack so ein Modell, wirkt das auf ALLE Items/Bloecke. Deshalb bekommt jeder Style
# eine eigene, umbenannte Kopie davon (models/arnestyles/<kategorie>/...), die nur seine eigenen Modelle nutzen.
SHARED_MODELS = set()


def load_shared_models(models_dir):
    counts = {}
    for f in Path(models_dir).rglob("*.json"):
        data = load_json(f)
        parent = data.get("parent") if isinstance(data, dict) else None
        if isinstance(parent, str):
            pid = parent.removeprefix("minecraft:")
            counts[pid] = counts.get(pid, 0) + 1
    return {pid for pid, n in counts.items() if n >= 2 and not pid.startswith("builtin/")}


def model_id(rel):
    """models/item/generated.json -> item/generated"""
    return rel[len("models/"):-len(".json")] if rel.startswith("models/") and rel.endswith(".json") else None


def isolated_id(cat, mid):
    return f"arnestyles/{cat}/{mid}"


def rewrite_refs(value, mapping):
    """Ersetzt in einer JSON-Struktur alle Verweise auf abgeschirmte Modelle."""
    if isinstance(value, str):
        key = value.removeprefix("minecraft:")
        return "minecraft:" + mapping[key] if key in mapping else value
    if isinstance(value, dict):
        return {k: rewrite_refs(v, mapping) for k, v in value.items()}
    if isinstance(value, list):
        return [rewrite_refs(v, mapping) for v in value]
    return value


def category_of(rel):
    for cat, pattern in RULES:
        if re.fullmatch(pattern, rel):
            return cat
    return None


def strings_in(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for v in value.values():
            yield from strings_in(v)
    elif isinstance(value, list):
        for v in value:
            yield from strings_in(v)


def load_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception:
        return None


def references(mc, rel):
    """Modelle und Texturen aus dem Pack, auf die eine JSON-Datei verweist."""
    data = load_json(mc / rel)
    if data is None:
        return []
    found = []
    for s in strings_in(data):
        if ":" in s:
            ns, path = s.split(":", 1)
        else:
            ns, path = "minecraft", s
        if not re.fullmatch(r"[a-z0-9_./-]+", path) or not re.fullmatch(r"[a-z0-9_.-]+", ns):
            continue
        # Andere Namespaces (z. B. Ess-Animationen eines Packs) liegen neben assets/minecraft.
        prefix = "" if ns == "minecraft" else f"../{ns}/"
        for candidate in (f"{prefix}models/{path}.json", f"{prefix}textures/{path}.png"):
            if (mc / candidate).is_file():
                found.append(candidate)
    return found


VANILLA_MODELS_DIR = None


def unresolved_models(mc, rel):
    """Modell-Verweise einer items/*.json, die weder im Pack noch in Minecraft existieren."""
    data = load_json(mc / rel)
    missing = []
    for key, value in iter_model_refs(data):
        ns, path = value.split(":", 1) if ":" in value else ("minecraft", value)
        prefix = "" if ns == "minecraft" else f"../{ns}/"
        in_pack = (mc / f"{prefix}models/{path}.json").is_file()
        in_vanilla = ns == "minecraft" and VANILLA_MODELS_DIR is not None and (Path(VANILLA_MODELS_DIR) / f"{path}.json").is_file()
        if not in_pack and not in_vanilla:
            missing.append(value)
    return missing


def iter_model_refs(value):
    """Alle "model": "..."-Eintraege (Text) in einer Item-Definition."""
    if isinstance(value, dict):
        for k, v in value.items():
            if k in ("model", "base") and isinstance(v, str):
                yield k, v
            else:
                yield from iter_model_refs(v)
    elif isinstance(value, list):
        for v in value:
            yield from iter_model_refs(v)


def copy(mc, rel, dest_root, dest_rel=None, mapping=None):
    dest_rel = dest_rel or rel
    # normpath loest "../<namespace>/" auf (Dateien aus anderen Namespaces neben assets/minecraft)
    target = Path(os.path.normpath(dest_root / "assets" / "minecraft" / dest_rel))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(mc / rel, target)
    if rel.endswith(".json"):
        data = load_json(target)
        changed = False
        # "builtin/entity" gibt es seit 1.21.4 nicht mehr: Verweis entfernen, Anzeige-Werte behalten.
        if isinstance(data, dict) and str(data.get("parent", "")).removeprefix("minecraft:") == "builtin/entity":
            del data["parent"]
            changed = True
        if mapping and data is not None:
            new = rewrite_refs(data, mapping)
            changed = changed or new != data
            data = new
        if changed:
            target.write_text(json.dumps(data, indent=2), encoding="utf-8")
    meta = mc / (rel + ".mcmeta")
    if meta.is_file():
        shutil.copyfile(meta, target.with_name(target.name + ".mcmeta"))


def find_root(pack_dir):
    """Ordner mit assets/ finden (ZIPs haben das Pack oft in einem Unterordner, Mac-Reste ignorieren)."""
    if (pack_dir / "assets").is_dir():
        return pack_dir
    for candidate in sorted(pack_dir.rglob("assets")):
        if candidate.is_dir() and "__MACOSX" not in candidate.parts and len(candidate.relative_to(pack_dir).parts) <= 3:
            return candidate.parent
    return None


def sky_namespaces(root):
    """Himmel-Namespaces (fabricskyboxes, celestial ...) und alle Namespaces, auf die deren JSON verweist."""
    assets = root / "assets"
    found = {ns for ns in SKY_NAMESPACES if (assets / ns).is_dir()}
    for ns in list(found):
        for f in (assets / ns).rglob("*.json"):
            data = load_json(f)
            for s in strings_in(data) if data is not None else ():
                m = re.match(r"([a-z0-9_.-]+):", s)
                if m and m.group(1) not in ("minecraft", "realms") and (assets / m.group(1)).is_dir():
                    found.add(m.group(1))
    return sorted(found)


def sky_preview(dest, root, sky_only):
    """preview.png fuer Himmel: bei reinen Himmel-Packs das pack.png, sonst das erste Himmel-Bild."""
    target = dest / "preview.png"
    if sky_only and (root / "pack.png").is_file():
        shutil.copyfile(root / "pack.png", target)
        return
    candidates = []
    for sub in ("assets/minecraft/optifine/sky", "assets/minecraft/mcpatcher/sky", *(f"assets/{ns}" for ns in SKY_NAMESPACES)):
        folder = dest / sub
        if folder.is_dir():
            candidates += sorted(folder.rglob("*.png"), key=lambda p: -p.stat().st_size)
    if candidates:
        shutil.copyfile(candidates[0], target)
    elif (root / "pack.png").is_file() and sky_only:
        shutil.copyfile(root / "pack.png", target)


def sort_pack(styles_root, pack_dir, style_id, name):
    root = find_root(pack_dir)
    if root is None:
        print(f"{style_id}: kein assets-Ordner gefunden, uebersprungen")
        return
    mc = root / "assets" / "minecraft"
    files = sorted(p.relative_to(mc).as_posix() for p in mc.rglob("*") if p.is_file() and not p.name.endswith(".mcmeta")) if mc.is_dir() else []
    by_cat = {}
    for rel in files:
        # Item-Definitionen fuer Items, die es in Minecraft nicht gibt (Reste anderer Mods), weglassen.
        m = re.fullmatch(r"items/(.+)\.json", rel)
        # mcpatcher/sky ist der alte Name von optifine/sky: doppelt, wenn beides da ist. Mac-Reste ebenfalls weglassen.
        if (rel.startswith("mcpatcher/sky/") and (mc / "optifine" / "sky").is_dir()) or Path(rel).name in (".DS_Store", "Thumbs.db")                 or Path(rel).name.startswith("._"):
            continue
        if m and VANILLA_ITEMS is not None and m.group(1) not in VANILLA_ITEMS:
            continue
        if model_id(rel) in SHARED_MODELS:
            continue
        cat = category_of(rel)
        if cat:
            by_cat.setdefault(cat, set()).add(rel)

    # Alte Dateinamen auf die aktuellen umstellen (z. B. grass.png heisst seit 1.20.3 short_grass.png).
    renamed = {}
    for old, new in RENAMES.items():
        if old in files and new not in files:
            by_cat.setdefault(category_of(new), set())
            renamed[new] = old

    # Alles mitnehmen, worauf Modelle/Blockstates verweisen (eigene Eltern-Modelle, eigene Texturen).
    isolated = {}
    for cat in WITH_REFERENCES:
        if cat not in by_cat:
            continue
        todo = [r for r in by_cat[cat] if r.endswith(".json")]
        seen = set(todo)
        while todo:
            for ref in references(mc, todo.pop()):
                if ref in seen:
                    continue
                seen.add(ref)
                mid = model_id(ref)
                if mid in SHARED_MODELS:
                    isolated.setdefault(cat, set()).add(ref)
                else:
                    other = category_of(ref)
                    if other is not None and other != cat and not ref.endswith(".json"):
                        # Textur einer anderen Kategorie: nicht mitnehmen, sonst aendert dieser Style auch die andere.
                        continue
                    by_cat[cat].add(ref)
                if ref.endswith(".json"):
                    todo.append(ref)

    # Item-Definitionen, deren Modelle es nirgends gibt (Reste fremder Mods), weglassen: Minecraft nimmt dann seine eigene.
    for cat, rels in by_cat.items():
        for rel in [r for r in rels if r.startswith("items/")]:
            if unresolved_models(mc, rel):
                rels.discard(rel)

    # Alte Ruestung umrechnen, wenn das Pack die Datei nicht schon im neuen Format hat.
    converted = {}
    for rel in files:
        m = OLD_ARMOR.fullmatch(rel)
        if not m or m.group(1) not in ARMOR_NAMES:
            continue
        folder = "humanoid" if m.group(2) == "1" else "humanoid_leggings"
        new_rel = f"textures/entity/equipment/{folder}/{ARMOR_NAMES[m.group(1)]}{m.group(3) or ''}.png"
        if new_rel not in by_cat.get("armor", set()) and not (mc / new_rel).is_file():
            converted[new_rel] = rel

    namespaces = sky_namespaces(root)
    cats = set(by_cat) | ({"armor"} if converted else set()) | ({"sky"} if namespaces else set())
    sky_only = cats <= {"sky", "sounds"}

    summary = []
    for cat in sorted(cats):
        dest = styles_root / cat / style_id
        if dest.exists():
            shutil.rmtree(dest)
        mapping = {model_id(r): isolated_id(cat, model_id(r)) for r in isolated.get(cat, ())}
        for rel in sorted(by_cat.get(cat, ())):
            copy(mc, rel, dest, mapping=mapping)
        for rel in sorted(isolated.get(cat, ())):
            copy(mc, rel, dest, dest_rel=f"models/{isolated_id(cat, model_id(rel))}.json", mapping=mapping)
        if cat == "armor":
            for new_rel, old_rel in converted.items():
                copy(mc, old_rel, dest, new_rel)
        for new_rel, old_rel in renamed.items():
            if category_of(new_rel) == cat:
                copy(mc, old_rel, dest, new_rel)
        if cat == "sky":
            for ns in namespaces:
                # Mac-Reste ("._*", .DS_Store) nicht mitkopieren
                shutil.copytree(root / "assets" / ns, dest / "assets" / ns, dirs_exist_ok=True,
                                ignore=shutil.ignore_patterns("._*", ".DS_Store", "Thumbs.db"))
            (dest / "assets" / "minecraft").mkdir(parents=True, exist_ok=True)
            sky_preview(dest, root, sky_only)
        (dest / "style.json").write_text(json.dumps({"name": name}, ensure_ascii=False), encoding="utf-8")
        count = sum(1 for p in dest.rglob("*") if p.is_file()) - 1
        summary.append(f"{cat}={count}")
    print(f"{style_id}: " + (", ".join(summary) if summary else "nichts Passendes"))


def main():
    global VANILLA_ITEMS
    global SHARED_MODELS
    folder = os.environ.get("ARNE_VANILLA_ITEMS")
    if folder and Path(folder).is_dir():
        VANILLA_ITEMS = {p.stem for p in Path(folder).glob("*.json")}
    global VANILLA_MODELS_DIR
    models = os.environ.get("ARNE_VANILLA_MODELS")
    if models and Path(models).is_dir():
        SHARED_MODELS = load_shared_models(models)
        VANILLA_MODELS_DIR = models
    styles_root = Path(sys.argv[1])
    args = sys.argv[2:]
    for i in range(0, len(args), 3):
        sort_pack(styles_root, Path(args[i]), args[i + 1], args[i + 2])


if __name__ == "__main__":
    main()
