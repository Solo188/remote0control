package com.screencast.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.screencast.R;
import com.screencast.config.Config;
import com.screencast.service.RemoteControlService;
import com.screencast.service.ScreenCastService;

public class MainActivity extends Activity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_ACCESSIBILITY = 1002;

    private MediaProjectionManager projectionManager;
    private Button btnStart;
    private Button btnStop;
    private Button btnAccessibility;
    private TextView tvStatus;
    private TextView tvServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        tvStatus = findViewById(R.id.tv_status);
        tvServer = findViewById(R.id.tv_server);

        tvServer.setText("Сервер: " + Config.SERVER_URL);

        btnStart.setOnClickListener(v -> requestScreenCapture());
        btnStop.setOnClickListener(v -> stopCasting());
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean accessibilityEnabled = isAccessibilityEnabled();
        if (accessibilityEnabled) {
            btnAccessibility.setText("Спец. возможности: ВКЛ");
            btnAccessibility.setEnabled(false);
            tvStatus.setText("Готов к трансляции");
        } else {
            btnAccessibility.setText("Включить спец. возможности");
            btnAccessibility.setEnabled(true);
            tvStatus.setText("Требуется разрешение спец. возможностей");
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabled != null && enabled.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    private void requestScreenCapture() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Сначала включите специальные возможности!", Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this,
                "Найдите 'ScreenCast Control' и включите его",
                Toast.LENGTH_LONG).show();
    }

    private void stopCasting() {
        Intent intent = new Intent(this, ScreenCastService.class);
        intent.setAction(ScreenCastService.ACTION_STOP);
        startService(intent);
        tvStatus.setText("Трансляция остановлена");
        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, ScreenCastService.class);
                serviceIntent.setAction(ScreenCastService.ACTION_START);
                serviceIntent.putExtra(ScreenCastService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(ScreenCastService.EXTRA_DATA, data);
                startForegroundService(serviceIntent);
                tvStatus.setText("Трансляция запущена → " + Config.SERVER_URL);
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
            } else {
                Toast.makeText(this, "Разрешение на трансляцию отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
