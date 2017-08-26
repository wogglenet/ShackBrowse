package net.woggle.shackbrowse;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by brad on 2/1/2015.
 *
 * ProgressDialog.show(getActivity(), "Loading Taggers", "ThomW's server is a loose cannon", true, true, new OnCancelListener(){
@Override
public void onCancel(DialogInterface arg0) {
cancel(true);
System.out.println("CANCELED");
}});
 */

public class MaterialProgressDialog {
    public static MaterialDialog show(Activity activity, String title, String desc) {
        return show(activity, title, desc, false, false, null);
    }
    public static MaterialDialog show(Activity activity, String title, String desc, boolean indeterminate, boolean cancelable) {
        return show(activity, title, desc, indeterminate, cancelable, null);
    }
    public static MaterialDialog show(Activity activity, String title, String desc, boolean indeterminate, boolean cancelable, DialogInterface.OnCancelListener cancellistener) {

        MaterialDialog progressDialog = new MaterialDialog.Builder(activity)
                .title(title)
                .customView(R.layout.progress_dialog_material, false)
                .build();
        View customView = progressDialog.getCustomView();
        TextView tvMessage = (TextView) customView.findViewById(R.id.message);
        tvMessage.setText(desc);
        progressDialog.setCancelable(cancelable);
        progressDialog.setOnCancelListener(cancellistener);
        progressDialog.show();

        return progressDialog;
    }
}
