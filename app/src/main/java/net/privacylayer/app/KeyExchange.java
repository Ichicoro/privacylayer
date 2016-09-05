package net.privacylayer.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;

public class KeyExchange extends AppCompatActivity {

    private KeyPair keyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_exchange);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        try {
            keyPair = DiffieHellman.prepareKeyPair(sharedPrefs);
            TextView pubKeyTextView = (TextView) findViewById(R.id.textPubkey);
            String pubKeyShort = DiffieHellman.savePublicKey(keyPair.getPublic());
            pubKeyTextView.setText(pubKeyShort);
            pubKeyTextView.setMovementMethod(new ScrollingMovementMethod());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // "Manage keys" button
        Button manageKeysButton = (Button) findViewById(R.id.buttonManageKeys);
        manageKeysButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(
                        getApplicationContext(),
                        KeyManagementActivity.class
                ));
            }
        });

        // Share button
        Button shareButton = (Button) findViewById(R.id.buttonSharePubKey);
        shareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    KeyPair keyPair = DiffieHellman.prepareKeyPair(sharedPrefs);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, DiffieHellman.savePublicKey(keyPair.getPublic()));
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share with..."));
                } catch (Exception e) {
                    Toast.makeText(KeyExchange.this, "An error happened.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button importPubKeyButton = (Button) findViewById(R.id.buttonImportPubKey);
        importPubKeyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText importedKeyNameText = (EditText) findViewById(R.id.textImportedKeyName);
                String importedKeyName = importedKeyNameText.getText().toString();
                if (importedKeyName.equals("")) {
                    Toast.makeText(KeyExchange.this, "You must supply a key name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText importedKeyText = (EditText) findViewById(R.id.textAddedPassword);
                String importedKeyString = importedKeyText.getText().toString();
                if (importedKeyString.equals("")) {
                    Toast.makeText(KeyExchange.this, "You must supply a key.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    byte[] derivedKey = DiffieHellman.generateSharedSecret(
                            keyPair.getPrivate(),
                            DiffieHellman.loadPublicKey(importedKeyString)
                    ).getEncoded();
                    getApplicationContext()
                            .getSharedPreferences("KeyStore", Context.MODE_PRIVATE)
                            .edit()
                            .putString(importedKeyName, new String(derivedKey, "UTF-8"))
                            .apply();
                    Toast.makeText(KeyExchange.this, "Key added successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(KeyExchange.this, "This shouldn't happen!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button addPasswordButton = (Button) findViewById(R.id.buttonAddPassword);
        addPasswordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText addedPasswordNameText = (EditText) findViewById(R.id.textAddedPasswordName);
                String addedPasswordName = addedPasswordNameText.getText().toString();
                if (addedPasswordName.equals("")) {
                    Toast.makeText(KeyExchange.this, "You must supply a key name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText addedPasswordText = (EditText) findViewById(R.id.textAddedPassword);
                String addedPasswordString = addedPasswordText.getText().toString();
                if (addedPasswordString.equals("")) {
                    Toast.makeText(KeyExchange.this, "You must supply a password.", Toast.LENGTH_SHORT).show();
                    return;
                }

                getApplicationContext()
                        .getSharedPreferences("KeyStore", Context.MODE_PRIVATE)
                        .edit()
                        .putString(addedPasswordName, addedPasswordString)
                        .apply();
            }
        });
    }

}
