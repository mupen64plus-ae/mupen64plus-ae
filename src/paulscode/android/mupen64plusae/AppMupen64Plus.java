/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import static org.acra.ReportField.*;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes( formUri = "http://www.bugsense.com/api/acra?api_key=ad73db70", formKey = "", customReportContent = {
    REPORT_ID,
    APP_VERSION_CODE,
    APP_VERSION_NAME,
    PACKAGE_NAME,
    FILE_PATH,
    PHONE_MODEL,
    BRAND,
    PRODUCT,
    ANDROID_VERSION,
    BUILD,
    TOTAL_MEM_SIZE,
    AVAILABLE_MEM_SIZE,
    CUSTOM_DATA,
    IS_SILENT,
    STACK_TRACE,
    INITIAL_CONFIGURATION,
    CRASH_CONFIGURATION,
    DISPLAY,
    USER_COMMENT,
    USER_EMAIL,
    USER_APP_START_DATE,
    USER_CRASH_DATE,
    DUMPSYS_MEMINFO,
    LOGCAT,
    INSTALLATION_ID,
    DEVICE_FEATURES,
    ENVIRONMENT,
    SHARED_PREFERENCES,
    SETTINGS_SYSTEM
} )
public class AppMupen64Plus extends android.app.Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        
        // Initialize ACRA crash reporting system
        ACRA.init( this );
    }
}
