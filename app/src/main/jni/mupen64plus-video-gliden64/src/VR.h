#ifndef VR_SENSOR_HEADER
#define VR_SENSOR_HEADER

#include <android/log.h>
#define  LOG_TAG    "VR-TESTING"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

int SetupSensor();
bool remapCoordinateSystem(float inR[4][4], const int X, const int Y, float outR[4][4]);
int pollForSensorData();
void UpdateVRTransform();
int DestroySensor();
extern float VR_TRANSFORM_MAT[4][4];
extern bool left_eye;
extern bool vr_enabled;
extern bool has_cleared;
#endif