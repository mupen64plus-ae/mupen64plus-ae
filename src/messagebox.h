/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - messagebox.h                                            *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
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

#ifndef __MESSAGEBOX_H__
#define __MESSAGEBOX_H__

#include <gtk/gtk.h>

/* Flags. */

/* Type of messagebox, i.e. what buttons does it contain. */
#define MB_ABORTRETRYIGNORE       (0x00000001)
#define MB_CANCELTRYCONTINUE      (0x00000002)
#define MB_OK                     (0x00000004) /* This is the default */
#define MB_OKCANCEL               (0x00000008)
#define MB_RETRYCANCEL            (0x00000010)
#define MB_YESNO                  (0x00000020)
#define MB_YESNOCANCEL            (0x00000040)

/* Themable icon for warnings. */
#define MB_ICONWARNING            (0x00000100)
#define MB_ICONEXCLAMATION        (0x00000100)
/* Themable icon for extra user information. */
#define MB_ICONINFORMATION        (0x00000200)
#define MB_ICONASTERISK           (0x00000200)
/* Themable icon for a question. */
#define MB_ICONQUESTION           (0x00000400)
/* Themable icon for errors. */
#define MB_ICONSTOP               (0x00000800)
#define MB_ICONERROR              (0x00000800)
#define MB_ICONHAND               (0x00000800)

/* Specify the default button, default is button 1. */
#define MB_DEFBUTTON1             (0x00000000)
#define MB_DEFBUTTON2             (0x00000100)
#define MB_DEFBUTTON3             (0x00000200)
#define MB_DEFBUTTON4             (0x00000300)

/* Justification flags. */
#define MB_RIGHT                  (0x00080000)

/* The following flags are technically part of the Win32 MessageBox() API 
but do not currently do anything in our Gtk2 implementation. */
#define MB_HELP =                 (0x00004000) /* Displays a WM_HELP message when Help pr F1 is pressed. */

#define MB_APPLMODAL =            (0x00000000) /* Gtk has no simple, cross-platform way to do system-wide modality */
#define MB_SYSTEMMODAL =          (0x00001000) /* Modal or non-modal relates only to the application. */
#define MB_TASKMODAL =            (0x00002000)
#define MB_NOFOCUS =              (0x00008000) /* Technically incorrect to use with MessageBox(), but often done so. */
#define MB_SETFOREGROUND =        (0x00010000)
#define MB_DEFAULT_DESKTOP_ONLY = (0x00020000)
#define MB_TOPMOST =              (0x00040000)

#define MB_RTLREADING =           (0x00100000) /* Handled by translation and internationalization subsystems. */

#ifdef __cplusplus
extern "C"
{
#endif
    /* Returns 1 if the first button was clicked, 2 for the second and 3 for the third. 
    Note, this is NOT the same behavior as the Win32 API. */
    int messagebox(const char *title, int flags, const char *fmt, ... );
//#define MessageBox(*title, flags, *fmt, ... ) messagebox(*title, flags, *fmt, ... );
    void gui_init();
#ifdef __cplusplus
}
#endif

#endif  /* __MESSAGEBOX_H__ */

