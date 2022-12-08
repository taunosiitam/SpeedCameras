package net.tralls.speedcameras;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Service extends android.app.Service {

    private static final String TAG = Service.class.getSimpleName();

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged " + location);
            handler.removeCallbacks(stop);
            handler.postDelayed(stop, Settings.SERVICE_IDLE_WATCHDOG);
            if (Cameras.find(location) != null) {
                Log.d(TAG, "detected nearby camera, starting activity ...");
                startActivity(activityIntent);
                stopSelf();
            }
        }
    };
    private Intent activityIntent;
    private LocationManager locationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable stop;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Toast.makeText(this, "Saving exception ...", Toast.LENGTH_SHORT).show();
            try {
                FileWriter f = new FileWriter(new File(Environment.getExternalStorageDirectory(), "exception.txt"));
                f.write(Log.getStackTraceString(e));
                f.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        activityIntent = new Intent(this, Activity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        locationManager = getSystemService(LocationManager.class);
        stop = () -> {
            Log.d(TAG, "no location updates, stopping ...");
            stopSelf();
        };
        handler.postDelayed(stop, Settings.SERVICE_IDLE_WATCHDOG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        locationManager.removeUpdates(locationListener);
        handler.removeCallbacks(stop);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(BuildConfig.APPLICATION_ID, TAG, NotificationManager.IMPORTANCE_LOW));
        startForeground(1, new Notification.Builder(this, BuildConfig.APPLICATION_ID)
                .setContentTitle(getText(R.string.background_message))
                .setSmallIcon(R.drawable.ic_camera)
                .setContentIntent(PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT))
                .setOngoing(true)
                .setAutoCancel(true)
                .build());

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestLocationUpdates");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Settings.LOCATION_UPDATE_INTERVAL, Settings.LOCATION_UPDATE_DISTANCE, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Settings.LOCATION_UPDATE_INTERVAL, Settings.LOCATION_UPDATE_DISTANCE, locationListener);
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
