#include <cstdio>

#include "polygons.h"
#include "lest.h"

void Polygons::load(const char *filename) {
    FILE *f = fopen(filename, "rb");
    if (!f) return;

    // load names
    for (unsigned i = 0; i < NAME_LEVELS; i++) {
        unsigned names_size = 0;
        fread((char *) &names_size, i < 2 ? 1 : 2, 1, f);
        std::string prev;
        char name[256];
        for (unsigned j = 0; j < names_size; j++) {
            unsigned char common_length;
            fread((char *) &common_length, 1, 1, f);
            unsigned char name_length;
            fread((char *) &name_length, 1, 1, f);
            fread(name, name_length, 1, f);
            name[name_length] = 0;
            prev = prev.substr(0, common_length) + name;
            names[i].push_back(prev);
        }
    }

    // load polygons
    unsigned short polygons_size;
    fread((char *) &polygons_size, 2, 1, f);
    for (unsigned short i = 0; i < polygons_size; i++) {
        unsigned char ring_type;
        fread((char *) &ring_type, 1, 1, f);

        std::vector<std::string> polygon_names;
        for (unsigned j = 0; j < NAME_LEVELS; j++) {
            unsigned name_index = 0;
            fread((char *) &name_index, j < 2 ? 1 : 2, 1, f);
            polygon_names.push_back(names[j][name_index]);
        }

        unsigned char rings_size;
        fread((char *) &rings_size, 1, 1, f);
        for (unsigned char j = 0; j < rings_size; j++) {
            ring_names.push_back(polygon_names);  // TODO: duplicate polygon names in rings, replace with indexes

            std::vector<XY> ring;
            unsigned short ring_size;
            fread((char *) &ring_size, 2, 1, f);
            int x = 0, y = 0;
            Box box{DBL_MAX, DBL_MAX, 0, 0};
            for (unsigned short k = 0; k < ring_size; k++) {
                int dx = 0, dy = 0;
                fread((char *) &dx, 3, 1, f);
                fread((char *) &dy, 3, 1, f);
                if (dx >= 0x800000) dx -= 0x1000000;
                if (dy >= 0x800000) dy -= 0x1000000;
                x += dx, y += dy;
                XY xy{FN + x / 20.0, FE + y / 20.0};
                ring.push_back(xy);
                if (box.p0.x > xy.x) box.p0.x = xy.x;
                if (box.p0.y > xy.y) box.p0.y = xy.y;
                if (box.p1.x < xy.x) box.p1.x = xy.x;
                if (box.p1.y < xy.y) box.p1.y = xy.y;
            }
            rings.push_back(ring);
            boxes.push_back(box);
        }
    }

    fclose(f);
}

const std::vector<std::string> &Polygons::query(unsigned x, unsigned y) {
    for (unsigned i = 0; i < boxes.size(); i++) {
        auto &box = boxes[i];
        if (x > box.p0.x && y > box.p0.y && x < box.p1.x && y < box.p1.y) {  // is the point within a bounding box?
            auto &ring = rings[i];
            double sum = 0;
            for (int j = 0; j < ring.size() - 1; j++) {
                double angle = atan2(ring[j + 1].y - y, ring[j + 1].x - x) - atan2(ring[j].y - y, ring[j].x - x);
                while (angle > PI) angle -= 2 * PI;
                while (angle < -PI) angle += 2 * PI;
                sum += angle;
            }
            if (abs(sum) >= PI) return ring_names[i];  // return if the point is within a ring as well
        }
    }
    return NOT_FOUND;
}
