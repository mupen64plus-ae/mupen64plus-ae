#ifndef VR_SENSOR_HEADER
#define VR_SENSOR_HEADER

#include <android/log.h>
#define  LOG_TAG    "VR-TESTING"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#include "VectorMath.h"

int SetupSensor();
bool remapCoordinateSystem(const XMATRIX &inR, const int X, const int Y, XMATRIX &outR);
int pollForSensorData();
int DestroySensor();
extern XMATRIX VR_TRANSFORM_MAT;
#endif