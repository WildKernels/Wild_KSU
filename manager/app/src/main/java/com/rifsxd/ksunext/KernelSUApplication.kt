package com.rifsxd.ksunext

import android.app.Application
import android.os.Build
import android.system.Os
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.rifsxd.ksunext.ui.util.createRootShellBuilder
import com.rifsxd.ksunext.ui.viewmodel.ModuleViewModel
import com.rifsxd.ksunext.ui.viewmodel.SuperUserViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.*

lateinit var ksuApp: KernelSUApplication

class KernelSUApplication : Application(), ViewModelStoreOwner {

    lateinit var okhttpClient: OkHttpClient
    private val appViewModelStore by lazy { ViewModelStore() }

    override fun onCreate() {
        super.onCreate()
        ksuApp = this
        Shell.setDefaultBuilder(createRootShellBuilder(true))
        Shell.enableVerboseLogging = BuildConfig.DEBUG

        val moduleViewModel = ViewModelProvider(this)[ModuleViewModel::class.java]
        val superUserViewModel = ViewModelProvider(this)[SuperUserViewModel::class.java]
        CoroutineScope(Dispatchers.Main).launch {
            if (superUserViewModel.appList.isEmpty()) {
                superUserViewModel.fetchAppList()
            }
            if (moduleViewModel.moduleList.isEmpty()) {
                moduleViewModel.fetchModuleList()
            }
        }

        val context = this
        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, context))
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        )

        val webroot = File(dataDir, "webroot")
        if (!webroot.exists()) {
            webroot.mkdir()
        }

        // Provide working env for rust's temp_dir()
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "KernelSU/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore


}
