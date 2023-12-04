package icu.nullptr.hidemyapplist.common;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.util.Objects;

// https://github.com/RikkaApps/Shizuku-API/blob/01e08879d58a5cb11a333535c6ddce9f7b7c88ff/api/src/main/java/rikka/shizuku/ShizukuBinderWrapper.java

public class BinderWrapper implements IBinder {
    public static final int TRANSACT_CODE = ('_' << 24) | ('H' << 16) | ('M' << 8) | 'A';
    public static final String BINDER_DESCRIPTOR = "io.github.a13e300.BINDER_WRAPPER";

    private final IBinder original;
    private final IBinder service;

    public BinderWrapper(@NonNull IBinder original, @NonNull IBinder service) {
        this.original = Objects.requireNonNull(original);
        this.service = Objects.requireNonNull(service);
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        Parcel newData = Parcel.obtain();
        try {
            newData.writeInterfaceToken(BINDER_DESCRIPTOR);
            newData.writeStrongBinder(original);
            newData.writeInt(code);
            newData.writeInt(flags);
            newData.appendFrom(data, 0, data.dataSize());
            return service.transact(TRANSACT_CODE, newData, reply, 0);
        } finally {
            newData.recycle();
        }
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return original.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return original.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return original.isBinderAlive();
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        original.dump(fd, args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        original.dumpAsync(fd, args);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        original.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return original.unlinkToDeath(recipient, flags);
    }
}