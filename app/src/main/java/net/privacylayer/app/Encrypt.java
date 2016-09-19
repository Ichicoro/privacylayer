package net.privacylayer.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class Encrypt extends AppCompatActivity {

    private static final String TAG = "PrivacyLayer/Encrypt";

    @Override
    protected void onResume() {

        super.onResume();
        ClipData clip;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);        // clipboard is the new clipboard :3

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        final SharedPreferences appData = getApplicationContext()
                .getSharedPreferences("appData", Context.MODE_PRIVATE);

        boolean showToastOnEnc = sharedPrefs.getBoolean("show_toast_on_enc", true);
        boolean saveOldToClip = sharedPrefs.getBoolean("save_old_to_clip", true);
        String encKey = appData.getString("encryption_key", "");

        Intent intent = getIntent();
        String action = intent.getAction();
        String text;

        /* if (Intent.EXTRA_PROCESS_TEXT.equals(action) && intent.getType() != null) */             // todo remove this completely

        boolean readonly = false;
        boolean oldMode;
        if (Intent.ACTION_SEND.equals(action) && intent.getType() != null) {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "Intent was EXTRA_TEXT");
            oldMode = true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_PROCESS_TEXT);                                     // grabbin' the text!
            readonly = getIntent()
                    .getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);
            oldMode = false;
            Log.i(TAG, "Intent was EXTRA_PROCESS_TEXT / READONLY:" + readonly);
            Log.i(TAG, "Captured: " + text);
        } else {
            throw new RuntimeException("Couldn't fetch text!");
        }

        setIntent(new Intent());


        CharSequence encText;
        try {
            encText = AESPlatform.encrypt(text, encKey).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Log.i(TAG, "Encrypted to " + encText);


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
                // TODO: figure out what to do if API level < 23
                outputIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, encText);                                    // Let's send back the encrypted text
                setResult(RESULT_OK, outputIntent);
                Log.i(TAG, "Replaced " + text + " with "+ encText);
            }
        } else {
            clip = ClipData.newPlainText("label", encText);                                         // the clipboard now has the encrypted text inside of it
            clipboard.setPrimaryClip(clip);
        }

        if (showToastOnEnc)
            Toast.makeText(Encrypt.this, "Text encrypted!", Toast.LENGTH_SHORT).show();

        finish();
    }
}
