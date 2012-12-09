package paulscode.android.mupen64plusae;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes( formKey = "dG5qd1ZFcXFSSFNJYkhuYWFxcUwwR2c6MQ" )
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
