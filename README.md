mupen64plus-ae
==============

Mupen64Plus, Android Edition (AE) is a port of Mupen64Plus to Android. (Not officially supported by the Mupen64Plus team)


Mupen64Plus, AE Build Instructions
==================================

Author: Paul Lamb

(for assistance, visit http://www.paulscode.com)






Downloading the source:
-----------------------

1. From the terminal, go to the folder where you want to put mupen64plus-ae
   (for Eclipse users, this should be ~/workspace/)

2. Clone the git repository
   (example: "git clone https://github.com/paulscode/mupen64plus-ae.git")






Setting up the project:
-----------------------

1. From the terminal, enter the mupen64plus-ae folder

2. Run "android list targets" to find the index ## for API level 17 or higher

3. Update the Android project
   (example: "android update project --target ## --name Mupen64Plus --path .")






Eclipse users:
--------------

1. Install or update to the latest ADT plug-in (and optionally the Sequoyah Android Native Support plug-ins)

2. Import existing project into the workspace, and choose the mupen64plus-ae folder

3. If using Sequoyah, right-click on the mupen64plus-ae project and choose "Android->Add native support"

4. Check the paths to the SDK and NDK






Building from the command line:
-------------------------------

1. Navigate to the mupen64plus-ae folder

2. Run "ndk-build" to generate the native libraries

3. Run "ant debug" (or "ant release") to generate the .apk file(s)
