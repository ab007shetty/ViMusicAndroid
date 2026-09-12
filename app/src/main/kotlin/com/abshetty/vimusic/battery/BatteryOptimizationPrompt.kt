package com.abshetty.vimusic.battery

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationPrompt {
    fun isExempt(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    fun shouldAsk(context: Context, hasPlayedBefore: Boolean, alreadyAsked: Boolean): Boolean =
        hasPlayedBefore && !alreadyAsked && !isExempt(context)

    fun intent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
