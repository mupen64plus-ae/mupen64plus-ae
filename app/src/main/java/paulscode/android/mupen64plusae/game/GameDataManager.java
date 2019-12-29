package paulscode.android.mupen64plusae.game;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;

public class GameDataManager
{
    private static final String V2 = "v2";

    private GlobalPrefs mGlobalPrefs;
    private final GamePrefs mGamePrefs;
    private String mAutoSavePath;
    private final int mMaxAutoSave;
    private static final String sFormatString = "yyyy-MM-dd-HH-mm-ss";
    private static final String sMatcherString = "^\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d\\..*sav$";
    private static final String sDefaultString = "yyyy-mm-dd-hh-mm-ss.sav";

    GameDataManager(GlobalPrefs globalPrefs, GamePrefs gamePrefs, int maxAutoSaves)
    {
        mGlobalPrefs = globalPrefs;
        mGamePrefs = gamePrefs;
        mAutoSavePath = mGamePrefs.getAutoSaveDir() + "/";
        mMaxAutoSave = maxAutoSaves;
    }

    String getLatestAutoSave()
    {
        final List<String> result = new ArrayList<>();
        final File savePath = new File(mAutoSavePath);

        //Only find files that end with .sav
        final FileFilter fileFilter = new FileFilter(){

            @Override
            public boolean accept(File pathname)
            {
                //It must match this format "yyyy-MM-dd-HH-mm-ss"
                final String fileName = pathname.getName();
                return fileName.matches(sMatcherString);
            }

        };

        final File[] fileList = savePath.listFiles(fileFilter);

        //Add all files found
        if(fileList != null)
        {
            for( final File file : fileList )
            {
                //Do not attempt to load states that are missing a corresponding ".complete" file
                //but only check for .complete if it's a V2 file
                File completeFile = new File(file.getPath() + "." + CoreService.COMPLETE_EXTENSION);
                if(!file.getPath().contains(V2) || completeFile.exists())
                {
                    result.add( file.getPath() );
                }
            }
        }

        //Sort by file name
        Collections.sort(result);

        String resultValue;
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

    void clearOldest()
    {
        final List<File> result = new ArrayList<>();
        final File savePath = new File(mAutoSavePath);

        File[] allFiles = savePath.listFiles();
        if(allFiles != null && allFiles.length != 0)
        {
            //Only find files that match the matcher string
            final FileFilter fileFilter = new FileFilter(){

                @Override
                public boolean accept(File pathname)
                {
                    //It must match this format "yyyy-MM-dd-HH-mm-ss"
                    final String fileName = pathname.getName();
                    return fileName.matches(sMatcherString);
                }

            };

            //Add all files found
            File[] filterFiles = savePath.listFiles(fileFilter);

            if (filterFiles != null) {
                Collections.addAll(result, filterFiles );

                //Sort by file name
                Collections.sort(result);
            }

            while(result.size() > mMaxAutoSave)
            {
                Log.i("GameDataManager", "Deleting old autosave file: " + result.get(0).getName());
                File theResult = result.get(0);

                if(!theResult.isDirectory())
                {
                    boolean deleteResult = theResult.delete();

                    //Also remove the corresponding ".complete" file
                    String completeFile = theResult.getAbsolutePath();
                    completeFile = completeFile + "." + CoreService.COMPLETE_EXTENSION;
                    deleteResult = deleteResult && new File(completeFile).delete();

                    if (!deleteResult) {
                        Log.w("GameDataManager", "Unable to delete autosave file: " + result.get(0).getName());
                    }
                }
                result.remove(0);
            }
        }
    }

    String getAutoSaveFileName()
    {
        final DateFormat dateFormat = new SimpleDateFormat(sFormatString, java.util.Locale.getDefault());
        final String dateAndTime = dateFormat.format(new Date());
        final String fileName = dateAndTime + "." + V2 + ".sav";

        return mAutoSavePath + fileName;
    }


    public void makeDirs()
    {
        // Attempt to make base game dir first, if we can't, switch to alternate
        // FAT32 compatible name
        FileUtil.makeDirs(mGamePrefs.getGameDataDir());

        if( !new File(mGamePrefs.getGameDataDir()).exists()) {
            mGamePrefs.useAlternateGameDataDir();
        }

        FileUtil.makeDirs(mGamePrefs.getGameDataDir());

        //If the above didn't work, go with 2nd alternative name, which is just the md5
        if( !new File(mGamePrefs.getGameDataDir()).exists()) {
            mGamePrefs.useSecondAlternateGameDataDir();
        }

        // Make sure various directories exist so that we can write to them
        FileUtil.makeDirs(mGamePrefs.getSramDataDir());
        FileUtil.makeDirs(mGamePrefs.getAutoSaveDir());
        FileUtil.makeDirs(mGamePrefs.getSlotSaveDir());
        FileUtil.makeDirs(mGamePrefs.getUserSaveDir());
        FileUtil.makeDirs(mGamePrefs.getCoreUserConfigDir());
        FileUtil.makeDirs(mGlobalPrefs.coreUserDataDir);
        FileUtil.makeDirs(mGlobalPrefs.coreUserCacheDir);
        mAutoSavePath = mGamePrefs.getAutoSaveDir() + "/";
    }
}
