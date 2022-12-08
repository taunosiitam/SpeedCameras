#include <cmath>

#include "lest.h"

/// Convert ellipsoidal EPSG:4326 latitude/longitude to cartesian EPSG:3301 northing/easting
void ll2lest(
        const double latitude,  ///<[in] latitude in degrees
        const double longitude,  ///<[in] longitude in degrees
        unsigned &x,  ///<[out] northing in meters
        unsigned &y  ///<[out] easting in meters
) {
    double lat = latitude * PI / 180;
    double lon = longitude * PI / 180;
    double p = A * FF1 * pow(sqrt((1 - sin(lat)) / (1 + sin(lat)) * pow((1 + E * sin(lat)) / (1 - E * sin(lat)), E)), N1);
    double fii = N1 * (lon - L0);
    x = (unsigned) (P1 - p * cos(fii) + FN);
    y = (unsigned) (p * sin(fii) + FE);
}
