package icu.nullptr.hidemyapplist.xposed.hook

import android.annotation.TargetApi
import android.os.Build
import com.android.server.pm.AppsFilterImpl
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import icu.nullptr.hidemyapplist.xposed.logW
import java.util.concurrent.atomic.AtomicReference

@TargetApi(Build.VERSION_CODES.TIRAMISU)
class PmsHookTarget33(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "PmsHookTarget33"
        private var sInstance: PmsHookTarget33? = null
        private var useFallback = true
        private var origAppFilter: Any? = null
        private var unhooked = false

        @JvmStatic
        fun shouldFilterApplication(
            snapshot: Any,
            callingUid: Int,
            targetPkgSetting: Any
        ): Boolean {
            return sInstance?.shouldFilterApp(snapshot, callingUid, targetPkgSetting) ?: false
        }
    }

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    private var hooks = mutableSetOf<XC_MethodHook.Unhook>()
    private var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    @Suppress("UNCHECKED_CAST")
    private fun shouldFilterApp(snapshot: Any, callingUid: Int, targetPkgSetting: Any): Boolean {
        var shouldFilter = false
        runCatching {
            if (unhooked) return@runCatching
            if (callingUid == Constants.UID_SYSTEM) return@runCatching
            val callingApps = Utils.binderLocalScope {
                getPackagesForUidMethod.invoke(snapshot, callingUid) as Array<String>?
            } ?: return@runCatching
            val targetApp =
                Utils.getPackageNameFromPackageSettings(targetPkgSetting) // PackageSettings <- PackageStateInternal
            for (caller in callingApps) {
                if (service.shouldHide(caller, targetApp)) {
                    shouldFilter = true
                    service.filterCount++
                    val last = lastFilteredApp.getAndSet(caller)
                    if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                    logD(
                        TAG,
                        "@shouldFilterApplication caller: $callingUid $caller, target: $targetApp"
                    )
                    return@runCatching
                }
            }
        }.onFailure {
            logE(TAG, "Fatal error occurred, disable hooks", it)
            unload()
        }
        return shouldFilter
    }

    override fun load() {
        logI(TAG, "Load hook")
        sInstance = this
        doOptimizeHook()
    }

    private fun doOptimizeHook() {
        logI(TAG, "installing optimize hook")
        runCatching {
            if (!HybridClassLoader.sInjected) {
                logI(TAG, "hybridClassLoader not injected, use fallback")
                return@runCatching
            }
            val pms = service.pms
            val filter = pms.getObject("mService").getObject("mAppsFilter")
            val appFilterImplClass = Class.forName(
                "com.android.server.pm.AppsFilterImpl",
                false,
                InitFields.ezXClassLoader
            )
            if (filter.javaClass != appFilterImplClass) {
                logW(
                    TAG,
                    "appsFilter class is not AppsFilterImpl: ${filter.javaClass.name}, use fallback"
                )
                return@runCatching
            }
            ArtHelper.setClassNonFinal(appFilterImplClass)
            val appFilterSnapshotImplClass = Class.forName(
                "com.android.server.pm.AppsFilterSnapshotImpl",
                false,
                InitFields.ezXClassLoader
            )
            ArtHelper.setClassNonFinal(appFilterSnapshotImplClass)
            val appFilterProxyClass =
                Class.forName("icu.nullptr.hidemyapplist.xposed.hook.AppsFilterImplProxy")
            ArtHelper.setObjectClass(filter, appFilterProxyClass)
            val appFilterSnapshotImplProxyClass =
                Class.forName("icu.nullptr.hidemyapplist.xposed.hook.AppsFilterSnapshotImplProxy")
            hooks.addAll(hookAllConstructorAfter("com.android.server.pm.AppsFilterSnapshotImpl") {
                ArtHelper.setObjectClass(it.thisObject, appFilterSnapshotImplProxyClass)
            })
            origAppFilter = filter
            useFallback = false
            service.currentHookType = "API33-Optimize"
            logI(TAG, "install optimize hook success")
        }.onFailure {
            logE(TAG, "failed to install, use fallback", it)
        }
        if (useFallback) {
            doFallbackHook()
        }
    }

    private fun doFallbackHook() {
        logI(TAG, "do fallback hook")
        hooks.add(findMethod("com.android.server.pm.AppsFilterImpl", findSuper = true) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            val snapshot = param.args[0]
            val callingUid = param.args[1] as Int
            val targetPkgSetting = param.args[3]
            if (shouldFilterApp(snapshot, callingUid, targetPkgSetting)) {
                param.result = true
            }
        })
        service.currentHookType = "API33-Fallback"
    }

    override fun unload() {
        hooks.forEach { it.unhook() }
        hooks.clear()
        unhooked = true
        sInstance = null
        if (!useFallback) {
            runCatching {
                ArtHelper.setObjectClass(
                    service.pms.getObject("mService").getObject("mAppsFilter"),
                    AppsFilterImpl::class.java
                )
            }.onFailure {
                logE(TAG, "failed to restore appFilter class")
            }
        }
    }
}
