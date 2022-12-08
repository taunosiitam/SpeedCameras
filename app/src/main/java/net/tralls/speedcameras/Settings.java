package net.tralls.speedcameras;

public class Settings {

    static final int CONFIRM_EXIT_TIMEOUT = 2000;  // in milliseconds
    static final int LOCATION_UPDATE_INTERVAL = 1000;  // in milliseconds
    static final int LOCATION_UPDATE_DISTANCE = 0;  // in meters
    static final int DISTANCE_UPDATE_INTERVAL = 100;  // in milliseconds
    static final int SPINNER_ROTATION_INTERVAL = 100;  // in milliseconds
    static final int ALERT_DISTANCE = 1000;  // in meters
    static final int ALERT_ANGLE = 30;  // in degrees
    static final int SERVICE_IDLE_WATCHDOG = 600 * 1000;  // stop the service after 10 minutes without location updates

}
