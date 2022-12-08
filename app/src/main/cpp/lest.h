#pragma once

#define PI 3.14159265358979323846
#define FN 6375000
#define FE 500000
#define A 6378137
#define B0 ((57 + 31.0 / 60 + 3.194148 / 3600) * PI / 180)
#define B1 ((59 + 20.0 / 60) * PI / 180)
#define B2 (58 * PI / 180)
#define L0 (24 * PI / 180)
#define F (1 / 298.257222100883)
#define ER (2 * F - F * F)
#define E sqrt(ER)
#define T0 (sqrt((1 - sin(B0)) / (1 + sin(B0)) * pow((1 + E * sin(B0)) / (1 - E * sin(B0)), E)))
#define T1 (sqrt((1 - sin(B1)) / (1 + sin(B1)) * pow((1 + E * sin(B1)) / (1 - E * sin(B1)), E)))
#define T2 (sqrt((1 - sin(B2)) / (1 + sin(B2)) * pow((1 + E * sin(B2)) / (1 - E * sin(B2)), E)))
#define M1 (cos(B1) / sqrt(1 - ER * sin(B1) * sin(B1)))
#define M2 (cos(B2) / sqrt(1 - ER * sin(B2) * sin(B2)))
#define N1 ((log(M1) - log(M2)) / (log(T1) - log(T2)))
#define FF1 (M1 / N1 / pow(T1, N1))
#define P1 (A * FF1 * pow(T0, N1))

void ll2lest(const double latitude, const double longitude, unsigned &x, unsigned &y);
