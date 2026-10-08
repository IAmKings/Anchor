package com.anchor.app.speech

/**
 * L0 标点启发式：把端点检测切出的倾诉段落连成带标点的文本——
 * 段间用「，」，末段加「。」。停顿≠语法，这是句读级而非语法级标点；
 * 语法级标点由高精度语音包（CT-transformer）在下载后接管。
 * 纯函数便于单测。空输入返回空串。
 */
fun punctuateTranscript(segments: List<String>): String {
    val clean = segments.map { it.trim() }.filter { it.isNotEmpty() }
    if (clean.isEmpty()) return ""
    return clean.joinToString("，") + "。"
}
