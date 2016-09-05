package net.privacylayer.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class Encrypt extends AppCompatActivity {

    public static final String TAG = "PrivacyLayer/Encrypt";

    public String encKey = "huehuehue";                                                             // Todo: implement custom keying

    public CharSequence encText;
    public boolean showToastOnEnc = true;
    public boolean saveOldToClip = false;
    public boolean readonly = false;
    public boolean oldMode;


    @Override
    protected void onResume() {

        super.onResume();
        ClipData clip;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);        // clipboard is the new clipboard :3

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        final SharedPreferences appData = getApplicationContext()
                .getSharedPreferences("appData", Context.MODE_PRIVATE);

        showToastOnEnc = sharedPrefs.getBoolean("show_toast_on_enc", true);
        saveOldToClip = sharedPrefs.getBoolean("save_old_to_clip", true);
        encKey = appData.getString("encryption_key", "");



        Context context = getApplicationContext();
        CharSequence toastText = "Text Encrypted!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, toastText, duration);



        Intent intent = getIntent();
        String action = intent.getAction();
        String text;

        /* if (Intent.EXTRA_PROCESS_TEXT.equals(action) && intent.getType() != null) */             // todo remove this completely

        if (Intent.ACTION_SEND.equals(action) && intent.getType() != null) {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "Intent was EXTRA_TEXT");
            oldMode = true;
        } else {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_PROCESS_TEXT);                                     // grabbin' the text!
            readonly = getIntent()
                    .getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);
            oldMode = false;
            Log.i(TAG, "Intent was EXTRA_PROCESS_TEXT / READONLY:" + readonly);
            Log.i(TAG, "Captured: " + text);
        }

        setIntent(new Intent());


        try {
            encText = AESPlatform.encrypt(text, encKey).toString();
            Log.i(TAG, "Encrypted to "+encText);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (!oldMode) {
            if (readonly) {
                clip = ClipData.newPlainText("label", encText);                                         // the clipboard now has the encrypted text inside of it
                clipboard.setPrimaryClip(clip);
            } else {
                if (saveOldToClip) {
                    clip = ClipData.newPlainText("label", text);                                        // the clipboard now has the encrypted text inside of it
                    clipboard.setPrimaryClip(clip);
                }
                Intent outputIntent = new Intent();
                outputIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, encText);                                    // Let's send back the encrypted text
                setResult(RESULT_OK, outputIntent);
                Log.i(TAG, "Replaced " + text + " with "+ encText);
            }
        } else {
            clip = ClipData.newPlainText("label", encText);                                         // the clipboard now has the encrypted text inside of it
            clipboard.setPrimaryClip(clip);
        }

        if (showToastOnEnc) { toast.show(); }

        finish();
    }
}
