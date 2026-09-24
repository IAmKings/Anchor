package com.anchor.app.update

internal data class PublishedRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
)

internal fun parseReleaseFeed(json: String): List<PublishedRelease> {
    val root = parseJson(json)
    val items = (root as? Json.Arr)?.items ?: error("版本列表不是数组。")
    return items.mapNotNull { item ->
        val obj = item as? Json.Obj ?: return@mapNotNull null
        if (obj.bool("draft") == true) return@mapNotNull null
        val body = obj.str("body") ?: return@mapNotNull null
        val versionCode = versionCodeIn(body) ?: return@mapNotNull null
        val versionName = versionNameIn(body) ?: return@mapNotNull null
        val apkUrl = apkUrlIn(obj) ?: return@mapNotNull null
        PublishedRelease(versionCode, versionName, apkUrl)
    }
}

internal fun interpretFeed(localVersionCode: Int, releases: List<PublishedRelease>): UpdatePhase {
    if (releases.isEmpty()) return UpdatePhase.Failed
    val best = releases.maxBy { it.versionCode }
    return if (best.versionCode > localVersionCode) {
        UpdatePhase.UpdateAvailable(best.versionName, best.versionCode, best.apkUrl)
    } else {
        UpdatePhase.UpToDate
    }
}

private fun versionCodeIn(body: String): Int? =
    Regex("""(?m)^anchor-version-code:\s*(\d+)\s*$""").find(body)?.groupValues?.get(1)?.toIntOrNull()

private fun versionNameIn(body: String): String? =
    Regex("""(?m)^anchor-version-name:\s*(\S+)\s*$""").find(body)?.groupValues?.get(1)

private fun apkUrlIn(release: Json.Obj): String? {
    val assets = release.obj("assets") as? Json.Arr ?: return null
    return assets.items.mapNotNull { asset ->
        val fields = asset as? Json.Obj ?: return@mapNotNull null
        val name = fields.str("name").orEmpty()
        val url = fields.str("browser_download_url") ?: return@mapNotNull null
        if (!name.endsWith(".apk")) return@mapNotNull null
        if (!name.startsWith("anchor-internal")) return@mapNotNull null
        if (!allowedDownloadUrl(url)) return@mapNotNull null
        url
    }.firstOrNull()
}

internal fun allowedDownloadUrl(url: String): Boolean {
    if (!url.startsWith("https://")) return false
    val host = url.removePrefix("https://").substringBefore("/").substringBefore(":")
    return host == "github.com" || host == "release-assets.githubusercontent.com"
}

private sealed interface Json {
    data class Obj(val fields: Map<String, Json>) : Json
    data class Arr(val items: List<Json>) : Json
    data class Str(val value: String) : Json
    data object BoolTrue : Json
    data object BoolFalse : Json
    data object Null : Json
    data class Num(val raw: String) : Json
}

private fun Json.Obj.str(key: String): String? = (fields[key] as? Json.Str)?.value

private fun Json.Obj.bool(key: String): Boolean? = when (fields[key]) {
    Json.BoolTrue -> true
    Json.BoolFalse -> false
    else -> null
}

private fun Json.Obj.obj(key: String): Json? = fields[key]

private fun parseJson(text: String): Json {
    val cursor = JsonCursor(text)
    val value = cursor.parseValue()
    cursor.skip()
    if (cursor.hasNext()) error("版本列表后面还有多余内容。")
    return value
}

private class JsonCursor(private val text: String) {
    private var index = 0

    fun hasNext(): Boolean = index < text.length

    fun skip() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    fun parseValue(): Json {
        skip()
        if (index >= text.length) error("版本列表不完整。")
        return when (text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> Json.Str(parseString())
            't' -> literal("true", Json.BoolTrue)
            'f' -> literal("false", Json.BoolFalse)
            'n' -> literal("null", Json.Null)
            else -> parseNumber()
        }
    }

    private fun parseObject(): Json.Obj {
        expect('{')
        val fields = linkedMapOf<String, Json>()
        skip()
        if (peek('}')) {
            index++
            return Json.Obj(fields)
        }
        while (index < text.length) {
            skip()
            val key = parseString()
            skip()
            expect(':')
            fields[key] = parseValue()
            skip()
            when {
                peek('}') -> {
                    index++
                    return Json.Obj(fields)
                }
                peek(',') -> index++
                else -> error("版本对象格式不对。")
            }
        }
        error("版本对象没有结束。")
    }

    private fun parseArray(): Json.Arr {
        expect('[')
        val items = mutableListOf<Json>()
        skip()
        if (peek(']')) {
            index++
            return Json.Arr(items)
        }
        while (index < text.length) {
            items += parseValue()
            skip()
            when {
                peek(']') -> {
                    index++
                    return Json.Arr(items)
                }
                peek(',') -> index++
                else -> error("版本数组格式不对。")
            }
        }
        error("版本数组没有结束。")
    }

    private fun parseString(): String {
        expect('"')
        val out = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when (char) {
                '"' -> return out.toString()
                '\\' -> {
                    if (index >= text.length) error("版本字符串转义不完整。")
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> out.append(escaped)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            if (index + 4 > text.length) error("版本字符串转义不完整。")
                            val hex = text.substring(index, index + 4)
                            out.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> error("版本字符串有未知转义。")
                    }
                }
                else -> out.append(char)
            }
        }
        error("版本字符串没有结束。")
    }

    private fun parseNumber(): Json.Num {
        val start = index
        if (peek('-')) index++
        while (index < text.length && text[index].isDigit()) index++
        if (index < text.length && text[index] == '.') {
            index++
            while (index < text.length && text[index].isDigit()) index++
        }
        if (start == index || (text[start] == '-' && index == start + 1)) error("版本数字不对。")
        return Json.Num(text.substring(start, index))
    }

    private fun literal(token: String, value: Json): Json {
        if (!text.startsWith(token, index)) error("版本列表有无法识别的值。")
        index += token.length
        return value
    }

    private fun expect(char: Char) {
        skip()
        if (index >= text.length || text[index] != char) error("版本列表缺少 $char。")
        index++
    }

    private fun peek(char: Char): Boolean = index < text.length && text[index] == char
}
