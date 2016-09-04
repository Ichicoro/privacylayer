package net.privacylayer.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        try {
            keyPair = DiffieHellman.prepareKeyPair(sharedPrefs);
            TextView pubKeyTextView = (TextView) findViewById(R.id.textPubkey);
            String pubKeyShort = DiffieHellman.savePublicKey(keyPair.getPublic()).substring(0, 32);
            pubKeyTextView.setText(pubKeyShort + "...");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
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

        Button importButton = (Button) findViewById(R.id.buttonImportPubKey);
        importButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText importedKeyNameText = (EditText) findViewById(R.id.textImportedKeyName);
                String importedKeyName = importedKeyNameText.getText().toString();
                if (importedKeyName.equals("")) {
                    Toast.makeText(KeyExchange.this, "You must supply a key name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText importedKeyText = (EditText) findViewById(R.id.textImportedKey);
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
    }

}
