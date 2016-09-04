package net.privacylayer.app;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "PrivacyLayer/MainAct";
    public int mode = 0;    // 0 = encrypt   -   1 = decrypt
    public Button encryptionButton;
    public Button decryptionButton;
    public Spinner spinner;
    public SharedPreferences sharedPrefs;
    private String key;
    private HashMap<String, String> keysMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Declare all interface items here to prevent null pointer exs.
        encryptionButton = (Button) findViewById(R.id.encryptButton);
        decryptionButton = (Button) findViewById(R.id.decryptButton);
        spinner = (Spinner) findViewById(R.id.keychainSpinner);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText inputBox = (EditText) findViewById(R.id.inputBox);
        Button shareButton = (Button) findViewById(R.id.shareButton);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        /* keystore */

        final SharedPreferences keyValues = getApplicationContext()
                .getSharedPreferences("KeyStore", Context.MODE_PRIVATE);


        keysMap = new HashMap<>(1 + keyValues.getAll().size());
        keysMap.put("Default key", "defkey");
        keysMap.putAll((Map<String, String>) keyValues.getAll());

        updateSpinner();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String thisKeyName = parent.getItemAtPosition(position).toString();
                key = keysMap.get(thisKeyName);
                setCurrentKey(key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Intent intent = getIntent();
        Boolean fromNotification = intent.getBooleanExtra("fromNotification", false);
        Log.i(TAG, "Did the user come from a notification? " + fromNotification.toString());
        if (fromNotification) {
            /* If the user came here clicking a notification, attempt to load the clipboard contents
             * in the input box.
             */
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null) {
                ClipData.Item item = clipData.getItemAt(0);
                CharSequence text = item.getText();
                if (text != null) {
                    inputBox.setText(text);
                    String type = intent.getStringExtra("type");
                    // TODO: use constants from MainActivity
                    Log.i(TAG, "Notification type is: " + type);
                    if (type.equals("encryption"))
                        setEncryptionMode();
                    else if (type.equals("decryption"))
                        setDecryptionMode();
                    /*
                    else if (the text is a valid AES message)
                        setDecryptionMode();
                    else
                        setEncryptionMode();
                    */
                }
            }
        }

        boolean showPermanentNotification = sharedPrefs.getBoolean("enable_persistent_notification", false);

        if (showPermanentNotification) {
            Intent selfIntent = new Intent(this, MainActivity.class);
            selfIntent.putExtra("fromNotification", true);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(selfIntent);
            PendingIntent selfPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            Intent encryptionIntent = new Intent(this, MainActivity.class);
            encryptionIntent.putExtra("fromNotification", true);
            encryptionIntent.putExtra("type", "encryption");
            Intent decryptionIntent = new Intent(this, MainActivity.class);
            decryptionIntent.putExtra("fromNotification", true);
            decryptionIntent.putExtra("type", "decryption");

            PermanentNotification.notify(getApplicationContext(), selfPendingIntent, encryptionIntent, decryptionIntent);
        }

        // Encrypt/decrypt button
        encryptionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    editText.setText(encryptBoxText(inputBox.getText().toString()));
                } catch (IllegalArgumentException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        decryptionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    editText.setText(decryptBoxText(inputBox.getText().toString()));
                } catch (IllegalArgumentException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // Share button
        shareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, editText.getText());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share with..."));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_keys:
                Intent keysIntent = new Intent(MainActivity.this, KeyExchange.class);
                startActivity(keysIntent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


    public String encryptBoxText(String text) throws Exception {
        return AESPlatform.encrypt(text, key).toString();
    }

    public String decryptBoxText(String text) throws Exception {
        return AESPlatform.decrypt(new AESMessage(text), key);
    }

    public void setEncryptionMode() {
        mode = 0;
    }

    public void setDecryptionMode() {
        mode = 1;
    }

    public void updateSpinner() {
        String[] keyNamesArray = keysMap.keySet().toArray(new String[keysMap.size()]);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, keyNamesArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public void setCurrentKey(String key) {
        sharedPrefs
                .edit()
                .putString("encryption_key", key)
                .apply();
        // Log.i(TAG, "Current key is now " + key);
    }
}
