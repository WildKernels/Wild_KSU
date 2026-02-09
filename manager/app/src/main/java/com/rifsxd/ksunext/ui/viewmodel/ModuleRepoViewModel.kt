package com.rifsxd.ksunext.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.util.listModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import okhttp3.CacheControl

class ModuleRepoViewModel : ViewModel() {

    companion object {
        private const val TAG = "ModuleRepoViewModel"
        private const val PREFS_NAME = "module_repo_prefs"
        private const val KEY_REPOS = "repos_json"
    }

    data class Repository(
        val name: String,
        val url: String,
        val enabled: Boolean = true
    )

    data class ModuleVersion(
        val version: String,
        val versionCode: Int,
        val zipUrl: String,
        val changelog: String,
        val timestamp: Long
    )

    data class RepoModule(
        val id: String,
        val name: String,
        val description: String,
        val version: String,
        val versionCode: Int,
        val author: String,
        val zipUrl: String,
        val changelog: String,
        val timestamp: Long,
        val versions: List<ModuleVersion> = emptyList()
    )

    enum class SortOption {
        NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, INSTALLED
    }

    sealed class RepoState {
        object Loading : RepoState()
        data class Success(val modules: List<RepoModule>) : RepoState()
        data class Error(val message: String) : RepoState()
    }

    var repoState by mutableStateOf<RepoState>(RepoState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    private val prefs by lazy {
        ksuApp.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var repositories by mutableStateOf(loadRepositories())
        private set
    
    var sortOption by mutableStateOf(SortOption.NAME_ASC)

    var searchQuery by mutableStateOf("")
        private set

    var installedModules by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
        updateDisplayedModules()
    }
    
    // Keep track of all fetched modules to allow filtering without re-fetching
    private var _allFetchedModules = listOf<RepoModule>()

    private fun loadRepositories(): List<Repository> {
        val json = prefs.getString(KEY_REPOS, null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<Repository>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(Repository(
                        obj.getString("name"),
                        obj.getString("url"),
                        obj.getBoolean("enabled")
                    ))
                }
                if (list.isNotEmpty()) {
                    return list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading repos", e)
            }
        }
        // Defaults
        val defaults = listOf(
            Repository("DerGooglers Repo", "https://gr.dergoogler.com/gmr", true),
            Repository("IzzyOnDroid Repo", "https://apt.izzysoft.de/magisk", true)
        )
        // Save defaults so they persist
        saveRepositories(defaults)
        return defaults
    }

    private fun saveRepositories(repos: List<Repository>) {
        val array = JSONArray()
        repos.forEach { repo ->
            val obj = JSONObject()
            obj.put("name", repo.name)
            obj.put("url", repo.url)
            obj.put("enabled", repo.enabled)
            array.put(obj)
        }
        prefs.edit().putString(KEY_REPOS, array.toString()).apply()
    }

    fun addRepository(name: String, url: String) {
        val newRepo = Repository(name, url, true)
        repositories = repositories + newRepo
        saveRepositories(repositories)
        fetchModules()
    }

    fun removeRepository(repo: Repository) {
        repositories = repositories - repo
        saveRepositories(repositories)
        fetchModules()
    }

    fun toggleRepository(repo: Repository) {
        repositories = repositories.map {
            if (it == repo) it.copy(enabled = !it.enabled) else it
        }
        saveRepositories(repositories)
        fetchModules()
    }
    
    fun resetRepositories() {
        prefs.edit().remove(KEY_REPOS).apply()
        // Reloading will apply defaults and SAVE them
        repositories = loadRepositories()
        fetchModules()
    }

    fun reloadRepositories() {
        repositories = loadRepositories()
        fetchModules()
    }
    
    fun updateSortOption(option: SortOption) {
        sortOption = option
        updateDisplayedModules()
    }
    
    private fun updateDisplayedModules() {
        val filtered = if (searchQuery.isBlank()) {
            _allFetchedModules
        } else {
            val q = searchQuery.lowercase()
            _allFetchedModules.filter { 
                it.name.lowercase().contains(q) || 
                it.description.lowercase().contains(q) || 
                it.author.lowercase().contains(q)
            }
        }
        repoState = RepoState.Success(sortModules(filtered))
    }
    
    private fun sortModules(modules: List<RepoModule>): List<RepoModule> {
        return when (sortOption) {
            SortOption.NAME_ASC -> modules.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> modules.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> modules.sortedByDescending { it.timestamp }
            SortOption.DATE_OLDEST -> modules.sortedBy { it.timestamp }
            SortOption.INSTALLED -> modules.sortedByDescending { installedModules.containsKey(it.id) }
        }
    }

    fun fetchModules() {
        viewModelScope.launch {
            isRefreshing = true
            repoState = RepoState.Loading
            
            val allModules = mutableListOf<RepoModule>()

            withContext(Dispatchers.IO) {
                val activeRepos = repositories.filter { it.enabled }
                val deferreds = activeRepos.map { repo ->
                    async {
                        try {
                            fetchRepo(repo)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching ${repo.name}", e)
                            emptyList<RepoModule>()
                        }
                    }
                }
                
                deferreds.awaitAll().forEach { allModules.addAll(it) }
            }
            
            // Deduplicate by ID
            val distinctModules = allModules.distinctBy { it.id }
            _allFetchedModules = distinctModules
            updateDisplayedModules()
            
            // Also fetch installed modules
            fetchInstalledModules()
            
            isRefreshing = false
        }
    }
    
    suspend fun fetchInstalledModules() = withContext(Dispatchers.IO) {
        try {
            val result = listModules()
            Log.d(TAG, "Fetched installed modules: $result")
            val array = JSONArray(result)
            val modules = mutableMapOf<String, String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id").trim()
                val version = obj.optString("version", "unknown")
                modules[id] = version
            }
            Log.d(TAG, "Parsed installed modules: $modules")
            withContext(Dispatchers.Main) {
                installedModules = modules
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching installed modules", e)
        }
    }

    private fun fetchRepo(repo: Repository): List<RepoModule> {
        val targetUrl = if (!repo.url.endsWith(".json")) {
             if (repo.url.endsWith("/")) "${repo.url}json/modules.json" else "${repo.url}/json/modules.json"
        } else {
            repo.url
        }

        try {
            val request = okhttp3.Request.Builder()
                .url(targetUrl)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .build()
            val response = ksuApp.okhttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Fetch failed for ${repo.name}: ${response.code}")
                return emptyList()
            }

            val jsonStr = response.body?.string() ?: return emptyList()
            val modules = mutableListOf<RepoModule>()
            
            // Try parsing as Object first (standard Magisk repo format)
            var parsedAsObject = false
            try {
                // Check if it starts with {
                if (jsonStr.trim().startsWith("{")) {
                    val jsonObject = JSONObject(jsonStr)
                    if (jsonObject.has("modules")) {
                        val modulesArray = jsonObject.getJSONArray("modules")
                        for (i in 0 until modulesArray.length()) {
                            val obj = modulesArray.getJSONObject(i)
                            parseModuleJson(obj)?.let { modules.add(it) }
                        }
                        parsedAsObject = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing repo object ${repo.name}", e)
            }

            if (parsedAsObject) return modules

            // Try parsing as Array (flat list)
            try {
                // Check if it starts with [
                if (jsonStr.trim().startsWith("[")) {
                    val jsonArray = JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        parseModuleJson(obj)?.let { modules.add(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing repo array ${repo.name}", e)
            }
            return modules
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching repo ${repo.name}", e)
            return emptyList()
        }
    }

    private fun parseModuleJson(obj: JSONObject): RepoModule? {
        try {
            var zipUrl = obj.optString("zipUrl")
            var version = obj.optString("version")
            var versionCode = obj.optInt("versionCode", 0)
            var changelog = obj.optString("changelog")
            val id = obj.optString("id")
            val name = obj.optString("name")
            var timestamp = obj.optDouble("timestamp", 0.0)
            
            if (id.isEmpty() || name.isEmpty()) return null

            val versionList = mutableListOf<ModuleVersion>()

            // Check for 'versions' array
            if (obj.has("versions")) {
                val versions = obj.getJSONArray("versions")
                for (j in 0 until versions.length()) {
                    val v = versions.getJSONObject(j)
                    val code = v.optInt("versionCode", -1)
                    val verTimestampRaw = v.optDouble("timestamp", 0.0)
                    val verTimestamp = if (verTimestampRaw > 1000000000000.0) {
                        verTimestampRaw.toLong()
                    } else {
                        (verTimestampRaw * 1000).toLong()
                    }
                    
                    versionList.add(ModuleVersion(
                        version = v.optString("version"),
                        versionCode = code,
                        zipUrl = v.optString("zipUrl").trim(),
                        changelog = v.optString("changelog"),
                        timestamp = verTimestamp
                    ))
                }
            }
            
            // Sort versions to find the latest
            versionList.sortByDescending { it.versionCode }
            val latest = versionList.firstOrNull()
            
            // If we have versions, use the latest one as the main source of truth
            if (latest != null) {
                zipUrl = latest.zipUrl
                version = latest.version
                versionCode = latest.versionCode
                changelog = latest.changelog
                // Use the timestamp from the latest version
                if (latest.timestamp > 0) {
                    timestamp = latest.timestamp.toDouble()
                }
            }
            
            // If we still don't have a zipUrl, this module is invalid for installation
            if (zipUrl.isEmpty()) {
                return null
            }
            
            // Convert timestamp to milliseconds if it looks like seconds (small number relative to current millis)
            // Current time in millis is ~1.7e12. If timestamp is < 1e11 (e.g. 1.7e9), it's likely seconds.
            // Also handle 0.
            val timestampMillis = if (timestamp > 1000000000000.0) {
                timestamp.toLong()
            } else {
                (timestamp * 1000).toLong()
            }

            return RepoModule(
                id = id,
                name = name,
                description = obj.optString("description"),
                version = version,
                versionCode = versionCode,
                author = obj.optString("author"),
                zipUrl = zipUrl,
                changelog = changelog,
                timestamp = timestampMillis,
                versions = versionList.sortedByDescending { it.versionCode }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing module", e)
            return null
        }
    }
    
    // Helper to download for installation
    fun downloadModule(zipUrl: String, id: String, version: String, context: Context): Uri? {
        val safeId = id.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val safeVersion = version.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val fileName = "$safeId-$safeVersion.zip"
        
        var resultUri: Uri? = null
        
        // Use the shared download utility (same as normal module updates)
        com.rifsxd.ksunext.ui.util.download(
            context,
            zipUrl,
            fileName,
            fileName, // Description
            onDownloaded = { uri -> resultUri = uri },
            onDownloading = { /* Already downloading, do nothing (will return null) */ }
        )
        
        return resultUri
    }
}
