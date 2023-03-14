package net.swigglesoft.shackbrowse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TermsAndConditionsDialogFragment extends DialogFragment {

    public static final String SHACKBROWSE_TCS = "SHACKBROWSE_TCS";
    public static final String TERMS_AND_CONDITIONS = "TERMS AND CONDITIONS";

    public static final int CURRENT_TERMS_VERSION = 1;

    private Context context;

    public TermsAndConditionsDialogFragment(MainActivity mainActivity) {
        this.context = mainActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(Html.fromHtml(context.getResources().getString(R.string.tcs_dialog)))
                .setCancelable(false)
                .setPositiveButton(context.getResources().getString(R.string.tcs_agree), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences shackbrowse_tcs = context.getSharedPreferences(SHACKBROWSE_TCS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = shackbrowse_tcs.edit();
                        edit.putInt(TERMS_AND_CONDITIONS, CURRENT_TERMS_VERSION);
                        edit.commit();
                    }
                });
        return builder.create();
    }
}
