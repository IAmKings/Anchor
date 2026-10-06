package com.anchor.app.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 纯录音兜底：转写引擎全部不可用时（几乎不会发生——sherpa 模型随包发布），
 * 录成 m4a 挂到卡片上，不转文字。
 */
class AndroidLocalAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    fun start(): File {
        check(recorder == null) { "Recording already started" }
        val directory = File(context.filesDir, "voice-notes").apply { mkdirs() }
        val file = File(directory, "voice-${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        output = file
        return file
    }

    fun stop(): File? {
        val file = output
        val stopped = runCatching { recorder?.stop() }.isSuccess
        recorder?.release()
        recorder = null
        output = null
        if (!stopped) file?.delete()
        return file?.takeIf { stopped && it.length() > 0 }
    }
}
