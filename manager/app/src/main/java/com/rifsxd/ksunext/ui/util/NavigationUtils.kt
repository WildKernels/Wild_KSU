package com.rifsxd.ksunext.ui.util

import android.annotation.SuppressLint
import android.content.Context

object NavigationUtils {
    @SuppressLint("DiscouragedApi")
    fun isThreeButtonNavigation(context: Context): Boolean {
        val resources = context.resources
        val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        return if (resourceId > 0) {
            resources.getInteger(resourceId) == 0
        } else {
            false
        }
    }
}
