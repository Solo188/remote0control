package com.screencast.server;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.screencast.service.RemoteControlService;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private final String url;
    private OkHttpClient client;
    private WebSocket webSocket;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private int screenWidth = 0;
    private int screenHeight = 0;

    public WebSocketClient(String url) {
        // Телефон подключается к /ws/phone (отдельный путь от браузеров)
        this.url = url.endsWith("/ws") ? url.replace("/ws", "/ws/phone") : url;
        this.client = new OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        Log.d(TAG, "Screen dimensions set: " + width + "x" + height);
    }

    public void connect() {
        if (reconnecting.get()) return;

        Log.d(TAG, "Connecting to: " + url);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "ScreenCast-Android/1.0")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.set(true);
                reconnecting.set(false);
                Log.d(TAG, "WebSocket connected to " + url);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleCommand(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected.set(false);
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                scheduleReconnect();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected.set(false);
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
                if (code != 1000) {
                    scheduleReconnect();
                }
            }
        });
    }

    private void scheduleReconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                reconnecting.set(false);
                connect();
            }, "WS-Reconnect").start();
        }
    }

    private void handleCommand(String text) {
        try {
            JSONObject cmd = new JSONObject(text);
            String type = cmd.getString("type");

            // Игнорируем служебные сообщения
            if (type.equals("frame") || type.equals("phone_connected") || type.equals("phone_disconnected")) {
                return;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.w(TAG, "Android < N: gestures not supported");
                return;
            }

            RemoteControlService svc = RemoteControlService.getInstance();
            if (svc == null) {
                Log.w(TAG, "RemoteControlService not available");
                return;
            }

            int w = screenWidth > 0 ? screenWidth : 1080;
            int h = screenHeight > 0 ? screenHeight : 1920;

            Log.d(TAG, "Command: " + type + " screen=" + w + "x" + h);

            switch (type) {
                case "click": {
                    double x = cmd.getDouble("x");
                    double y = cmd.getDouble("y");
                    svc.performClick((float) x, (float) y, w, h);
                    break;
                }
                case "swipe": {
                    double x1 = cmd.getDouble("x1");
                    double y1 = cmd.getDouble("y1");
                    double x2 = cmd.getDouble("x2");
                    double y2 = cmd.getDouble("y2");
                    svc.performSwipe((float) x1, (float) y1, (float) x2, (float) y2, w, h);
                    break;
                }
                case "longclick": {
                    double x = cmd.getDouble("x");
                    double y = cmd.getDouble("y");
                    svc.performLongClick((float) x, (float) y, w, h);
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
                default:
                    Log.d(TAG, "Unknown command: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling command: " + text, e);
        }
    }

    public void sendFrame(byte[] jpegBytes) {
        if (!connected.get() || webSocket == null) return;
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
        return connected.get();
    }

    public void disconnect() {
        connected.set(false);
        reconnecting.set(true); // предотвращаем переподключение
        if (webSocket != null) {
            webSocket.close(1000, "Stopped by user");
            webSocket = null;
        }
    }
}
