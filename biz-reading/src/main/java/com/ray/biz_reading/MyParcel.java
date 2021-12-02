package com.ray.biz_reading;

import android.os.Parcel;
import android.os.Parcelable;

public class MyParcel implements Parcelable {

    String msg;

    public MyParcel(String msg) {
        this.msg = msg;
    }

    protected MyParcel(Parcel in) {
        msg = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(msg);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MyParcel> CREATOR = new Creator<MyParcel>() {
        @Override
        public MyParcel createFromParcel(Parcel in) {
            return new MyParcel(in);
        }

        @Override
        public MyParcel[] newArray(int size) {
            return new MyParcel[size];
        }
    };
}
