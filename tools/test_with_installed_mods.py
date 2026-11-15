"""Startet das installierte Fabric-Minecraft wie der Launcher, aber offline und mit eigenem Test-Spielordner.

Aufruf: python -I launch_test.py <java.exe> <game-dir>
"""
import json
import os
import subprocess
import sys
from pathlib import Path

MC = Path(os.environ["APPDATA"]) / ".minecraft"
VERSION = "fabric-loader-0.19.3-1.21.11"


def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for rule in rules:
        os_name = rule.get("os", {}).get("name")
        if os_name is None or os_name == "windows":
            ok = rule["action"] == "allow"
    return ok


def lib_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art and "path" in art:
        return MC / "libraries" / art["path"]
    group, name, version, *classifier = lib["name"].split(":")
    file = f"{name}-{version}" + (f"-{classifier[0]}" if classifier else "") + ".jar"
    return MC / "libraries" / Path(*group.split(".")) / name / version / file


def main():
    java, game_dir = sys.argv[1], Path(sys.argv[2])
    child = json.loads((MC / "versions" / VERSION / f"{VERSION}.json").read_text(encoding="utf-8"))
    parent = json.loads((MC / "versions" / child["inheritsFrom"] / f"{child['inheritsFrom']}.json").read_text(encoding="utf-8"))
    classpath, seen = [], set()
    for lib in child.get("libraries", []) + parent.get("libraries", []):
        if not allowed(lib):
            continue
        name = lib["name"]
        if "natives-windows-arm64" in name or "natives-windows-x86" in name:
            continue
        key = ":".join(name.split(":")[:2]) + (":" + name.split(":")[3] if name.count(":") >= 3 else "")
        if key in seen:
            continue
        seen.add(key)
        path = lib_path(lib)
        if path.is_file():
            classpath.append(str(path))
        else:
            print("fehlt:", path)
    classpath.append(str(MC / "versions" / child["inheritsFrom"] / f"{child['inheritsFrom']}.jar"))
    cmd = [java, "-Xmx3G", "-cp", os.pathsep.join(classpath), child["mainClass"],
           "--username", "ArneTest", "--version", VERSION, "--gameDir", str(game_dir),
           "--assetsDir", str(MC / "assets"), "--assetIndex", parent["assetIndex"]["id"],
           "--uuid", "00000000000000000000000000000000", "--accessToken", "0", "--userType", "legacy", "--versionType", "release"]
    env = dict(os.environ, ARNESTYLES_SELFTEST="1")
    print("Starte mit", len(classpath), "Bibliotheken")
    result = subprocess.run(cmd, cwd=game_dir, env=env, timeout=540)
    print("Exit-Code:", result.returncode)


if __name__ == "__main__":
    main()
