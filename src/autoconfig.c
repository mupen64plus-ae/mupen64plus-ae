/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-input-sdl - autoconfig.c                                  *
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

#include <string.h>

#include "m64p_types.h"

#include "autoconfig.h"
#include "plugin.h"

/* Data definitions for joystick auto-configuration */
enum eJoyType
{
    KBD_DEFAULT = 1,
    JOY_DRAGON_RISE,
    JOY_GASIA_GAMEPAD,
    JOY_GREEN_ASIA_USB,
    JOY_LOGITECH_CORDLESS_RUMBLEPAD_2,
    JOY_LOGITECH_DUAL_ACTION,
    JOY_MEGA_WORLD_USB,
    JOY_MICROSOFT_XBOX_360,
    JOY_N64_CONTROLLER,
    JOY_SAITEK_P880,
    JOY_SAITEK_P990
};

typedef struct
{
    const char    *name;
    enum eJoyType  type;
} sJoyConfigMap;

static sJoyConfigMap l_JoyConfigMap[] = {
    { "DragonRise Inc. Generic USB Joystick", JOY_DRAGON_RISE},
    { "Gasia Co.,Ltd PS(R) Gamepad",          JOY_GASIA_GAMEPAD},
    { "GreenAsia Inc. USB Joystick",          JOY_GREEN_ASIA_USB},
    { "Logitech Cordless Rumblepad 2",        JOY_LOGITECH_CORDLESS_RUMBLEPAD_2},
    { "Logitech Dual Action",                 JOY_LOGITECH_DUAL_ACTION},
    { "Mega World USB Game Controllers",      JOY_MEGA_WORLD_USB},
    { "Microsoft X-Box 360 pad",              JOY_MICROSOFT_XBOX_360},
    { "N64 controller",                       JOY_N64_CONTROLLER},
    { "SAITEK P880",                          JOY_SAITEK_P880},
    { "Saitek P990 Dual Analog Pad",          JOY_SAITEK_P990},
    { "Keyboard",                             KBD_DEFAULT}
};

/* local functions */
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
        case JOY_DRAGON_RISE:
            pCtrl->button[R_DPAD].axis = pCtrl->button[L_DPAD].axis = 0;
            pCtrl->button[D_DPAD].axis = pCtrl->button[U_DPAD].axis = 1;
            pCtrl->button[R_DPAD].axis_dir = pCtrl->button[D_DPAD].axis_dir = 1;
            pCtrl->button[L_DPAD].axis_dir = pCtrl->button[U_DPAD].axis_dir = -1;
            pCtrl->button[R_DPAD].key = SDLK_RIGHT;
            pCtrl->button[L_DPAD].key = SDLK_LEFT;
            pCtrl->button[D_DPAD].key = SDLK_DOWN;
            pCtrl->button[U_DPAD].key = SDLK_UP;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 8;
            pCtrl->button[B_BUTTON].button = 3;
            pCtrl->button[A_BUTTON].button = 2;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 4;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = 1;
            pCtrl->button[R_CBUTTON].button = 7;
            pCtrl->button[L_CBUTTON].button = 6;
            pCtrl->button[D_CBUTTON].button = 1;
            pCtrl->button[U_CBUTTON].button = 0;
            pCtrl->button[R_TRIG].button = 5;
            pCtrl->button[L_TRIG].button = 4;
            /* no MEMPAK or RUMBLEPAK defined */
            pCtrl->axis[0].axis_a = 0;
            pCtrl->axis[0].axis_b = 2;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[1].axis_dir_b = 1;
            break;
        case JOY_GASIA_GAMEPAD:
            pCtrl->button[R_DPAD].axis = pCtrl->button[L_DPAD].axis = 0;
            pCtrl->button[D_DPAD].axis = pCtrl->button[U_DPAD].axis = 1;
            pCtrl->button[R_DPAD].axis_dir = pCtrl->button[D_DPAD].axis_dir = 1;
            pCtrl->button[L_DPAD].axis_dir = pCtrl->button[U_DPAD].axis_dir = -1;
            pCtrl->button[R_DPAD].hat = pCtrl->button[D_DPAD].hat = 0;
            pCtrl->button[R_DPAD].hat_pos = SDL_HAT_RIGHT;
            pCtrl->button[D_DPAD].hat_pos = SDL_HAT_DOWN;
            pCtrl->button[R_DPAD].key = SDLK_RIGHT;
            pCtrl->button[L_DPAD].key = SDLK_LEFT;
            pCtrl->button[D_DPAD].key = SDLK_DOWN;
            pCtrl->button[U_DPAD].key = SDLK_UP;
            pCtrl->button[START_BUTTON].button = 9;
            pCtrl->button[Z_TRIG].button = 6;
            pCtrl->button[B_BUTTON].button = 3;
            pCtrl->button[A_BUTTON].button = 2;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 2;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 3;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_TRIG].button = 5;
            pCtrl->button[L_TRIG].button = 4;
            pCtrl->button[MEMPAK].button = 1;
            pCtrl->button[RUMBLEPAK].button = 0;
            pCtrl->axis[0].axis_a = pCtrl->axis[0].axis_b = 0;
            pCtrl->axis[1].axis_a = pCtrl->axis[1].axis_b = 1;
            pCtrl->axis[0].axis_dir_a = pCtrl->axis[1].axis_dir_a = -1;
            pCtrl->axis[0].axis_dir_b = pCtrl->axis[1].axis_dir_b = 1;
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
        case JOY_SAITEK_P990:
            pCtrl->button[R_DPAD].axis = pCtrl->button[L_DPAD].axis = 4;
            pCtrl->button[D_DPAD].axis = pCtrl->button[U_DPAD].axis = 5;
            pCtrl->button[R_DPAD].axis_dir = pCtrl->button[D_DPAD].axis_dir = 1;
            pCtrl->button[L_DPAD].axis_dir = pCtrl->button[U_DPAD].axis_dir = -1;
            pCtrl->button[START_BUTTON].button = 5;
            pCtrl->button[Z_TRIG].button = 4;
            pCtrl->button[B_BUTTON].button = 0;
            pCtrl->button[A_BUTTON].button = 1;
            pCtrl->button[R_CBUTTON].axis = pCtrl->button[L_CBUTTON].axis = 3;
            pCtrl->button[D_CBUTTON].axis = pCtrl->button[U_CBUTTON].axis = 2;
            pCtrl->button[R_CBUTTON].axis_dir = pCtrl->button[D_CBUTTON].axis_dir = 1;
            pCtrl->button[L_CBUTTON].axis_dir = pCtrl->button[U_CBUTTON].axis_dir = -1;
            pCtrl->button[R_CBUTTON].button = 8;
            pCtrl->button[L_CBUTTON].button = 3;
            pCtrl->button[D_CBUTTON].button = 2;
            pCtrl->button[U_CBUTTON].button = 9;
            pCtrl->button[R_TRIG].button = 7;
            pCtrl->button[L_TRIG].button = 6;
            pCtrl->button[MEMPAK].button = 11;
            pCtrl->button[RUMBLEPAK].axis = 10;
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

/* global functions */
int auto_load_defaults(int iCtrlIdx, int iDeviceIdx, const char *joySDLName)
{
    const int numJoyModels = sizeof(l_JoyConfigMap) / sizeof(sJoyConfigMap);
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
            set_model_defaults(iCtrlIdx, iDeviceIdx, l_JoyConfigMap[i].type);
            return 1;
        }
    }

    DebugMessage(M64MSG_INFO, "N64 Controller #%i: Disabled, no configuration data for '%s'", iCtrlIdx + 1, joySDLName);
    return 0;
}


