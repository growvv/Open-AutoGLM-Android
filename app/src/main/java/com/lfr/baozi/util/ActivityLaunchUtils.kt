package com.example.open_autoglm_android.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent

object ActivityLaunchUtils {
    fun startActivityNoAnimation(context: Context, intent: Intent) {
        try {
            val safeIntent =
                if (context is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val options = ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            context.startActivity(safeIntent, options)
        } catch (_: Exception) {
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}

