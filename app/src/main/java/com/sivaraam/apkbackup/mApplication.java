package com.sivaraam.apkbackup;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sivaraam on 20/6/17.
 */

class mApplication
{
    public final String label;
    public final String version;
    public final String size;
    public Drawable icon;
    public final int id;
    public final long fileSizeBytes;

    mApplication(String label, String version, Drawable icon, String size, long fileSizeBytes, int appid)
    {
        this.label = label;
        this.version = version;
        this.icon = icon;
        this.id = appid;
        this.size = size;
        this.fileSizeBytes = fileSizeBytes;
    }

    protected mApplication(Parcel in)
    {
        label = in.readString();
        version = in.readString();
        size = in.readString();
        id = in.readInt();
        fileSizeBytes = in.readLong();
    }
}
