package net.privacylayer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyManagementActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_management);
        final ListView listView = (ListView) findViewById(R.id.listKeys);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Delete keys");

        final SharedPreferences keyValues = getApplicationContext()
                .getSharedPreferences("KeyStore", Context.MODE_PRIVATE);

        // TODO: fix unchecked thingie warning
        HashMap<String, String> keysMap = new HashMap<>((Map<String, String>) keyValues.getAll());
        final String[] keyNamesArray = keysMap.keySet().toArray(new String[keysMap.size()]);

        final ArrayList<String> keyNamesList = new ArrayList<>(Arrays.asList(keyNamesArray));

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                keyNamesList
        );
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                String keyName = adapter.getItem(position);
                getApplicationContext()
                        .getSharedPreferences("KeyStore", Context.MODE_PRIVATE)
                        .edit()
                        .remove(keyName)
                        .apply();
                keyNamesList.remove(keyName);
                adapter.notifyDataSetChanged();
                Toast.makeText(KeyManagementActivity.this, "Key removed.", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}
