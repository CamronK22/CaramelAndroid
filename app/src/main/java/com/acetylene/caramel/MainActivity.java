package com.acetylene.caramel;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity 
        implements Bluetooth.OnConnectedListener,
	Bluetooth.OnReceivedMessageListener,
        Bluetooth.OnConnectionClosedListener {
    private Bluetooth bt;
    
    EditText entry;
    TextView terminal;
    Button sendButton;
    ScrollView scroll;

    boolean connected;

    /*

    ** Overridden methods, Android first, BT library second **

    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	
	    entry = (EditText) findViewById(R.id.edittext);
	    terminal = (TextView) findViewById(R.id.terminal);
	    sendButton = (Button) findViewById(R.id.sendbutton);
        scroll = (ScrollView) findViewById(R.id.terminalsv);

        connected = false;

        firstRun();
        scroll.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @Override
    protected void onDestroy() {
	    super.onDestroy();
	    disconnect();
    }

    @Override
    protected void onPause() {
	    super.onPause();
	    disconnect();
    }
    
    @Override
    public void OnConnected(BluetoothDevice device) {
        bt.sendMessage(entry.getText().toString());
    }

    @Override
    public void ErrorConnecting(IOException e) {
	    print("Error connecting. Retrying in five seconds...");
	    try {
	        Thread.sleep(5000);
	    } catch (InterruptedException ie) {}
	    connect();
    }

    @Override
    public void OnReceivedMessage(String message) {
        if (message.startsWith("$")) {
            if (message.equals("$okDisconnect")) disconnect();
            return;
        }
	    print("< " + message);
    }

    @Override
    public void OnConnectionClosed(BluetoothDevice device, String message) {
        connected = false;
    }

    /*

    ** Buttons and UI elements **

    */

    public void sendClick(View v) {
        if (connected) return;
        print("> " + entry.getText().toString());
        connect();
    }

    public void clearClick(View v) {
        terminal.setText("");
    }

    /*

    ** Helper methods **

    */

    public void disconnect() {
        try {
            bt.disconnect();
            bt = null;
        } catch (Exception e) {}
    }

    public void connect() {
        connected = true;
        bt = null;
        bt = new Bluetooth();
        bt.enableBluetooth();
        bt.setOnConnectedListener(this);
        bt.setOnReceivedMessageListener(this);
        bt.setOnConnectionClosedListener(this);
        bt.connectByName("Caramel");
    }

    private void print(final String s) {
	    runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
                terminal.append(s + "\n");
	        }
        });
    }

    public void firstRun() {
        boolean isFirstRun =
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun){
            new AlertDialog.Builder(this)
                    .setTitle("Welcome!")
                    .setMessage("Make sure you've successfully paired with your Caramel device. " +
                            "If not, go into your device's bluetooth settings. You should see a " +
                            "device named Caramel. If you don't, it may display as series of mumbo" +
                            "jumbo letters and numbers. Select any of them. After you select it, " +
                            "it may finally display as Caramel. Enter the password you set. Make " +
                            "sure to select \"PIN contains letters or symbols\" if your password " +
                            "does. Enter your password and select OK. After pairing is complete, " +
                            "return to this app.")
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
        }
    }
}