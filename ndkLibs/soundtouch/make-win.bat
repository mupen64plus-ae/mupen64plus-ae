@REM 
@REM SoundTouch & SoundStretch Build script for Win32 platform
@REM 
@REM You'll need Visual C++ 6.0 installed to compile - also execute the 
@REM "vcvars32.bat" in VC install directotry before running this one.
@REM 
@REM Copyright (c) Olli Parviainen
@REM File Created: 09/Sep/2003
@REM
@REM $Id: make-win.bat 254 2017-07-30 09:56:40Z oparviai $ 

@rem ****************************
@rem try first for VS6.0 support

@if "%MsDevDir%"=="" goto nomsdevdir

md bin
md lib
msdev source\SoundTouch\SoundTouch.dsw /MAKE ALL
msdev source\SoundStretch\SoundStretch.dsw /MAKE ALL

goto end

:nomsdevdir

@rem **********************************
@rem try with devenv for VS2003 support

@if "%DevEnvDir%"=="" goto nodevdir

md bin
md lib
devenv source\SoundTouch\SoundTouch.vcproj /upgrade
devenv source\SoundTouch\SoundTouch.vcproj /build debug
devenv source\SoundTouch\SoundTouch.vcproj /build release
devenv source\SoundTouch\SoundTouch.vcproj /build releasex64

devenv source\SoundStretch\SoundStretch.sln /upgrade
devenv source\SoundStretch\SoundStretch.sln /build debug
devenv source\SoundStretch\SoundStretch.sln /build release
devenv source\SoundStretch\SoundStretch.sln /build releasex64

devenv source\SoundTouchDll\SoundTouchDll.sln /upgrade
devenv source\SoundTouchDll\SoundTouchDll.sln /build debug
devenv source\SoundTouchDll\SoundTouchDll.sln /build release
devenv source\SoundTouchDll\SoundTouchDll.sln /build releasex64

@goto end


:nodevdir

@echo off
echo ****************************************************************************
echo **
echo ** ERROR: Visual Studio path not set.
echo **
echo ** Run "vsvars32.bat" or "vcvars32.bat" from Visual Studio installation dir,
echo ** e.g. "C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin",
echo ** then try again.
echo **
echo ****************************************************************************

:end
