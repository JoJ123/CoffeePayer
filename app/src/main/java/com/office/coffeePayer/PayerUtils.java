package com.office.coffeePayer;

import android.content.Context;
import android.os.Build;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class PayerUtils {
    static String PAYERS_STORE = "data";

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Optional<Payer> containsPayer(final ArrayList<Payer> payers, final String id) {
        return payers.stream().filter(o -> o.id.equals(id)).findFirst();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Optional<Payer> containsPayerName(final ArrayList<Payer> payers, final String id) {
        return payers.stream().filter(o -> o.id.equals(id)).findFirst();
    }

    public static ArrayAdapter<String> getDisplayAdapter(ArrayList<Payer> al_payers, Context context, int layout) {
        String[] payerString = al_payers.stream().map(Objects::toString).toArray(String[]::new);
        return new ArrayAdapter(context, layout, android.R.id.text1, al_payers) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                String name = al_payers.get(position).getName();
                text1.setText(name);
                if (!name.startsWith("Guest ")) {
                    text2.setText(al_payers.get(position).getStat() + "%");
                } else {
                    text2.setText("- %");
                }

                return view;
            }
        };
    }

    public static void filterPayerList(Iterator<Payer> it, SparseBooleanArray checked) {

        int index = 0;
        while (it.hasNext()) {
            it.next();
            boolean a = checked.get(index);
            if (a) {
                it.remove();
            }
            index++;
        }
    }

    public static String convertBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte temp : bytes) {
            int decimal = (int) temp & 0xff;  // bytes widen to int, need mask, prevent sign extension
            String hex = Integer.toHexString(decimal);
            result.append(hex);
        }

        return result.toString();
    }

    public static ArrayList<Payer> getPayerList(TinyDB tinydb) {
        ArrayList<Object> payerObjects = tinydb.getListObject("data", Payer.class);
        ArrayList<Payer> payerList = new ArrayList<Payer>();
        for (Object payer : payerObjects) {
            payerList.add((Payer) payer);
        }

        payerList.sort(new PayerCompare());

        return payerList;
    }

    public static void setPayerList(TinyDB tinydb, ArrayList<Payer> al_payers) {
        ArrayList<Object> payerObjects = new ArrayList<Object>();
        for (Payer payer : al_payers) {
            payerObjects.add((Object) payer);
        }
        tinydb.putListObject("data", payerObjects);
    }
}
