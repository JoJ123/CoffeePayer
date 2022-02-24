package com.office.coffeePayer;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.Charset;

public class Payer implements Parcelable {
    String id;
    String name;

    public Payer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    protected Payer(Parcel in) {
        id = in.readString();
        name = in.readString();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static final Creator<Payer> CREATOR = new Creator<Payer>() {
        @Override
        public Payer createFromParcel(Parcel in) {
            return new Payer(in);
        }

        @Override
        public Payer[] newArray(int size) {
            return new Payer[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
    }
}
