package net.privacylayer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;

public class KeyExchange extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_exchange);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        try {
            KeyPair keyPair = DiffieHellman.prepareKeyPair(sharedPrefs);
            TextView pubKeyTextView = (TextView) findViewById(R.id.textPubkey);
            String pubKeyShort = DiffieHellman.savePublicKey(keyPair.getPublic()).substring(0, 32);
            pubKeyTextView.setText(pubKeyShort + "...");
        } catch (Exception e) {
            e.printStackTrace();
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
