package com.rifsxd.ksunext.ui.theme

enum class AppTheme(val value: Int) {
    AUTO(0),
    DARK_DYNAMIC(1),
    LIGHT_DYNAMIC(2),
    LIGHT(3),
    DARK(4),
    AMOLED(5),
    CUSTOM(6);

    companion object {
        fun fromValue(value: Int): AppTheme {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}
