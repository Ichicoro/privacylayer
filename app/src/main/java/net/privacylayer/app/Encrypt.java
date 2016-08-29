package net.privacylayer.app;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ClipboardManager;
import android.widget.Toast;

public class Encrypt extends AppCompatActivity {

    public String ENCKEY = "huehuehue";                                                              // Todo: implement custom keying

    public CharSequence encText;
    public boolean showToastOnEnc = true;
    public boolean saveOldToClip = false;
    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);            // clipboard is the new clipboard :3

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence text = getIntent()
                .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);                                   // grabbin' the text!

        boolean readonly = getIntent()
                .getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);                        // is our text from a readonly source?



        if (readonly) {
            ClipData clip = ClipData.newPlainText("label", encText);                                // the clipboard now has the encrypted text inside of it
            clipboard.setPrimaryClip(clip);                                                         // the encrypted text is now the primary clip in the clipboard
            if (showToastOnEnc) {                                                                   // We're only showing the toast if the user has the option enabled (as per default!)
                CharSequence toastContent = "Encrypted message copied to clipboard!";
                Toast toast = Toast.makeText(getApplicationContext(), toastContent, Toast.LENGTH_SHORT);
                toast.show();
            }


        } else {
            if (saveOldToClip) {                                                                    // We're firing only if the
                ClipData clip = ClipData.newPlainText("label", text);                               // the clipboard now has the encrypted text inside of it
                clipboard.setPrimaryClip(clip);                                                     // the unencrypted text is now the primary clip in the clipboard
            }
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, encText);                                    // Let's send back the encrypted text
            setResult(RESULT_OK, intent);
        }

    }
}
