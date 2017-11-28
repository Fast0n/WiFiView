package com.fast0n.wifiview;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RestoreActivity extends AppCompatActivity {

    AdView mAdView;
    TextView mText;
    ListView mList;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // address
        mAdView = (AdView) findViewById(R.id.adView1);
        mText = (TextView) findViewById(R.id.textView);
        mList = (ListView) findViewById(R.id.list1);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh1);

        // ads
        MobileAds.initialize(this, "ca-app-pub-9646303341923759~9003031985");
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        show_list();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                refreshItems();
            }
        });

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
                        .exec(new String[] { "su", "-c", "ls sdcard/WiFiViewQR/ | grep .txt" }).getInputStream();
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
                for (int b = 0; b < count; b++) {
                    InputStream n = Runtime.getRuntime()
                            .exec(new String[] { "su", "-c", "cat sdcard/WiFiViewQR/" + list[b] }).getInputStream();
                    InputStreamReader ns = new InputStreamReader(n);
                    BufferedReader bufff = new BufferedReader(ns);

                    while ((lines = bufff.readLine()) != null)
                        fLines += lines + "\n";

                    nameWifi += list[b].replace(".txt", "") + ",";

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
                mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                        final CharSequence[] items = { getString(R.string.restore), getString(R.string.delete) };

                        AlertDialog.Builder builder = new AlertDialog.Builder(RestoreActivity.this);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                try {
                                    show_menu(position, item);
                                } catch (IOException e) {
                                    Intent myIntent = new Intent(RestoreActivity.this, RootActivity.class);
                                    RestoreActivity.this.startActivity(myIntent);
                                }
                            }

                        });
                        AlertDialog alert = builder.create();
                        alert.show();

                    }

                });

            } catch (NullPointerException e) {
                Intent myIntent = new Intent(RestoreActivity.this, MainActivity.class);
                RestoreActivity.this.startActivity(myIntent);

                try {
                    Runtime.getRuntime().exec(new String[] { "su", "-c", "rm -rf sdcard/WiFiViewQR" }).getInputStream();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Intent i = new Intent(RestoreActivity.this, RestoreActivity.class);
                RestoreActivity.this.startActivity(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void show_menu(int position, int item) throws IOException {
        final ListView lista = (ListView) findViewById(R.id.list1);

        if (item == 0) {

            Toast.makeText(RestoreActivity.this, getString(R.string.csoon), Toast.LENGTH_LONG).show();
        }

        if (item == 1) {

            String items = (String) lista.getItemAtPosition(position);
            String[] item1 = items.split("\n");
            String item2 = item1[0].replace("SSID: ", "");

            File file = new File(Environment.getExternalStorageDirectory() + File.separator
                    + getString(R.string.folder_name) + File.separator + item2 + ".txt");
            boolean deleted = file.delete();

            mSwipeRefreshLayout.setRefreshing(true);
            refreshItems();

            Toast.makeText(RestoreActivity.this, getString(R.string.deleted), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();

            Intent mainActivity = new Intent(RestoreActivity.this, MainActivity.class);
            startActivity(mainActivity);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();

        Intent mainActivity = new Intent(RestoreActivity.this, MainActivity.class);
        startActivity(mainActivity);
    }
}
