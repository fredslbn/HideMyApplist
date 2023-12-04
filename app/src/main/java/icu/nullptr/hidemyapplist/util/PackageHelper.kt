package icu.nullptr.hidemyapplist.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.util.Log
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.UserManagerApis
import java.text.Collator
import java.util.Locale

object PackageHelper {

    class PackageCache(
        val info: PackageInfo,
        val label: String
    )

    private object Comparators {
        val byLabel = Comparator<String> { o1, o2 ->
            val n1 = loadAppLabel(o1).lowercase(Locale.getDefault())
            val n2 = loadAppLabel(o2).lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byPackageName = Comparator<String> { o1, o2 ->
            val n1 = o1.lowercase(Locale.getDefault())
            val n2 = o2.lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byInstallTime = Comparator<String> { o1, o2 ->
            val n1 = loadPackageInfo(o1).firstInstallTime
            val n2 = loadPackageInfo(o2).firstInstallTime
            n2.compareTo(n1)
        }
        val byUpdateTime = Comparator<String> { o1, o2 ->
            val n1 = loadPackageInfo(o1).lastUpdateTime
            val n2 = loadPackageInfo(o2).lastUpdateTime
            n2.compareTo(n1)
        }
    }
    private val pm = hmaApp.packageManager

    private val packageCache = MutableSharedFlow<Map<String, PackageCache>>(replay = 1)
    private val mAppList = MutableSharedFlow<MutableList<String>>(replay = 1)
    private val mSortedList = MutableSharedFlow<MutableList<String>>(replay = 1)
    val appList: SharedFlow<List<String>> = mSortedList
    val lowerAppList: SharedFlow<List<String>> = mAppList

    init {
        invalidateCache()
    }

    fun invalidateCache() {
        hmaApp.globalScope.launch {
            val cache = withContext(Dispatchers.IO) {
                val start = System.currentTimeMillis()
                val packageMap = mutableMapOf<String, PackageCache>()
                for (userId in UserManagerApis.getUserIdsNoThrow()) {
                    val packages = PackageManagerApis.getInstalledPackagesNoThrow(0, userId)
                    packages.forEach { packageInfo ->
                        if (packageInfo.packageName in Constants.packagesShouldNotHide) return@forEach
                        packageMap.computeIfAbsent(packageInfo.packageName) {
                            packageInfo.applicationInfo.uid %= 100000
                            val label =
                                pm.getApplicationLabel(packageInfo.applicationInfo).toString()
                            PackageCache(packageInfo, label)
                        }
                    }
                }
                Log.d("PackageHelper", "get list in ${System.currentTimeMillis() - start}ms")
                packageMap
            }
            packageCache.emit(cache)
            mAppList.emit(cache.keys.toMutableList())
        }
    }

    suspend fun sortList(firstComparator: Comparator<String>) {
        var comparator = when (PrefManager.appFilter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> Comparators.byLabel
            PrefManager.SortMethod.BY_PACKAGE_NAME -> Comparators.byPackageName
            PrefManager.SortMethod.BY_INSTALL_TIME -> Comparators.byInstallTime
            PrefManager.SortMethod.BY_UPDATE_TIME -> Comparators.byUpdateTime
        }
        if (PrefManager.appFilter_reverseOrder) comparator = comparator.reversed()
        val list = mAppList.first()
        list.sortWith(firstComparator.then(comparator))
        mSortedList.emit(list)
    }

    fun loadPackageInfo(packageName: String): PackageInfo = runBlocking {
        packageCache.first()[packageName]!!.info
    }

    fun loadAppLabel(packageName: String): String = runBlocking {
        packageCache.first()[packageName]!!.label
    }

    fun loadAppIcon(packageName: String): Bitmap = runBlocking {
        val packageInfo = packageCache.first()[packageName]!!.info
        hmaApp.appIconLoader.loadIcon(packageInfo.applicationInfo)
    }

    fun isSystem(packageName: String): Boolean = runBlocking {
        packageCache.first()[packageName]!!.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }
}
