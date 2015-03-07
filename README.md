mupen64plus-ae
==============

Mupen64Plus, Android Edition (AE) is an Android user interface for Mupen64Plus.

Please visit [the official forum](http://www.paulscode.com/forum/index.php) for support and discussion.


Build Instructions
==================

For now, we recommend using Eclipse rather than Android Studio, since the project contains a heavy mix of C, C++, and Java source code.

1. Download and install the prerequisites
   - [Android SDK](http://developer.android.com/sdk/index.html)
   - [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html)
   - [Eclipse ADT plugin](http://developer.android.com/sdk/installing/installing-adt.html)
2. Clone the mupen64plus-ae repository and initialize the working copy
   - `git clone https://github.com/mupen64plus-ae/mupen64plus-ae.git`
   - `cp .project.init .project`
3. Import the Eclipse project and dependencies
   - Open Eclipse
   - Select File → Import → Android → Existing Android Code Into Workspace, and press Next
   - Browse to root of cloned repository, and press OK
   - Select all projects, and press Finish
4. Add native support to the Eclipse project
   - Right-click the mupen64plus-ae project in the Eclipse Package Explorer window
   - Select Android Tools → Add Native Support...
   - Accept the default library name (mupen64plus-ae), and press Finish
   - Delete the unneeded generated C++ file: `rm jni/mupen64plus-ae.cpp`
5. Build and run the app from Eclipse
   - Select the mupen64plus-ae project in the Eclipse Package Explorer window
   - Select Run → Run
