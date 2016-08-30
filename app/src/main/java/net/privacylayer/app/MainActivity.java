package net.privacylayer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "PrivacyLayer/MainAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText editText = (EditText) findViewById(R.id.editText);
        final EditText inputBox = (EditText) findViewById(R.id.inputBox);

        final Button button = (Button) findViewById(R.id.actionButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText.setText(encryptBoxText(inputBox.getText().toString()));
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


    public String encryptBoxText(String text) {
        String encText = null;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String encKey = sharedPrefs.getString("encryption_key", "huehuehue");

        try {
            encText = AESPlatform.encrypt(text, encKey).toString();
            Log.i(TAG, "Encrypted to "+encText);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encText;
    }


}
