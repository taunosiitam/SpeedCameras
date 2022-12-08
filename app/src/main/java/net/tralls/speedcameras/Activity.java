package net.tralls.speedcameras;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import net.tralls.speedcameras.databinding.ActivityBinding;

public class Activity extends android.app.Activity {

    private static final String TAG = Activity.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    static {
        System.loadLibrary("speedcameras");
    }

    public native void initJNI(String pointsPath, String polygonsPath, int indexGridSize, int queryDistance);

    public native String queryLocation(double lat, double lon);

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged " + location);
            onLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled " + provider);
            if (LocationManager.NETWORK_PROVIDER.equals(provider) || LocationManager.GPS_PROVIDER.equals(provider)) onLocation(null);
        }
    };
    private final GnssStatus.Callback statusCallback = new GnssStatus.Callback() {
        @Override
        public void onStopped() {
            Log.d(TAG, "onStopped");
            onLocation(null);
        }
    };
    private LocationManager locationManager;

    private ActivityBinding binding;
    private GestureDetector gestureDetector;
    private Toast exitToast;
    private boolean backPressed = false;
    private boolean askingPermissions = false;
    private long lastLocationTime = System.currentTimeMillis();
    private float distanceToCamera;
    private float distanceDelta;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateDistance;
    private Runnable rotateSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        File pointsFile = new File(getFilesDir(), "points.dat");
        File polygonsFile = new File(getFilesDir(), "polygons.dat");
        try {
            Files.copy(getResources().openRawResource(R.raw.points), pointsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(getResources().openRawResource(R.raw.polygons), polygonsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initJNI(pointsFile.getAbsolutePath(), polygonsFile.getAbsolutePath(), 2000, 200);

        File file = new File(Environment.getExternalStorageDirectory(), "exception.txt");
        if (file.exists()) {
            Toast.makeText(this, "exception file exists, finishing ...", Toast.LENGTH_SHORT).show();
            finish();
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.d(TAG, "saving exception ...");
            try {
                FileWriter f = new FileWriter(file);
                f.write(Log.getStackTraceString(e));
                f.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        binding = ActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            insetsController.hide(WindowInsets.Type.systemBars());
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else setSystemUiVisibility();

        gestureDetector = new GestureDetector(this, new MyGestureListener());

        exitToast = Toast.makeText(this, R.string.confirm_exit, Toast.LENGTH_SHORT);

        binding.cameraDistanceBar.setMax(Settings.ALERT_DISTANCE);

        Cameras.load(getResources().openRawResource(R.raw.cameras));

        locationManager = getSystemService(LocationManager.class);
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "asking permissions");
            askingPermissions = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }

        updateDistance = () -> {
            distanceToCamera -= distanceDelta;
            binding.cameraDistance.setText(String.format(Locale.US, "%.0f m", distanceToCamera));
            binding.cameraDistanceBar.setProgress(Settings.ALERT_DISTANCE - (int) distanceToCamera);
            if (distanceToCamera <= 0) {
                setCameraGroupVisibility(View.INVISIBLE);
                binding.speed.setTextColor(getResources().getColor(R.color.white, getTheme()));
            } else handler.postDelayed(updateDistance, Settings.DISTANCE_UPDATE_INTERVAL);
        };

        rotateSpinner = () -> {
            binding.noSignalSpinner.setRotation(binding.noSignalSpinner.getRotation() + 30);
            handler.postDelayed(rotateSpinner, Settings.SPINNER_ROTATION_INTERVAL);
        };
        handler.postDelayed(rotateSpinner, Settings.SPINNER_ROTATION_INTERVAL);
    }

    private void setSystemUiVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "only approximate location access granted");
                requestLocationUpdates();
            } else if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "precise location access granted");
                requestLocationUpdates();
            } else Log.d(TAG, "no location access granted");
            askingPermissions = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        stopService(new Intent(this, Service.class));
        requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        removeCallbacks();
        removeLocationUpdates();
        if (!backPressed && !askingPermissions) {
            Toast.makeText(this, R.string.background_message, Toast.LENGTH_SHORT).show();
            startForegroundService(new Intent(this, Service.class));
            finish();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) return true;
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (backPressed) {
            exitToast.cancel();
            finish();
        } else {
            backPressed = true;
            exitToast.show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> backPressed = false, Settings.CONFIRM_EXIT_TIMEOUT);
        }
    }

    private void requestLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestLocationUpdates");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Settings.LOCATION_UPDATE_INTERVAL, Settings.LOCATION_UPDATE_DISTANCE, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Settings.LOCATION_UPDATE_INTERVAL, Settings.LOCATION_UPDATE_DISTANCE, locationListener);
            locationManager.registerGnssStatusCallback(statusCallback, null);
        }
    }

    private void removeLocationUpdates() {
        Log.d(TAG, "removeLocationUpdates");

        locationManager.unregisterGnssStatusCallback(statusCallback);
        locationManager.removeUpdates(locationListener);
    }

    private void onLocation(Location location) {
        removeCallbacks();
        if (location == null) {
            Log.d(TAG, "signal lost");
            binding.noSignalSpinner.setVisibility(View.VISIBLE);
            binding.speed.setVisibility(View.INVISIBLE);
            binding.debug.setVisibility(View.INVISIBLE);
            setCameraGroupVisibility(View.INVISIBLE);
            binding.noSignalSpinner.setRotation(0);
            handler.postDelayed(rotateSpinner, Settings.SPINNER_ROTATION_INTERVAL);
        } else {
            int carSpeedKmph = location.hasSpeed() ? Math.round((3.6f * location.getSpeed())) : 0;
            Camera camera = Cameras.find(location);
            Log.d(TAG, camera == null ? "no nearby cameras" : "camera: " + camera);
            binding.noSignalSpinner.setVisibility(View.INVISIBLE);
            binding.speed.setVisibility(View.VISIBLE);
            binding.debug.setVisibility(View.VISIBLE);
            int color;
            if (camera == null) {
                color = getResources().getColor(R.color.white, getTheme());
                setCameraGroupVisibility(View.INVISIBLE);
            } else {
                int cameraSpeedKmph = Math.round((3.6f * camera.getSpeed()));
                color = getResources().getColor(carSpeedKmph > cameraSpeedKmph ? R.color.red : R.color.green, getTheme());
                distanceToCamera = camera.getDistance();
                distanceDelta = (float) (Math.cos(Math.toRadians(camera.getAngle())) * (Settings.DISTANCE_UPDATE_INTERVAL * location.getSpeed() / 1000));
                setCameraGroupVisibility(View.VISIBLE);
                binding.cameraName.setText(camera.getName());
                binding.cameraSpeed.setText(String.format(Locale.US, "%d", cameraSpeedKmph));
                binding.cameraDistance.setText(String.format(Locale.US, "%.0f m", distanceToCamera));
                binding.cameraDistanceBar.setProgress(Settings.ALERT_DISTANCE - (int) distanceToCamera);
                handler.postDelayed(updateDistance, Settings.DISTANCE_UPDATE_INTERVAL);
            }
            binding.speed.setTextColor(color);
            binding.speed.setText(String.format(Locale.US, "%d", carSpeedKmph));

            String text = queryLocation(location.getLatitude(), location.getLongitude());
            Log.d(TAG, "queryLocation: " + text);
            if (BuildConfig.DEBUG) {
                long currentTime = System.currentTimeMillis();
                int interval = (int) (currentTime - lastLocationTime);
                lastLocationTime = currentTime;
                binding.debug.setText(String.format(Locale.US, "%s N%.5f E%.5f %.0fm %.0f° %s %dms %s",
                        location.getProvider(), location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getBearing(),
                        camera == null ? "-" : String.format(Locale.US, "%.1f°", camera.getAngle()), interval, text));
            } else binding.debug.setText(text);
        }
    }

    private void setCameraGroupVisibility(int visibility) {
        binding.cameraIcon.setVisibility(visibility);
        binding.cameraName.setVisibility(visibility);
        binding.cameraSpeedIcon.setVisibility(visibility);
        binding.cameraSpeed.setVisibility(visibility);
        binding.cameraDistance.setVisibility(visibility);
        binding.cameraDistanceBar.setVisibility(visibility);
    }

    private void removeCallbacks() {
        handler.removeCallbacks(updateDistance);
        handler.removeCallbacks(rotateSpinner);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY) {
            // do not handle flings that start close to the edge of the screen
            int w, h;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Rect r = getWindowManager().getCurrentWindowMetrics().getBounds();
                w = r.width();
                h = r.height();
            } else {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                w = dm.widthPixels;
                h = dm.heightPixels;
            }
            int downX = (int) down.getX();
            if (downX > up.getX()) downX = w - downX;
            int downY = (int) down.getY();
            if (downY > up.getY()) downY = h - downY;
            if (10 * downX > w && 10 * downY > h) binding.getRoot().toggleMirror();
            return true;
        }
    }

}
