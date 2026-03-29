package com.twj.wksu.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import com.twj.wksu.IKsuInterface
import com.twj.wksu.Natives
import com.twj.wksu.ksuApp
import com.twj.wksu.ui.KsuService
import com.twj.wksu.ui.util.HanziToPinyin
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.text.Collator
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuperUserViewModel : ViewModel() {

    companion object {
        private const val TAG = "SuperUserViewModel"
         var apps by mutableStateOf<List<AppInfo>>(emptyList())

        @JvmStatic
        fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
            val appDetail = apps.find { it.packageName == packageName }
            return appDetail?.packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        }
        private var profileOverrides by mutableStateOf<Map<String, Natives.Profile>>(emptyMap())
    }

    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val profile: Natives.Profile?,
    ) : Parcelable {
        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid

        val allowSu: Boolean
            get() = profile != null && profile.allowSu
        val hasCustomProfile: Boolean
            get() {
                if (profile == null) {
                    return false
                }

                return if (profile.allowSu) {
                    !profile.rootUseDefault
                } else {
                    !profile.nonRootUseDefault
                }
            }
    }

    private val prefs = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)!!

    var search by mutableStateOf("")
    var showSystemApps by mutableStateOf(prefs.getBoolean("show_system_apps", false))
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    fun updateShowSystemApps(newValue: Boolean) {
        showSystemApps = newValue
        prefs.edit { putBoolean("show_system_apps", newValue) }
    }

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.profile != null && it.profile.allowSu -> 0
                it.profile != null && (
                    if (it.profile.allowSu) !it.profile.rootUseDefault else !it.profile.nonRootUseDefault
                ) -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))
        apps.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    val appList by derivedStateOf {
        sortedList.map { app ->
            profileOverrides[app.packageName]?.let { app.copy(profile = it) } ?: app
        }.filter {
            it.label.contains(search, true) || it.packageName.contains(
                search,
                true
            ) || HanziToPinyin.getInstance()
                .toPinyinString(it.label).contains(search, true)
        }.filter {
            it.uid == 2000 // Always show shell
                    || showSystemApps || it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    fun updateAppProfile(packageName: String, newProfile: Natives.Profile) {
        profileOverrides = profileOverrides.toMutableMap().apply {
            put(packageName, newProfile)
        }
    }

    private suspend inline fun connectKsuService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = suspendCoroutine {
        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                onDisconnect()
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                it.resume(binder as IBinder to this)
            }
        }

        val intent = Intent(ksuApp, KsuService::class.java)

        val task = RootService.bindOrTask(
            intent,
            Shell.EXECUTOR,
            connection,
        )
        task?.let { it1 -> Shell.getShell().execTask(it1) }
    }

    private fun stopKsuService() {
        val intent = Intent(ksuApp, KsuService::class.java)
        RootService.stop(intent)
    }

    suspend fun fetchAppList() {
        Mutex().withLock {

            isRefreshing = true

            val result = connectKsuService {
                Log.w(TAG, "KsuService disconnected")
            }

            withContext(Dispatchers.IO) {
                val pm = ksuApp.packageManager
                val start = SystemClock.elapsedRealtime()

                val binder = result.first
                val allPackages = IKsuInterface.Stub.asInterface(binder).getPackages(0)

                withContext(Dispatchers.Main) {
                    stopKsuService()
                }

                val packages = allPackages.list

                apps = packages.map {
                    val appInfo = it.applicationInfo
                    val uid = appInfo!!.uid
                    val profile = Natives.getAppProfile(it.packageName, uid)
                    AppInfo(
                        label = appInfo.loadLabel(pm).toString(),
                        packageInfo = it,
                        profile = profile,
                    )
                }
                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}")
            }
        }
    }
}
