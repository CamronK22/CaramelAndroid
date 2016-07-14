package com.acetylene.caramel;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity 
        implements Bluetooth.OnConnectedListener,
	Bluetooth.OnReceivedMessageListener,
        Bluetooth.OnConnectionClosedListener {
    private Bluetooth bt;
    private EditText entry;
    private Button sendButton;
    private ScrollView scroll;
    private TextView terminal;
    private boolean connected;

    /*

    ** Overridden methods, Android first, BT library second **

    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	
	    entry = (EditText) findViewById(R.id.edittext);
	    sendButton = (Button) findViewById(R.id.sendbutton);
        scroll = (ScrollView) findViewById(R.id.terminalsv);
        terminal = (TextView) findViewById(R.id.terminal);

        connected = false;

        firstRun();
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
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Device Name");
                alertDialog.setMessage("Enter name of Caramel device:");

                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                input.setHint("e.g. \"Caramel\" or \"SUPER MEGA CARAMELTRON VI\"");
                input.setText(getSharedPreferences("PREFERENCE", MODE_PRIVATE).getString("devname", "Caramel"));
                input.setPadding(input.getPaddingLeft(), input.getPaddingTop() + 36,
                        input.getPaddingRight() + 36, input.getPaddingBottom());
                alertDialog.setView(input);

                alertDialog.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setName(input.getText().toString());
                            }
                        });

                alertDialog.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                alertDialog.show();
                return true;

            case R.id.bluetooth:
                final Intent btSettingsIntent = new Intent();
                btSettingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(btSettingsIntent);
                return true;

            case R.id.paw:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
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

    public void clearClick(View v) {
        terminal.setText("");
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

    /*private void print(final Spanned s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                terminal.append(s + "\n");
            }
        });
    }*/

    private void setName(String name) {
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
        if (isFirstRun){
            new AlertDialog.Builder(this)
                    .setTitle("Welcome!")
                    .setMessage("Make sure you've successfully paired with your Caramel device. " +
                            "If not, go into your device's bluetooth settings. You should see a " +
                            "device named Caramel (or whatever you named it). If you don't, it may display as series of mumbo" +
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
                    .putString("devname", "Caramel")
                    .apply();
        }
    }
}