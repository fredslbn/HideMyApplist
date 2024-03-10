package com.android.server.pm;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.snapshot.PackageDataSnapshot;

@RequiresApi(33)
public /*private*/ class AppsFilterImpl extends AppsFilterBase {

    public boolean shouldFilterApplication(PackageDataSnapshot snapshot, int callingUid,
                                           @Nullable Object callingSetting, PackageStateInternal targetPkgSetting, int userId) {
        throw new RuntimeException("");
    }
}
