package com.screencast.config;

public class Config {
    // Адрес сервера (bore.pub туннель или прямой IP сервера)
    public static final String SERVER_URL = "http://bore.pub:53347";
    public static final String WS_URL = "ws://bore.pub:53347/ws/phone";

    public static final int FRAME_RATE = 15;
    public static final int JPEG_QUALITY = 50;
    public static final int SCREEN_SCALE = 2;
    public static final String NOTIFICATION_CHANNEL_ID = "screencast_channel";
    public static final int NOTIFICATION_ID = 1001;
}
