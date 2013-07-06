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
#include "ticks.h"

FrameSkipper::FrameSkipper()
	: skipType(AUTO), maxSkips(2), targetFPS(60)
{
}

void FrameSkipper::start()
{
	initialTicks = 0;
	virtualCount = 0;
	skipCounter = 0;
}

void FrameSkipper::update()
{
	// for the first frame
	if (initialTicks == 0) {
		initialTicks = ticksGetTicks();
		return;
	}

	unsigned int elapsed = ticksGetTicks() - initialTicks;
	unsigned int realCount = elapsed * targetFPS / 1000;

	virtualCount++;
	if (realCount >= virtualCount) {
		if (realCount > virtualCount &&
				skipType == AUTO && skipCounter < maxSkips) {
			skipCounter++;
		} else {
			virtualCount = realCount;
			if (skipType == AUTO)
				skipCounter = 0;
		}
	}
	if (skipType == MANUAL) {
		if (++skipCounter > maxSkips)
			skipCounter = 0;
	}
}
