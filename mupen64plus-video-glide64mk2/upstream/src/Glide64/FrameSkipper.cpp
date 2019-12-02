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
#include "Gfx_1.3.h"
#include <SDL_timer.h>

FrameSkipper::FrameSkipper()
        : _skipType(AUTO), _maxSkips(2), _targetFPS(60),
          _skipCounter(0), _initialTicks(0), _actualFrame(0), _desiredFrame(0) {
}


bool FrameSkipper::willSkipNext() {
    //Frame skip disabled
    if (_maxSkips < 1) {
        return false;
    }

    //Manual frame skip
    if (_skipType == MANUAL) {
        if (_skipCounter < _maxSkips) {
            _skipCounter++;
            return true;
        } else {
            _skipCounter = 0;
            return false;
        }
    }

    //If we got this far, it's AUTO
    if (_desiredFrame > _actualFrame + 2 && _skipCounter < _maxSkips) {
        _skipCounter++;

        return true;
    } else if (_skipCounter == _maxSkips || _actualFrame == _desiredFrame) {
        _skipCounter = 0;
        _actualFrame = _desiredFrame;
    }

    return false;
}

void FrameSkipper::update() {
    if (_initialTicks == 0) {
        // First frame, initialize auto-skip variables
        _initialTicks = SDL_GetTicks();
        _actualFrame = 0;
        _skipCounter = 0;
    }

    if (_skipType == AUTO) {
        // Compute the frame number we want be at, based on elapsed time and target FPS
        unsigned int elapsedMilliseconds = SDL_GetTicks() - _initialTicks;
        _desiredFrame = (elapsedMilliseconds * _targetFPS) / 1000;

        // Record the frame number we are actually at
        _actualFrame++;
    }
}