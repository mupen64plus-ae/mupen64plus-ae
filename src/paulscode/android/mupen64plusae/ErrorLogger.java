package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.persistent.Config;

public class ErrorLogger
{
    private static Config error_log;
    private static String lastMessage = null;

    public static void initialize( String filename )
    {
        error_log = new Config( filename );
    }
    
    public static boolean hasError()
    {
        return lastMessage != null;
    }
    
    public static void clearLastError()
    {
        lastMessage = null;
    }
    
    public static String getLastError()
    {
        return lastMessage;
    }
    
    public static void setLastError( String message )
    {
        lastMessage = message;
    }    
    
    public static void put(String section, String parameter, String value )
    {
        error_log.put( section, parameter, value );
        error_log.save();
    }
    
    public static void putLastError(String section, String parameter)
    {
        put(section, parameter, lastMessage);
    }
}
