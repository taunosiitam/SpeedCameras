#include <jni.h>
#include <map>
#include <string>
#include <vector>

#include "points.h"
#include "polygons.h"
#include "lest.h"

Points points;
Polygons polygons;

std::string utf8(const std::string &s) {
    char buf[2 * PATH_MAX];
    char *p = buf;
    for (const auto &c: s) {
        switch ((unsigned char) c) {
            case 0xc4: *p++ = -61; *p++ = -124; break;  // Ä
            case 0xd5: *p++ = -61; *p++ = -107; break;  // Õ
            case 0xd6: *p++ = -61; *p++ = -106; break;  // Ö
            case 0xdc: *p++ = -61; *p++ = -100; break;  // Ü
            case 0xe4: *p++ = -61; *p++ = -92; break;  // ä
            case 0xf5: *p++ = -61; *p++ = -75; break;  // õ
            case 0xf6: *p++ = -61; *p++ = -74; break;  // ö
            case 0xfc: *p++ = -61; *p++ = -68; break;  // ü
            default: *p++ = c;
        }
    }
    *p = 0;
    return buf;
}

extern "C" JNIEXPORT void JNICALL
Java_net_tralls_speedcameras_Activity_initJNI(
        JNIEnv *env, jobject /* this */, jstring pointsPath, jstring polygonsPath, int indexGridSize, int queryDistance) {

    const char *pointsPathUtf = env->GetStringUTFChars(pointsPath, nullptr);
    const char *polygonsPathUtf = env->GetStringUTFChars(polygonsPath, nullptr);

    points.load(pointsPathUtf, indexGridSize, queryDistance);
    polygons.load(polygonsPathUtf);

    env->ReleaseStringUTFChars(pointsPath, pointsPathUtf);
    env->ReleaseStringUTFChars(polygonsPath, polygonsPathUtf);
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_tralls_speedcameras_Activity_queryLocation(JNIEnv *env, jobject /* this */, jdouble lat, jdouble lon) {

    std::string result;
    unsigned x, y;
    ll2lest(lat, lon, x, y);

    std::string nearestPoint = points.query(x, y);
    if (!nearestPoint.empty()) result = utf8(nearestPoint) + ", ";

    std::vector<std::string> coveringPolygon = polygons.query(x, y);
    if (!coveringPolygon.empty()) result += utf8(coveringPolygon[ASULA]) + ", " + utf8(coveringPolygon[VALD]) + ", " + utf8(coveringPolygon[MAAKOND]);

    return env->NewStringUTF(result.c_str());
}
