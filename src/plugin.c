/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-input-sdl - plugin.c                                      *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2008-2009 Richard Goedeken                              *
 *   Copyright (C) 2008 Tillin9                                            *
 *   Copyright (C) 2002 Blight                                             *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>

#include <sys/types.h>
#include <sys/stat.h>

#include <SDL.h>

#include "m64p_types.h"
#include "m64p_plugin.h"
#include "m64p_config.h"

#include "plugin.h"
#include "version.h"
#include "osal_dynamiclib.h"

#ifdef __linux__
#include <linux/input.h>
#endif /* __linux__ */

#include <errno.h>

/* defines for the force feedback rumble support */
#ifdef __linux__
#define BITS_PER_LONG (sizeof(long) * 8)
#define OFF(x)  ((x)%BITS_PER_LONG)
#define BIT(x)  (1UL<<OFF(x))
#define LONG(x) ((x)/BITS_PER_LONG)
#define test_bit(bit, array)    ((array[LONG(bit)] >> OFF(bit)) & 1)
#endif //__linux__

/* definitions of pointers to Core config functions */
ptr_ConfigOpenSection      ConfigOpenSection = NULL;
ptr_ConfigSetParameter     ConfigSetParameter = NULL;
ptr_ConfigGetParameter     ConfigGetParameter = NULL;
ptr_ConfigGetParameterHelp ConfigGetParameterHelp = NULL;
ptr_ConfigSetDefaultInt    ConfigSetDefaultInt = NULL;
ptr_ConfigSetDefaultFloat  ConfigSetDefaultFloat = NULL;
ptr_ConfigSetDefaultBool   ConfigSetDefaultBool = NULL;
ptr_ConfigSetDefaultString ConfigSetDefaultString = NULL;
ptr_ConfigGetParamInt      ConfigGetParamInt = NULL;
ptr_ConfigGetParamFloat    ConfigGetParamFloat = NULL;
ptr_ConfigGetParamBool     ConfigGetParamBool = NULL;
ptr_ConfigGetParamString   ConfigGetParamString = NULL;

ptr_ConfigGetSharedDataFilepath ConfigGetSharedDataFilepath = NULL;
ptr_ConfigGetUserConfigPath     ConfigGetUserConfigPath = NULL;
ptr_ConfigGetUserDataPath       ConfigGetUserDataPath = NULL;
ptr_ConfigGetUserCachePath      ConfigGetUserCachePath = NULL;

/* static data definitions */
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;
static int l_PluginInit = 0;

static unsigned short button_bits[] = {
    0x0001,  // R_DPAD
    0x0002,  // L_DPAD
    0x0004,  // D_DPAD
    0x0008,  // U_DPAD
    0x0010,  // START_BUTTON
    0x0020,  // Z_TRIG
    0x0040,  // B_BUTTON
    0x0080,  // A_BUTTON
    0x0100,  // R_CBUTTON
    0x0200,  // L_CBUTTON
    0x0400,  // D_CBUTTON
    0x0800,  // U_CBUTTON
    0x1000,  // R_TRIG
    0x2000,  // L_TRIG
    0x4000,  // Mempak switch
    0x8000   // Rumblepak switch
};

static SController controller[4];   // 4 controllers
static int romopen = 0;         // is a rom opened
static char configdir[PATH_MAX] = {0};  // holds config dir path

static unsigned char myKeyState[SDLK_LAST];

static const char *button_names[] = {
    "DPad R",       // R_DPAD
    "DPad L",       // L_DPAD
    "DPad D",       // D_DPAD
    "DPad U",       // U_DPAD
    "Start",        // START_BUTTON
    "Z Trig",       // Z_TRIG
    "B Button",     // B_BUTTON
    "A Button",     // A_BUTTON
    "C Button R",   // R_CBUTTON
    "C Button L",   // L_CBUTTON
    "C Button D",   // D_CBUTTON
    "C Button U",   // U_CBUTTON
    "R Trig",       // R_TRIG
    "L Trig",       // L_TRIG
    "Mempak switch",
    "Rumblepak switch",
    "Y Axis",       // Y_AXIS
    "X Axis"        // X_AXIS
};

#ifdef __linux__
static struct ff_effect ffeffect[3];
static struct ff_effect ffstrong[3];
static struct ff_effect ffweak[3];
#endif //__linux__

/* Global functions */
void DebugMessage(int level, const char *message, ...)
{
  char msgbuf[1024];
  va_list args;

  if (l_DebugCallback == NULL)
      return;

  va_start(args, message);
  vsprintf(msgbuf, message, args);

  (*l_DebugCallback)(l_DebugCallContext, level, msgbuf);

  va_end(args);
}


/* Mupen64Plus plugin functions */
EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
    if (l_PluginInit)
        return M64ERR_ALREADY_INIT;

    /* first thing is to set the callback function for debug info */
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    /* Get the core config function pointers from the library handle */
    ConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle, "ConfigOpenSection");
    ConfigSetParameter = (ptr_ConfigSetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigSetParameter");
    ConfigGetParameter = (ptr_ConfigGetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParameter");
    ConfigSetDefaultInt = (ptr_ConfigSetDefaultInt) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultInt");
    ConfigSetDefaultFloat = (ptr_ConfigSetDefaultFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultFloat");
    ConfigSetDefaultBool = (ptr_ConfigSetDefaultBool) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultBool");
    ConfigSetDefaultString = (ptr_ConfigSetDefaultString) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultString");
    ConfigGetParamInt = (ptr_ConfigGetParamInt) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamInt");
    ConfigGetParamFloat = (ptr_ConfigGetParamFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamFloat");
    ConfigGetParamBool = (ptr_ConfigGetParamBool) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamBool");
    ConfigGetParamString = (ptr_ConfigGetParamString) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamString");

    ConfigGetSharedDataFilepath = (ptr_ConfigGetSharedDataFilepath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetSharedDataFilepath");
    ConfigGetUserConfigPath = (ptr_ConfigGetUserConfigPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserConfigPath");
    ConfigGetUserDataPath = (ptr_ConfigGetUserDataPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserDataPath");
    ConfigGetUserCachePath = (ptr_ConfigGetUserCachePath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserCachePath");

    if (!ConfigOpenSection || !ConfigSetParameter || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool || !ConfigSetDefaultString ||
        !ConfigGetParamInt   || !ConfigGetParamFloat   || !ConfigGetParamBool   || !ConfigGetParamString ||
        !ConfigGetSharedDataFilepath || !ConfigGetUserConfigPath || !ConfigGetUserDataPath || !ConfigGetUserCachePath)
    {
        DebugMessage(M64MSG_ERROR, "Couldn't connect to Core configuration functions");
        return M64ERR_INCOMPATIBLE;
    }


    l_PluginInit = 1;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    if (!l_PluginInit)
        return M64ERR_NOT_INIT;

    /* reset some local variables */
    l_DebugCallback = NULL;
    l_DebugCallContext = NULL;

    l_PluginInit = 0;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_INPUT;

    if (PluginVersion != NULL)
        *PluginVersion = PLUGIN_VERSION;

    if (APIVersion != NULL)
        *APIVersion = PLUGIN_API_VERSION;
    
    if (PluginNamePtr != NULL)
        *PluginNamePtr = PLUGIN_NAME;

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }
                    
    return M64ERR_SUCCESS;
}

/* static functions */
static int
get_button_num_by_name( const char *name )
{
    int i;

    for( i = 0; i < NUM_BUTTONS; i++ )
        if( !strncasecmp( name, button_names[i], strlen( button_names[i] ) ) )
        {
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "%s, %d: name = %s, button = %d\n", __FILE__, __LINE__, name, i);
#endif
            return i;
        }

#ifdef _DEBUG
    DebugMessage(M64MSG_INFO, "%s, %d: button '%s' unknown\n", __FILE__, __LINE__, name);
#endif
    return -1;
}

static int
get_hat_pos_by_name( const char *name )
{
    if( !strcasecmp( name, "up" ) )
        return SDL_HAT_UP;
    if( !strcasecmp( name, "down" ) )
        return SDL_HAT_DOWN;
    if( !strcasecmp( name, "left" ) )
        return SDL_HAT_LEFT;
    if( !strcasecmp( name, "right" ) )
        return SDL_HAT_RIGHT;
    return -1;
}

static void read_configuration( void )
{
    FILE *f;
    int cont, plugged, plugin, mouse, i, b, dev;
    char line[200], device[200], key_a[200], key_b[200], button_a[200], button_b[200],
             axis[200], axis_a[200], axis_b[200], button[200], hat[200], hat_pos_a[200], hat_pos_b[200], mbutton[200];
    char chAxisDir;
    const char *p;
    char path[PATH_MAX];

    for( i = 0; i < 4; i++ )
    {
        controller[i].device = DEVICE_NONE;
        controller[i].control.Present = 0;
        controller[i].control.RawData = 0;
        controller[i].control.Plugin = PLUGIN_NONE;
        for( b = 0; b < 16; b++ )
        {
            controller[i].button[b].button = -1;
            controller[i].button[b].key = SDLK_UNKNOWN;
            controller[i].button[b].axis = -1;
            controller[i].button[b].hat = -1;
            controller[i].button[b].hat_pos = -1;
            controller[i].button[b].mouse = -1;
        }
        for( b = 0; b < 2; b++ )
        {
            controller[i].axis[b].button_a = controller[i].axis[b].button_b = -1;
            controller[i].axis[b].key_a = controller[i].axis[b].key_a = SDLK_UNKNOWN;
            controller[i].axis[b].axis_a = -1;
            controller[i].axis[b].axis_dir_a = 1;
            controller[i].axis[b].axis_b = -1;
            controller[i].axis[b].axis_dir_b = 1;
            controller[i].axis[b].hat = -1;
            controller[i].axis[b].hat_pos_a = -1;
            controller[i].axis[b].hat_pos_b = -1;
        }
    }

    path[0] = '\0';
    if(strlen(configdir) > 0)
        strncpy(path, configdir, PATH_MAX);
    strncat(path, "blight_input.conf", PATH_MAX - strlen(path));
    f = fopen( path, "r" );
    if( f == NULL )
    {
        DebugMessage(M64MSG_ERROR, "Couldn't open blight_input.conf for reading: %s\n", strerror( errno ) );
        return;
    }
    while( !feof( f ) )
    {
        if( fgets( line, 200, f ) == NULL )
            break;
        if( line[0] == '\n' || line[0] == '\0' )
            continue;
        if( sscanf( line, "[controller %d]", &cont ) == 1 )
            continue;
        if( sscanf( line, "plugged=%d", &plugged ) == 1 )
        {
            controller[cont].control.Present = plugged;
            continue;
        }
        if( sscanf( line, "plugin=%d", &plugin ) == 1 )
        {
            controller[cont].control.Plugin = plugin;
            continue;
        }
        if( sscanf( line, "mouse=%d", &mouse ) == 1 )
        {
            controller[cont].mouse = mouse;
            continue;
        }
        if( sscanf( line, "device=%200s", device ) == 1 )
        {
            dev = DEVICE_NONE;
            if( !strcasecmp( device, "keyboard" ) )
                dev = DEVICE_KEYBOARD;
            else if( sscanf( device, "%d", &i ) == 1 )
                dev = i;
            controller[cont].device = dev;
            continue;
        }
        p = strchr( line, '=' );
        if( p )
        {
            int len = p - line;
            int num;

            strncpy( button, line, len );
            button[len] = '\0';
            p++;

            b = get_button_num_by_name( button );
            if( (b == X_AXIS) || (b == Y_AXIS) )
            {
                num = sscanf( p, "key( %s , %s ); button( %s , %s ); axis( %s , %s ); hat( %s , %s , %s )",
                    key_a, key_b, button_a, button_b, axis_a, axis_b, hat, hat_pos_a, hat_pos_b );

#ifdef _DEBUG
                DebugMessage(M64MSG_INFO, "%s, %d: num = %d, key_a = %s, key_b = %s, button_a = %s, button_b = %s, axis_a = %s, axis_b = %s, hat = %s, hat_pos_a = %s, hat_pos_b = %s\n", __FILE__, __LINE__, num,
                        key_a, key_b, button_a, button_b, axis_a, axis_b, hat, hat_pos_a, hat_pos_b );
#endif
                if( sscanf( key_a, "%d", (int *)&controller[cont].axis[b - Y_AXIS].key_a ) != 1 )
                    controller[cont].axis[b - Y_AXIS].key_a = -1;
                if( sscanf( key_b, "%d", (int *)&controller[cont].axis[b - Y_AXIS].key_b ) != 1 )
                    controller[cont].axis[b - Y_AXIS].key_b = -1;
                if( sscanf( button_a, "%d", &controller[cont].axis[b - Y_AXIS].button_a ) != 1 )
                    controller[cont].axis[b - Y_AXIS].button_a = -1;
                if( sscanf( button_b, "%d", &controller[cont].axis[b - Y_AXIS].button_b ) != 1 )
                    controller[cont].axis[b - Y_AXIS].button_b = -1;
                num = sscanf( axis_a, "%d%c", &controller[cont].axis[b - Y_AXIS].axis_a, &chAxisDir );
                if( num != 2 )
                {
                    controller[cont].axis[b - Y_AXIS].axis_a = -1;
                    controller[cont].axis[b - Y_AXIS].axis_dir_a = 0;
                }
                else
                {
                    if( chAxisDir == '+' )
                        controller[cont].axis[b - Y_AXIS].axis_dir_a = 1;
                    else if( chAxisDir == '-' )
                        controller[cont].axis[b - Y_AXIS].axis_dir_a = -1;
                    else
                        controller[cont].axis[b - Y_AXIS].axis_dir_a = 0;
                }
              
                num = sscanf( axis_b, "%d%c", &controller[cont].axis[b - Y_AXIS].axis_b, &chAxisDir);
                if( num != 2 )
                {
                    controller[cont].axis[b - Y_AXIS].axis_b = -1;
                    controller[cont].axis[b - Y_AXIS].axis_dir_b = 0;
                }
                else
                {
                    if( chAxisDir == '+' )
                        controller[cont].axis[b - Y_AXIS].axis_dir_b = 1;
                    else if( chAxisDir == '-' )
                        controller[cont].axis[b - Y_AXIS].axis_dir_b = -1;
                    else
                        controller[cont].axis[b - Y_AXIS].axis_dir_b = 0;
                }
                if( sscanf( hat, "%d", &controller[cont].axis[b - Y_AXIS].hat ) != 1 )
                    controller[cont].axis[b - Y_AXIS].hat = -1;
                controller[cont].axis[b - Y_AXIS].hat_pos_a = get_hat_pos_by_name( hat_pos_a );
                controller[cont].axis[b - Y_AXIS].hat_pos_b = get_hat_pos_by_name( hat_pos_b );
            }
            else
            {
                num = sscanf( p, "key( %s ); button( %s ); axis( %s ); hat( %s , %s ); mouse( %s )",
                        key_a,
                        button_a,
                        axis,
                        hat,
                        hat_pos_a,
                        mbutton );
#ifdef _DEBUG
                DebugMessage(M64MSG_INFO, "%s, %d: num = %d, key = %s, button = %s, axis = %s, hat = %s, hat_pos = %s, mbutton = %s\n", __FILE__, __LINE__, num, key_a, button_a, axis, hat, hat_pos_a, mbutton );
#endif
                num = sscanf( axis, "%d%c", &controller[cont].button[b].axis, &chAxisDir );
                if( num != 2 )
                {
                    controller[cont].button[b].axis = -1;
                    controller[cont].button[b].axis_dir = 0;
                }
                else
                {
                    if( chAxisDir == '+' )
                        controller[cont].button[b].axis_dir = 1;
                    else if( chAxisDir == '-' )
                        controller[cont].button[b].axis_dir = -1;
                    else
                        controller[cont].button[b].axis_dir = 0;
                }
                if( sscanf( key_a, "%d", (int *)&controller[cont].button[b].key ) != 1 )
                    controller[cont].button[b].key = -1;
                if( sscanf( button_a, "%d", &controller[cont].button[b].button ) != 1 )
                    controller[cont].button[b].button = -1;
                if( sscanf( hat, "%d", &controller[cont].button[b].hat ) != 1 )
                    controller[cont].button[b].hat = -1;
                controller[cont].button[b].hat_pos = get_hat_pos_by_name( hat_pos_a );
                if( sscanf( mbutton, "%d", &controller[cont].button[b].mouse ) != 1 )
                    controller[cont].button[b].mouse = -1;
            }
            continue;
        }
        DebugMessage(M64MSG_WARNING, "Unknown config line: %s", line );
    }
    fclose( f );
}

#define HAT_POS_NAME( hat )         \
       ((hat == SDL_HAT_UP) ? "Up" :        \
       ((hat == SDL_HAT_DOWN) ? "Down" :    \
       ((hat == SDL_HAT_LEFT) ? "Left" :    \
       ((hat == SDL_HAT_RIGHT) ? "Right" :  \
         "None"))))

static int write_configuration( void )
{
    FILE *f;
    int i, b;
    char cKey_a[100], cKey_b[100];
    char cButton_a[100], cButton_b[100], cAxis[100], cAxis_a[100], cAxis_b[100];
    char cHat[100];
    char cMouse[100];
    char path[PATH_MAX];

    path[0] = '\0';
    if(strlen(configdir) > 0)
        strncpy(path, configdir, PATH_MAX);
    strncat(path, "blight_input.conf", PATH_MAX - strlen(path));
    f = fopen( path, "w" );
    if (f == NULL)
    {
        DebugMessage(M64MSG_ERROR, "Couldn't open blight_input.conf for writing: %s", strerror(errno));
        return -1;
    }

    for( i = 0; i < 4; i++ )
    {
        fprintf( f, "[controller %d]\n", i );
        fprintf( f, "plugged=%d\n", controller[i].control.Present );
        fprintf( f, "plugin=%d\n", controller[i].control.Plugin );
        fprintf( f, "mouse=%d\n", controller[i].mouse );
        if( controller[i].device == DEVICE_KEYBOARD )
            fprintf( f, "device=Keyboard\n" );
        else if( controller[i].device >= 0 )
            fprintf( f, "device=%d\n", controller[i].device );
        else
            fprintf( f, "device=None\n" );

        for( b = 0; b < 16; b++ )
        {
//          cKey_a = (controller[i].button[b].key == SDLK_UNKNOWN) ? "None" : SDL_GetKeyName( controller[i].button[b].key );
            if( controller[i].button[b].key >= 0 )
                sprintf( cKey_a, "%d", controller[i].button[b].key );
            else
                strcpy( cButton_a, "None" );

            if( controller[i].button[b].button >= 0 )
                sprintf( cButton_a, "%d", controller[i].button[b].button );
            else
                strcpy( cButton_a, "None" );

            if( controller[i].button[b].axis >= 0 )
                sprintf( cAxis, "%d%c", controller[i].button[b].axis, (controller[i].button[b].axis_dir == -1) ? '-' : '+' );
            else
                strcpy( cAxis, "None" );

            if( controller[i].button[b].hat >= 0 )
                sprintf( cHat, "%d", controller[i].button[b].hat );
            else
                strcpy( cHat, "None" );

            if( controller[i].button[b].mouse >= 0 )
                sprintf( cMouse, "%d", controller[i].button[b].mouse );
            else
                strcpy( cMouse, "None" );

            fprintf( f, "%s=key( %s ); button( %s ); axis( %s ); hat( %s , %s ); mouse( %s )\n", button_names[b],
                    cKey_a, cButton_a, cAxis, cHat, HAT_POS_NAME(controller[i].button[b].hat_pos), cMouse );
        }
        for( b = 0; b < 2; b++ )
        {
//          cKey_a = (controller[i].axis[b].key_a == SDLK_UNKNOWN) ? "None" : SDL_GetKeyName( controller[i].axis[b].key_a );
//          cKey_b = (controller[i].axis[b].key_b == SDLK_UNKNOWN) ? "None" : SDL_GetKeyName( controller[i].axis[b].key_b );
            if( controller[i].axis[b].key_a >= 0 )
                sprintf( cKey_a, "%d", controller[i].axis[b].key_a );
            else
                strcpy( cKey_a, "None" );
            if( controller[i].axis[b].key_b >= 0 )
                sprintf( cKey_b, "%d", controller[i].axis[b].key_b );
            else
                strcpy( cKey_b, "None" );

            if( controller[i].axis[b].button_a >= 0 )
                sprintf( cButton_a, "%d", controller[i].axis[b].button_a );
            else
                strcpy( cButton_a, "None" );

            if( controller[i].axis[b].button_b >= 0 )
                sprintf( cButton_b, "%d", controller[i].axis[b].button_b );
            else
                strcpy( cButton_b, "None" );

            if( controller[i].axis[b].axis_a >= 0 )
                sprintf( cAxis_a, "%d%c", controller[i].axis[b].axis_a, (controller[i].axis[b].axis_dir_a <= 0) ? '-' : '+' );
            else
                strcpy( cAxis_a, "None" );
                
            if( controller[i].axis[b].axis_b >= 0 )
                sprintf( cAxis_b, "%d%c", controller[i].axis[b].axis_b, (controller[i].axis[b].axis_dir_b <= 0) ? '-' : '+' );
            else
                strcpy( cAxis_b, "None" );
           
            if( controller[i].axis[b].hat >= 0 )
                sprintf( cHat, "%d", controller[i].axis[b].hat );
            else
                strcpy( cHat, "None" );

            fprintf( f, "%s=key( %s , %s ); button( %s , %s ); axis( %s , %s ); hat( %s , %s , %s )\n", button_names[b+16],
                        cKey_a, cKey_b, cButton_a, cButton_b, cAxis_a, cAxis_b, cHat, HAT_POS_NAME(controller[i].axis[b].hat_pos_a), HAT_POS_NAME(controller[i].axis[b].hat_pos_b) );
        }
        fprintf( f, "\n" );
    }

    fclose( f );
    return 0;
}

/* Helper function to handle the SDL keys */
static void
doSdlKeys(unsigned char* keystate)
{
    int c, b, axis_val, axis_max_val;
    int grabmouse = -1;

    axis_max_val = 80;
    if (keystate[SDLK_LCTRL])
        axis_max_val -= 40;
    if (keystate[SDLK_LSHIFT])
        axis_max_val -= 20;

    for( c = 0; c < 4; c++ )
    {
        for( b = 0; b < 16; b++ )
        {
            if( controller[c].button[b].key == SDLK_UNKNOWN || ((int) controller[c].button[b].key) < 0)
                continue;
            if( keystate[controller[c].button[b].key] )
                controller[c].buttons.Value |= button_bits[b];
        }
        for( b = 0; b < 2; b++ )
        {
            // from the N64 func ref: The 3D Stick data is of type signed char and in
            // the range between 80 and -80. (32768 / 409 = ~80.1)
            if( b == 0 )
                axis_val = controller[c].buttons.X_AXIS;
            else
                axis_val = -controller[c].buttons.Y_AXIS;

            if( controller[c].axis[b].key_a != SDLK_UNKNOWN && ((int) controller[c].axis[b].key_a) > 0)
                if( keystate[controller[c].axis[b].key_a] )
                    axis_val = axis_max_val;
            if( controller[c].axis[b].key_b != SDLK_UNKNOWN && ((int) controller[c].axis[b].key_b) > 0)
                if( keystate[controller[c].axis[b].key_b] )
                    axis_val = -axis_max_val;

            if( b == 0 )
                controller[c].buttons.X_AXIS = axis_val;
            else
                controller[c].buttons.Y_AXIS = -axis_val;
        }
        if (controller[c].mouse)
        {
            if (keystate[SDLK_LCTRL] && keystate[SDLK_LALT])
            {
                grabmouse = 0;
            }
            if (grabmouse >= 0)
            {
                // grab/ungrab mouse
                SDL_WM_GrabInput( grabmouse ? SDL_GRAB_ON : SDL_GRAB_OFF );
                SDL_ShowCursor( grabmouse ? 0 : 1 );
            }
        }
    }
}

static unsigned char DataCRC( unsigned char *Data, int iLenght )
{
    unsigned char Remainder = Data[0];

    int iByte = 1;
    unsigned char bBit = 0;

    while( iByte <= iLenght )
    {
        int HighBit = ((Remainder & 0x80) != 0);
        Remainder = Remainder << 1;

        Remainder += ( iByte < iLenght && Data[iByte] & (0x80 >> bBit )) ? 1 : 0;

        Remainder ^= (HighBit) ? 0x85 : 0;

        bBit++;
        iByte += bBit/8;
        bBit %= 8;
    }

    return Remainder;
}

/******************************************************************
  Function: ControllerCommand
  Purpose:  To process the raw data that has just been sent to a
            specific controller.
  input:    - Controller Number (0 to 3) and -1 signalling end of
              processing the pif ram.
            - Pointer of data to be processed.
  output:   none

  note:     This function is only needed if the DLL is allowing raw
            data, or the plugin is set to raw

            the data that is being processed looks like this:
            initilize controller: 01 03 00 FF FF FF
            read controller:      01 04 01 FF FF FF FF
*******************************************************************/
EXPORT void CALL ControllerCommand(int Control, unsigned char *Command)
{
    unsigned char *Data = &Command[5];

    if (Control == -1)
        return;

    switch (Command[2])
    {
        case RD_GETSTATUS:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Get status");
#endif
            break;
        case RD_READKEYS:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Read keys");
#endif
            break;
        case RD_READPAK:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Read pak");
#endif
            if (controller[Control].control.Plugin == PLUGIN_RAW)
            {
                unsigned int dwAddress = (Command[3] << 8) + (Command[4] & 0xE0);

                if(( dwAddress >= 0x8000 ) && ( dwAddress < 0x9000 ) )
                    memset( Data, 0x80, 32 );
                else
                    memset( Data, 0x00, 32 );

                Data[32] = DataCRC( Data, 32 );
                break;
                }
        case RD_WRITEPAK:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Write pak");
#endif
            if (controller[Control].control.Plugin == PLUGIN_RAW)
            {
                unsigned int dwAddress = (Command[3] << 8) + (Command[4] & 0xE0);
              if (dwAddress == PAK_IO_RUMBLE && *Data)
                    DebugMessage(M64MSG_VERBOSE, "Triggering rumble pack.");
#ifdef __linux__
                struct input_event play;
                if( dwAddress == PAK_IO_RUMBLE && controller[Control].event_joystick != 0)
                {
                    if( *Data )
                    {
                        play.type = EV_FF;
                        play.code = ffeffect[Control].id;
                        play.value = 1;

                        if (write(controller[Control].event_joystick, (const void*) &play, sizeof(play)) == -1)
                            perror("Error starting rumble effect");

                    }
                    else
                    {
                        play.type = EV_FF;
                        play.code = ffeffect[Control].id;
                        play.value = 0;

                        if (write(controller[Control].event_joystick, (const void*) &play, sizeof(play)) == -1)
                            perror("Error stopping rumble effect");
                    }
                }
#endif //__linux__
                Data[32] = DataCRC( Data, 32 );
            }
            break;
        case RD_RESETCONTROLLER:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Reset controller");
#endif
            break;
        case RD_READEEPROM:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Read eeprom");
#endif
            break;
        case RD_WRITEEPROM:
#ifdef _DEBUG
            DebugMessage(M64MSG_INFO, "Write eeprom");
#endif
            break;
        }
}

/******************************************************************
  Function: GetKeys
  Purpose:  To get the current state of the controllers buttons.
  input:    - Controller Number (0 to 3)
            - A pointer to a BUTTONS structure to be filled with
            the controller state.
  output:   none
*******************************************************************/
EXPORT void CALL GetKeys( int Control, BUTTONS *Keys )
{
    int b, axis_val, axis_val_tmp;
    SDL_Event event;

    // Handle keyboard input first
    doSdlKeys( SDL_GetKeyState( NULL ) );
    doSdlKeys( myKeyState );

    // read joystick state
    SDL_JoystickUpdate();

    if( controller[Control].device >= 0 )
    {
        for( b = 0; b < 16; b++ )
        {
            if( controller[Control].button[b].button >= 0 )
                if( SDL_JoystickGetButton( controller[Control].joystick, controller[Control].button[b].button ) )
                    controller[Control].buttons.Value |= button_bits[b];

            if( controller[Control].button[b].axis >= 0 )
            {
                axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].button[b].axis );
                if( (controller[Control].button[b].axis_dir < 0) && (axis_val <= -6000) )
                    controller[Control].buttons.Value |= button_bits[b];
                else if( (controller[Control].button[b].axis_dir > 0) && (axis_val >= 6000) )
                    controller[Control].buttons.Value |= button_bits[b];
            }

            if( controller[Control].button[b].hat >= 0 )
            {
                if( controller[Control].button[b].hat_pos > 0 )
                    if( SDL_JoystickGetHat( controller[Control].joystick, controller[Control].button[b].hat ) & controller[Control].button[b].hat_pos )
                        controller[Control].buttons.Value |= button_bits[b];
            }
        }
        for( b = 0; b < 2; b++ )
        {
            // from the N64 func ref: The 3D Stick data is of type signed char and in
            // the range between 80 and -80. (32768 / 409 = ~80.1)
            axis_val = 0;
            axis_val_tmp = 0;
            
            
            if( controller[Control].axis[b].axis_a >= 0 )
            {
                axis_val_tmp = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_a );
                // if you push a positive axis... and your directions are flipped...
                if( (controller[Control].axis[b].axis_dir_a < 0) && (axis_val_tmp <= -6000) )
                {
                    if (b == 0)
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_a ) / -409;
                    }
                    else
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_a ) / 409;
                    }
                }
                else if( (controller[Control].axis[b].axis_dir_a > 0) && (axis_val_tmp >= 6000) )
                {
                    if (b == 1)
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_a ) / -409;
                    }
                    else
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_a ) / 409;
                    }
                }
            }
            // up and left
            if( controller[Control].axis[b].axis_b >= 0 )
            {
                axis_val_tmp = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_b );
                // if you push a positive axis... and your directions are flipped...
                if( (controller[Control].axis[b].axis_dir_b < 0) && (axis_val_tmp <= -6000) )
                {
                    if (b == 1)
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_b ) / -409;
                    }
                    else
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_b ) / 409;
                    }
                }
                else if( (controller[Control].axis[b].axis_dir_b > 0) && (axis_val_tmp >= 6000) )
                {
                    if (b == 0)
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_b ) / -409;
                    }
                    else
                    {
                        axis_val = SDL_JoystickGetAxis( controller[Control].joystick, controller[Control].axis[b].axis_b ) / 409;
                    }
                }
            }
            if( controller[Control].axis[b].hat >= 0 )
            {
                if( controller[Control].axis[b].hat_pos_a >= 0 )
                    if( SDL_JoystickGetHat( controller[Control].joystick, controller[Control].axis[b].hat ) & controller[Control].axis[b].hat_pos_a )
                        axis_val = 80;
                if( controller[Control].axis[b].hat_pos_b >= 0 )
                    if( SDL_JoystickGetHat( controller[Control].joystick, controller[Control].axis[b].hat ) & controller[Control].axis[b].hat_pos_b )
                        axis_val = -80;
            }

            if( controller[Control].axis[b].button_a >= 0 )
                if( SDL_JoystickGetButton( controller[Control].joystick, controller[Control].axis[b].button_a ) )
                    axis_val = 80;
            if( controller[Control].axis[b].button_b >= 0 )
                if( SDL_JoystickGetButton( controller[Control].joystick, controller[Control].axis[b].button_b ) )
                    axis_val = -80;

            if( b == 0 )
                controller[Control].buttons.X_AXIS = axis_val;
            else
                controller[Control].buttons.Y_AXIS = axis_val;
        }
    }

    // process mouse events
    {
        unsigned char mstate = SDL_GetMouseState( NULL, NULL );

        for( b = 0; b < 16; b++ )
        {
            if( controller[Control].button[b].mouse < 1 )
                continue;
            if( mstate & SDL_BUTTON(controller[Control].button[b].mouse) )
                controller[Control].buttons.Value |= button_bits[b];
        }
    }

    if (controller[Control].mouse)
    {
        int grabmouse = -1;
        while (SDL_PollEvent(&event))
        {
            if (event.type == SDL_MOUSEMOTION && SDL_WM_GrabInput( SDL_GRAB_QUERY ) == SDL_GRAB_ON)
            {
                if (event.motion.xrel)
                {
                    axis_val = (event.motion.xrel * 10);
                    if (axis_val < -80)
                        axis_val = -80;
                    else if (axis_val > 80)
                        axis_val = 80;
                    controller[Control].buttons.Y_AXIS = axis_val;
                }
                if (event.motion.yrel)
                {
                    axis_val = (event.motion.yrel * 10);
                    if (axis_val < -80)
                        axis_val = -80;
                    else if (axis_val > 80)
                        axis_val = 80;
                    controller[Control].buttons.X_AXIS = -axis_val;
                }
            }
            else if (event.type == SDL_MOUSEBUTTONUP)
            {
                if (event.button.button == SDL_BUTTON_LEFT)
                {
                    grabmouse = 1;
                }
            }
        }
    }

#ifdef _DEBUG
    DebugMessage(M64MSG_INFO, "Controller #%d value: 0x%8.8X\n", Control, *(int *)&controller[Control].buttons );
#endif
    *Keys = controller[Control].buttons;

    /* handle mempack / rumblepak switching (only if rumble is active on joystick) */
#ifdef __linux__
    if (controller[Control].event_joystick != 0)
    {
        struct input_event play;
        if (controller[Control].buttons.Value & button_bits[14])
        {
            controller[Control].control.Plugin = PLUGIN_MEMPAK;
            play.type = EV_FF;
            play.code = ffweak[Control].id;
            play.value = 1;
            if (write(controller[Control].event_joystick, (const void*) &play, sizeof(play)) == -1)
                perror("Error starting rumble effect");
        }
        if (controller[Control].buttons.Value & button_bits[15])
        {
            controller[Control].control.Plugin = PLUGIN_RAW;
            play.type = EV_FF;
            play.code = ffstrong[Control].id;
            play.value = 1;
            if (write(controller[Control].event_joystick, (const void*) &play, sizeof(play)) == -1)
                perror("Error starting rumble effect");
        }
    }
#endif /* __linux__ */

    controller[Control].buttons.Value = 0;
    //controller[Control].buttons.stick_x = 0;
    //controller[Control].buttons.stick_y = 0;
}

static void InitiateRumble(int cntrl)
{
#ifdef __linux__
    DIR* dp;
    struct dirent* ep;
    unsigned long features[4];
    char temp[128];
    char temp2[128];
    int iFound = 0;

    controller[cntrl].event_joystick = 0;

    sprintf(temp,"/sys/class/input/js%d/device", controller[cntrl].device);
    dp = opendir(temp);

    if(dp==NULL)
        return;

    while ((ep=readdir(dp)))
        {
        if (strncmp(ep->d_name, "event",5)==0)
            {
            sprintf(temp, "/dev/input/%s", ep->d_name);
            iFound = 1;
            break;
            }
        else if(strncmp(ep->d_name,"input:event", 11)==0)
            {
            sscanf(ep->d_name, "input:%s", temp2);
            sprintf(temp, "/dev/input/%s", temp2);
            iFound = 1;
            break;
            }
        else if(strncmp(ep->d_name,"input:input", 11)==0)
            {
            strcat(temp, "/");
            strcat(temp, ep->d_name);
            closedir (dp);
            dp = opendir(temp);
            if(dp==NULL)
                return;
            }
       }

    closedir(dp);

    if (!iFound)
    {
        DebugMessage(M64MSG_WARNING, "Couldn't find input event for rumble support.");
        return;
    }

    controller[cntrl].event_joystick = open(temp, O_RDWR);
    if(controller[cntrl].event_joystick==-1)
        {
        DebugMessage(M64MSG_WARNING, "Couldn't open device file '%s' for rumble support.", temp);
        controller[cntrl].event_joystick = 0;
        return;
        }

    if(ioctl(controller[cntrl].event_joystick, EVIOCGBIT(EV_FF, sizeof(unsigned long) * 4), features)==-1)
        {
        DebugMessage(M64MSG_WARNING, "Linux kernel communication failed for force feedback (rumble).\n");
        controller[cntrl].event_joystick = 0;
        return;
        }

    if(!test_bit(FF_RUMBLE, features))
        {
        DebugMessage(M64MSG_WARNING, "No rumble supported on N64 joystick #%i", cntrl + 1);
        controller[cntrl].event_joystick = 0;
        return;
        }

    ffeffect[cntrl].type = FF_RUMBLE;
    ffeffect[cntrl].id = -1;
    ffeffect[cntrl].u.rumble.strong_magnitude = 0xFFFF;
    ffeffect[cntrl].u.rumble.weak_magnitude = 0xFFFF;

    ioctl(controller[cntrl].event_joystick, EVIOCSFF, &ffeffect[cntrl]);

    ffstrong[cntrl].type = FF_RUMBLE;
    ffstrong[cntrl].id = -1;
    ffstrong[cntrl].u.rumble.strong_magnitude = 0xFFFF;
    ffstrong[cntrl].u.rumble.weak_magnitude = 0x0000;
    ffstrong[cntrl].replay.length = 500;
    ffstrong[cntrl].replay.delay = 0;

    ioctl(controller[cntrl].event_joystick, EVIOCSFF, &ffstrong[cntrl]);

    ffweak[cntrl].type = FF_RUMBLE;
    ffweak[cntrl].id = -1;
    ffweak[cntrl].u.rumble.strong_magnitude = 0x0000;
    ffweak[cntrl].u.rumble.weak_magnitude = 0xFFFF;
    ffweak[cntrl].replay.length = 500;
    ffweak[cntrl].replay.delay = 0;

    ioctl(controller[cntrl].event_joystick, EVIOCSFF, &ffweak[cntrl]);

    DebugMessage(M64MSG_INFO, "Rumble activated on N64 joystick #%i", cntrl + 1);
#endif /* __linux__ */
}

/******************************************************************
  Function: InitiateControllers
  Purpose:  This function initialises how each of the controllers
            should be handled.
  input:    - The handle to the main window.
            - A controller structure that needs to be filled for
              the emulator to know how to handle each controller.
  output:   none
*******************************************************************/
EXPORT void CALL InitiateControllers(CONTROL_INFO ControlInfo)
{
    int i;

    // reset controllers
    memset( controller, 0, sizeof( SController ) * 4 );

    for ( i = 0; i < SDLK_LAST; i++)
    {
        myKeyState[i] = 0;
    }

    // read configuration
    read_configuration();

    for( i = 0; i < 4; i++ )
    {
        // test for rumble support for this joystick
        InitiateRumble(i);
        // if rumble not supported, switch to mempack
        // Comment out if statement to test rumble on systems without necessary hardware.
        if (controller[i].control.Plugin == PLUGIN_RAW && controller[i].event_joystick == 0)
            controller[i].control.Plugin = PLUGIN_MEMPAK;
        // copy control data struct to the core
        memcpy( ControlInfo.Controls + i, &controller[i].control, sizeof( CONTROL ) );
    }

    DebugMessage(M64MSG_INFO, "%s version %i.%i.%i initialized.", PLUGIN_NAME, VERSION_PRINTF_SPLIT(PLUGIN_VERSION));
}

/******************************************************************
  Function: ReadController
  Purpose:  To process the raw data in the pif ram that is about to
            be read.
  input:    - Controller Number (0 to 3) and -1 signalling end of
              processing the pif ram.
            - Pointer of data to be processed.
  output:   none
  note:     This function is only needed if the DLL is allowing raw
            data.
*******************************************************************/
EXPORT void CALL ReadController(int Control, unsigned char *Command)
{
#ifdef _DEBUG
    DebugMessage(M64MSG_INFO, "Raw Read (cont=%d):  %02X %02X %02X %02X %02X %02X", Control,
                 Command[0], Command[1], Command[2], Command[3], Command[4], Command[5]);
#endif
}

/******************************************************************
  Function: RomClosed
  Purpose:  This function is called when a rom is closed.
  input:    none
  output:   none
*******************************************************************/
EXPORT void CALL RomClosed(void)
{
    int i;

    // close joysticks
    for( i = 0; i < 4; i++ )
        if( controller[i].joystick )
        {
            SDL_JoystickClose( controller[i].joystick );
            controller[i].joystick = NULL;
        }

    // quit SDL joystick subsystem
    SDL_QuitSubSystem( SDL_INIT_JOYSTICK );

    // release/ungrab mouse
    SDL_WM_GrabInput( SDL_GRAB_OFF );
    SDL_ShowCursor( 1 );

    romopen = 0;
}

/******************************************************************
  Function: RomOpen
  Purpose:  This function is called when a rom is open. (from the
            emulation thread)
  input:    none
  output:   none
*******************************************************************/
EXPORT void CALL RomOpen(void)
{
    int i;

    // init SDL joystick subsystem
    if( !SDL_WasInit( SDL_INIT_JOYSTICK ) )
        if( SDL_InitSubSystem( SDL_INIT_JOYSTICK ) == -1 )
        {
            DebugMessag(M64MSG_ERROR, "Couldn't init SDL joystick subsystem: %s", SDL_GetError() );
            return;
        }

    // open joysticks
    for( i = 0; i < 4; i++ )
        if( controller[i].device >= 0 )
        {
            controller[i].joystick = SDL_JoystickOpen( controller[i].device );
            if( controller[i].joystick == NULL )
                DebugMessage(M64MSG_WARNING, "Couldn't open joystick for controller #%d: %s", i + 1, SDL_GetError() );
        }
        else
            controller[i].joystick = NULL;

    // grab mouse
    if (controller[0].mouse || controller[1].mouse || controller[2].mouse || controller[3].mouse)
    {
        SDL_ShowCursor( 0 );
        if (SDL_WM_GrabInput( SDL_GRAB_ON ) != SDL_GRAB_ON)
        {
            DebugMessage(M64MSG_WARNING, "Couldn't grab input! Mouse support won't work!");
        }
    }

    romopen = 1;
}

/******************************************************************
  Function: SDL_KeyDown
  Purpose:  To pass the SDL_KeyDown message from the emulator to the
            plugin.
  input:    keymod and keysym of the SDL_KEYDOWN message.
  output:   none
*******************************************************************/
EXPORT void CALL SDL_KeyDown(int keymod, int keysym)
{
    myKeyState[keysym] = 1; /* fixme */
}

/******************************************************************
  Function: SDL_KeyUp
  Purpose:  To pass the SDL_KeyUp message from the emulator to the
            plugin.
  input:    keymod and keysym of the SDL_KEYUP message.
  output:   none
*******************************************************************/
EXPORT void CALL SDL_KeyUp(int keymod, int keysym)
{
    myKeyState[keysym] = 0; /* fixme */
}

