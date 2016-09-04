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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    public static final int STATUS_NOTIFICATION_ID = 0;
    public static final String TAG = "PrivacyLayer/MainAct";
    public int mode = 0;    // 0 = encrypt   -   1 = decrypt
    public Button actionButton;
    public Spinner spinner;
    public SharedPreferences sharedPrefs;
    public Switch workMode;
    private String key;
    private HashMap<String, String> keysMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Declare all interface items here to prevent null pointer exs.
        actionButton = (Button) findViewById(R.id.actionButton);
        spinner = (Spinner) findViewById(R.id.keychainSpinner);
        final EditText newItemNameBox = (EditText) findViewById(R.id.newItemNameBox);
        final EditText newItemKeyBox = (EditText) findViewById(R.id.newItemKeyBox);
        Button addToKeystoreButton = (Button) findViewById(R.id.addToKeystoreButton);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText inputBox = (EditText) findViewById(R.id.inputBox);
        workMode = (Switch) findViewById(R.id.modeswitch);
        Button shareButton = (Button) findViewById(R.id.shareButton);
        Button buttonManageKeys = (Button) findViewById(R.id.buttonManageKeys);

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

        addToKeystoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = newItemNameBox.getText().toString();
                String key = newItemKeyBox.getText().toString();
                addKey(name, key);
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

        workMode.setOnCheckedChangeListener(this);

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
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (mode == 0) {
                        editText.setText(encryptBoxText(inputBox.getText().toString()));
                    } else {
                        editText.setText(decryptBoxText(inputBox.getText().toString()));
                    }
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

        buttonManageKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), KeyExchange.class);
                startActivity(intent);
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
                openSettings();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


    public void openSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }


    public String encryptBoxText(String text) throws Exception {
        return AESPlatform.encrypt(text, key).toString();
    }

    public String decryptBoxText(String text) throws Exception {
        return AESPlatform.decrypt(new AESMessage(text), key);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked)
            setDecryptionMode();
        else
            setEncryptionMode();
    }

    public void setEncryptionMode() {
        mode = 0;
        actionButton.setText("Encrypt");
        workMode.setChecked(false);
    }

    public void setDecryptionMode() {
        mode = 1;
        actionButton.setText("Decrypt");
        workMode.setChecked(true);
    }

    public void updateSpinner() {
        String[] keyNamesArray = keysMap.keySet().toArray(new String[keysMap.size()]);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, keyNamesArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Add a key to the shared preferences
    public void addKey(String name, String key) {
        getApplicationContext()
                .getSharedPreferences("KeyStore", Context.MODE_PRIVATE)
                .edit()
                .putString(name, key)
                .apply();
        keysMap.put(name, key);
        updateSpinner();
    }

    public void setCurrentKey(String key) {
        sharedPrefs
                .edit()
                .putString("encryption_key", key)
                .apply();
        // Log.i(TAG, "Current key is now " + key);
    }
}
