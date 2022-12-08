package net.tralls.speedcameras;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Cameras {

    private static final String TAG = Cameras.class.getSimpleName();
    private static final List<Camera> cameras = new ArrayList<>();

    static void load(InputStream is) {
        cameras.clear();
        try {
            JSONArray a = new JSONArray(new Scanner(is, "UTF-8").useDelimiter("\\A").next());
            for (int i = 0; i < a.length(); i++) {
                cameras.add(new Camera(a.getJSONObject(i)));
            }
            is.close();
            Log.d(TAG, "loaded " + cameras.size() + " cameras");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    static Camera find(Location car) {
        if (car == null || car.getSpeed() < 1) return null;  // car has no speed or is moving very slow
        List<Camera> found = new ArrayList<>();
        for (Camera camera : cameras) {
            if (car.distanceTo(camera) > Settings.ALERT_DISTANCE) continue;  // camera is too far away
            float bearingToCamera = car.bearingTo(camera);
            if (bearingToCamera < 0) bearingToCamera += 360;
            float angleToCamera = Math.abs(car.getBearing() - bearingToCamera);
            if (angleToCamera > 180) angleToCamera = 360 - angleToCamera;
            if (angleToCamera > Settings.ALERT_ANGLE) continue;  // we are not moving towards the camera
            if (camera.hasBearing()) {  // consider camera with no bearing as always directed at us
                float bearingToCar = camera.bearingTo(car);
                if (bearingToCar < 0) bearingToCar += 360;
                float angleToCar = Math.abs(camera.getBearing() - bearingToCar);
                if (angleToCar > 180) angleToCar = 360 - angleToCar;
                if (angleToCar > Settings.ALERT_ANGLE) continue;  // camera is not directed at us
            }
            camera.setDistance(car.distanceTo(camera));
            camera.setAngle(angleToCamera);
            found.add(camera);
        }
        if (found.isEmpty()) return null;  // no cameras found
        if (found.size() == 1) return found.get(0);  // only one camera found
        found.sort(Comparator.comparingDouble(Camera::getDistance));  // if more than one, sort by distance
        return found.get(0);  // and return the closest
    }

}
