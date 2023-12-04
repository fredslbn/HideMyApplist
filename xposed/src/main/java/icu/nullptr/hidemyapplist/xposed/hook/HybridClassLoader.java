package icu.nullptr.hidemyapplist.xposed.hook;

import static icu.nullptr.hidemyapplist.xposed.LogcatKt.logD;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedBridge;

public class HybridClassLoader extends ClassLoader {
    public static boolean sInjected = false;
    private final ClassLoader mBase;
    private final ClassLoader mParent;

    public HybridClassLoader(ClassLoader parent, ClassLoader base) {
        mBase = base;
        mParent = parent;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    public static void injectClassLoader(ClassLoader classLoader) throws Throwable {
        Field fParent = ClassLoader.class.getDeclaredField("parent");
        fParent.setAccessible(true);
        ClassLoader mine = HybridClassLoader.class.getClassLoader();
        ClassLoader curr = (ClassLoader) fParent.get(mine);
        if (curr == null) curr = XposedBridge.class.getClassLoader();
        if (!curr.getClass().getName().equals(HybridClassLoader.class.getName())) {
            fParent.set(mine, new HybridClassLoader(curr, classLoader));
        }
        sInjected = true;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        logD("HybridClassLoader", "loading: " + name, null);
        if (name.startsWith("com.android.server.")) {
            return mBase.loadClass(name);
        }
        try {
            return mParent.loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }
}
