package com.riva.atsmobile.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator

fun playSoundWithVibration(
    context: Context,
    soundResId: Int,
    vibrate: Boolean = true
) {
    if (vibrate) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    val mediaPlayer = MediaPlayer.create(context, soundResId)
    mediaPlayer.setOnCompletionListener { it.release() }
    mediaPlayer.start()
}
