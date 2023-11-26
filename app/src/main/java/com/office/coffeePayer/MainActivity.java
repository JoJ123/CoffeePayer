package com.office.coffeePayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static com.office.coffeePayer.PayerUtils.containsPayer;
import static com.office.coffeePayer.PayerUtils.convertBytesToHex;
import static com.office.coffeePayer.PayerUtils.getPayerList;

public class MainActivity extends AppCompatActivity {
    public static final String WINNER = "com.office.coffee.WINNER";
    private static final int READER_FLAGS =  NfcAdapter.FLAG_READER_NFC_A |
            NfcAdapter.FLAG_READER_NFC_B |
            NfcAdapter.FLAG_READER_NFC_F |
            NfcAdapter.FLAG_READER_NFC_V |
            NfcAdapter.FLAG_READER_NFC_BARCODE |
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    ListView lv_payers;

    TinyDB tinydb;
    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    ArrayList<Payer> al_payers = new ArrayList<Payer>();
    ArrayList<Payer> al_possiblePayers = new ArrayList<Payer>();
    Integer guestCounter = 1;

    private SensorManager mSensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv_payers = findViewById(R.id.lv_payers);
        lv_payers.setOnItemClickListener(onPlayerItemClickListener);

        tinydb = new TinyDB(this);
        al_possiblePayers = getPayerList(tinydb);

        updatePayersListDisplay();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "NFC not supported on this phone!", Toast.LENGTH_LONG).show();
        }
        int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F |
                NfcAdapter.FLAG_READER_NFC_V |
                NfcAdapter.FLAG_READER_NFC_BARCODE |
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        mAdapter.enableReaderMode(this, getReaderCallback(), READER_FLAGS,null);

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(mSensorManager).registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            if (mAccel > 35) {
                getWinner();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private NfcAdapter.ReaderCallback getReaderCallback() {
        return tag -> {
            String id = convertBytesToHex(tag.getId());
            Optional<Payer> possiblePayer = containsPayer(al_possiblePayers, id);
            Optional<Payer> existingPayer = containsPayer(al_payers, id);

            if (!existingPayer.isPresent()) {
                Payer payer;
                if (possiblePayer.isPresent())
                    payer = possiblePayer.get();
                else {
                    payer = new Payer(id, "Guest " + guestCounter++);
                }
                al_payers.add(0, payer);
            } else {
                payerAlreadyInList(existingPayer.get().getName());
            }
            updatePayersListDisplay();
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.managePayers:
                Intent managePayers = new Intent(this, ManagePayers.class);
                startActivity(managePayers);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void onGoClick(View view) {
        getWinner();
    }


    public void getWinner() {
        int payerCount = al_payers.size();
        if (payerCount > 0) {
            Random r = new Random();

            Integer previous;
            Integer random = -1;
            do {
                previous = random;
                random = r.nextInt(payerCount + 1);
            } while (random == 0 || previous != random);
            Payer payerWin = al_payers.get(random - 1);

            Optional<Payer> winnerToUpdate = al_possiblePayers.stream().filter(o -> o.id.equals(payerWin.id)).findFirst();
            if (winnerToUpdate.isPresent()) {
                winnerToUpdate.get().setWins(winnerToUpdate.get().wins + 1);
            }

            List<Payer> al_possiblePayersToUpdate = al_possiblePayers.stream().filter(o ->
                    al_payers.stream().filter(al_payer -> al_payer.id.equals(o.id)).findFirst().orElse(null) != null).collect(Collectors.toList());

            al_possiblePayersToUpdate.forEach(payer -> {
                    payer.setTotal(payer.total + 1);
            });

            PayerUtils.setPayerList(tinydb, al_possiblePayers);

            al_payers.forEach(gamePayer -> {
                Payer updatePayer = al_possiblePayers.stream().filter(o -> gamePayer.id.equals(o.id)).findFirst().orElse(null);
                if (updatePayer != null) {
                    gamePayer.setWins(updatePayer.getWins());
                    gamePayer.setTotal(updatePayer.getTotal());
                }
            });

            final Intent intent = new Intent(this, Winner.class);
            intent.putExtra(WINNER, payerWin.name);
            startActivity(intent);
        }
    }

    OnItemClickListener onPlayerItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            al_payers.remove(position);
            updatePayersListDisplay();
        }
    };

    protected void payerAlreadyInList(String name) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                "This tag is already used by '" + name + "'.", Toast.LENGTH_SHORT).show());
    }

    protected void updatePayersListDisplay() {
        ArrayAdapter adapter = PayerUtils.getDisplayAdapter(al_payers, getApplicationContext(),
                android.R.layout.simple_list_item_2);
        runOnUiThread(() -> lv_payers.setAdapter(adapter));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSensorManager != null) {
            mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (mAdapter != null) {
            mAdapter.enableReaderMode(this, getReaderCallback(), READER_FLAGS,null);
        }

        Log.d("RESUME", "1");
        ArrayList<Object> payerObjects = tinydb.getListObject("data", Payer.class);
        al_possiblePayers.clear();
        for (Object payer : payerObjects) {
            al_possiblePayers.add((Payer) payer);
        }

        ListIterator<Payer> iterator = al_payers.listIterator();
        while (iterator.hasNext()) {
            Payer payer = iterator.next();
            Optional<Payer> possiblePayer = containsPayer(al_possiblePayers, payer.id);
            if (possiblePayer.isPresent()) {
                payer.name = possiblePayer.get().name;
                payer.setTotal(possiblePayer.get().getTotal());
                payer.setWins(possiblePayer.get().getWins());
            } else {
                if (!payer.name.toLowerCase().startsWith("guest")) {
                    iterator.remove();
                }
            }
        }

        updatePayersListDisplay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        if (mAdapter != null) {
            mAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        if (mAdapter != null) {
            mAdapter.disableReaderMode(this);
        }
    }
}
