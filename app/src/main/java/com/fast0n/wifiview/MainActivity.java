package com.fast0n.wifiview;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.BuildConfig;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout mSwipeRefreshLayout;
    ListView mList;
    Button more, settings;
    PopupMenu popup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // java addresses
        more = toolbar.findViewById(R.id.more);
        settings = toolbar.findViewById(R.id.settings);
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        popup = new PopupMenu(MainActivity.this, settings);
        popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);

        show_list();
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            refreshItems();
        });

        // items toolbar
        settings.setOnClickListener(view -> Toast.makeText(MainActivity.this, getString(R.string.csoon), Toast.LENGTH_LONG).show());
        more.setOnClickListener(view -> showPopup());


    }

    private void showPopup() {

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_restore:
                    startActivity(new Intent(MainActivity.this, RestoreActivity.class));
                    break;
                case R.id.action_about:
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

                    LayoutInflater inflater = this.getLayoutInflater();

                    View mView = inflater.inflate(R.layout.activity_about, null);
                    dialogBuilder.setView(mView);

                    TextView version = mView.findViewById(R.id.textView);
                    TextView name = mView.findViewById(R.id.textView2);
                    TextView mail = mView.findViewById(R.id.textView3);

                    String appVersion = BuildConfig.VERSION_NAME;
                    String msg_vrs = String.format(getString(R.string.version), appVersion);
                    version.setText(msg_vrs);

                    String msg_name = String.format(getString(R.string.name), "Massimiliano Montaleone (Fast0n)");
                    name.setText(msg_name);

                    mail.setText("theplayergame97@gmail.com");

                    dialogBuilder.create().show();
                    break;
            }
            return true;
        });
        popup.show();

    }

    void refreshItems() {
        onItemsLoadComplete();
    }

    void onItemsLoadComplete() {
        show_list();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void show_list() {

        mList = findViewById(R.id.list);
        // show list
        try {

            InputStream is = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "cat /data/misc/wifi/WifiConfigStore.xml"}).getInputStream();
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
            for (String s : list) {
                if (s.equals("<WifiConfiguration"))
                    nWifi += 1;

            }

            // takes the name of wifi networks
            String nameWifi = "";
            for (int b = 0; b < count; b++) {
                if (list[b].equals("<string name=\"SSID\""))
                    nameWifi += list[b + 1].replace("&quot;", "").replace("</string", "") + ",";

            }

            // take the password of wifi networks
            String passWifi = "";
            for (int c = 0; c < count; c++) {
                if (list[c].equals("<string name=\"PreSharedKey\""))
                    passWifi += list[c + 1].replace("&quot;", "").replace("</string", "") + ",";


                if (list[c].equals("<null name=\"PreSharedKey\" /"))
                    passWifi += getString(R.string.p_nf) + ",";


            }

            // if the number of password is null

            switch (nWifi) {
                case 0:
                    Toast.makeText(MainActivity.this, getString(R.string.n_nf), Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    String[] networks = {
                            "SSID: " + nameWifi.replace(",", "") + "\nPassword: " + passWifi.replace(",", "")};
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                            android.R.layout.simple_list_item_1, networks);
                    mList.setAdapter(adapter);

                    // show menu
                    mList.setOnItemLongClickListener((parent, view, position, id) -> {

                        String item = (String) mList.getItemAtPosition(position);
                        String[] item1 = item.split("\n");
                        String item2 = item1[1].replace("Password: ", "");

                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(item2);
                        Toast.makeText(MainActivity.this, getString(R.string.cp_pwd), Toast.LENGTH_LONG).show();
                        return false;

                    });


                    // copy password
                    mList.setOnItemClickListener((parent, view, position, id) -> {


                        final CharSequence[] items = {getString(R.string.cp_ssid), getString(R.string.cp_pwd),
                                getString(R.string.ct_qr), getString(R.string.share), getString(R.string.backup)};

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setItems(items, (dialog, item) -> {

                            try {
                                show_menu(position, item);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        });
                        AlertDialog alert = builder.create();
                        alert.show();

                    });
                    break;
                case 2:

                    String[] z = nameWifi.split(",");
                    String[] x = passWifi.split(",");

                    String totWifi = "";
                    for (int d = 0; d < nWifi; d++) {

                        totWifi += "SSID: " + z[d] + "\nPassword: " + x[d] + "\n\n";

                    }

                    String[] networks1 = totWifi.split("\n\n");

                    ArrayAdapter<CharSequence> adapter1 = new ArrayAdapter<CharSequence>(MainActivity.this,
                            android.R.layout.simple_list_item_1, networks1);
                    mList.setAdapter(adapter1);


                    // show menu
                    mList.setOnItemLongClickListener((parent, view, position, id) -> {

                        String item = (String) mList.getItemAtPosition(position);
                        String[] item1 = item.split("\n");

                        String item2 = item1[1].replace("Password: ", "");

                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(item2);
                        Toast.makeText(MainActivity.this, getString(R.string.cp_pwd), Toast.LENGTH_LONG).show();
                        return false;

                    });


                    // copy password
                    mList.setOnItemClickListener((parent, view, position, id) -> {


                        final CharSequence[] items = {getString(R.string.cp_ssid), getString(R.string.cp_pwd),
                                getString(R.string.ct_qr), getString(R.string.share), getString(R.string.backup)};


                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {

                                try {
                                    show_menu(position, item);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();

                    });


                    break;
            }

        } catch (NullPointerException e) {
            Intent myIntent = new Intent(MainActivity.this, RootActivity.class);
            MainActivity.this.startActivity(myIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void show_menu(int position, int item) throws IOException {

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ListView lista = findViewById(R.id.list);
        String items = (String) lista.getItemAtPosition(position);
        String[] item1 = items.split("\n");
        String ssidReplace = item1[0].replace("SSID: ", "");
        String pwdReplace = item1[1].replace("Password: ", "");

        switch (item) {

            case 0:
                clipboard.setText(ssidReplace);
                Toast.makeText(MainActivity.this, getString(R.string.t_cp_ssid), Toast.LENGTH_LONG).show();
                break;
            case 1:


                clipboard.setText(pwdReplace);
                Toast.makeText(MainActivity.this, getString(R.string.t_cp_pwd), Toast.LENGTH_LONG).show();
                break;
            case 2:
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                try {
                    BitMatrix bitMatrix = multiFormatWriter.encode("WIFI:T:WPA;S:" + ssidReplace + ";P:" + pwdReplace + ";;",
                            BarcodeFormat.QR_CODE, 200, 200);
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

                    File file = new File(MainActivity.this.getCacheDir(), "qr.png");
                    FileOutputStream fOut = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();

                } catch (WriterException | IOException e) {
                    e.printStackTrace();
                }

                String bitmapPath = null;
                try {
                    bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(),
                            "/data/data/com.fast0n.wifiview/cache/qr.png", getString(R.string.share_qr), null);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Uri bitmapUri = Uri.parse(bitmapPath);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_qr));
                intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                startActivity(Intent.createChooser(intent, getString(R.string.share)));

                break;
            case 3:
                String msg = String.format(getString(R.string.share_msg), items);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
            case 4:
                File folder = new File(Environment.getExternalStorageDirectory() +
                        File.separator + getString(R.string.folder_name));

                boolean success = true;
                if (!folder.exists()) {
                    success = folder.mkdirs();
                }
                if (success) {

                    File myFile = new File(Environment.getExternalStorageDirectory() +
                            File.separator + getString(R.string.folder_name) + File.separator + ssidReplace + ".txt");
                    myFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(myFile);
                    OutputStreamWriter myOutWriter =
                            new OutputStreamWriter(fOut);
                    myOutWriter.append(pwdReplace);
                    myOutWriter.close();
                    fOut.close();

                }

                Toast.makeText(MainActivity.this, getString(R.string.b_ct), Toast.LENGTH_LONG).show();
                break;
        }

    }
}