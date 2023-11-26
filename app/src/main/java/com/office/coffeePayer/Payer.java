package com.office.coffeePayer;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;

public class Payer implements Parcelable {
    String id;
    String name;
    Long total;
    Long wins;

    public Payer(String id, String name) {
        this.id = id;
        this.name = name;
        this.total = 0L;
        this.wins = 0L;
    }

    protected Payer(Parcel in) {
        id = in.readString();
        name = in.readString();
        total = in.readLong();
        wins = in.readLong();
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

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getWins() {
        return wins;
    }

    public void setWins(Long wins) {
        this.wins = wins;
    }

    public Long getStat() {
        if (total == 0) {
            return 0L;
        }

        return (Long)((wins * 100L) / total);
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
        dest.writeLong(this.total);
        dest.writeLong(this.wins);
    }
}
