package com.android.server.pm;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(30)
public class AppsFilter {

    public boolean shouldFilterApplication(int callingUid, @Nullable SettingBase callingSetting,
                                           PackageSetting targetPkgSetting, int userId) {
        throw new RuntimeException();
    }
}
