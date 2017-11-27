package com.fast0n.wifiview;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    AdView mAdView;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // addresses

        mAdView = (AdView) findViewById(R.id.adView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        MobileAds.initialize(this, "ca-app-pub-9646303341923759~9003031985");
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        requestPermissions(new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA }, 1);
        show_list();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items

                mSwipeRefreshLayout.setRefreshing(true);
                refreshItems();
            }
        });

    }

    void refreshItems() {
        // Load items
        // ...

        // Load complete
        onItemsLoadComplete();

    }

    void onItemsLoadComplete() {
        // Update the adapter and notify data set changed
        // ...

        show_list();
        // Stop refresh animation
        mSwipeRefreshLayout.setRefreshing(false);

    }

    public void show_list() {

        final ListView lista = (ListView) findViewById(R.id.list);
        // show list
        try {

            InputStream is = Runtime.getRuntime()
                    .exec(new String[] { "su", "-c", "cat /data/misc/wifi/WifiConfigStore.xml" }).getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader buff = new BufferedReader(isr);
            String line;
            String fLine = null;
            while ((line = buff.readLine()) != null)
                fLine += line;
            String[] list = fLine.split(">");
            int count = list.length;

            // takes the number of wifi networks
            int nWifi = 0;
            for (int a = 0; a < count; a++) {
                if (list[a].equals("<WifiConfiguration")) {
                    nWifi += 1;
                }
            }

            // takes the name of wifi networks
            String nameWifi = "";
            for (int b = 0; b < count; b++) {
                if (list[b].equals("<string name=\"SSID\"")) {
                    nameWifi += list[b + 1].replace("&quot;", "").replace("</string", "") + ",";
                }
            }

            // take the password of wifi networks
            String passWifi = "";
            for (int c = 0; c < count; c++) {
                if (list[c].equals("<string name=\"PreSharedKey\"")) {
                    passWifi += list[c + 1].replace("&quot;", "").replace("</string", "") + ",";
                }

                if (list[c].equals("<null name=\"PreSharedKey\" /")) {
                    passWifi += getString(R.string.p_nf) + ",";
                }

            }

            // if the number of password is null
            if (nWifi == 0) {
                Toast.makeText(MainActivity.this, getString(R.string.n_nf), Toast.LENGTH_LONG).show();
            }

            // if the number of password is 1
            if (nWifi == 1) {

                String[] networks = {
                        "SSID: " + nameWifi.replace(",", "") + "\nPassword: " + passWifi.replace(",", "") };
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_list_item_1, networks);
                lista.setAdapter(adapter);

                // show menu
                lista.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                        String item = (String) lista.getItemAtPosition(position);
                        String[] item1 = item.split("\n");
                        String item2 = item1[1].replace("Password: ", "");

                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(item2);
                        Toast.makeText(MainActivity.this, getString(R.string.cp_pwd), Toast.LENGTH_LONG).show();
                        return false;

                    }
                });

                // copy password
                lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                        final CharSequence[] items = { getString(R.string.cp_ssid), getString(R.string.cp_pwd),
                                getString(R.string.ct_qr), getString(R.string.share) };
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {

                                    String items = (String) lista.getItemAtPosition(position);
                                    String[] item1 = items.split("\n");
                                    String item2 = item1[0].replace("SSID: ", "");

                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    clipboard.setText(item2);
                                    Toast.makeText(MainActivity.this, getString(R.string.t_cp_ssid), Toast.LENGTH_LONG)
                                            .show();

                                } else if (item == 1) {

                                    String p = (String) lista.getItemAtPosition(position);
                                    String[] item1 = p.split("\n");
                                    String item2 = item1[1].replace("Password: ", "");

                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    clipboard.setText(item2);
                                    Toast.makeText(MainActivity.this, getString(R.string.t_cp_pwd), Toast.LENGTH_LONG)
                                            .show();

                                } else if (item == 2) {

                                    String items = (String) lista.getItemAtPosition(position);
                                    String[] item1 = items.split("\n");
                                    String item2 = item1[0].replace("SSID: ", "");

                                    String p = (String) lista.getItemAtPosition(position);
                                    String[] item3 = p.split("\n");
                                    String item4 = item3[1].replace("Password: ", "");

                                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                                    try {
                                        BitMatrix bitMatrix = multiFormatWriter.encode(
                                                "WIFI:T:WPA;S:" + item2 + ";P:" + item4 + ";;", BarcodeFormat.QR_CODE,
                                                200, 200);
                                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

                                        File file = new File(MainActivity.this.getCacheDir(), "qr.png");
                                        FileOutputStream fOut = new FileOutputStream(file);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                                        fOut.flush();
                                        fOut.close();

                                        Runtime.getRuntime()
                                                .exec(new String[] { "su", "-c", "mkdir sdcard/WifiViewQR" })
                                                .getInputStream();

                                    } catch (WriterException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        Runtime.getRuntime().exec(new String[] { "su", "-c",
                                                "mv /data/data/com.fast0n.wifiview/cache/qr.png sdcard/WifiViewQR/"
                                                        + item2 + ".png" })
                                                .getInputStream();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Toast.makeText(MainActivity.this, getString(R.string.csoon), Toast.LENGTH_LONG)
                                            .show();

                                } else if (item == 3) {
                                    String q = (String) lista.getItemAtPosition(position);
                                    String msg = String.format(getString(R.string.share_msg), q);
                                    String shareBody = msg;
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                                    sendIntent.setType("text/plain");
                                    startActivity(sendIntent);
                                }
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                });

            }
            // if the number of password is greater than 1
            if (nWifi >= 2) {
                String[] z = nameWifi.split(",");
                String[] x = passWifi.split(",");

                String totWifi = "";
                for (int d = 0; d < nWifi; d++) {

                    totWifi += "SSID: " + z[d] + "\nPassword: " + x[d] + "\n\n";

                }

                String[] networks = totWifi.split("\n\n");

                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(MainActivity.this,
                        android.R.layout.simple_list_item_1, networks);
                lista.setAdapter(adapter);

                lista.setAdapter(adapter);

                // show menu
                lista.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                        String item = (String) lista.getItemAtPosition(position);
                        String[] item1 = item.split("\n");
                        String item2 = item1[1].replace("Password: ", "");

                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(item2);
                        Toast.makeText(MainActivity.this, getString(R.string.cp_pwd), Toast.LENGTH_LONG).show();
                        return false;

                    }
                });

                // copy password
                lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                        final CharSequence[] items = { getString(R.string.cp_ssid), getString(R.string.cp_pwd),
                                getString(R.string.ct_qr), getString(R.string.share) };
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {

                                    String items = (String) lista.getItemAtPosition(position);
                                    String[] item1 = items.split("\n");
                                    String item2 = item1[0].replace("SSID: ", "");

                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    clipboard.setText(item2);
                                    Toast.makeText(MainActivity.this, getString(R.string.t_cp_ssid), Toast.LENGTH_LONG)
                                            .show();

                                } else if (item == 1) {

                                    String p = (String) lista.getItemAtPosition(position);
                                    String[] item1 = p.split("\n");
                                    String item2 = item1[1].replace("Password: ", "");

                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    clipboard.setText(item2);
                                    Toast.makeText(MainActivity.this, getString(R.string.t_cp_pwd), Toast.LENGTH_LONG)
                                            .show();

                                } else if (item == 2) {

                                    String items = (String) lista.getItemAtPosition(position);
                                    String[] item1 = items.split("\n");
                                    String item2 = item1[0].replace("SSID: ", "");

                                    String p = (String) lista.getItemAtPosition(position);
                                    String[] item3 = p.split("\n");
                                    String item4 = item3[1].replace("Password: ", "");

                                    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                                    try {
                                        BitMatrix bitMatrix = multiFormatWriter.encode(
                                                "WIFI:T:WPA;S:" + item2 + ";P:" + item4 + ";;", BarcodeFormat.QR_CODE,
                                                200, 200);
                                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

                                        File file = new File(MainActivity.this.getCacheDir(), "qr.png");
                                        FileOutputStream fOut = new FileOutputStream(file);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                                        fOut.flush();
                                        fOut.close();

                                        Runtime.getRuntime()
                                                .exec(new String[] { "su", "-c", "mkdir sdcard/WifiViewQR" })
                                                .getInputStream();

                                    } catch (WriterException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Toast.makeText(MainActivity.this, getString(R.string.csoon), Toast.LENGTH_LONG)
                                            .show();

                                    try {
                                        Runtime.getRuntime().exec(new String[] { "su", "-c",
                                                "mv /data/data/com.fast0n.wifiview/cache/qr.png sdcard/WifiViewQR/"
                                                        + item2 + ".png" })
                                                .getInputStream();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else if (item == 3) {
                                    String q = (String) lista.getItemAtPosition(position);
                                    String msg = String.format(getString(R.string.share_msg), q);
                                    String shareBody = msg;
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                                    sendIntent.setType("text/plain");
                                    startActivity(sendIntent);
                                }
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                });

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_about) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            // ...Irrelevant code for customizing the buttons and title

            LayoutInflater inflater = this.getLayoutInflater();

            View mView = inflater.inflate(R.layout.about, null);
            dialogBuilder.setView(mView);

            TextView version = (TextView) mView.findViewById(R.id.textView);
            TextView name = (TextView) mView.findViewById(R.id.textView2);
            TextView mail = (TextView) mView.findViewById(R.id.textView3);

            String appVersion = BuildConfig.VERSION_NAME;
            String msg_vrs = String.format(getString(R.string.version), appVersion);
            version.setText(msg_vrs);

            String msg_name = String.format(getString(R.string.name), "Massimiliano Montaleone (Fast0n)");
            name.setText(msg_name);

            mail.setText("theplayergame97@gmail.com");

            dialogBuilder.create().show();

        }

        return super.onOptionsItemSelected(item);
    }

}