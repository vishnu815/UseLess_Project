package com.example.chargeurinutile;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;  // Import MediaPlayer
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private TextView batteryLevelTextView;
    private TextView specificationsTextView;
    private ImageView imageView;
    private BroadcastReceiver batteryReceiver;
    private int initialBatteryLevel = -1;
    private boolean isFakeCharging = false;
    private MediaPlayer mediaPlayer;  // Declare MediaPlayer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        batteryLevelTextView = findViewById(R.id.battery_level);
        specificationsTextView = findViewById(R.id.specifications);
        imageView = findViewById(R.id.image_view);

        // Check if launched from notification
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && intent.getAction().equals("SHOW_IMAGE")) {
            imageView.setImageResource(R.drawable.my_image); // Set your image resource
            imageView.setVisibility(View.VISIBLE);
            playMusic(); // Play music when the app is opened from the notification
        }

        // Create the BroadcastReceiver for battery status
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                // Check if charging
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    handleUselessCharger(level);
                } else {
                    resetChargingState();
                }
            }
        };

        // Register the receiver for battery changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Check for notification permission and request if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }

        createNotificationChannel();

        // Start displaying specifications text
        displaySpecificationsWithDelay();
    }

    private void handleUselessCharger(int level) {
        if (initialBatteryLevel == -1) {
            initialBatteryLevel = level;

            // Start the timer for fake charging
            new Handler().postDelayed(() -> {
                if (initialBatteryLevel == level) {
                    isFakeCharging = true;
                    displayFakeBatteryInfo();
                    showNotification();
                }
            }, 1 * 60 * 1000); // 3 minutes
        }
    }

    private void displayFakeBatteryInfo() {
        if (isFakeCharging) {
            batteryLevelTextView.setVisibility(View.VISIBLE);
            batteryLevelTextView.setText(getString(R.string.battery_level_text, 75)); // Example fake level
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.my_image); // Ensure the correct image resource is set
        }
    }

    private void showNotification() {
        if (isFakeCharging) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction("SHOW_IMAGE"); // Set an action to indicate image display
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "charging_channel")
                    .setSmallIcon(R.drawable.ic_notification) // Replace with your actual icon
                    .setContentTitle(getString(R.string.charging_notification_title))
                    .setContentText(getString(R.string.charging_notification_content))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent); // Set the pending intent

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());
        }
    }

    private void resetChargingState() {
        initialBatteryLevel = -1;
        isFakeCharging = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("charging_channel", "Charging Status", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void displaySpecificationsWithDelay() {
        String[] specifications = {
                "Charging: Fast Charging",
                "Temperature: 30°C",
                "Voltage: 4.2V"
        };

        specificationsTextView.setText(""); // Clear previous text
        Handler handler = new Handler();
        for (int i = 0; i < specifications.length; i++) {
            final int index = i;
            handler.postDelayed(() -> {
                String currentText = specificationsTextView.getText().toString();
                specificationsTextView.setText(currentText + (currentText.isEmpty() ? "" : "\n") + specifications[index]);
            }, i * 2000); // Display each specification every 2 seconds
        }
    }

    private void playMusic() {
        // Initialize MediaPlayer and start playing
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.your_music_file); // Replace with your actual music file
            mediaPlayer.start();

            // Stop music after 10 seconds
            new Handler().postDelayed(() -> {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }, 10000); // 10 seconds
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNotification();
            } else {
                Toast.makeText(this, "Permission denied to show notifications", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver); // Unregister the battery receiver
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release MediaPlayer resources
            mediaPlayer = null;
        }
    }
}
