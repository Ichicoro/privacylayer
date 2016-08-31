package net.privacylayer.app;

import android.app.Notification;
import android.app.NotificationManager;
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

    public static final String TAG = "PrivacyLayer/MainAct";
    public static final int STATUS_NOTIFICATION_ID = 0;

    public boolean showPermanentNotification;
    public int mode = 0;    // 0 = encrypt   -   1 = decrypt
    public Button actionButton;
    public Button shareButton;
    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SharedPreferences keyValues = getApplicationContext()
                .getSharedPreferences("KeyStore", Context.MODE_PRIVATE);

        final HashMap<String, String> defaultKeys = new HashMap<>(1 + keyValues.getAll().size());
        defaultKeys.put("Default key", "defkey");
        defaultKeys.putAll((Map<String, String>) keyValues.getAll());
        String[] keyNamesArray = defaultKeys.keySet().toArray(new String[defaultKeys.size()]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, keyNamesArray);
        Spinner spinner = (Spinner) findViewById(R.id.keychainSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String thisKeyName = parent.getItemAtPosition(position).toString();
                key = defaultKeys.get(thisKeyName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText inputBox = (EditText) findViewById(R.id.inputBox);

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
                    /*
                    if (the text is a valid AES message)
                        setDecryptionMode();
                    else
                        setEncryptionMode();
                    */
                }
            }
        }

        final Switch workMode = (Switch) findViewById(R.id.modeswitch);

        workMode.setOnCheckedChangeListener(this);

        showPermanentNotification = sharedPrefs.getBoolean("enable_persistent_notification", false);

        // Create a permanent notification.
        if (showPermanentNotification) { // done :D
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder notification = new Notification.Builder(this)
                    .setContentTitle("PrivacyLayer")
                    .setContentText("Click here to go to PrivacyLayer.")
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setOngoing(true);
            // Copy-pasted from the Android docs:
            // https://developer.android.com/guide/topics/ui/notifiers/notifications.html
            Intent selfIntent = new Intent(this, MainActivity.class);
            selfIntent.putExtra("fromNotification", true);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(selfIntent);
            PendingIntent selfPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setContentIntent(selfPendingIntent);
            notificationManager.notify(STATUS_NOTIFICATION_ID, notification.build());
        }

        // Encrypt/decrypt button
        actionButton = (Button) findViewById(R.id.actionButton);
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

        // Encrypt/decrypt button
        shareButton = (Button) findViewById(R.id.shareButton);
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
    }

    public void setDecryptionMode() {
        mode = 1;
        actionButton.setText("Decrypt");
    }
}
