# ScreenCast Android App

Приложение для трансляции экрана Android на удалённый сервер с возможностью управления телефоном через браузер.

## Структура проекта

```
android-screencast/
├── app/                    ← Android-приложение (Java)
│   └── src/main/
│       ├── java/com/screencast/
│       │   ├── config/Config.java          ← Адрес сервера здесь
│       │   ├── service/ScreenCastService.java   ← Трансляция экрана
│       │   ├── service/RemoteControlService.java ← Управление жестами
│       │   ├── server/WebSocketClient.java ← WebSocket клиент
│       │   └── ui/MainActivity.java        ← UI
│       ├── res/                            ← Ресурсы
│       └── AndroidManifest.xml
├── server/
│   ├── server.js           ← Сервер (Node.js, для Termux)
│   └── package.json
└── README.md
```

## Шаг 1: Настройка адреса сервера

Откройте `app/src/main/java/com/screencast/config/Config.java`:
```java
public static final String SERVER_URL = "http://bore.pub:53347";
public static final String WS_URL = "ws://bore.pub:53347/ws";
```
Замените адрес на ваш, если нужно.

## Шаг 2: Сборка APK

### Способ A — Android Studio (рекомендуется)
1. Откройте папку `android-screencast` в Android Studio
2. Подождите синхронизации Gradle
3. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. APK будет в `app/build/outputs/apk/debug/app-debug.apk`

### Способ B — командная строка (нужен Android SDK)
```bash
cd android-screencast
chmod +x gradlew
./gradlew assembleDebug
```

## Шаг 3: Установка на телефон
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
Или перенесите APK на телефон и установите вручную (нужно разрешить "Установку из неизвестных источников").

## Шаг 4: Запуск сервера в Termux

```bash
# Установить Node.js
pkg install nodejs

# Перейти в папку сервера
cd screencast-server

# Установить зависимости
npm install

# Запустить сервер
node server.js
```

Для внешнего доступа через bore:
```bash
# Установить bore
pkg install bore-cli
# или скачать вручную: https://github.com/ekzhang/bore

bore local 53347 --to bore.pub
```

## Шаг 5: Использование приложения

1. Запустите приложение на телефоне
2. Нажмите **"Включить спец. возможности"** → найдите "ScreenCast Control" → включите
3. Вернитесь в приложение → нажмите **"Начать трансляцию"** → дайте разрешение
4. Откройте в браузере: `http://bore.pub:53347`
5. Вы увидите экран телефона — кликайте на изображение для управления!

## Управление из браузера

- **Клик** — нажатие на экран
- **Свайп** (на мобильном браузере) — свайп на экране
- **Долгое нажатие** (удержание >500мс) — долгое нажатие
- **Кнопки** — Назад, Домой, Задачи

## Технические детали

- Трансляция: Android MediaProjection API → JPEG → WebSocket → браузер
- Управление: Команды из браузера → WebSocket → AccessibilityService → GestureDispatch
- Сервер: Node.js WebSocket relay (перенаправляет кадры к браузерам, команды к телефону)
- Минимальная версия Android: 8.0 (API 26)
