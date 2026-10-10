# site/ — 锚点介绍站

单页静态落地页，部署在 Cloudflare Pages：**https://anchor-125457.pages.dev**

零构建、零外部依赖、无统计脚本——与 `legal/` 法务站同一哲学。视觉 token 严格取自 [DESIGN.md](../DESIGN.md)（羊皮纸底 `#FCF9F3`、静绿 `#466552`、卡片 16px 圆角、陶土红 `#8E4D34` 仅用于危机语义），深浅色跟随系统。

## 部署

```bash
# 首次之后只需这一条（CI 模式避免交互确认）
CI=true npx wrangler pages deploy site --project-name=anchor-125457 --branch=main
```

- Cloudflare Pages 项目名 `anchor-125457`（与域名绑定账户，名字全局唯一所以带了 125457 后缀段）
- 上传 `site/` 整个目录，约 1MB（11 个文件），3 秒完成
- 部署完返回一个 `xxxxxx.anchor-125457.pages.dev` 的预览链接；生产域不带前缀

## 更新内容

| 要改什么 | 改哪里 |
| --- | --- |
| 文案 / 章节 | `site/index.html` 直接编辑 |
| 版本号 / 下载链接 | Hero 区 `当前版本` 徽章与两个 `.apk` 直链（tag 升级时同步，直链格式 `releases/download/vX.Y.Z/anchor-internal-X.Y.Z.apk`） |
| 截图 | 先更新 `docs/screenshots/`（真机 adb 拓屏），再跑下方素材流水线 |
| 色板 / 排版 | 改前先对照 [DESIGN.md](../DESIGN.md) token；应用内色板变了要同步站点 |

## 截图素材流水线

截图源文件在 `docs/screenshots/`（1440 宽真机图），网站用 720 宽 JPEG（单张 ≤100KB）：

```bash
python3 - <<'EOF'
from PIL import Image
im = Image.open('docs/screenshots/home.png').convert('RGB')
im = im.resize((720, round(im.height * 720 / im.width)), Image.LANCZOS)
im.save('site/assets/home.jpg', 'JPEG', quality=80, optimize=True)
EOF
```

长截图（`settings-export.jpg` 11910px、`help.jpg` 4304px）先 `im.crop((0, 0, w, 3168))` 取首屏再缩放。

## 已知约束

- Pages 项目子域全局唯一：`anchor-site`、`anchor-app` 等常见名已被占用，不要试图改名，`anchor-125457` 已与 125457.xyz 域族呼应
- 深色模式只跟随系统（`prefers-color-scheme`），没有站内切换——与应用行为一致
- 本站不做英文版、不加统计、不绑自定义域（需要时在 Cloudflare 控制台给 `anchor-125457` 项目加 Custom domain 即可）
