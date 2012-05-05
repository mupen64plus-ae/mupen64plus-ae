mupen64plus-ae
==============

Mupen64Plus, Android Edition (AE) is a port of Mupen64Plus to Android. (Not officially supported by the Mupen64Plus team)


Mupen64Plus, AE Build Instructions

Author: Paul Lamb

(for assistance, visit http://www.paulscode.com)


The setup for this project is slightly more complex than a normal Android project.  This is because there is a problem with the core if it is not built with the "APP_OPTIM := debug" option specified in Application.mk.  However, adding this option to the entire project affects all the libraries, not just the core.  This results in the emulator running noticeably slower.  To work around this problem, I've separated the core from the main project, and set up the Android.mk files to link with it.  That way, the core project can have "APP_OPTIM := debug" specified, and the main project can have "APP_OPTIM := release".  At some point it would be useful to figure out why the core has this problem in the first place.  Fixing the problem so the core could be built along with the main project with "APP_OPTIM := release" would not only reduce the complexity of the project, but it would most likely speed up the emulator as well.


To build from source (follow steps in this order):

1) Run ndk-build in the core-debug folder

2) Run ndk-build in the main folder

3) Run "ant debug" (or "ant release") in the main folder, to generate the .apk file(s)
