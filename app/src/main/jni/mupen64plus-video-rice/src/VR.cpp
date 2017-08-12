#include <android/log.h>
#define  LOG_TAG    "VR-TESTING"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define ASENSOR_TYPE_ROTATION_VECTOR 15
#include <android/looper.h>
#include <android/sensor.h>
#include <cstring>
#include "VR.h"

static ASensorEventQueue* VR_SENSOR_QUEUE = NULL;
static ASensorRef VR_SENSOR = NULL;
XMATRIX VR_TRANSFORM_MAT(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1);

int SetupSensor() {
    if (VR_SENSOR_QUEUE != NULL) {
        LOGD("**************** VR_SENSOR_QUEUE Already Initialized!\n");
        return 1;
    }

    ASensorManager* sensor_manager =
            ASensorManager_getInstance();
    if (!sensor_manager) {
        LOGD("**************** Failed to get a sensor manager\n");
        return 1;
    }
    ASensorList sensor_list = NULL;
    int sensor_count = ASensorManager_getSensorList(sensor_manager, &sensor_list);
    LOGD("**************** Found %d supported sensors\n", sensor_count);
    for (int i = 0; i < sensor_count; i++) {
        LOGD("**************** HAL supports sensor %s\n", ASensor_getName(sensor_list[i]));
    }
    const int kLooperId = 1;
    VR_SENSOR_QUEUE = ASensorManager_createEventQueue(
            sensor_manager,
            ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS),
            kLooperId,
            NULL, /* no callback */
            NULL  /* no private data for a callback  */);
    if (!VR_SENSOR_QUEUE) {
        LOGD("**************** Failed to create a sensor event queue\n");
        return 1;
    }
    // Find the first sensor of the specified type that can be opened
    const int kTimeoutMicroSecs = 10000;
    bool sensor_found = false;
    for (int i = 0; i < sensor_count; i++) {
        ASensorRef sensor = sensor_list[i];
        if (ASensor_getType(sensor) != ASENSOR_TYPE_ROTATION_VECTOR)
            continue;
        if (ASensorEventQueue_enableSensor(VR_SENSOR_QUEUE, sensor) < 0)
            continue;
        if (ASensorEventQueue_setEventRate(VR_SENSOR_QUEUE, sensor, kTimeoutMicroSecs) < 0) {
            LOGD("**************** Failed to set the %s sample rate\n",
                 ASensor_getName(sensor));
            return 1;
        }
        // Found an equipped sensor of the specified type.
        sensor_found = true;
        VR_SENSOR = sensor;
        break;
    }
    if (!sensor_found) {
        LOGD("**************** No sensor of the specified type found\n");
        int ret = ASensorManager_destroyEventQueue(sensor_manager, VR_SENSOR_QUEUE);
        if (ret < 0)
            LOGD("**************** Failed to destroy event queue: %s\n", strerror(-ret));
        VR_SENSOR_QUEUE = NULL;
        return 1;
    }
    LOGD("\n**************** Sensor %s activated\n", ASensor_getName(VR_SENSOR));

    return 0;
}

const int AXIS_X = 1;
const int AXIS_Y = 2;
const int AXIS_Z = 3;
const int AXIS_MINUS_X = AXIS_X | 0x80;
const int AXIS_MINUS_Y = AXIS_Y | 0x80;
const int AXIS_MINUS_Z = AXIS_Z | 0x80;

bool remapCoordinateSystem(const XMATRIX &inR, const int X, const int Y, XMATRIX &outR)
{
    /*
     * X and Y define a rotation matrix 'r':
     *
     *  (X==1)?((X&0x80)?-1:1):0    (X==2)?((X&0x80)?-1:1):0    (X==3)?((X&0x80)?-1:1):0
     *  (Y==1)?((Y&0x80)?-1:1):0    (Y==2)?((Y&0x80)?-1:1):0    (Y==3)?((X&0x80)?-1:1):0
     *                              r[0] ^ r[1]
     *
     * where the 3rd line is the vector product of the first 2 lines
     *
     */
    if ((X & 0x7C)!=0 || (Y & 0x7C)!=0)
        return false;   // invalid parameter
    if (((X & 0x3)==0) || ((Y & 0x3)==0))
        return false;   // no axis specified
    if ((X & 0x3) == (Y & 0x3))
        return false;   // same axis specified
    // Z is "the other" axis, its sign is either +/- sign(X)*sign(Y)
    // this can be calculated by exclusive-or'ing X and Y; except for
    // the sign inversion (+/-) which is calculated below.
    int Z = X ^ Y;
    // extract the axis (remove the sign), offset in the range 0 to 2.
    const int x = (X & 0x3)-1;
    const int y = (Y & 0x3)-1;
    const int z = (Z & 0x3)-1;
    // compute the sign of Z (whether it needs to be inverted)
    const int axis_y = (z+1)%3;
    const int axis_z = (z+2)%3;
    if (((x^axis_y)|(y^axis_z)) != 0)
        Z ^= 0x80;
    const bool sx = (X>=0x80);
    const bool sy = (Y>=0x80);
    const bool sz = (Z>=0x80);
    // Perform R * r, in avoiding actual muls and adds.
    for (unsigned int j=0 ; j<3 ; j++) {
        for (unsigned int i=0 ; i<3 ; i++) {
            if (x==i)   outR(j, i) = sx ? -inR(j,0) : inR(j,0);
            if (y==i)   outR(j, i) = sy ? -inR(j,1) : inR(j,1);
            if (z==i)   outR(j, i) = sz ? -inR(j,2) : inR(j,2);
        }
    }
    outR._14 = outR._24 = outR._34 = outR._41 = outR._42 = outR._43 = 0;
    outR._44 = 1;
    return true;
}

int pollForSensorData() {
    if (!VR_SENSOR_QUEUE || !VR_SENSOR) {
        LOGD("**************** Sensors not initialized\n");
        return 1;
    }

    ASensorEvent data[1];
    memset(data, 0, sizeof(data));
    ALooper_pollAll(
            0, // timeout
            NULL /* no output file descriptor */,
            NULL /* no output event */,
            NULL /* no output data */);
    if (ASensorEventQueue_getEvents(VR_SENSOR_QUEUE, data, 1) <= 0) {
        //LOGD("**************** Failed to read data from the sensor.\n");
        return 1;
    }

    float q1 = data[0].data[0];
    float q2 = data[0].data[1];
    float q3 = data[0].data[2];
    float q0 = data[0].data[3];

    float d = (float) sqrt(q1*q1+q2*q2+q3*q3+q0*q0);
    q1 /= d; q2 /= d; q3 /= d; q0 /= d;

    float sq_q1 = 2 * q1 * q1;
    float sq_q2 = 2 * q2 * q2;
    float sq_q3 = 2 * q3 * q3;
    float q1_q2 = 2 * q1 * q2;
    float q3_q0 = 2 * q3 * q0;
    float q1_q3 = 2 * q1 * q3;
    float q2_q0 = 2 * q2 * q0;
    float q2_q3 = 2 * q2 * q3;
    float q1_q0 = 2 * q1 * q0;

    XMATRIX R(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1);

    R._11 = 1 - sq_q2 - sq_q3;
    R._12 = q1_q2 - q3_q0;
    R._13 = q1_q3 + q2_q0;
    R._21 = q1_q2 + q3_q0;
    R._22 = 1 - sq_q1 - sq_q3;
    R._23 = q2_q3 - q1_q0;
    R._31 = q1_q3 - q2_q0;
    R._32 = q2_q3 + q1_q0;
    R._33 = 1 - sq_q1 - sq_q2;

    VR_TRANSFORM_MAT = XMATRIX(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1);
    remapCoordinateSystem(R, AXIS_Y, AXIS_MINUS_X, VR_TRANSFORM_MAT);
    VR_TRANSFORM_MAT = XMATRIX(1,0,0,0, 0,0,1,0, 0,-1,0,0, 0,0,0,1) * VR_TRANSFORM_MAT;

    while (ASensorEventQueue_getEvents(VR_SENSOR_QUEUE, data, 1) > 0);

    return 0;
}

int DestroySensor() {
    ASensorManager* sensor_manager =
            ASensorManager_getInstance();
    if (!sensor_manager) {
        LOGD("**************** Failed to get a sensor manager\n");
        return 1;
    }

    int ret = ASensorEventQueue_disableSensor(VR_SENSOR_QUEUE, VR_SENSOR);
    if (ret < 0) {
        LOGD("**************** Failed to disable %s: %s\n",
             ASensor_getName(VR_SENSOR), strerror(-ret));
    }
    ret = ASensorManager_destroyEventQueue(sensor_manager, VR_SENSOR_QUEUE);
    if (ret < 0) {
        LOGD("**************** Failed to destroy event queue: %s\n", strerror(-ret));
        return 1;
    }

    return 0;
}
