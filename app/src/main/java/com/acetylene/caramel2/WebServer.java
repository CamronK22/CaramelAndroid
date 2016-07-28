package com.acetylene.caramel2;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD
        implements Bluetooth.OnConnectedListener,
        Bluetooth.OnReceivedMessageListener,
        Bluetooth.OnConnectionClosedListener {
    Context context;
    boolean connected;
    Bluetooth bt;
    StringBuilder responseBuilder;
    String response;
    String message;

    public static final String
            MIME_PLAINTEXT = "text/plain",
            MIME_HTML = "text/html",
            MIME_JS = "application/javascript",
            MIME_CSS = "text/css",
            MIME_PNG = "image/png",
            MIME_ASP = "application/asp",
            MIME_XML = "text/xml",
            MIME_ICO = "image/x-icon",
            MIME_JPEG = "image/jpeg";

    public static final Response.Status
            HTTP_OK = Response.Status.OK;

    public WebServer(int port, Context context) {
        super(port);
        this.context = context;

        connected = false;
        bt = new Bluetooth();
        responseBuilder = new StringBuilder();
        response = "";
        message = "";
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        InputStream file;
        System.out.println("Req for " + uri);

        try {
            if(uri != null) {
                if (Method.GET.equals(method)) {
                    if (uri.equals("/")) {
                        file = getFile("index.html");
                        return new Response(HTTP_OK, MIME_HTML, file);
                    } else if (uri.endsWith(".ico")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_ICO, file);
                    } else if (uri.endsWith(".png")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_PNG, file);
                    } else if (uri.endsWith(".js")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_JS, file);
                    } else if (uri.endsWith(".css")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_CSS, file);
                    } else if (uri.endsWith(".xml")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_XML, file);
                    } else if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_JPEG, file);
                    } else if (uri.endsWith(".html")) {
                        file = getFile(uri.substring(1));
                        return new Response(HTTP_OK, MIME_HTML, file);
                    }
                }

                if (Method.POST.equals(method)) {
                    if (uri.substring(1).equals("")) return null;
                    if (connected) return null;
                    btSend(uri.substring(1));
                    String sendBack = response;
                    response = "";
                    responseBuilder = new StringBuilder();
                    disconnect("serve()");
                    System.out.println("Sendback: \"" + sendBack + "\".");
                    return new Response(HTTP_OK, MIME_ASP, toIS(sendBack));
                }
            }
        } catch (Exception e) {
            System.out.println("Error serving: " + e.getMessage());
        }

        return null;
    }

    private void btSend(String message) {
        this.message = message;
        bt = new Bluetooth();
        bt.enableBluetooth();
        bt.setOnConnectedListener(this);
        bt.setOnReceivedMessageListener(this);
        bt.setOnConnectionClosedListener(this);
        if(!bt.connectByName(
                context.getSharedPreferences("PREFERENCE", context.MODE_PRIVATE)
                        .getString("devname", "Caramel"))) {
            System.out.println("Error connecting to bluetooth");
        }

        int interval = 50;
        for (int i = 0; i < 10000 / interval; i++) {
            if (!response.equals("")) break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {e.printStackTrace();}
        }

        disconnect("btSend()");
    }

    @Override
    public void OnConnected(BluetoothDevice device) {
        connected = true;
        bt.sendMessage(message);
        message = "";
    }

    @Override
    public void ErrorConnecting(IOException e) {
        responseBuilder.append("Error connecting. Is the Caramel on, in range, and named \"" +
                context.getSharedPreferences("PREFERENCE", context.MODE_PRIVATE)
                        .getString("devname", "Caramel") +
                "\"?");
        disconnect("btSend()");
    }

    @Override
    public void OnConnectionClosed(BluetoothDevice device, String message) {
        connected = false;
        response = responseBuilder.toString();
        bt = null;
    }

    @Override
    public void OnReceivedMessage(String recmsg) {
        if (recmsg.equals("$okDisconnect")) {
            disconnect("OnRecievedMessage()");
        } else {
            responseBuilder.append(recmsg + "<br>");
        }
    }

    private void disconnect(String src) {
        System.out.println("disconnect() [WebServer]." + src);
        connected = false;
        response = responseBuilder.toString();
        try {
            bt.disconnect();
        } catch (Throwable none){ System.out.println("bt doesn't want to disconnect: "); }
        bt = null;
    }

    private InputStream getFile(String loc) {
        try {
            return context.getAssets().open(loc);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long getByteLength(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream).length;
        } catch (IOException e) {
            return -1;
        }
    }

    private InputStream toIS(String str) {
        try {
            return IOUtils.toInputStream(str, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}