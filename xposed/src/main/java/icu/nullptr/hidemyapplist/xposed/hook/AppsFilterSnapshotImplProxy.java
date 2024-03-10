package icu.nullptr.hidemyapplist.xposed.hook;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.server.pm.AppsFilterSnapshotImpl;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.snapshot.PackageDataSnapshot;

@Keep
@RequiresApi(33)
public class AppsFilterSnapshotImplProxy extends AppsFilterSnapshotImpl {

    @Override
    public boolean shouldFilterApplication(PackageDataSnapshot snapshot, int callingUid, @Nullable Object callingSetting, PackageStateInternal targetPkgSetting, int userId) {
        if (PmsHookTarget33.shouldFilterApplication(snapshot, callingUid, targetPkgSetting)) {
            return true;
        }
        return super.shouldFilterApplication(snapshot, callingUid, callingSetting, targetPkgSetting, userId);
    }
}
