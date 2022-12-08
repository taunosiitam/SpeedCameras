#pragma once

#include <map>
#include <string>
#include <vector>

struct Point {
    unsigned x;  ///< northing
    unsigned y;  ///< easting
    unsigned short nameIndex;  ///< name position in names list
};

class Points {
public:
    void load(const char *filename, int indexGridSize, int queryDistance);

    const std::string &query(unsigned x, unsigned y);

private:
    void insert(unsigned x, unsigned y, unsigned short nameIndex);

    int _indexGridSize;
    int _queryDistance;
    std::vector<std::string> names;
    std::vector<Point> points;
    std::map<unsigned long long, std::vector<unsigned>> index;
    const std::string NOT_FOUND;
};
