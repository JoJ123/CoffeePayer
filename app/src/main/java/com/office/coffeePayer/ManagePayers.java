package com.office.coffeePayer;

import static com.office.coffeePayer.PayerUtils.containsPayer;
import static com.office.coffeePayer.PayerUtils.convertBytesToHex;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class ManagePayers extends AppCompatActivity implements AddPayerDialog.AddPayerListener {
    ListView lv_possiblePayers;

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    TinyDB tinydb;

    ArrayList<Payer> al_possiblePayers = new ArrayList<Payer>();

    private static final int READER_FLAGS =  NfcAdapter.FLAG_READER_NFC_A |
            NfcAdapter.FLAG_READER_NFC_B |
            NfcAdapter.FLAG_READER_NFC_F |
            NfcAdapter.FLAG_READER_NFC_V |
            NfcAdapter.FLAG_READER_NFC_BARCODE |
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

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
        mAdapter.enableReaderMode(this, getReaderCallback(), READER_FLAGS,null);

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);
    }

    public void resetStats(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ManagePayers.this);
        alert.setTitle("Delete");
        alert.setMessage("Are you sure you want to delete?");
        alert.setPositiveButton("Yes", (dialog, which) -> {
            al_possiblePayers.forEach(payer -> {
                payer.setWins(0L);
                payer.setTotal(0L);
            });
            PayerUtils.setPayerList(tinydb, al_possiblePayers);

            updatePayersList();

            dialog.dismiss();
        });

        alert.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    private NfcAdapter.ReaderCallback getReaderCallback() {
        return tag -> {
            String id = PayerUtils.convertBytesToHex(tag.getId());
            Optional<Payer> existingPayer = PayerUtils.containsPayer(al_possiblePayers, id);
            if (!existingPayer.isPresent()) {
                showAddPayerDialog(id);
            } else {
                existingPayer.ifPresent(payer -> {
                    payerAlreadyInList(payer.getName());
                });
            }
        };
    }

    protected void payerAlreadyInList(String name) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                "This tag is already used by '" + name + "'.", Toast.LENGTH_SHORT).show());
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
                android.R.layout.simple_list_item_activated_2);
        runOnUiThread(() -> lv_possiblePayers.setAdapter(adapter));
    }

    public void showAddPayerDialog(String id) {
        DialogFragment dialog = new AddPayerDialog(id);
        dialog.show(getSupportFragmentManager(), "AppPayerDialog");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableReaderMode(this, getReaderCallback(), READER_FLAGS,null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAdapter != null) {
            mAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableReaderMode(this);
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
