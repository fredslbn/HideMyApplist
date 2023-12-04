package icu.nullptr.hidemyapplist.xposed.hook;

import static icu.nullptr.hidemyapplist.xposed.LogcatKt.logD;
import static icu.nullptr.hidemyapplist.xposed.LogcatKt.logE;
import static icu.nullptr.hidemyapplist.xposed.LogcatKt.logW;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

// https://github.com/LSPosed/LSPlant/blob/439f38f48c7fb7ae1942ffda81d02719985665af/lsplant/src/main/jni/lsplant.cc#L855
// https://github.com/LSPosed/LSPlant/blob/439f38f48c7fb7ae1942ffda81d02719985665af/lsplant/src/main/jni/art/runtime/art_method.hpp#L198

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint({"DiscouragedPrivateApi", "SoonBlockedPrivateApi"})
public class ArtHelper {
    private static final String TAG = "ArtHelper";

    private static final int sAccessFlagOffset;
    private static final Field sArtMethodField;
    private static final Field sClassAccessFlags;
    private static final Field sObjectClassField;
    private static final Unsafe theUnsafe;
    private static final int kAccFinal = 0x0010;
    private static final int kAccPublic = 0x0001;

    static {
        Unsafe unsafe = null;
        Field artMethodF = null;
        Field classAccessFlagsF = null;
        Field objectClassField = null;
        int accessFlagsOffset = 0;
        try {
            objectClassField = Object.class.getDeclaredField("shadow$_klass_");
            objectClassField.setAccessible(true);
            classAccessFlagsF = Class.class.getDeclaredField("accessFlags");
            classAccessFlagsF.setAccessible(true);
            unsafe = (Unsafe) Unsafe.class.getDeclaredMethod("getUnsafe").invoke(null);
            Constructor<?>[] cstrs = Throwable.class.getDeclaredConstructors();
            Constructor<?> cstr1 = cstrs[0];
            Constructor<?> cstr2 = cstrs[1];
            artMethodF = Executable.class.getDeclaredField("artMethod");
            artMethodF.setAccessible(true);
            long artMethodSize = (long) artMethodF.get(cstr2) - (long) artMethodF.get(cstr1);
            Field accessFlagsF = Executable.class.getDeclaredField("accessFlags");
            accessFlagsF.setAccessible(true);
            long addr1 = (long) artMethodF.get(cstr1);
            int flags = (int) accessFlagsF.get(cstr1);
            for (int i = 0; i < artMethodSize; i += 4) {
                if (unsafe.getInt(addr1 + i) == flags) {
                    accessFlagsOffset = i;
                }
            }
            if (accessFlagsOffset == 0) {
                logW(TAG, "failed to get access flags, use 4 fallback", null);
                accessFlagsOffset = 4;
            } else {
                logD(TAG, "accessFlagsOffset=" + accessFlagsOffset, null);
            }
        } catch (Throwable t) {
            logE(TAG, "failed to init artMethodHelper", t);
        }
        theUnsafe = unsafe;
        sAccessFlagOffset = accessFlagsOffset;
        sArtMethodField = artMethodF;
        sClassAccessFlags = classAccessFlagsF;
        sObjectClassField = objectClassField;
    }

    public static void setObjectClass(Object o, Class<?> c) throws Throwable {
        try {
            sObjectClassField.set(o, c);
        } catch (Throwable t) {
            logE(TAG, "failed to set " + o + " class to " + c, t);
            throw t;
        }
    }

    public static void setMethodNonFinal(Executable method) throws Throwable {
        try {
            long addr = (long) sArtMethodField.get(method);
            int flags = theUnsafe.getInt(addr + sAccessFlagOffset);
            flags &= ~kAccFinal;
            theUnsafe.putInt(addr + sAccessFlagOffset, flags);
        } catch (Throwable t) {
            logE(TAG, "failed to set " + method + " to non-final", t);
            throw t;
        }
    }

    public static void setClassNonFinal(Class<?> clazz) throws Throwable {
        try {
            int flags = (int) sClassAccessFlags.get(clazz);
            sClassAccessFlags.set(clazz, flags & ~kAccFinal);
        } catch (Throwable t) {
            logE(TAG, "failed to set " + clazz + " to non-final", t);
            throw t;
        }
    }

    public static void setClassPublic(Class<?> clazz) throws Throwable {
        try {
            int flags = (int) sClassAccessFlags.get(clazz);
            sClassAccessFlags.set(clazz, flags | kAccPublic);
        } catch (Throwable t) {
            logE(TAG, "failed to set " + clazz + " to public", t);
            throw t;
        }
    }
}
