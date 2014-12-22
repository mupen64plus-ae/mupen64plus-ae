/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Copyright (C) 2011 yongzh (freeman.yong@gmail.com)                    *
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

#include "FrameSkipper.h"
#include <time.h>

FrameSkipper::FrameSkipper()
	: skipType(AUTO), maxSkips(2), targetFPS(60),
	  skipCounter(0), initialTicks(0), actualFrame(0)
{
}

void FrameSkipper::start()
{
	initialTicks = 0;
}

void FrameSkipper::update()
{
	if (maxSkips < 1)
	{
		// Frameskip disabled, do nothing
	}
	else if (skipType == MANUAL)
	{
		// Skip this frame based on a deterministic skip rate
		if (++skipCounter > maxSkips)
			skipCounter = 0;
	}
	else if (initialTicks > 0) // skipType == AUTO, running
	{
		// Compute the frame number we want be at, based on elapsed time and target FPS
		unsigned int elapsedMilliseconds = getCurrentTicks() - initialTicks;
		unsigned int desiredFrame = (elapsedMilliseconds * targetFPS) / 1000;

		// Record the frame number we are actually at
		actualFrame++;

		// See if we need to skip
		if (desiredFrame < actualFrame)
		{
			// We are ahead of schedule, so do nothing
		}
		else if (desiredFrame > actualFrame && skipCounter < maxSkips)
		{
			// We are behind schedule and we are allowed to skip this frame, so skip this frame
			skipCounter++;
		}
		else
		{
			// We are on schedule, or we are not allowed to skip this frame...
			// ... so do not skip this frame
			skipCounter = 0;
			// ... and pretend we are on schedule (if not already)
			actualFrame = desiredFrame;
		}
	}
	else // skipType == AUTO, initializing
	{
		// First frame, initialize auto-skip variables
		initialTicks = getCurrentTicks();
		actualFrame = 0;
		skipCounter = 0;
	}
}

unsigned int FrameSkipper::getCurrentTicks()
{
	// Get the number of milliseconds since system epoch
	struct timespec now;
	clock_gettime(CLOCK_MONOTONIC, &now);
	return (now.tv_sec) * 1000 + (now.tv_nsec) / 1000000;
}
