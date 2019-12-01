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
#include <SDL_timer.h>

FrameSkipper::FrameSkipper()
  : _skipType(AUTO), _maxSkips(2), _targetFPS(60),
    _skipCounter(0), _initialTicks(0), _actualFrame(0)
{
}

void FrameSkipper::update()
{
  if (_maxSkips < 1)
  {
    // Frameskip disabled, do nothing
  }
  else if (_skipType == MANUAL)
  {
    // Skip this frame based on a deterministic skip rate
    if (++_skipCounter > _maxSkips)
      _skipCounter = 0;
  }
  else if (_initialTicks > 0) // skipType == AUTO, running
  {
    // Compute the frame number we want be at, based on elapsed time and target FPS
    unsigned int elapsedMilliseconds = SDL_GetTicks() - _initialTicks;
    unsigned int desiredFrame = (elapsedMilliseconds * _targetFPS) / 1000;

    // Record the frame number we are actually at
    _actualFrame++;

    // See if we need to skip
    if (desiredFrame < _actualFrame)
    {
      // We are ahead of schedule, so do nothing
    }
    else if (desiredFrame > _actualFrame && _skipCounter < _maxSkips)
    {
      // We are behind schedule and we are allowed to skip this frame, so skip this frame
      _skipCounter++;
    }
    else
    {
      // We are on schedule, or we are not allowed to skip this frame...
      // ... so do not skip this frame
      _skipCounter = 0;
      // ... and pretend we are on schedule (if not already)
      _actualFrame = desiredFrame;
    }
  }
  else // skipType == AUTO, initializing
  {
    // First frame, initialize auto-skip variables
    _initialTicks = SDL_GetTicks();
    _actualFrame = 0;
    _skipCounter = 0;
  }
}
