package com.fast0n.wifiview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RestoreActivity extends AppCompatActivity {

    TextView mText;
    ListView mList;
    SwipeRefreshLayout mSwipeRefreshLayout;
    Button arrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // java addresses
        arrow = toolbar.findViewById(R.id.arrow);
        mText = findViewById(R.id.textView);
        mList = findViewById(R.id.list1);
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh1);

        show_list();
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            refreshItems();
        });

        arrow.setOnClickListener(view -> finish());
    }

    void refreshItems() {
        onItemsLoadComplete();
    }

    void onItemsLoadComplete() {
        show_list();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void show_list() {
        File folder = new File(
                Environment.getExternalStorageDirectory() + File.separator + getString(R.string.folder_name));

        if (!folder.exists()) {
            mList.setVisibility(View.INVISIBLE);
            mText.setVisibility(View.VISIBLE);
        } else {

            try {

                // Controlla quanti file ci sono dentro la cartella
                InputStream is = Runtime.getRuntime()
                        .exec(new String[]{"su", "-c", "ls sdcard/WiFiViewQR/ | grep .txt"}).getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader buff = new BufferedReader(isr);
                String line;
                String fLine = null;
                while ((line = buff.readLine()) != null)
                    fLine += line + "\n";

                String[] list = fLine.replace("null", "").split("\n");
                int count = list.length;

                String lines;
                String fLines = null;

                // takes the name of wifi networks
                String nameWifi = "";
                String passWifi = "";
                for (String s : list) {
                    InputStream n = Runtime.getRuntime()
                            .exec(new String[]{"su", "-c", "cat sdcard/WiFiViewQR/" + s}).getInputStream();
                    InputStreamReader ns = new InputStreamReader(n);
                    BufferedReader bufff = new BufferedReader(ns);

                    while ((lines = bufff.readLine()) != null)
                        fLines += lines + "\n";

                    nameWifi += s.replace(".txt", "") + ",";

                }

                // takes the password of wifi networks
                String[] z = nameWifi.split(",");
                String[] x = fLines.replace("null", "").split("\n");

                String totWifi = "";
                for (int d = 0; d < count; d++) {

                    totWifi += "SSID: " + z[d] + "\nPassword: " + x[d] + "\n\n";

                }

                String[] networks = totWifi.split("\n\n");

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(RestoreActivity.this,

                        android.R.layout.simple_list_item_1, networks);

                mList.setAdapter(adapter);

                // delete network
                mList.setOnItemClickListener((parent, view, position, id) -> {

                    final CharSequence[] items = {getString(R.string.restore), getString(R.string.delete)};

                    AlertDialog.Builder builder = new AlertDialog.Builder(RestoreActivity.this);
                    builder.setItems(items, (dialog, item) -> {

                        try {
                            show_menu(position, item);
                        } catch (IOException e) {
                            RestoreActivity.this.startActivity(new Intent(RestoreActivity.this, RootActivity.class));
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();

                });

            } catch (NullPointerException e) {
                RestoreActivity.this.startActivity(new Intent(RestoreActivity.this, MainActivity.class));

                try {
                    Runtime.getRuntime().exec(new String[]{"su", "-c", "rm -rf sdcard/WiFiViewQR"}).getInputStream();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                RestoreActivity.this.startActivity(new Intent(RestoreActivity.this, RestoreActivity.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void show_menu(int position, int item) throws IOException {
        final ListView lista = findViewById(R.id.list1);

        switch (item) {
            case 0:
                Toast.makeText(RestoreActivity.this, getString(R.string.csoon), Toast.LENGTH_LONG).show();
                break;
            case 1:
                String items = (String) lista.getItemAtPosition(position);
                String[] item1 = items.split("\n");
                String item2 = item1[0].replace("SSID: ", "");

                File file = new File(Environment.getExternalStorageDirectory() + File.separator
                        + getString(R.string.folder_name) + File.separator + item2 + ".txt");
                boolean deleted = file.delete();

                mSwipeRefreshLayout.setRefreshing(true);
                refreshItems();

                Toast.makeText(RestoreActivity.this, getString(R.string.deleted), Toast.LENGTH_LONG).show();
                break;
        }
    }

}
