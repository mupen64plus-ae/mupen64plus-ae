/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-input-sdl - config.c                                      *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2009 Richard Goedeken                                   *
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

#include <SDL.h>

#include "m64p_types.h"
#include "m64p_plugin.h"
#include "m64p_config.h"

#include "plugin.h"

#define HAT_POS_NAME( hat )         \
       ((hat == SDL_HAT_UP) ? "Up" :        \
       ((hat == SDL_HAT_DOWN) ? "Down" :    \
       ((hat == SDL_HAT_LEFT) ? "Left" :    \
       ((hat == SDL_HAT_RIGHT) ? "Right" :  \
         "None"))))


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
    "X Axis",       // X_AXIS
    "Y Axis"        // Y_AXIS
};

/* definitions for joystick auto-configuration */
enum eJoyType
{
    KBD_DEFAULT = 1,
    JOY_GREEN_ASIA_USB,
    JOY_LOGITECH_CORDLESS_RUMBLEPAD_2,
    JOY_LOGITECH_DUAL_ACTION,
    JOY_MEGA_WORLD_USB,
    JOY_MICROSOFT_XBOX_360,
    JOY_N64_CONTROLLER,
    JOY_SAITEK_P880
};

typedef struct
{
    const char    *name;
    enum eJoyType  type;
} sJoyConfigMap;

static sJoyConfigMap l_JoyConfigMap[] = {
    { "GreenAsia Inc. USB Joystick",     JOY_GREEN_ASIA_USB},
    { "Logitech Cordless Rumblepad 2",   JOY_LOGITECH_CORDLESS_RUMBLEPAD_2},
    { "Logitech Dual Action",            JOY_LOGITECH_DUAL_ACTION},
    { "Mega World USB Game Controllers", JOY_MEGA_WORLD_USB},
    { "Microsoft X-Box 360 pad",         JOY_MICROSOFT_XBOX_360},
    { "N64 controller",                  JOY_N64_CONTROLLER},
    { "SAITEK P880",                     JOY_SAITEK_P880}
};


/* static functions */
static int get_hat_pos_by_name( const char *name )
{
    if( !strcasecmp( name, "up" ) )
        return SDL_HAT_UP;
    if( !strcasecmp( name, "down" ) )
        return SDL_HAT_DOWN;
    if( !strcasecmp( name, "left" ) )
        return SDL_HAT_LEFT;
    if( !strcasecmp( name, "right" ) )
        return SDL_HAT_RIGHT;
    DebugMessage(M64MSG_WARNING, "get_hat_pos_by_name(): direction '%s' unknown", name);
    return -1;
}

static void clear_controller(int iCtrlIdx)
{
    int b;

    controller[iCtrlIdx].device = DEVICE_NONE;
    controller[iCtrlIdx].control.Present = 0;
    controller[iCtrlIdx].control.RawData = 0;
    controller[iCtrlIdx].control.Plugin = PLUGIN_NONE;
    for( b = 0; b < 16; b++ )
    {
        controller[iCtrlIdx].button[b].button = -1;
        controller[iCtrlIdx].button[b].key = SDLK_UNKNOWN;
        controller[iCtrlIdx].button[b].axis = -1;
        controller[iCtrlIdx].button[b].hat = -1;
        controller[iCtrlIdx].button[b].hat_pos = -1;
        controller[iCtrlIdx].button[b].mouse = -1;
    }
    for( b = 0; b < 2; b++ )
    {
        controller[iCtrlIdx].axis[b].button_a = controller[iCtrlIdx].axis[b].button_b = -1;
        controller[iCtrlIdx].axis[b].key_a = controller[iCtrlIdx].axis[b].key_b = SDLK_UNKNOWN;
        controller[iCtrlIdx].axis[b].axis_a = -1;
        controller[iCtrlIdx].axis[b].axis_dir_a = 1;
        controller[iCtrlIdx].axis[b].axis_b = -1;
        controller[iCtrlIdx].axis[b].axis_dir_b = 1;
        controller[iCtrlIdx].axis[b].hat = -1;
        controller[iCtrlIdx].axis[b].hat_pos_a = -1;
        controller[iCtrlIdx].axis[b].hat_pos_b = -1;
    }
}

static void set_model_defaults(int iCtrlIdx, int iDeviceIdx, enum eJoyType type)
{
    SController *pCtrl = &controller[iCtrlIdx];

    /* set the general parameters */
    pCtrl->control.Present = 1;
    pCtrl->control.Plugin = PLUGIN_MEMPAK;
    pCtrl->mouse = 0;
    pCtrl->device = iDeviceIdx;
    pCtrl->axis_deadzone[0] = pCtrl->axis_deadzone[1] = 4096;
    pCtrl->axis_peak[0]     = pCtrl->axis_peak[1] = 32768;

    switch (type)
    {
        case KBD_DEFAULT:
            pCtrl->button[R_DPAD].key = SDLK_d;
            pCtrl->button[L_DPAD].key = SDLK_a;
            pCtrl->button[D_DPAD].key = SDLK_s;
            pCtrl->button[U_DPAD].key = SDLK_w;
            pCtrl->button[START_BUTTON].key = SDLK_RETURN;
            pCtrl->button[Z_TRIG].key = SDLK_z;
            pCtrl->button[B_BUTTON].key = SDLK_LALT;
            pCtrl->button[A_BUTTON].key = SDLK_LMETA;
            pCtrl->button[R_CBUTTON].key = SDLK_l;
            pCtrl->button[L_CBUTTON].key = SDLK_j;
            pCtrl->button[D_CBUTTON].key = SDLK_k;
            pCtrl->button[U_CBUTTON].key = SDLK_i;
            pCtrl->button[R_TRIG].key = SDLK_c;
            pCtrl->button[L_TRIG].key = SDLK_x;
            pCtrl->button[MEMPAK].key = SDLK_COMMA;
            pCtrl->button[RUMBLEPAK].key = SDLK_PERIOD;
            pCtrl->axis[0].key_a = SDLK_LEFT;
            pCtrl->axis[0].key_b = SDLK_RIGHT;
            pCtrl->axis[1].key_a = SDLK_UP;
            pCtrl->axis[1].key_b = SDLK_DOWN;
            break;
        case JOY_GREEN_ASIA_USB:
            pCtrl->button[R_DPAD].hat = pCtrl->button[L_DPAD].hat = 0;
            pCtrl->button[D_DPAD].hat = pCtrl->button[U_DPAD].hat = 0;
            pCtrl->button[R_DPAD].hat_pos = SDL_HAT_RIGHT;
            pCtrl->button[L_DPAD].hat_pos = SDL_HAT_LEFT;
            pCtrl->button[D_DPAD].hat_pos = SDL_HAT_DOWN;
            pCtrl->button[U_DPAD].hat_pos = SDL_HAT_UP;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 1;
            pCtrl->button[B_BUTTON].button = 3;
            pCtrl->button[A_BUTTON].button = 2;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 2;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_TRIG].button = 7;
            pCtrl->button[L_TRIG].button = 6;
            /* no MEMPAK or RUMBLEPAK defined */
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_LOGITECH_CORDLESS_RUMBLEPAD_2:
        case JOY_LOGITECH_DUAL_ACTION:
            pCtrl->button[R_DPAD].axis = pCtrl->button[L_DPAD].axis = 4;
            pCtrl->button[D_DPAD].axis = pCtrl->button[U_DPAD].axis = 5;
            pCtrl->button[R_DPAD].axis_dir = pCtrl->button[D_DPAD].axis_dir = 1;
            pCtrl->button[L_DPAD].axis_dir = pCtrl->button[U_DPAD].axis_dir = -1;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 3;
            pCtrl->button[B_BUTTON].button = 0;
            pCtrl->button[A_BUTTON].button = 1;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 2;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 3;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_TRIG].button = 5;
            pCtrl->button[L_TRIG].button = 4;
            pCtrl->button[MEMPAK].button = 6;
            pCtrl->button[RUMBLEPAK].button = 7;
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_MEGA_WORLD_USB:
            pCtrl->button[R_DPAD].hat = pCtrl->button[L_DPAD].hat = 0;
            pCtrl->button[D_DPAD].hat = pCtrl->button[U_DPAD].hat = 0;
            pCtrl->button[R_DPAD].hat_pos = SDL_HAT_RIGHT;
            pCtrl->button[L_DPAD].hat_pos = SDL_HAT_LEFT;
            pCtrl->button[D_DPAD].hat_pos = SDL_HAT_DOWN;
            pCtrl->button[U_DPAD].hat_pos = SDL_HAT_UP;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 7;
            pCtrl->button[B_BUTTON].button = 0;
            pCtrl->button[A_BUTTON].button = 2;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 2;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_TRIG].button = 6;
            pCtrl->button[L_TRIG].button = 4;
            /* no MEMPAK or RUMBLEPAK defined */
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_MICROSOFT_XBOX_360:
            pCtrl->button[R_DPAD].axis = pCtrl->button[L_DPAD].axis = 6;
            pCtrl->button[D_DPAD].axis = pCtrl->button[U_DPAD].axis = 7;
            pCtrl->button[R_DPAD].axis_dir = pCtrl->button[D_DPAD].axis_dir = 1;
            pCtrl->button[L_DPAD].axis_dir = pCtrl->button[U_DPAD].axis_dir = -1;
            pCtrl->button[START_BUTTON].button = 6;
            pCtrl->button[Z_TRIG].axis = 2;
            pCtrl->button[Z_TRIG].axis_dir = 1;
            pCtrl->button[B_BUTTON].button = 2;
            pCtrl->button[A_BUTTON].button = 0;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 4;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_CBUTTON].button = 1;
            pCtrl->button[U_CBUTTON].button = 3;
            pCtrl->button[R_TRIG].button = 5;
            pCtrl->button[L_TRIG].button = 4;
            pCtrl->button[MEMPAK].button = 9;
            pCtrl->button[RUMBLEPAK].axis = 5;
            pCtrl->button[RUMBLEPAK].axis_dir = 1;
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_N64_CONTROLLER:
            pCtrl->button[R_DPAD].hat = pCtrl->button[L_DPAD].hat = 0;
            pCtrl->button[D_DPAD].hat = pCtrl->button[U_DPAD].hat = 0;
            pCtrl->button[R_DPAD].hat_pos = SDL_HAT_RIGHT;
            pCtrl->button[L_DPAD].hat_pos = SDL_HAT_LEFT;
            pCtrl->button[D_DPAD].hat_pos = SDL_HAT_DOWN;
            pCtrl->button[U_DPAD].hat_pos = SDL_HAT_UP;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 0;
            pCtrl->button[B_BUTTON].button = 2;
            pCtrl->button[A_BUTTON].button = 1;
            pCtrl->button[R_CBUTTON].button = 4;
            pCtrl->button[L_CBUTTON].button = 5;
            pCtrl->button[D_CBUTTON].button = 3;
            pCtrl->button[U_CBUTTON].button = 6;
            pCtrl->button[R_TRIG].button = 8;
            pCtrl->button[L_TRIG].button = 7;
            pCtrl->button[MEMPAK].key = SDLK_m;
            pCtrl->button[RUMBLEPAK].key = SDLK_r;
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_SAITEK_P880:
            pCtrl->button[R_DPAD].hat = pCtrl->button[L_DPAD].hat = 0;
            pCtrl->button[D_DPAD].hat = pCtrl->button[U_DPAD].hat = 0;
            pCtrl->button[R_DPAD].hat_pos = SDL_HAT_RIGHT;
            pCtrl->button[L_DPAD].hat_pos = SDL_HAT_LEFT;
            pCtrl->button[D_DPAD].hat_pos = SDL_HAT_DOWN;
            pCtrl->button[U_DPAD].hat_pos = SDL_HAT_UP;
            pCtrl->button[START_BUTTON].button = 10;
            pCtrl->button[Z_TRIG].button = 3;
            pCtrl->button[B_BUTTON].button = 0;
            pCtrl->button[A_BUTTON].button = 2;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 2;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_CBUTTON].button = 5;
            pCtrl->button[L_CBUTTON].button = 1;
            pCtrl->button[D_CBUTTON].button = 9;
            pCtrl->button[U_CBUTTON].button = 4;
            pCtrl->button[R_TRIG].button = 7;
            pCtrl->button[L_TRIG].button = 6;
            /* no MEMPAK or RUMBLEPAK defined */
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
            break;
        default:
            DebugMessage(M64MSG_ERROR, "set_model_defaults(): invalid eJoyType %i", (int) type);
            return;
    }
}

static const char * get_sdl_joystick_name(int iCtrlIdx)
{
    static char JoyName[256];
    int joyWasInit = SDL_WasInit(SDL_INIT_JOYSTICK);
    
    /* initialize the joystick subsystem if necessary */
    if (!joyWasInit)
        if (SDL_InitSubSystem(SDL_INIT_JOYSTICK) == -1)
        {
            DebugMessage(M64MSG_ERROR, "Couldn't init SDL joystick subsystem: %s", SDL_GetError() );
            return NULL;
        }

    /* get the name of the corresponding joystick */
    const char *joySDLName = SDL_JoystickName(iCtrlIdx);

    /* copy the name to our local string */
    if (joySDLName != NULL)
    {
        strncpy(JoyName, joySDLName, 255);
        JoyName[255] = 0;
    }

    /* quit the joystick subsystem if necessary */
    if (!joyWasInit)
        SDL_QuitSubSystem(SDL_INIT_JOYSTICK);

    /* if the SDL function had an error, then return NULL, otherwise return local copy of joystick name */
    if (joySDLName == NULL)
        return NULL;
    else
        return JoyName;
}

static int auto_load_defaults(int iCtrlIdx)
{
    const int numJoyModels = sizeof(l_JoyConfigMap) / sizeof(sJoyConfigMap);
    const char *joySDLName = get_sdl_joystick_name(iCtrlIdx);
    int i;

    /* if we couldn't get a name (no joystick plugged in to given port), then return with a failure */
    if (joySDLName == NULL)
        return 0;

    /* iterate through the list of all known joystick models */
    for (i = 0; i < numJoyModels; i++)
    {
        char Word[32];
        const char *wordPtr = l_JoyConfigMap[i].name;
        int  joyFound = 1;
        /* search in the SDL name for all the words in the joystick name.  If any are missing, then this is not the right joystick model */
        while (wordPtr != NULL && strlen(wordPtr) > 0)
        {
            char *nextSpace = strchr(wordPtr, ' ');
            if (nextSpace == NULL)
            {
                strcpy(Word, wordPtr);
                wordPtr = NULL;
            }
            else
            {
                int length = (int) (nextSpace - wordPtr);
                strncpy(Word, wordPtr, length);
                Word[length] = 0;
                wordPtr = nextSpace + 1;
            }
            if (strcasestr(joySDLName, Word) == NULL)
                joyFound = 0;
        }
        /* if we found the right joystick, then set the defaults and break out of this loop */
        if (joyFound)
        {
            DebugMessage(M64MSG_INFO, "N64 Controller #%i: Enabled, using auto-configuration for joystick '%s'", iCtrlIdx + 1, joySDLName);
            set_model_defaults(iCtrlIdx, iCtrlIdx, l_JoyConfigMap[i].type);
            return 1;
        }
    }

    DebugMessage(M64MSG_INFO, "N64 Controller #%i: Disabled, no configuration data for '%s'", iCtrlIdx + 1, joySDLName);
    return 0;
}


/* global functions */
void save_controller_config(int iCtrlIdx)
{
    m64p_handle pConfig;
    char SectionName[32], Param[32], ParamString[128];
    int j;

    /* Delete the configuration section for this controller, so we can use SetDefaults and save the help comments also */
    sprintf(SectionName, "Input-SDL-Control%i", iCtrlIdx + 1);
    ConfigDeleteSection(SectionName);
    /* Open the configuration section for this controller (create a new one) */
    if (ConfigOpenSection(SectionName, &pConfig) != M64ERR_SUCCESS)
    {
        DebugMessage(M64MSG_ERROR, "Couldn't open config section '%s'", SectionName);
        return;
    }

    /* save the general controller parameters */
    ConfigSetDefaultBool(pConfig, "plugged", controller[iCtrlIdx].control.Present, "Specifies whether this controller is 'plugged in' to the simulated N64");
    ConfigSetDefaultInt(pConfig, "plugin", controller[iCtrlIdx].control.Plugin, "Specifies which type of expansion pak is in the controller: 1=None, 2=Mem pak, 5=Rumble pak");
    ConfigSetDefaultBool(pConfig, "mouse", controller[iCtrlIdx].mouse, "If True, then mouse buttons may be used with this controller");
    ConfigSetDefaultInt(pConfig, "device", controller[iCtrlIdx].device, "Specifies which joystick is bound to this controller: -1=None, 0 or more= SDL Joystick number");

    sprintf(Param, "%i,%i", controller[iCtrlIdx].axis_deadzone[0], controller[iCtrlIdx].axis_deadzone[1]);
    ConfigSetDefaultString(pConfig, "AnalogDeadzone", Param, "The minimum absolute value of the SDL analog joystick axis to move the N64 controller axis value from 0.  For X, Y axes.");
    sprintf(Param, "%i,%i", controller[iCtrlIdx].axis_peak[0], controller[iCtrlIdx].axis_peak[1]);
    ConfigSetDefaultString(pConfig, "AnalogPeak", Param, "An absolute value of the SDL joystick axis >= AnalogPeak will saturate the N64 controller axis value (at 80).  For X, Y axes. For each axis, this must be greater than the corresponding AnalogDeadzone value");

    /* save configuration for all the digital buttons */
    for (j = 0; j < X_AXIS; j++ )
    {
        const char *Help;
        ParamString[0] = 0;
        if (controller[iCtrlIdx].button[j].key > 0)
        {
            sprintf(Param, "key(%i) ", controller[iCtrlIdx].button[j].key);
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].button[j].button >= 0)
        {
            sprintf(Param, "button(%i) ", controller[iCtrlIdx].button[j].button);
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].button[j].axis >= 0)
        {
            sprintf(Param, "axis(%i%c) ", controller[iCtrlIdx].button[j].axis, (controller[iCtrlIdx].button[j].axis_dir == -1) ? '-' : '+' );
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].button[j].hat >= 0)
        {
            sprintf(Param, "hat(%i %s) ", controller[iCtrlIdx].button[j].hat, HAT_POS_NAME(controller[iCtrlIdx].button[j].hat_pos));
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].button[j].mouse >= 0)
        {
            sprintf(Param, "mouse(%i) ", controller[iCtrlIdx].button[j].mouse);
            strcat(ParamString, Param);
        }
        if (j == 0)
            Help = "Digital button configuration mappings";
        else
            Help = NULL;
        /* if last character is a space, chop it off */
        int len = strlen(ParamString);
        if (len > 0 && ParamString[len-1] == ' ')
            ParamString[len-1] = 0;
        ConfigSetDefaultString(pConfig, button_names[j], ParamString, Help);
    }

    /* save configuration for the 2 analog axes */
    for (j = 0; j < 2; j++ )
    {
        const char *Help;
        ParamString[0] = 0;
        if (controller[iCtrlIdx].axis[j].key_a > 0 && controller[iCtrlIdx].axis[j].key_b > 0)
        {
            sprintf(Param, "key(%i,%i) ", controller[iCtrlIdx].axis[j].key_a, controller[iCtrlIdx].axis[j].key_b);
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].axis[j].button_a >= 0 && controller[iCtrlIdx].axis[j].button_b >= 0)
        {
            sprintf(Param, "button(%i,%i) ", controller[iCtrlIdx].axis[j].button_a, controller[iCtrlIdx].axis[j].button_b);
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].axis[j].axis_a >= 0 && controller[iCtrlIdx].axis[j].axis_b >= 0)
        {
            sprintf(Param, "axis(%i%c,%i%c) ", controller[iCtrlIdx].axis[j].axis_a, (controller[iCtrlIdx].axis[j].axis_dir_a <= 0) ? '-' : '+',
                                               controller[iCtrlIdx].axis[j].axis_b, (controller[iCtrlIdx].axis[j].axis_dir_b <= 0) ? '-' : '+' );
            strcat(ParamString, Param);
        }
        if (controller[iCtrlIdx].axis[j].hat >= 0)
        {
            sprintf(Param, "hat(%i %s %s) ", controller[iCtrlIdx].axis[j].hat,
                                             HAT_POS_NAME(controller[iCtrlIdx].axis[j].hat_pos_a),
                                             HAT_POS_NAME(controller[iCtrlIdx].axis[j].hat_pos_b));
            strcat(ParamString, Param);
        }
        if (j == 0)
            Help = "Analog axis configuration mappings";
        else
            Help = NULL;
        /* if last character is a space, chop it off */
        int len = strlen(ParamString);
        if (len > 0 && ParamString[len-1] == ' ')
            ParamString[len-1] = 0;
        ConfigSetDefaultString(pConfig, button_names[X_AXIS + j], ParamString, Help);
    }

}

void load_configuration(void)
{
    m64p_handle pConfig;
    char SectionName[32];
    char input_str[256], value1_str[16], value2_str[16];
    const char *config_ptr;
    int i, j;

    /* loop through all 4 simulated N64 controllers */
    for (i = 0; i < 4; i++)
    {
        /* reset the controller configuration */
        clear_controller(i);
        /* Open the configuration section for this controller */
        sprintf(SectionName, "Input-SDL-Control%i", i + 1);
        if (ConfigOpenSection(SectionName, &pConfig) != M64ERR_SUCCESS)
        {
            DebugMessage(M64MSG_ERROR, "Couldn't open config section '%s'", SectionName);
            continue;
        }
        /* try to read all of the configuration values */
        int readOK;
        for (readOK = 0; readOK == 0; readOK = 1)
        {
            if (ConfigGetParameter(pConfig, "plugged", M64TYPE_INT, &controller[i].control.Present, sizeof(int)) != M64ERR_SUCCESS)
                break;
            if (ConfigGetParameter(pConfig, "plugin", M64TYPE_INT, &controller[i].control.Plugin, sizeof(int)) != M64ERR_SUCCESS)
                break;
            if (ConfigGetParameter(pConfig, "mouse", M64TYPE_INT, &controller[i].mouse, sizeof(int)) != M64ERR_SUCCESS)
                break;
            if (ConfigGetParameter(pConfig, "device", M64TYPE_INT, &controller[i].device, sizeof(int)) != M64ERR_SUCCESS)
                break;
            if (ConfigGetParameter(pConfig, "AnalogDeadzone", M64TYPE_STRING, input_str, 256) != M64ERR_SUCCESS)
                break;
            if (sscanf(input_str, "%i,%i", &controller[i].axis_deadzone[0], &controller[i].axis_deadzone[1]) != 2)
                DebugMessage(M64MSG_WARNING, "parsing error in AnalogDeadzone parameter for controller %i", i + 1);
            if (ConfigGetParameter(pConfig, "AnalogPeak", M64TYPE_STRING, input_str, 256) != M64ERR_SUCCESS)
                break;
            if (sscanf(input_str, "%i,%i", &controller[i].axis_peak[0], &controller[i].axis_peak[1]) != 2)
                DebugMessage(M64MSG_WARNING, "parsing error in AnalogPeak parameter for controller %i", i + 1);
            /* load configuration for all the digital buttons */
            for (j = 0; j < X_AXIS; j++)
            {
                if (ConfigGetParameter(pConfig, button_names[j], M64TYPE_STRING, input_str, 256) != M64ERR_SUCCESS)
                    break;
                if ((config_ptr = strstr(input_str, "key")) != NULL)
                    if (sscanf(config_ptr, "key(%i)", &controller[i].button[j].key) != 1)
                        DebugMessage(M64MSG_WARNING, "parsing error in key() parameter of button '%s' for controller %i", button_names[j], i + 1);
                if ((config_ptr = strstr(input_str, "button")) != NULL)
                    if (sscanf(config_ptr, "button(%i)", &controller[i].button[j].button) != 1)
                        DebugMessage(M64MSG_WARNING, "parsing error in button() parameter of button '%s' for controller %i", button_names[j], i + 1);
                if ((config_ptr = strstr(input_str, "axis")) != NULL)
                {
                    char chAxisDir;
                    if (sscanf(config_ptr, "axis(%i%c)", &controller[i].button[j].axis, &chAxisDir) != 2)
                        DebugMessage(M64MSG_WARNING, "parsing error in axis() parameter of button '%s' for controller %i", button_names[j], i + 1);
                    controller[i].button[j].axis_dir = (chAxisDir == '+' ? 1 : (chAxisDir == '-' ? -1 : 0));
                }
                if ((config_ptr = strstr(input_str, "hat")) != NULL)
                {
                    if (sscanf(config_ptr, "hat(%i %15s", &controller[i].button[j].hat, value1_str) != 2)
                        DebugMessage(M64MSG_WARNING, "parsing error in hat() parameter of button '%s' for controller %i", button_names[j], i + 1);
                    value1_str[15] = 0;
                    /* chop off the last character of value1_str if it is the closing parenthesis */
                    char *lastchar = &value1_str[strlen(value1_str) - 1];
                    if (lastchar > value1_str && *lastchar == ')') *lastchar = 0;
                    controller[i].button[j].hat_pos = get_hat_pos_by_name(value1_str);
                }
                if ((config_ptr = strstr(input_str, "mouse")) != NULL)
                    if (sscanf(config_ptr, "mouse(%i)", &controller[i].button[j].mouse) != 1)
                        DebugMessage(M64MSG_WARNING, "parsing error in mouse() parameter of button '%s' for controller %i", button_names[j], i + 1);
            }
            if (j < X_AXIS)
                break;
            /* load configuration for the 2 analog joystick axes */
            for (j = X_AXIS; j <= Y_AXIS; j++)
            {
                int axis_idx = j - X_AXIS;
                if (ConfigGetParameter(pConfig, button_names[j], M64TYPE_STRING, input_str, 256) != M64ERR_SUCCESS)
                    break;
                if ((config_ptr = strstr(input_str, "key")) != NULL)
                    if (sscanf(config_ptr, "key(%i,%i)", &controller[i].axis[axis_idx].key_a, &controller[i].axis[axis_idx].key_b) != 2)
                        DebugMessage(M64MSG_WARNING, "parsing error in key() parameter of axis '%s' for controller %i", button_names[j], i + 1);
                if ((config_ptr = strstr(input_str, "button")) != NULL)
                    if (sscanf(config_ptr, "button(%i,%i)", &controller[i].axis[axis_idx].button_a, &controller[i].axis[axis_idx].button_b) != 2)
                        DebugMessage(M64MSG_WARNING, "parsing error in button() parameter of axis '%s' for controller %i", button_names[j], i + 1);
                if ((config_ptr = strstr(input_str, "axis")) != NULL)
                {
                    char chAxisDir1, chAxisDir2;
                    if (sscanf(config_ptr, "axis(%i%c,%i%c)", &controller[i].axis[axis_idx].axis_a, &chAxisDir1,
                                                              &controller[i].axis[axis_idx].axis_b, &chAxisDir2) != 4)
                        DebugMessage(M64MSG_WARNING, "parsing error in axis() parameter of axis '%s' for controller %i", button_names[j], i + 1);
                    controller[i].axis[axis_idx].axis_dir_a = (chAxisDir1 == '+' ? 1 : (chAxisDir1 == '-' ? -1 : 0));
                    controller[i].axis[axis_idx].axis_dir_b = (chAxisDir2 == '+' ? 1 : (chAxisDir2 == '-' ? -1 : 0));
                }
                if ((config_ptr = strstr(input_str, "hat")) != NULL)
                {
                    if (sscanf(config_ptr, "hat(%i %15s %15s", &controller[i].axis[axis_idx].hat, value1_str, value2_str) != 3)
                        DebugMessage(M64MSG_WARNING, "parsing error in hat() parameter of axis '%s' for controller %i", button_names[j], i + 1);
                    value1_str[15] = value2_str[15] = 0;
                    /* chop off the last character of value2_str if it is the closing parenthesis */
                    char *lastchar = &value2_str[strlen(value2_str) - 1];
                    if (lastchar > value2_str && *lastchar == ')') *lastchar = 0;
                    controller[i].axis[axis_idx].hat_pos_a = get_hat_pos_by_name(value1_str);
                    controller[i].axis[axis_idx].hat_pos_b = get_hat_pos_by_name(value2_str);
                }
            }
            if (j <= Y_AXIS)
                break;
        }

        /* if a valid joystick configuration was read, then check if the specified joystick is available through SDL */
        if (readOK && controller[i].device >= 0)
        {
            const char *JoyName = get_sdl_joystick_name(controller[i].device);
            if (JoyName == NULL)
            {
                controller[i].device = DEVICE_NONE;
                controller[i].control.Present = 0;
                DebugMessage(M64MSG_INFO, "N64 Controller #%i: Disabled, SDL joystick is not available", i+1);
            }
            else
                DebugMessage(M64MSG_INFO, "N64 Controller #%i: Enabled, using stored configuration with joystick '%s'", i+1, JoyName);
        }
        else /* otherwise reset the controller configuration again and load the defaults */
        {
            clear_controller(i);
            if (auto_load_defaults(i))
                save_controller_config(i);
        }
    }

    /* see how many joysticks were found */
    int joy_found = 0, joy_plugged = 0;
    for (i = 0; i < 4; i++)
    {
        if (controller[i].device >= 0)
        {
            joy_found++;
            if (controller[i].control.Present)
                joy_plugged++;
        }
    }
    if (joy_found > 0 && joy_plugged > 0)
    {
        DebugMessage(M64MSG_INFO, "%i SDL joysticks found, %i plugged in and usable in the emulator", joy_found, joy_plugged);
    }
    else
    {
        if (joy_found == 0)
            DebugMessage(M64MSG_WARNING, "No SDL joysticks found");
        else if (joy_plugged == 0)
            DebugMessage(M64MSG_WARNING, "%i SDL joysticks found, but none are 'plugged in'", joy_found);
        DebugMessage(M64MSG_INFO, "Forcing keyboard input for N64 controller #1");
        set_model_defaults(0, -1, KBD_DEFAULT);
    }

}


