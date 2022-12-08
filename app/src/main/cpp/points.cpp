#include <cstdio>

#include "points.h"
#include "lest.h"

/// Load point names and coordinates from points binary file.
/// Names are in alphabetic order and slightly packed.
/// Points are in EPSG:3301 system, original values offset with false easting/northing,
/// multiplied by 20, and stored as 24-bit numbers.
void Points::load(
        const char *filename,  ///<[in] absolute path of points.dat
        int indexGridSize,  ///<[in] point geometry index tile size
        int queryDistance  ///<[in] distance limit for query function
) {
    FILE *f = fopen(filename, "rb");  // would have used fstream here if it wasn't so huge
    if (!f) return;

    _indexGridSize = indexGridSize;
    _queryDistance = queryDistance;

    // load and unpack names
    unsigned short namesCount;
    fread((char *) &namesCount, 2, 1, f);  // 2 bytes for number of names
    std::string prev;
    char name[256];
    for (unsigned i = 0; i < namesCount; i++) {
        unsigned char commonLength;
        fread((char *) &commonLength, 1, 1, f);  // 1 byte for overlapping length from previous string
        unsigned char nameLength;
        fread((char *) &nameLength, 1, 1, f);  // 1 byte for remaining length
        fread(name, nameLength, 1, f);  // remaining string itself
        name[nameLength] = 0;
        prev = prev.substr(0, commonLength) + name;  // concatenate with common length from previous string
        names.push_back(prev);
    }

    // load points
    unsigned pointsCount;
    fread((char *) &pointsCount, 4, 1, f);  // 4 bytes for number of points
    for (unsigned i = 0; i < pointsCount; i++) {
        int x = 0, y = 0;
        unsigned short nameIndex;
        fread((char *) &x, 3, 1, f);  // 3 bytes for x
        fread((char *) &y, 3, 1, f);  // 3 bytes for y
        fread((char *) &nameIndex, 2, 1, f);  // 2 bytes for name position in names list
        if (x >= 0x800000) x -= 0x1000000;  // convert 24-bit unsigned values to signed values
        if (y >= 0x800000) y -= 0x1000000;
        insert((FN + x / 20), (FE + y / 20), nameIndex);  // insert the point to points list and index
    }

    fclose(f);
}

/// Insert a point to points list and its position to point index.
/// 3 additional grid tiles are used for searches near the tile border (TODO: needs testing).
void Points::insert(
        unsigned x,  ///<[in] northing
        unsigned y,  ///<[in] easting
        unsigned short nameIndex  ///<[in] name position in names list
) {
    auto pos = points.size();
    Point point{x, y, nameIndex};
    points.push_back(point);
    auto ix = point.x / _indexGridSize;
    auto iy = point.y / _indexGridSize;
    index[(unsigned long long) ix << 32 | iy].push_back(pos);
    index[(unsigned long long) (ix + 1) << 32 | iy].push_back(pos);
    index[(unsigned long long) ix << 32 | (iy + 1)].push_back(pos);
    index[(unsigned long long) (ix + 1) << 32 | (iy + 1)].push_back(pos);
}

/// Find a closest point to the given location and return its name.
/// \returns Point name in Windows-1252 encoding, or empty string if not found
const std::string &Points::query(
        unsigned x,  ///< northing of the searched location
        unsigned y  ///< easting of the searched location
) {
    unsigned minDistance = UINT32_MAX;
    unsigned nameIndex;
    unsigned long long k = ((unsigned long long) (x / _indexGridSize) << 32) | (y / _indexGridSize);
    try {
        for (auto &idx: index.at(k)) {
            auto &p = points[idx];
            int dx = (int) (p.x - x), dy = (int) (p.y - y);
            auto d = (unsigned) sqrt((dx * dx) + (dy * dy));
            if (minDistance > d) {
                minDistance = d;
                nameIndex = p.nameIndex;
            }
        }
    }
    catch (std::exception &e) {
        return NOT_FOUND;
    }
    if (minDistance < _queryDistance) return names[nameIndex];
    return NOT_FOUND;
}
