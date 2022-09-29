package ir.mahdiparastesh.homechat.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import ir.mahdiparastesh.homechat.ActivityMain;
import ir.mahdiparastesh.homechat.Network.DownloadFile;
import ir.mahdiparastesh.homechat.Network.NetInfo;
import ir.mahdiparastesh.homechat.R;

public class DbUpdate extends AsyncTask<Void, String, Void> {

    private final static String TAG = "DbUpdate";
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/%s.gz";
    private final static String REQ = "select count(%1$s) from %2$s";
    private int nb;
    private final String file;
    private final String database;
    private final String field;
    private ProgressDialog progress;
    protected WeakReference<Activity> mActivity;

    public DbUpdate(Activity activity, String file, String database, String field, int kilobytes) {
        this.file = file;
        this.database = database;
        this.field = field;
        mActivity = new WeakReference<>(activity);
        final Activity d = mActivity.get();
        if (d != null) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(d);
            dialog.setMessage(d.getString(R.string.preferences_resetdb_action, file,
                    kilobytes));
            dialog.setPositiveButton(R.string.btn_yes, (dialog12, which) -> {
                if (!getStatus().equals(Status.RUNNING)) execute();
            });
            dialog.setNegativeButton(R.string.btn_no, (dialog1, which) -> cancel(true));
            dialog.show();
        }
    }

    @Override
    protected void onPreExecute() {
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                d.setProgressBarIndeterminateVisibility(true);
                progress = ProgressDialog.show(d, "", d.getString(R.string.task_db, file));
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            nb = countEntries();
            if (mActivity != null) {
                final Activity d = mActivity.get();
                if (d != null) {
                    remoteCopy(d);
                }
            }
        } catch (Exception e) {
            cancel(true);
        }
        return null;
    }

    private void remoteCopy(final Context ctxt) throws IOException, NullPointerException {
        Log.v(TAG, "Copying " + file + " remotly");
        if (NetInfo.isConnected(ctxt)) {
            new DownloadFile(ctxt, String.format(DB_REMOTE, file), ctxt.openFileOutput(file,
                    Context.MODE_PRIVATE));
        }
    }

    private int countEntries() {
        SQLiteDatabase db = null;
        int nb = 0;
        try {
            db = SQLiteDatabase.openDatabase(Db.PATH + file, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = db.rawQuery(String.format(REQ, field, database), null);
            if (c.moveToFirst()) {
                nb = c.getInt(0);
            }
            c.close();
            db.close();
        } catch (SQLiteException e) {
            if (db != null) {
                db.close();
            }
            return nb;
        }
        return nb;
    }

    @Override
    protected void onPostExecute(Void unused) {
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d
                        .getApplication());
                d.setProgressBarIndeterminateVisibility(false);
                Toast.makeText(d.getApplicationContext(),
                        d.getString(R.string.preferences_resetdb_ok, (countEntries() - nb)),
                        Toast.LENGTH_LONG).show();
                try {
                    Editor edit = prefs.edit();
                    edit.putInt(Prefs.KEY_RESET_NICDB, d.getPackageManager().getPackageInfo(
                            ActivityMain.PKG, 0).versionCode);
                    edit.apply();
                } catch (NameNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (progress != null && progress.isShowing()) progress.dismiss();
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null)
                Toast.makeText(d.getApplicationContext(), R.string.preferences_error3,
                        Toast.LENGTH_SHORT).show();
        }
    }
}
