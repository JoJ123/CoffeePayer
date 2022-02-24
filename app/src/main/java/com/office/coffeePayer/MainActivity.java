package com.office.coffeePayer;

import android.app.PendingIntent;
import android.content.Intent;
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
import java.util.ListIterator;
import java.util.Optional;
import java.util.Random;

import static com.office.coffeePayer.PayerUtils.containsPayer;
import static com.office.coffeePayer.PayerUtils.convertBytesToHex;
import static com.office.coffeePayer.PayerUtils.getPayerList;

public class MainActivity extends AppCompatActivity {
    public static final String WINNER = "com.office.coffee.WINNER";

    ListView lv_payers;

    TinyDB tinydb;
    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    ArrayList<Payer> al_payers = new ArrayList<Payer>();
    ArrayList<Payer> al_possiblePayers = new ArrayList<Payer>();
    Integer guestCounter = 1;

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
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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
        int payerCount = al_payers.size();
        if (payerCount > 0) {
            Random r = new Random();
            String name = al_payers.get(r.nextInt(payerCount)).name;

            final Intent intent = new Intent(this, Winner.class);
            intent.putExtra(WINNER, name);
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

    protected void updatePayersListDisplay() {
        ArrayAdapter adapter = PayerUtils.getDisplayAdapter(al_payers, getApplicationContext(),
                android.R.layout.simple_list_item_1);
        runOnUiThread(() -> lv_payers.setAdapter(adapter));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();
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
            } else {
                if (!payer.name.toLowerCase().startsWith("guest")) {
                    iterator.remove();
                }
            }
        }

        updatePayersListDisplay();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

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
                Toast.makeText(getApplicationContext(),
                        "This tag is already used by '" + existingPayer.get().name + "'.", Toast.LENGTH_SHORT).show();
            }
        }

        super.onNewIntent(intent);
    }
}
