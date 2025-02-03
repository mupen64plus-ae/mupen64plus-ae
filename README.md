# Mupen64Plus-AE

Mupen64Plus, Android Edition (AE) is an Android user interface for Mupen64Plus.

Please visit [the official forum](http://www.paulscode.com/forum/index.php) for support and discussion.

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/org.mupen64plusae.v3.alpha/)


## Nightly Builds

### Download the latest builds from continuous integration:

| Name           | Status                            | File                                       |
|----------------|-----------------------------------|--------------------------------------------|
| Mupen64Plus-AE | [![Build Status][Build]][Actions] | [![Emulator][Download]][mupen64plus-ae]  |

[Actions]: https://github.com/mupen64plus-ae/mupen64plus-ae/actions/workflows/build.yml
[Build]: https://github.com/mupen64plus-ae/mupen64plus-ae/actions/workflows/build.yml/badge.svg
[Download]: https://img.shields.io/badge/Download-blue
[mupen64plus-ae]: https://github.com/mupen64plus-ae/mupen64plus-ae/releases/download/Pre-release/mupen64plus-ae-master.zip

## Build Instructions

1. Download and install the prerequisites
   - [Android Studio](https://developer.android.com/studio/index.html)
   - During the installation, make sure the latest SDK and NDK
   - If running Windows, make sure you install Git, Python, awk and required Microsoft Visual C++ Redistributable (i.e. cmake 3.18.1 requires Microsoft Visual C++ Redistributable 2015) and that the binaries are in your path environment variable.
2. Clone the mupen64plus-ae repository and initialize the working copy
   - `git clone https://github.com/mupen64plus-ae/mupen64plus-ae.git`
3. Open the project using Android Studio
4. Build and run the app from Android Studio
   - Select Build --> Make Project to build
   - Select Run --> Run app to run
