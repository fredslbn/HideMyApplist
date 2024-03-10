package icu.nullptr.hidemyapplist.xposed.hook;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import com.android.server.pm.AppsFilter;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.SettingBase;

@Keep
@RequiresApi(30)
public class AppsFilterProxy extends AppsFilter {
    @Override
    public boolean shouldFilterApplication(int callingUid, SettingBase callingSetting, PackageSetting targetPkgSetting, int userId) {
        if (PmsHookTarget30.shouldFilterApplication(callingUid, targetPkgSetting)) return true;
        return super.shouldFilterApplication(callingUid, callingSetting, targetPkgSetting, userId);
    }
}
