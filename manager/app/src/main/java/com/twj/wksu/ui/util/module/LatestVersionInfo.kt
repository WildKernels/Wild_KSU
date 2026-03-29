package com.twj.wksu.ui.util.module

data class LatestVersionInfo(
    val versionCode : Int = 0,
    val downloadUrl : String = "",
    val changelog : String = "",
    val versionTag : String = "v0.0.0"
)
