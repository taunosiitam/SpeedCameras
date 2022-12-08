#pragma once

#include <map>
#include <string>
#include <vector>

/// Polygon name types
enum {
    MAAKOND,  ///< county
    VALD,  ///< town/parish
    ASULA,  ///< settlement
    NAME_LEVELS  ///< number of name types
};

/// Ring vertex
struct XY {
    double x;  ///< northing
    double y;  ///< easting
};

/// Bounding box
struct Box {
    XY p0;  ///< bottom left corner
    XY p1;  ///< upper right corner
};

class Polygons {
public:
    void load(const char *filename);

    const std::vector<std::string> &query(unsigned x, unsigned y);

private:
    std::vector<std::string> names[3];
    std::vector<std::vector<std::string>> ring_names;
    std::vector<std::vector<XY>> rings;
    std::vector<Box> boxes;
    const std::vector<std::string> NOT_FOUND;
};
