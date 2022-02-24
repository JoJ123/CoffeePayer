package com.office.coffeePayer;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Optional;

public class ManagePayers extends AppCompatActivity implements AddPayerDialog.AddPayerListener {
    ListView lv_possiblePayers;

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    TinyDB tinydb;

    ArrayList<Payer> al_possiblePayers = new ArrayList<Payer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_payers);

        lv_possiblePayers = findViewById(R.id.lv_payers);

        tinydb = new TinyDB(this);
        al_possiblePayers = PayerUtils.getPayerList(tinydb);

        updatePayersList();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            //nfc not support your device.
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_payers_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.removePayers:
                if (lv_possiblePayers.getCheckedItemCount() > 0) {
                    PayerUtils.filterPayerList(al_possiblePayers.listIterator(), lv_possiblePayers.getCheckedItemPositions());
                    PayerUtils.setPayerList(tinydb, al_possiblePayers);
                    updatePayersList();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void updatePayersList() {
        ArrayAdapter adapter = PayerUtils.getDisplayAdapter(al_possiblePayers, getApplicationContext(),
                android.R.layout.simple_list_item_multiple_choice);
        runOnUiThread(() -> lv_possiblePayers.setAdapter(adapter));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            String id = PayerUtils.convertBytesToHex(tag.getId());
            Optional<Payer> existingPayer = PayerUtils.containsPayer(al_possiblePayers, id);
            if (!existingPayer.isPresent()) {
                showAddPayerDialog(id);
            } else {
                existingPayer.ifPresent(payer -> {
                    Toast.makeText(getApplicationContext(),
                            "This tag is already used by '" + payer.name + "'.", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }


    public void showAddPayerDialog(String id) {
        DialogFragment dialog = new AddPayerDialog(id);
        dialog.show(getSupportFragmentManager(), "AppPayerDialog");
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    @Override
    public void onDialogPositiveClick(String id, String name) {
        if (id.isEmpty() || name.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "You have to enter a name.", Toast.LENGTH_SHORT).show();
        } else if (PayerUtils.containsPayerName(al_possiblePayers, name).isPresent()) {
            Toast.makeText(getApplicationContext(),
                    "This name is already used.", Toast.LENGTH_SHORT).show();
        } else if (name.toLowerCase().startsWith("guest")) {
            Toast.makeText(getApplicationContext(),
                    "The name 'Guest' is reserved.", Toast.LENGTH_SHORT).show();
        } else {
            al_possiblePayers.add(new Payer(id, name));
            PayerUtils.setPayerList(tinydb, al_possiblePayers);
            updatePayersList();
        }
    }
}
