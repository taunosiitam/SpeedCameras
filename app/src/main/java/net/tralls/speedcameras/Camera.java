package net.tralls.speedcameras;

import android.location.Location;
import android.location.LocationManager;

import org.json.JSONException;
import org.json.JSONObject;

public class Camera extends Location {

    private String name;  // camera name
    private float distance;  // our distance from this camera
    private float angle;  // our approach angle towards this camera

    public Camera(JSONObject c) {
        super(LocationManager.PASSIVE_PROVIDER);
        try {
            name = c.getString("name");
            setLatitude(c.getDouble("lat"));
            setLongitude(c.getDouble("lon"));
            setSpeed(c.getInt("speed") / 3.6f);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            setBearing(c.getInt("bearing"));
        } catch (JSONException ignored) {
        }
    }

    public String getName() {
        return name;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

}
