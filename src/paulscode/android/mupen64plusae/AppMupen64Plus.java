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

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.AVAILABLE_MEM_SIZE;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.BUILD;
import static org.acra.ReportField.CRASH_CONFIGURATION;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.DEVICE_FEATURES;
import static org.acra.ReportField.DISPLAY;
import static org.acra.ReportField.DUMPSYS_MEMINFO;
import static org.acra.ReportField.ENVIRONMENT;
import static org.acra.ReportField.FILE_PATH;
import static org.acra.ReportField.INITIAL_CONFIGURATION;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.IS_SILENT;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PACKAGE_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.SHARED_PREFERENCES;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.TOTAL_MEM_SIZE;
import static org.acra.ReportField.USER_APP_START_DATE;
import static org.acra.ReportField.USER_COMMENT;
import static org.acra.ReportField.USER_CRASH_DATE;
import static org.acra.ReportField.USER_EMAIL;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender.Method;
import org.acra.sender.HttpSender.Type;

// @formatter:off
@ReportsCrashes
(
    formKey = "",
    formUri = "http://paulscode.iriscouch.com/acra-mupen64plusae/_design/acra-storage/_update/report",
    reportType = Type.JSON,
    httpMethod = Method.PUT,
    formUriBasicAuthLogin = "reporter",
    logcatArguments = { "-t", "300" },
    logcatFilterByPid = true,
    customReportContent =
    {
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
        SHARED_PREFERENCES
    }
)
// @formatter:on
public class AppMupen64Plus extends android.app.Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        
        // Initialize ACRA crash reporting system
        try
        {
            ACRAConfiguration config = ACRA.getNewDefaultConfig( this );
            config.setFormUriBasicAuthPassword( getPackageName().substring( 0, 29 ) );
            ACRA.setConfig( config );
            ACRA.init( this );
        }
        catch( Exception ignored )
        {
        }
    }
}
