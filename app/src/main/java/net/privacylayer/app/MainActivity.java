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
import android.text.Spannable;
import android.text.SpannableString;
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

    private static final String TAG = "PrivacyLayer/MainAct";
    private Spinner spinner;
    private String key;
    private HashMap<String, String> keysMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean useDarkTheme = sharedPrefs.getBoolean("use_dark_theme", false);

        Log.i(TAG, "Using dark theme: " + useDarkTheme);
        if (useDarkTheme) {
            setTheme(R.style.DarkAppTheme);
        } else {
            super.setTheme(R.style.AppTheme);
        }

        setContentView(R.layout.activity_main);

        // Update the action bar title with the TypefaceSpan instance
        boolean useCustomFont = sharedPrefs.getBoolean("use_custom_font", false);
        if (useCustomFont) {     // todo: add a preference toggle!
            final SpannableString s = new SpannableString("PrivacyLayer");
            s.setSpan(new TypefaceSpan(this, "RobotoMono-Medium.ttf"), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(s);
        }



        // Declare all interface items here to prevent null pointer exs.
        Button encryptionButton = (Button) findViewById(R.id.encryptButton);
        Button decryptionButton = (Button) findViewById(R.id.decryptButton);
        spinner = (Spinner) findViewById(R.id.keychainSpinner);
        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText inputBox = (EditText) findViewById(R.id.inputBox);
        Button shareButton = (Button) findViewById(R.id.shareButton);





        /* keystore */

        final SharedPreferences keyValues = getApplicationContext()
                .getSharedPreferences("KeyStore", Context.MODE_PRIVATE);

        // TODO: fix unchecked thingie warning
        keysMap = new HashMap<>((Map<String, String>) keyValues.getAll());
        keysMap.put("Default key", "defkey");

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
                if (text != null)
                    inputBox.setText(text);
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

            PermanentNotification.notify(getApplicationContext(), selfPendingIntent);
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
            case R.id.action_help:
                /*Intent helpIntent = new Intent(MainActivity.this, KeyExchange.class);
                startActivity(helpIntent); */
                Toast.makeText(MainActivity.this, "Still working on this!", Toast.LENGTH_LONG).show();
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

    public void updateSpinner() {
        String[] keyNamesArray = keysMap.keySet().toArray(new String[keysMap.size()]);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, keyNamesArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public void setCurrentKey(String key) {
        final SharedPreferences appData = getApplicationContext()
                .getSharedPreferences("appData", Context.MODE_PRIVATE);
        appData
                .edit()
                .putString("encryption_key", key)
                .apply();
        // Log.i(TAG, "Current key is now " + key);
    }


    @Override
    public void onResume() {
        super.onResume();

        updateSpinner();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());



    }


}
