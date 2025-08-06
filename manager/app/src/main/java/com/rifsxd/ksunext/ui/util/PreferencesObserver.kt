package com.rifsxd.ksunext.ui.util

import android.content.SharedPreferences
import androidx.compose.runtime.*

/**
 * Observes SharedPreferences changes and provides reactive state for Compose
 */
@Composable
fun observePreferenceAsState(
    preferences: SharedPreferences,
    key: String,
    defaultValue: String? = null
): State<String?> {
    val state = remember { mutableStateOf(preferences.getString(key, defaultValue)) }
    
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = preferences.getString(key, defaultValue)
            }
        }
        
        preferences.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    return state
}

@Composable
fun observePreferenceAsState(
    preferences: SharedPreferences,
    key: String,
    defaultValue: Boolean
): State<Boolean> {
    val state = remember { mutableStateOf(preferences.getBoolean(key, defaultValue)) }
    
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = preferences.getBoolean(key, defaultValue)
            }
        }
        
        preferences.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    return state
}

@Composable
fun observePreferenceAsState(
    preferences: SharedPreferences,
    key: String,
    defaultValue: Float
): State<Float> {
    val state = remember { mutableStateOf(preferences.getFloat(key, defaultValue)) }
    
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = preferences.getFloat(key, defaultValue)
            }
        }
        
        preferences.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    return state
}

@Composable
fun observePreferenceAsState(
    preferences: SharedPreferences,
    key: String,
    defaultValue: Int
): State<Int> {
    val state = remember { mutableStateOf(preferences.getInt(key, defaultValue)) }
    
    DisposableEffect(preferences, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = preferences.getInt(key, defaultValue)
            }
        }
        
        preferences.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    return state
}