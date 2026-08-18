"""Privacy surface check — no network permission, no analytics/network SDKs.

Usage: python3 scripts/verify_privacy.py
Exit 0 if all checks pass, 1 otherwise.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

FORBIDDEN_PERMISSIONS = (
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_WIFI_STATE",
)

FORBIDDEN_COORDINATES = (
    "firebase",
    "crashlytics",
    "analytics",
    "okhttp",
    "ktor",
    "retrofit",
    "volley",
    "play-services-ads",
    "amplitude",
    "mixpanel",
    "appsflyer",
    "flurry",
    "sentry",
    "instabug",
    "bugsnag",
)

FORBIDDEN_IMPORTS = (
    "com.google.firebase",
    "okhttp3",
    "io.ktor",
    "retrofit2",
    "com.android.volley",
    "com.google.android.gms.analytics",
)

checks = []


def check(name, ok, detail=""):
    checks.append((name, ok, detail))


def strip_comments(text: str) -> str:
    without_block = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    without_line = re.sub(r"//.*?$", "", without_block, flags=re.M)
    return without_line


def source_manifests():
    roots = [ROOT / "androidApp" / "src", ROOT / "shared" / "src"]
    for root in roots:
        if not root.is_dir():
            continue
        for path in root.rglob("AndroidManifest.xml"):
            yield path


def gradle_files():
    for path in [
        ROOT / "build.gradle.kts",
        ROOT / "settings.gradle.kts",
        ROOT / "androidApp" / "build.gradle.kts",
        ROOT / "shared" / "build.gradle.kts",
    ]:
        if path.is_file():
            yield path


def kotlin_sources():
    for folder in (ROOT / "androidApp" / "src", ROOT / "shared" / "src"):
        if not folder.is_dir():
            continue
        for path in folder.rglob("*.kt"):
            if "build" in path.parts:
                continue
            yield path


for path in source_manifests():
    text = path.read_text(encoding="utf-8")
    rel = path.relative_to(ROOT)
    for permission in FORBIDDEN_PERMISSIONS:
        check(
            f"manifest {rel} has no {permission}",
            permission not in text,
        )

for path in gradle_files():
    text = strip_comments(path.read_text(encoding="utf-8")).lower()
    rel = path.relative_to(ROOT)
    for token in FORBIDDEN_COORDINATES:
        check(f"gradle {rel} has no {token}", token not in text)

import_hits = []
for path in kotlin_sources():
    text = path.read_text(encoding="utf-8")
    rel = str(path.relative_to(ROOT))
    for token in FORBIDDEN_IMPORTS:
        if token in text:
            import_hits.append(f"{rel}:{token}")
check("kotlin sources have no forbidden network/analytics import", not import_hits, ", ".join(import_hits))

failed = [item for item in checks if not item[1]]
for name, ok, detail in checks:
    if not ok:
        print("FAIL", name, detail)
print(f"\n{len(checks) - len(failed)}/{len(checks)} passed")
if failed:
    sys.exit(1)
