package net.privacylayer.app;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Encrypt extends AppCompatActivity {

    public static final String TAG = "PrivacyLayer/Encrypt";

    public String ENCKEY = "huehuehue";                                                             // Todo: implement custom keying



    public String text;

    public CharSequence encText = "GG";
    public boolean showToastOnEnc = true;
    public boolean saveOldToClip = false;
    public boolean readonly = false;
    public boolean newMode;


    @Override
    protected void onResume() {
        super.onResume();
        ClipData clip;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);        // clipboard is the new clipboard :3



        Context context = getApplicationContext();
        CharSequence toastText = "Hello toast!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, toastText, duration);



        Intent intent = getIntent();
        String action = intent.getAction();
        String text = "<error>";

        /* if (Intent.EXTRA_PROCESS_TEXT.equals(action) && intent.getType() != null) */             // todo remove this completely

        if (Intent.ACTION_SEND.equals(action) && intent.getType() != null) {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_TEXT);
            newMode = true;
        } else {
            text = getIntent()
                    .getStringExtra(Intent.EXTRA_PROCESS_TEXT);                                     // grabbin' the text!
            readonly = getIntent()
                    .getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);
            newMode = false;
            Log.i(TAG, "Captured: " + text);
        }

        setIntent(new Intent());


        try {
            encText = AESPlatform.encrypt(text, ENCKEY).toString();
            Log.i(TAG, "Encrypted to "+encText);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (newMode) {
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
