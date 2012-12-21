/*
  Simple DirectMedia Layer
  Copyright (C) 1997-2011 Sam Lantinga <slouken@libsdl.org>

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.
*/
#include <stdio.h>
#include "SDL_config.h"

#include <android/log.h>

#include "../../events/SDL_events_c.h"

#include "SDL_androidkeyboard.h"

#define printf(...) __android_log_print(ANDROID_LOG_VERBOSE, "SDL_androidkeyboard", __VA_ARGS__)

void Android_InitKeyboard()
{
    SDL_Keycode keymap[SDL_NUM_SCANCODES];

    /* Add default scancode to key mapping */
    SDL_GetDefaultKeymap(keymap);
    SDL_SetKeymap(0, keymap, SDL_NUM_SCANCODES);
}

static SDL_Scancode Android_Keycodes[] = {
/*  0 */    SDL_SCANCODE_UNKNOWN, /* AKEYCODE_UNKNOWN */
/*  1 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_SOFT_LEFT */
            SDL_SCANCODE_KP_00, /* AKEYCODE_SOFT_LEFT */
/*  2 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_SOFT_RIGHT */
            SDL_SCANCODE_KP_000, /* AKEYCODE_SOFT_RIGHT */
/*  3 */    SDL_SCANCODE_AC_HOME, /* AKEYCODE_HOME */
/*  4 */    SDL_SCANCODE_AC_BACK, /* AKEYCODE_BACK */
/*  5 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_CALL */
            SDL_SCANCODE_THOUSANDSSEPARATOR, /* AKEYCODE_CALL */
/*  6 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_ENDCALL */
            SDL_SCANCODE_DECIMALSEPARATOR, /* AKEYCODE_ENDCALL */
/*  7 */    SDL_SCANCODE_0, /* AKEYCODE_0 */
/*  8 */    SDL_SCANCODE_1, /* AKEYCODE_1 */
/*  9 */    SDL_SCANCODE_2, /* AKEYCODE_2 */
/* 10 */    SDL_SCANCODE_3, /* AKEYCODE_3 */
/* 11 */    SDL_SCANCODE_4, /* AKEYCODE_4 */
/* 12 */    SDL_SCANCODE_5, /* AKEYCODE_5 */
/* 13 */    SDL_SCANCODE_6, /* AKEYCODE_6 */
/* 14 */    SDL_SCANCODE_7, /* AKEYCODE_7 */
/* 15 */    SDL_SCANCODE_8, /* AKEYCODE_8 */
/* 16 */    SDL_SCANCODE_9, /* AKEYCODE_9 */
/* 17 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_STAR */
            SDL_SCANCODE_KP_MEMMULTIPLY, /* AKEYCODE_STAR */
/* 18 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_POUND */
            SDL_SCANCODE_KP_HASH, /* AKEYCODE_POUND */
/* 19 */    SDL_SCANCODE_UP, /* AKEYCODE_DPAD_UP */
/* 20 */    SDL_SCANCODE_DOWN, /* AKEYCODE_DPAD_DOWN */
/* 21 */    SDL_SCANCODE_LEFT, /* AKEYCODE_DPAD_LEFT */
/* 22 */    SDL_SCANCODE_RIGHT, /* AKEYCODE_DPAD_RIGHT */
/* 23 */    SDL_SCANCODE_SELECT, /* AKEYCODE_DPAD_CENTER */
/* 24 */    SDL_SCANCODE_VOLUMEUP, /* AKEYCODE_VOLUME_UP */
/* 25 */    SDL_SCANCODE_VOLUMEDOWN, /* AKEYCODE_VOLUME_DOWN */
/* 26 */    SDL_SCANCODE_POWER, /* AKEYCODE_POWER */
/* 27 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_CAMERA */
            SDL_SCANCODE_KP_CLEAR, /* AKEYCODE_CAMERA */
/* 28 */    SDL_SCANCODE_CLEAR, /* AKEYCODE_CLEAR */
/* 29 */    SDL_SCANCODE_A, /* AKEYCODE_A */
/* 30 */    SDL_SCANCODE_B, /* AKEYCODE_B */
/* 31 */    SDL_SCANCODE_C, /* AKEYCODE_C */
/* 32 */    SDL_SCANCODE_D, /* AKEYCODE_D */
/* 33 */    SDL_SCANCODE_E, /* AKEYCODE_E */
/* 34 */    SDL_SCANCODE_F, /* AKEYCODE_F */
/* 35 */    SDL_SCANCODE_G, /* AKEYCODE_G */
/* 36 */    SDL_SCANCODE_H, /* AKEYCODE_H */
/* 37 */    SDL_SCANCODE_I, /* AKEYCODE_I */
/* 38 */    SDL_SCANCODE_J, /* AKEYCODE_J */
/* 39 */    SDL_SCANCODE_K, /* AKEYCODE_K */
/* 40 */    SDL_SCANCODE_L, /* AKEYCODE_L */
/* 41 */    SDL_SCANCODE_M, /* AKEYCODE_M */
/* 42 */    SDL_SCANCODE_N, /* AKEYCODE_N */
/* 43 */    SDL_SCANCODE_O, /* AKEYCODE_O */
/* 44 */    SDL_SCANCODE_P, /* AKEYCODE_P */
/* 45 */    SDL_SCANCODE_Q, /* AKEYCODE_Q */
/* 46 */    SDL_SCANCODE_R, /* AKEYCODE_R */
/* 47 */    SDL_SCANCODE_S, /* AKEYCODE_S */
/* 48 */    SDL_SCANCODE_T, /* AKEYCODE_T */
/* 49 */    SDL_SCANCODE_U, /* AKEYCODE_U */
/* 50 */    SDL_SCANCODE_V, /* AKEYCODE_V */
/* 51 */    SDL_SCANCODE_W, /* AKEYCODE_W */
/* 52 */    SDL_SCANCODE_X, /* AKEYCODE_X */
/* 53 */    SDL_SCANCODE_Y, /* AKEYCODE_Y */
/* 54 */    SDL_SCANCODE_Z, /* AKEYCODE_Z */
/* 55 */    SDL_SCANCODE_COMMA, /* AKEYCODE_COMMA */
/* 56 */    SDL_SCANCODE_PERIOD, /* AKEYCODE_PERIOD */
/* 57 */    SDL_SCANCODE_LALT, /* AKEYCODE_ALT_LEFT */
/* 58 */    SDL_SCANCODE_RALT, /* AKEYCODE_ALT_RIGHT */
/* 59 */    SDL_SCANCODE_LSHIFT, /* AKEYCODE_SHIFT_LEFT */
/* 60 */    SDL_SCANCODE_RSHIFT, /* AKEYCODE_SHIFT_RIGHT */
/* 61 */    SDL_SCANCODE_TAB, /* AKEYCODE_TAB */
/* 62 */    SDL_SCANCODE_SPACE, /* AKEYCODE_SPACE */
/* 63 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_SYM */
            SDL_SCANCODE_KBDILLUMTOGGLE, /* AKEYCODE_SYM */
/* 64 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_EXPLORER */
            SDL_SCANCODE_WWW, /* AKEYCODE_EXPLORER */
/* 65 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_ENVELOPE */
            SDL_SCANCODE_MAIL, /* AKEYCODE_ENVELOPE */
/* 66 */    SDL_SCANCODE_RETURN, /* AKEYCODE_ENTER */
/* 67 */    SDL_SCANCODE_DELETE, /* AKEYCODE_DEL */
/* 68 */    SDL_SCANCODE_GRAVE, /* AKEYCODE_GRAVE */
/* 69 */    SDL_SCANCODE_MINUS, /* AKEYCODE_MINUS */
/* 70 */    SDL_SCANCODE_EQUALS, /* AKEYCODE_EQUALS */
/* 71 */    SDL_SCANCODE_LEFTBRACKET, /* AKEYCODE_LEFT_BRACKET */
/* 72 */    SDL_SCANCODE_RIGHTBRACKET, /* AKEYCODE_RIGHT_BRACKET */
/* 73 */    SDL_SCANCODE_BACKSLASH, /* AKEYCODE_BACKSLASH */
/* 74 */    SDL_SCANCODE_SEMICOLON, /* AKEYCODE_SEMICOLON */
/* 75 */    SDL_SCANCODE_APOSTROPHE, /* AKEYCODE_APOSTROPHE */
/* 76 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_SLASH */
            SDL_SCANCODE_KP_MEMDIVIDE, /* AKEYCODE_SLASH */
/* 77 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_AT */
            SDL_SCANCODE_KP_AT, /* AKEYCODE_AT */
/* 78 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_NUM */
            SDL_SCANCODE_KP_DBLAMPERSAND, /* AKEYCODE_NUM */
/* 79 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_HEADSETHOOK */
            SDL_SCANCODE_KP_DBLVERTICALBAR, /* AKEYCODE_HEADSETHOOK */
/* 80 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_FOCUS */
            SDL_SCANCODE_KP_CLEARENTRY, /* AKEYCODE_FOCUS */
/* 81 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_PLUS */
            SDL_SCANCODE_KP_PLUSMINUS, /* AKEYCODE_PLUS */
/* 82 */    SDL_SCANCODE_MENU, /* AKEYCODE_MENU */
/* 83 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_NOTIFICATION */
            SDL_SCANCODE_KP_EXCLAM, /* AKEYCODE_NOTIFICATION */
/* 84 */    SDL_SCANCODE_AC_SEARCH, /* AKEYCODE_SEARCH */
/* 85 */    SDL_SCANCODE_AUDIOPLAY, /* AKEYCODE_MEDIA_PLAY_PAUSE */
/* 86 */    SDL_SCANCODE_AUDIOSTOP, /* AKEYCODE_MEDIA_STOP */
/* 87 */    SDL_SCANCODE_AUDIONEXT, /* AKEYCODE_MEDIA_NEXT */
/* 88 */    SDL_SCANCODE_AUDIOPREV, /* AKEYCODE_MEDIA_PREVIOUS */
/* 89 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_MEDIA_REWIND */
            SDL_SCANCODE_AC_BACK, /* AKEYCODE_MEDIA_REWIND */
/* 90 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_MEDIA_FAST_FORWARD */
            SDL_SCANCODE_AC_FORWARD, /* AKEYCODE_MEDIA_FAST_FORWARD */
/* 91 */    SDL_SCANCODE_MUTE, /* AKEYCODE_MUTE */
/* 92 */    SDL_SCANCODE_PAGEUP, /* AKEYCODE_PAGE_UP */
/* 93 */    SDL_SCANCODE_PAGEDOWN, /* AKEYCODE_PAGE_DOWN */
/* 94 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_PICTSYMBOLS */
            SDL_SCANCODE_MEDIASELECT, /* AKEYCODE_PICTSYMBOLS */
/* 95 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_SWITCH_CHARSET */
            SDL_SCANCODE_KP_HEXADECIMAL, /* AKEYCODE_SWITCH_CHARSET */
/* 96 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_A */
            SDL_SCANCODE_KP_A, /* AKEYCODE_BUTTON_A */
/* 97 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_B */
            SDL_SCANCODE_KP_B, /* AKEYCODE_BUTTON_B */
/* 98 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_C */
            SDL_SCANCODE_KP_C, /* AKEYCODE_BUTTON_C */
/* 99 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_X */
            SDL_SCANCODE_KP_D, /* AKEYCODE_BUTTON_X */
/*100 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_Y */
            SDL_SCANCODE_KP_GREATER, /* AKEYCODE_BUTTON_Y */
/*101 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_Z */
            SDL_SCANCODE_KP_LESS, /* AKEYCODE_BUTTON_Z */
/*102 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_L1 */
            SDL_SCANCODE_KP_LEFTBRACE, /* AKEYCODE_BUTTON_L1 */
/*103 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_R1 */
            SDL_SCANCODE_KP_RIGHTBRACE, /* AKEYCODE_BUTTON_R1 */
/*104 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_L2 */
            SDL_SCANCODE_KP_LEFTPAREN, /* AKEYCODE_BUTTON_L2 */
/*105 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_R2 */
            SDL_SCANCODE_KP_RIGHTPAREN, /* AKEYCODE_BUTTON_R2 */
/*106 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_THUMBL */
            SDL_SCANCODE_AUDIOPREV, /* AKEYCODE_BUTTON_THUMBL */
/*107 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_THUMBR */
            SDL_SCANCODE_AUDIONEXT, /* AKEYCODE_BUTTON_THUMBR */
/*108 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_START */
            SDL_SCANCODE_KP_PLUS, /* AKEYCODE_BUTTON_START */
/*109 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_SELECT */
            SDL_SCANCODE_KP_ENTER, /* AKEYCODE_BUTTON_SELECT */
/*110 */  //SDL_SCANCODE_UNKNOWN, /* AKEYCODE_BUTTON_MODE */
            SDL_SCANCODE_MODE, /* AKEYCODE_BUTTON_MODE */
};

char str[256];
static SDL_Scancode
TranslateKeycode(int keycode)
{
    SDL_Scancode scancode = SDL_SCANCODE_UNKNOWN;

    if (keycode < SDL_arraysize(Android_Keycodes)) {
        scancode = Android_Keycodes[keycode];
    }
    return scancode;
}

int
Android_OnKeyDown(int keycode)
{
//    return SDL_SendKeyboardKey(SDL_PRESSED, TranslateKeycode(keycode));
    return SDL_SendKeyboardKey(SDL_PRESSED, keycode);
}

int
Android_OnKeyUp(int keycode)
{
//    return SDL_SendKeyboardKey(SDL_RELEASED, TranslateKeycode(keycode));
    return SDL_SendKeyboardKey(SDL_RELEASED, keycode);
}

int
Android_OnSDLKeyDown(int keycode)
{
    return SDL_SendKeyboardKey(SDL_PRESSED, keycode);
}

int
Android_OnSDLKeyUp(int keycode)
{
    return SDL_SendKeyboardKey(SDL_RELEASED, keycode);
}

/* vi: set ts=4 sw=4 expandtab: */
