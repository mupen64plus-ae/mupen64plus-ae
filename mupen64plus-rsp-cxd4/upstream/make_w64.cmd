@ECHO OFF
TITLE MinGW Compiler Suite Invocation

REM If you have installed MinGW-w64 without using MSYS2 to obtain the package
REM (or just not yet installed at all), this build script may not work out of
REM the box for most Windows users.  If you have Cygwin instead or whatever
REM else, be sure to adjust the path below, or execute "make.sh" in a Git shell.

REM The following line is the only one you should ever need to change.
set mingw64=C:\msys64\mingw64

REM The following two variables are irrelevant, unless you set a 32-bit target.
set mingw32=%mingw64%\..\mingw32
set lib=%mingw32%\i686-w64-mingw32\lib

set lib64=%mingw64%\x86_64-w64-mingw32\lib
set bin=%mingw64%\bin
set inc=%lib64%\..\include

REM set rsp=%USERPROFILE%\rsp
set rsp=%CD%
set obj=%rsp%\obj

set OBJ_LIST=^
 "%obj%\module.o"^
 "%obj%\su.o"^
 "%obj%\vu\vu.o"^
 "%obj%\vu\multiply.o"^
 "%obj%\vu\add.o"^
 "%obj%\vu\select.o"^
 "%obj%\vu\logical.o"^
 "%obj%\vu\divide.o"

set FLAGS_ANSI=-Wall -pedantic^
 -DPLUGIN_API_VERSION=0x0101^
 -mstackrealign^
 -march=native
set FLAGS_x86=-Wall -pedantic^
 -DPLUGIN_API_VERSION=0x0101^
 -DARCH_MIN_SSE2^
 -masm=intel^
 -mstackrealign^
 -march=native
set C_FLAGS=%FLAGS_x86%

if not exist obj (
mkdir obj
cd obj
mkdir vu
)
cd /D %bin%

ECHO Compiling C source code...
@ECHO ON
gcc -S -Os %C_FLAGS% -o "%obj%\module.asm"      "%rsp%\module.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\su.asm"          "%rsp%\su.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\vu\vu.asm"       "%rsp%\vu\vu.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\vu\multiply.asm" "%rsp%\vu\multiply.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\vu\add.asm"      "%rsp%\vu\add.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\vu\select.asm"   "%rsp%\vu\select.c"
gcc -S -O3 %C_FLAGS% -o "%obj%\vu\logical.asm"  "%rsp%\vu\logical.c"
gcc -S -O2 %C_FLAGS% -o "%obj%\vu\divide.asm"   "%rsp%\vu\divide.c"
@ECHO OFF
ECHO.

ECHO Assembling compiled sources...
as -o "%obj%\module.o"            "%obj%\module.asm"
as -o "%obj%\su.o"                "%obj%\su.asm"
as -o "%obj%\vu\vu.o"             "%obj%\vu\vu.asm"
as -o "%obj%\vu\multiply.o"       "%obj%\vu\multiply.asm"
as -o "%obj%\vu\add.o"            "%obj%\vu\add.asm"
as -o "%obj%\vu\select.o"         "%obj%\vu\select.asm"
as -o "%obj%\vu\logical.o"        "%obj%\vu\logical.asm"
as -o "%obj%\vu\divide.o"         "%obj%\vu\divide.asm"
ECHO.

ECHO Linking assembled object files...
ld --shared -e DllMain -o "%obj%\rspdebug.dll" -L%lib64% %OBJ_LIST% -lmsvcrt
strip -o "%obj%\rsp.dll" "%obj%\rspdebug.dll" --strip-all
PAUSE
