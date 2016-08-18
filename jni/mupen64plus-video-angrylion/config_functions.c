#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdarg.h>
#include "m64p_types.h"
#include "m64p_config.h"
#include "m64p_vidext.h"
#include <stdlib.h>
#include "osal_dynamiclib.h"

extern void DebugMessage(int level, const char *message, ...);
static m64p_handle l_ConfigAngrylion;

/* definitions of pointers to Core config functions */
ptr_ConfigOpenSection      myConfigOpenSection = NULL;
ptr_ConfigSetParameter     myConfigSetParameter = NULL;
ptr_ConfigGetParameter     myConfigGetParameter = NULL;
ptr_ConfigGetParameterHelp myConfigGetParameterHelp = NULL;
ptr_ConfigSetDefaultInt    myConfigSetDefaultInt = NULL;
ptr_ConfigSetDefaultFloat  myConfigSetDefaultFloat = NULL;
ptr_ConfigSetDefaultBool   myConfigSetDefaultBool = NULL;
ptr_ConfigSetDefaultString myConfigSetDefaultString = NULL;
ptr_ConfigGetParamInt      myConfigGetParamInt = NULL;
ptr_ConfigGetParamFloat    myConfigGetParamFloat = NULL;
ptr_ConfigGetParamBool     myConfigGetParamBool = NULL;
ptr_ConfigGetParamString   myConfigGetParamString = NULL;

ptr_ConfigGetSharedDataFilepath myConfigGetSharedDataFilepath = NULL;
ptr_ConfigGetUserConfigPath     myConfigGetUserConfigPath = NULL;
ptr_ConfigGetUserDataPath       myConfigGetUserDataPath = NULL;
ptr_ConfigGetUserCachePath      myConfigGetUserCachePath = NULL;

void InitializeConfigFunctions(m64p_dynlib_handle CoreLibHandle)
{
   myConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle, "ConfigOpenSection");
   myConfigSetParameter = (ptr_ConfigSetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigSetParameter");
   myConfigGetParameter = (ptr_ConfigGetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParameter");
   myConfigSetDefaultInt = (ptr_ConfigSetDefaultInt) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultInt");
   myConfigSetDefaultFloat = (ptr_ConfigSetDefaultFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultFloat");
   myConfigSetDefaultBool = (ptr_ConfigSetDefaultBool) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultBool");
   myConfigSetDefaultString = (ptr_ConfigSetDefaultString) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultString");
   myConfigGetParamInt = (ptr_ConfigGetParamInt) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamInt");
   myConfigGetParamFloat = (ptr_ConfigGetParamFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamFloat");
   myConfigGetParamBool = (ptr_ConfigGetParamBool) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamBool");
   myConfigGetParamString = (ptr_ConfigGetParamString) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamString");
   myConfigGetSharedDataFilepath = (ptr_ConfigGetSharedDataFilepath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetSharedDataFilepath");
   myConfigGetUserConfigPath = (ptr_ConfigGetUserConfigPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserConfigPath");
   myConfigGetUserDataPath = (ptr_ConfigGetUserDataPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserDataPath");
   myConfigGetUserCachePath = (ptr_ConfigGetUserCachePath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserCachePath");

   /* get a configuration section handle */
   if (ConfigOpenSection("Video-Angrylion", &l_ConfigAngrylion) != M64ERR_SUCCESS)
   {
       DebugMessage(M64MSG_ERROR, "Couldn't open config section 'rsp-cxd4'");
   }

   ConfigSetDefaultBool(l_ConfigAngrylion, "VIOverlay", 0, "Enable VI overlay filter");
}

//Ignore the handle, we have our own
EXPORT m64p_error CALL ConfigOpenSection(const char * string, m64p_handle * handle)
{
   return myConfigOpenSection(string, &l_ConfigAngrylion);
}

//Ignore the handle, we have our own
EXPORT int CALL ConfigGetParamBool(m64p_handle handle, const char * string)
{
   return myConfigGetParamBool(l_ConfigAngrylion, string);
}

//Ignore the handle, we have our own
EXPORT m64p_error CALL ConfigSetDefaultBool(m64p_handle handle, const char * param, int defaultValue, const char * description)
{
   return myConfigSetDefaultBool(l_ConfigAngrylion, param, defaultValue, description);
}
