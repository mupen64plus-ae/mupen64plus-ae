package paulscode.android.mupen64plusae.game;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import paulscode.android.mupen64plusae.persistent.GamePrefs;
import android.util.Log;

public class GameAutoSaveManager
{
    private final GamePrefs mGamePrefs;
    private String mAutoSavePath;
    private final int mMaxAutoSave;
    private static final String sFormatString = "yyyy-MM-dd-HH-mm-ss";
    private static final String sMatcherString = "^\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d\\.sav$";
    private static final String sDefaultString = "yyyy-mm-dd-hh-mm-ss.sav";
    
    public GameAutoSaveManager(GamePrefs gamePrefs, int maxAutoSaves)
    {
        mGamePrefs = gamePrefs;
        mAutoSavePath = mGamePrefs.autoSaveDir + "/";
        mMaxAutoSave = maxAutoSaves;
    }
    
    public String getLatestAutoSave()
    {
        List<String> result = new ArrayList<String>();
        File savePath = new File(mAutoSavePath);
        
        //Only find files that end with .sav
        FileFilter fileFilter = new FileFilter(){

            @Override
            public boolean accept(File pathname)
            {
                //It must match this format "yyyy-MM-dd-HH-mm-ss"
                String fileName = pathname.getName();
                return fileName.matches(sMatcherString);
            }
            
        };
        
        File[] fileList = savePath.listFiles(fileFilter);
        
        //Add all files found
        if(fileList != null)
        {
            for( File file : fileList )
            {            
                result.add( file.getPath() );
            }
        }
        
        //Sort by file name
        Collections.sort(result);
        
        String resultValue = "";
        if(result.size() > 0)
        {
            resultValue = result.get(result.size()-1);
        }
        else
        {
            //Fall back to this if we can't find a valid filename
            resultValue = mAutoSavePath + sDefaultString;
        }
        
        //Grab the last file
        return resultValue;
    }
    
    public void clearOldest()
    {
        List<File> result = new ArrayList<File>();
        File savePath = new File(mAutoSavePath);
        
        //Only find files that end with .sav
        FileFilter fileFilter = new FileFilter(){

            @Override
            public boolean accept(File pathname)
            {
                //It must match this format "yyyy-MM-dd-HH-mm-ss"
                String fileName = pathname.getName();
                return fileName.matches(sMatcherString);
            }
            
        };
        
        //Add all files found
        for( File file : savePath.listFiles(fileFilter) )
        {            
            result.add( file );
        }
        
        //Sort by file name
        Collections.sort(result);
        
        while(result.size() > (mMaxAutoSave-1))
        {
            Log.i("GameAutoSaveManager", "Deleting old autosave file: " + result.get(0).getName());
            result.get(0).delete();
            result.remove(0);
        }
    }
    
    public String getAutoSaveFileName()
    {
        DateFormat dateFormat = new SimpleDateFormat(sFormatString, java.util.Locale.getDefault());
        String dateAndTime = dateFormat.format(new Date()).toString();
        String fileName = dateAndTime + ".sav";
        
        return mAutoSavePath + fileName;
    }
}
