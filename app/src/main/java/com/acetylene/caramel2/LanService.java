package com.acetylene.caramel2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import java.io.IOException;

public class LanService extends Service {
    private NotificationManager notificationManager;
    private String addr;
    private final int port = 2008;
    private WebServer webServer;
    private BroadcastReceiver stopReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("LanService Started");
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!networkInfo.isConnected()) {
            System.out.println("No wifi, disconnecting");
            stopSelf();
        }

        addr = NetUtils.getIPAddress(true);

        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopSelf();
            }
        };
        registerReceiver(stopReceiver, new IntentFilter("stop"));

        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, new Intent("stop"), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_pets_white_24dp)
                .setContentTitle("Caramel Wi-Fi Bridge Running")
                .setContentText("Enter \"" + addr+":"+port + "\" into your browser")
                .setColor(Color.parseColor("#FF9800"))
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
                .build();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        startServer();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startServer() {
        webServer = new WebServer(port, getApplicationContext());
        try {
            webServer.start();
        } catch (IOException e) {
            System.out.println("WebServer could not start");
            stopSelf();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        unregisterReceiver(stopReceiver);
        notificationManager.cancel(0);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            notificationManager.cancel(0);
        } catch (Exception e) {}

        try {
            webServer.stop();
        } catch (Exception e) {}
    }

    // Uncharted territory

    @Override
    public IBinder onBind(Intent intent) { return new LocalBinder(); }

    public class LocalBinder extends Binder {
        LanService getService() {
            return LanService.this;
        }
    }
}