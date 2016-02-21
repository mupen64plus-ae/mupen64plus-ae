@REM 
@REM SoundTouch & SoundStretch Build script for Win32 platform
@REM 
@REM You'll need Visual C++ 6.0 installed to compile - also execute the 
@REM "vcvars32.bat" in VC install directotry before running this one.
@REM 
@REM Copyright (c) Olli Parviainen
@REM File Created: 09/Sep/2003
@REM
@REM $Id: make-win.bat 197 2014-04-06 18:03:48Z oparviai $ 

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

devenv source\SoundStretch\SoundStretch.vcproj /upgrade
devenv source\SoundStretch\SoundStretch.vcproj /build debug
devenv source\SoundStretch\SoundStretch.vcproj /build release
devenv source\SoundStretch\SoundStretch.vcproj /build releasex64

devenv source\SoundTouchDll\SoundTouchDll.vcproj /upgrade
devenv source\SoundTouchDll\SoundTouchDll.vcproj /build debug
devenv source\SoundTouchDll\SoundTouchDll.vcproj /build release
devenv source\SoundTouchDll\SoundTouchDll.vcproj /build releasex64

@goto end


:nodevdir

@echo off
echo *********************************************************************
echo **
echo ** ERROR: Visual Studio path not set.
echo **
echo ** Run "vsvars32.bat" or "vcvars32.bat" from Visual Studio installation
echo ** directory, e.g. "\Program Files\Microsoft Visual Studio\VC98\Bin",
echo ** then try again.
echo **
echo *********************************************************************

:end
