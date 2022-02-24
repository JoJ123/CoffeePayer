package com.office.coffeePayer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AddPayerDialog extends DialogFragment {
    public interface AddPayerListener {
        public void onDialogPositiveClick(String id, String name);
    }

    String id;
    AddPayerListener listener;

    public AddPayerDialog(String id) {
        this.id = id;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (AddPayerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("You must implement NoticeDialogListener");
        }
    }

    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(getResources().getString(R.string.addPayer));
        alertDialog.setMessage(getResources().getString(R.string.enterName));

        final EditText input = new EditText(getActivity());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        LinearLayout ll = new LinearLayout(alertDialog.getContext());
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(
                dpToPx(24, alertDialog.getContext().getResources()),
                0,
                dpToPx(24, alertDialog.getContext().getResources()),
                0);
        ll.addView(input, layoutParams);

        alertDialog.setView(ll);
        alertDialog.setPositiveButton("Add",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        listener.onDialogPositiveClick(id, name);
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        return alertDialog.create();
    }
}