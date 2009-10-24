/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - nogui.h                                                 *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2007-2008 Richard42 Ebenblues                           *
 *   Copyright (C) 2002 Hacktarux                                          *
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

/* Sound volume functions. */
#if defined(__linux__)
#include <sys/soundcard.h>
#endif
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <unistd.h> /* close() */
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>

#include "volume.h"
#include "../main/translate.h"

/* volSet
 *  Sets volume of left and right PCM channels to given percentage (0-100) value.
 */
void volSet(int percent)
{
    int ret, vol;
    int mixerfd = open("/dev/mixer", O_RDONLY);

    if(mixerfd < 0)
    {
        perror("/dev/mixer: ");
        return;
    }

    if(percent > 100)
        percent = 100;
    else if(percent < 0)
        percent = 0;

    vol = (percent << 8) + percent; // set both left/right channels to same vol
#if defined(__linux__)
    ret = ioctl(mixerfd, MIXER_WRITE(SOUND_MIXER_PCM), &vol);
#endif
    if(ret < 0)
        perror("Setting PCM volume: ");

    close(mixerfd);
}

/* volGet
 *  Returns volume of PCM channel as a percentage (0-100).
 *  Returns 0 on error.
 */
int volGet(void)
{
    int vol, ret;
    int mixerfd = open("/dev/mixer", O_RDONLY);

    if(mixerfd < 0)
    {
        perror("/dev/mixer: ");
        return 0;
    }

#if defined(__linux__)
    ret = ioctl(mixerfd, MIXER_READ(SOUND_MIXER_PCM), &vol);
#endif
    if(ret < 0)
        perror("Reading PCM volume: ");

    close(mixerfd);

    return vol & 0xff; // just return the left channel
}

