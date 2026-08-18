#!/usr/bin/env python3
"""Export and classify Stitch design resources for local UI development."""

from __future__ import annotations

import concurrent.futures
import json
import mimetypes
import re
import sys
import time
import tomllib
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONFIG = Path.home() / ".codex" / "config.toml"
PROJECT_ID = "4724290021803215791"
PROJECT_NAME = f"projects/{PROJECT_ID}"
DESIGN_DIR = ROOT / "design"

CATEGORIES = [
    ("00-brand-assets", "品牌与技术素材", ("Anchor App Icon", "Animated SVG", "Shader")),
    ("01-onboarding-assessment", "首启、量表与复评", ("首启", "PHQ-9", "GAD-7", "评估", "复评")),
    ("02-home-daily", "首页与日常状态", ("首页",)),
    ("03-micro-actions", "微行动流程", ("微行动", "挑选微行动", "挑选新微行动")),
    ("04-worry-vault", "忧虑保险箱与忧虑专场", ("忧虑",)),
    ("05-relationships-altruism", "关系能量、表演耗竭与利他任务", ("关系", "表演", "利他")),
    ("06-insights", "洞察与趋势", ("洞察",)),
    ("07-journaling", "记录与双栏日志", ("记录", "双栏")),
    ("08-medical-safety", "就医、安全与浪潮等待", ("就医", "Medical Guide", "危机", "浪潮")),
    ("09-settings-export", "设置、隐私与导出", ("设置", "导出")),
    ("10-rhythm-notifications", "晨间节律与通知", ("晨间", "通知", "lock screen")),
    ("11-reference", "产品参考资料", ("PRD.md",)),
]


def mcp_request(method: str, params: dict, request_id: int = 1) -> dict:
    config = tomllib.loads(CONFIG.read_text(encoding="utf-8"))["mcp_servers"]["stitch"]
    payload = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": method,
        "params": params,
    }
    request = urllib.request.Request(
        config["url"],
        json.dumps(payload).encode(),
        {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
            "X-Goog-Api-Key": config["http_headers"]["X-Goog-Api-Key"],
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        body = response.read().decode("utf-8")
    if body.startswith("event:"):
        data_lines = [line[6:].strip() for line in body.splitlines() if line.startswith("data:")]
        body = data_lines[-1]
    result = json.loads(body)
    if "error" in result:
        raise RuntimeError(json.dumps(result["error"], ensure_ascii=False))
    return result["result"]


def call_tool(name: str, arguments: dict, request_id: int = 1) -> dict:
    last_error = "unknown response"
    for attempt in range(4):
        result = mcp_request("tools/call", {"name": name, "arguments": arguments}, request_id)
        structured = result.get("structuredContent")
        if isinstance(structured, dict):
            return structured
        for block in result.get("content", []):
            text = block.get("text", "").strip()
            if block.get("type") == "text" and text:
                try:
                    return json.loads(text)
                except json.JSONDecodeError:
                    last_error = text[:160].replace("\n", " ")
        if attempt < 3:
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"Stitch tool {name} returned no parseable JSON: {last_error}")


def category_for(title: str) -> tuple[str, str]:
    folded = title.casefold()
    priority = [
        ("00-brand-assets", "品牌与技术素材", ("anchor app icon", "animated svg", "shader")),
        ("02-home-daily", "首页与日常状态", ("首页",)),
        ("06-insights", "洞察与趋势", ("洞察",)),
        ("10-rhythm-notifications", "晨间节律与通知", ("晨间", "通知", "lock screen")),
        ("08-medical-safety", "就医、安全与浪潮等待", ("就医", "medical guide", "危机", "浪潮")),
        ("09-settings-export", "设置、隐私与导出", ("设置", "导出")),
        ("01-onboarding-assessment", "首启、量表与复评", ("首启", "phq-9", "gad-7", "评估", "复评")),
        ("04-worry-vault", "忧虑保险箱与忧虑专场", ("忧虑",)),
        ("03-micro-actions", "微行动流程", ("微行动", "挑选")),
        ("05-relationships-altruism", "关系能量、表演耗竭与利他任务", ("关系", "表演", "利他")),
        ("07-journaling", "记录与双栏日志", ("记录", "双栏")),
        ("11-reference", "产品参考资料", ("prd.md",)),
    ]
    for directory, label, keywords in priority:
        if any(keyword in folded for keyword in keywords):
            return directory, label
    for directory, label, keywords in CATEGORIES:
        if any(keyword.casefold() in folded for keyword in keywords):
            return directory, label
    return "12-other", "其他与实验稿"


def safe_name(title: str, screen_id: str) -> str:
    title = re.sub(r"[\\/:*?\"<>|\n\r]+", "-", title).strip(" .-")
    title = re.sub(r"\s+", "-", title)
    if len(title) > 64:
        title = title[:64].rstrip(" .-")
    return f"{title or 'untitled'}__{screen_id[:8]}"


def download(url: str, destination: Path) -> tuple[bool, str | None]:
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "Codex-Stitch-Exporter/1.0"})
        with urllib.request.urlopen(request, timeout=90) as response:
            destination.write_bytes(response.read())
        return True, None
    except Exception as exc:  # preserve partial export and report failures in metadata
        return False, str(exc)


def extension_for(asset: dict, fallback: str) -> str:
    mime = asset.get("mimeType", "").split(";", 1)[0]
    extension = mimetypes.guess_extension(mime) if mime else None
    if extension == ".jpe":
        extension = ".jpg"
    return extension or fallback


def export_screen(screen: dict, index: int) -> dict:
    screen_id = screen["name"].split("/")[-1]
    detail = call_tool(
        "get_screen",
        {"name": screen["name"], "projectId": PROJECT_ID, "screenId": screen_id},
        1000 + index,
    )
    title = detail.get("title") or screen.get("title") or "Untitled"
    category, category_label = category_for(title)
    directory = DESIGN_DIR / category
    directory.mkdir(parents=True, exist_ok=True)
    stem = safe_name(title, screen_id)

    screenshot = detail.get("screenshot") or {}
    html = detail.get("htmlCode") or {}
    screenshot_path = directory / f"{stem}{extension_for(screenshot, '.png')}"
    html_path = directory / f"{stem}.html"
    metadata_path = directory / f"{stem}.json"

    screenshot_ok, screenshot_error = (False, None)
    if screenshot.get("downloadUrl"):
        screenshot_ok, screenshot_error = download(screenshot["downloadUrl"], screenshot_path)
    html_ok, html_error = (False, None)
    if html.get("downloadUrl"):
        html_ok, html_error = download(html["downloadUrl"], html_path)

    metadata = {
        "id": screen_id,
        "name": detail.get("name", screen["name"]),
        "title": title,
        "category": category,
        "categoryLabel": category_label,
        "deviceType": detail.get("deviceType"),
        "width": detail.get("width"),
        "height": detail.get("height"),
        "source": {
            "screenshot": screenshot.get("name"),
            "htmlCode": html.get("name"),
        },
        "local": {
            "screenshot": screenshot_path.relative_to(ROOT).as_posix() if screenshot_ok else None,
            "html": html_path.relative_to(ROOT).as_posix() if html_ok else None,
        },
        "errors": {
            key: value
            for key, value in (("screenshot", screenshot_error), ("html", html_error))
            if value
        },
        "unavailable": [
            kind
            for kind, asset in (("screenshot", screenshot), ("html", html))
            if not asset.get("downloadUrl")
        ],
    }
    metadata_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return metadata


def write_design_md(project: dict) -> None:
    design_md = project.get("designTheme", {}).get("designMd", "").strip()
    if not design_md:
        raise RuntimeError("Project has no designTheme.designMd")
    if design_md.startswith("---\n"):
        design_md = design_md.replace(
            "---\n",
            f"---\nprojectId: '{PROJECT_ID}'\nstitchProject: '{PROJECT_NAME}'\n",
            1,
        )
    provenance = (
        "\n\n---\n\n"
        "## Stitch 来源\n\n"
        f"- 项目：{project.get('title', 'Anchor')}\n"
        f"- Project ID：`{PROJECT_ID}`\n"
        f"- 设备基准：`{project.get('deviceType', 'MOBILE')}`\n"
        f"- 最近同步：`{project.get('updateTime', 'unknown')}`\n"
        "- 本文件由 Stitch 项目中的设计系统规范同步生成；页面级资源见 `design/README.md`。\n"
    )
    (ROOT / "DESIGN.md").write_text(design_md + provenance, encoding="utf-8")


def write_indexes(project: dict, resources: list[dict]) -> None:
    resources.sort(key=lambda item: (item["category"], item["title"], item["id"]))
    manifest = {
        "project": {
            "id": PROJECT_ID,
            "name": PROJECT_NAME,
            "title": project.get("title"),
            "updateTime": project.get("updateTime"),
        },
        "resourceCount": len(resources),
        "resources": resources,
    }
    (DESIGN_DIR / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    grouped: dict[str, list[dict]] = {}
    for resource in resources:
        grouped.setdefault(resource["category"], []).append(resource)

    labels = {directory: label for directory, label, _ in CATEGORIES}
    labels["12-other"] = "其他与实验稿"
    lines = [
        "# Anchor Stitch 设计资源",
        "",
        f"- Stitch 项目：`{PROJECT_NAME}`",
        f"- 页面与素材：{len(resources)} 项",
        f"- Stitch 最近更新：`{project.get('updateTime', 'unknown')}`",
        "- 全局设计规范：[DESIGN.md](../DESIGN.md)",
        "- 机器可读清单：[manifest.json](manifest.json)",
        "",
        "每项资源通常包含同名截图、HTML 和 JSON 元数据；JSON 保留 Stitch ID、尺寸及本地路径。",
        "",
    ]
    for category in sorted(grouped):
        lines.extend([f"## {labels[category]}", ""])
        for item in grouped[category]:
            screenshot = item["local"].get("screenshot")
            html = item["local"].get("html")
            links = []
            if screenshot:
                target = Path(screenshot).relative_to("design").as_posix()
                links.append(f"[截图](<{target}>)")
            if html:
                target = Path(html).relative_to("design").as_posix()
                links.append(f"[HTML](<{target}>)")
            metadata_name = safe_name(item["title"], item["id"]) + ".json"
            links.append(f"[元数据](<{category}/{metadata_name}>)")
            lines.append(
                f"- **{item['title']}** — {item.get('deviceType') or 'ASSET'} "
                f"{item.get('width')}×{item.get('height')} · {' · '.join(links)}"
            )
        lines.append("")
    (DESIGN_DIR / "README.md").write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    DESIGN_DIR.mkdir(parents=True, exist_ok=True)
    project = call_tool("get_project", {"name": PROJECT_NAME}, 10)
    screens = call_tool("list_screens", {"projectId": PROJECT_ID}, 11).get("screens", [])
    write_design_md(project)
    resources: list[dict] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        futures = {
            executor.submit(export_screen, screen, index): screen
            for index, screen in enumerate(screens)
        }
        for completed, future in enumerate(concurrent.futures.as_completed(futures), 1):
            screen = futures[future]
            try:
                resource = future.result()
                resources.append(resource)
                print(f"[{completed:02d}/{len(screens)}] {resource['title']}")
            except Exception as exc:
                print(f"[{completed:02d}/{len(screens)}] FAILED {screen.get('title')}: {exc}", file=sys.stderr)
    write_indexes(project, resources)
    failed = sum(bool(item["errors"]) for item in resources)
    print(f"Exported {len(resources)}/{len(screens)} resources; {failed} have asset download errors")
    return 1 if len(resources) != len(screens) or failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
