package com.screencast.server;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.screencast.service.RemoteControlService;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private final String url;
    private OkHttpClient client;
    private WebSocket webSocket;
    private boolean connected = false;
    private int screenWidth = 0;
    private int screenHeight = 0;

    public WebSocketClient(String url) {
        this.url = url;
        this.client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void connect() {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                Log.d(TAG, "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleCommand(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                reconnectDelayed();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                Log.d(TAG, "WebSocket closed: " + reason);
            }
        });
    }

    private void reconnectDelayed() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void handleCommand(String text) {
        try {
            JSONObject cmd = new JSONObject(text);
            String type = cmd.getString("type");

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

            RemoteControlService svc = RemoteControlService.getInstance();
            if (svc == null) return;

            switch (type) {
                case "click": {
                    double x = cmd.getDouble("x");
                    double y = cmd.getDouble("y");
                    svc.performClick((float) x, (float) y, screenWidth, screenHeight);
                    break;
                }
                case "swipe": {
                    double x1 = cmd.getDouble("x1");
                    double y1 = cmd.getDouble("y1");
                    double x2 = cmd.getDouble("x2");
                    double y2 = cmd.getDouble("y2");
                    svc.performSwipe((float) x1, (float) y1, (float) x2, (float) y2,
                            screenWidth, screenHeight);
                    break;
                }
                case "longclick": {
                    double x = cmd.getDouble("x");
                    double y = cmd.getDouble("y");
                    svc.performLongClick((float) x, (float) y, screenWidth, screenHeight);
                    break;
                }
                case "back":
                    svc.performBack();
                    break;
                case "home":
                    svc.performHome();
                    break;
                case "recents":
                    svc.performRecents();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling command: " + text, e);
        }
    }

    public void sendFrame(byte[] jpegBytes) {
        if (!connected || webSocket == null) return;
        try {
            String b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
            JSONObject msg = new JSONObject();
            msg.put("type", "frame");
            msg.put("data", b64);
            webSocket.send(msg.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending frame", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Stopped by user");
            webSocket = null;
        }
        connected = false;
    }
}
