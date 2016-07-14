package com.acetylene.caramel;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity 
        implements Bluetooth.OnConnectedListener,
	Bluetooth.OnReceivedMessageListener,
        Bluetooth.OnConnectionClosedListener,
        AdapterView.OnItemSelectedListener{
    private Bluetooth bt;
    private EditText entry;
    private ImageButton sendButton;
    private ScrollView scroll;
    private TextView terminal;
    private boolean connected;
    private Spinner spinner;
    private RelativeLayout main;
    private String spinnerKey;
    private List<String> deviceNameList;

    /*

    ** Overridden methods **

    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	
	    entry = (EditText) findViewById(R.id.edittext);
	    sendButton = (ImageButton) findViewById(R.id.sendbutton);
        scroll = (ScrollView) findViewById(R.id.terminalsv);
        terminal = (TextView) findViewById(R.id.terminal);
        main = (RelativeLayout) findViewById(R.id.mainLayout);
        spinnerKey = "Select Caramel device:";

        connected = false;

        firstRun();

        String devname = "";
        if (getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("devname", "Caramel") != null) {
            devname = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("devname", "Caramel");
        }

        spinner = new AppCompatSpinner(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        spinner.setLayoutParams(params);
        spinner.setGravity(Gravity.TOP | Gravity.RIGHT);
        spinner.setOnItemSelectedListener(this);
        deviceNameList = new ArrayList<>();
        deviceNameList.add(spinnerKey);
        for (BluetoothDevice d : new Bluetooth().getPairedDevices()) {
            deviceNameList.add(d.getName());
        }
        ArrayAdapter<String> deviceAdapter =
                new ArrayAdapter<String>
                        (this, android.R.layout.simple_spinner_item, deviceNameList);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(deviceAdapter);
        spinner.setEnabled(false);
        spinner.setVisibility(View.INVISIBLE);
        main.addView(spinner);

        if (!devname.equals("")) {
            setName(devname);
        }
    }

    @Override
    protected void onDestroy() {
        disconnect();
	    super.onDestroy();
    }

    @Override
    protected void onStop() {
        disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        disconnect();
	    super.onPause();
    }
    
    @Override
    public void OnConnected(BluetoothDevice device) {
        bt.sendMessage(entry.getText().toString());
    }

    @Override
    public void ErrorConnecting(IOException e) {
	    error(e);
    }

    @Override
    public void OnReceivedMessage(String message) {
        if (message.startsWith("$")) {
            if (message.equals("$okDisconnect")) disconnect();
            return;
        }
	    print("· " + message);
    }

    @Override
    public void OnConnectionClosed(BluetoothDevice device, String message) {
        connected = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit:
                spinner.performClick();
                return true;

            case R.id.bluetooth:
                final Intent btSettingsIntent = new Intent();
                btSettingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(btSettingsIntent);
                return true;

            case R.id.paw:
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setNegativeButton("Close", null)
                    .setView(getLayoutInflater().inflate(R.layout.chewie, null));
                    //.setView(doggo);

                AlertDialog alert = builder.create();
                alert.show();
                return true;

            default: return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getItemAtPosition(position).toString().equals(spinnerKey)) return;
        setName(parent.getItemAtPosition(position).toString());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /*

    ** Buttons and UI elements **

    */

    public void sendClick(View v) {
        if (connected) return;
        terminal.setText("");
        String msg = entry.getText().toString();
        print("<font color=\"#009688\"><b>" + msg + "</b></font>");
        connect();
    }

    /*

    ** Helper methods **

    */

    private void disconnect() {
        System.out.println("Disconnect");
        try {
            bt.disconnect();
        } catch (Exception e) {}
        bt = null;
        connected = false;
    }

    private void connect() {
        connected = true;
        bt = null;
        bt = new Bluetooth();
        bt.enableBluetooth();
        bt.setOnConnectedListener(this);
        bt.setOnReceivedMessageListener(this);
        bt.setOnConnectionClosedListener(this);
        if(!bt.connectByName(
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("devname", "Caramel"))) {
            error(new IOException("No compatible devices available."));
        }
    }

    private void print(final String s) {
	    runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
                Spanned span = Html.fromHtml("<br>" + s + "&nbsp;");
                terminal.append(span);
	        }
        });
    }

    private void setName(String name) {
        System.out.println("Name set to " + name);
        getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putString("devname", name)
                .apply();
    }

    private void error(IOException e) {
        print("Error connecting. Is the Caramel on, in range, and named \"" +
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("devname", "Caramel") +
                "\"?");
        Log.e("Caramel", e.getMessage());
        disconnect();
    }

    private void firstRun() {
        boolean isFirstRun =
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun) {
            new AlertDialog.Builder(this)
                    .setTitle("Welcome!")
                    .setMessage("Make sure you've successfully paired with your Caramel device. " +
                            "If not, go into your device's bluetooth settings. You should see a " +
                            "device named Caramel (or whatever you named it). If you don't see " +
                            "it, it may display as series of mumbo" +
                            "jumbo letters and numbers. Select any of them. After you select it, " +
                            "it may finally display as Caramel. Enter the password you set. Make " +
                            "sure to select \"PIN contains letters or symbols\" if your password " +
                            "does. Enter your password and select OK. After pairing is complete, " +
                            "return to this app. Remember to set the name of the device with the " +
                            "pencil button at the top of the screen.")
                    .setNeutralButton("OK", null)
                    .setNegativeButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent btSettingsIntent = new Intent();
                            btSettingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivity(btSettingsIntent);
                        }
                    })
                    .show();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();

            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putString("devname", deviceNameList.get(1))
                    .apply();
        }
    }
}