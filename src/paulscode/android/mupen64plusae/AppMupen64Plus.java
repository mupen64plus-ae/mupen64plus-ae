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
