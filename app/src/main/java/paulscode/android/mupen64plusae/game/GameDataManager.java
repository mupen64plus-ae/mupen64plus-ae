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

        if(savePath.listFiles() != null && savePath.listFiles().length != 0)
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
            Collections.addAll(result, savePath.listFiles(fileFilter) );

            //Sort by file name
            Collections.sort(result);

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
        FileUtil.makeDirs(mGamePrefs.getScreenshotDir());
        FileUtil.makeDirs(mGamePrefs.getCoreUserConfigDir());
        FileUtil.makeDirs(mGlobalPrefs.coreUserDataDir);
        FileUtil.makeDirs(mGlobalPrefs.coreUserCacheDir);
        mAutoSavePath = mGamePrefs.getAutoSaveDir() + "/";
    }

    /**
     * Move any legacy files to new folder structure
     */
    void moveFromLegacy()
    {
        final File legacySlotPath = new File(mGlobalPrefs.legacySlotSaves);
        final File legacyAutoSavePath = new File(mGlobalPrefs.legacyAutoSaves);

        if (legacySlotPath.listFiles() != null)
        {
            // Move sra, mpk, fla, and eep files
            final FileFilter fileSramFilter = new FileFilter()
            {

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();

                    return fileName.contains(mGamePrefs.gameGoodName + ".sra")
                            || fileName.contains(mGamePrefs.gameGoodName + ".eep")
                            || fileName.contains(mGamePrefs.gameGoodName + ".mpk")
                            || fileName.contains(mGamePrefs.gameGoodName + ".fla");
                }
            };

            // Move all files found
            for (final File file : legacySlotPath.listFiles(fileSramFilter))
            {
                String targetPath = mGamePrefs.getSramDataDir() + "/" + file.getName();
                File targetFile = new File(targetPath);

                if (!targetFile.exists())
                {
                    Log.i("GameDataManager", "Found legacy SRAM file: " + file + " Moving to " + targetFile.getPath());

                    if( !file.renameTo(targetFile)) {
                        Log.w("GameDataManager", "Error renaming legacy SRAM file: " + file + " target " + targetFile.getPath());
                    }
                }
                else
                {
                    Log.i("GameDataManager", "Found legacy SRAM file: " + file + " but can't move");
                }
            }

            // Move all st files
            final FileFilter fileSlotFilter = new FileFilter()
            {

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();
                    return fileName.contains(mGamePrefs.gameGoodName)
                            && fileName.substring(fileName.length() - 3).contains("st");
                }
            };

            for (final File file : legacySlotPath.listFiles(fileSlotFilter))
            {
                String targetPath = mGamePrefs.getSlotSaveDir() + "/" + file.getName();
                File targetFile = new File(targetPath);

                if (!targetFile.exists())
                {
                    Log.i("GameDataManager", "Found legacy ST file: " + file + " Moving to " + targetFile.getPath());

                    if (!file.renameTo(targetFile)) {
                        Log.w("GameDataManager", "Error renaming legacy ST file: " + file + " to " + targetFile.getPath());
                    }
                }
                else
                {
                    Log.i("GameDataManager", "Found legacy ST file: " + file + " but can't move");
                }
            }
        }

        if(legacyAutoSavePath.listFiles() != null)
        {
            //Move auto saves
            final FileFilter fileAutoSaveFilter = new FileFilter(){

                @Override
                public boolean accept(File pathname)
                {
                    final String fileName = pathname.getName();
                    return fileName.equals(mGamePrefs.legacySaveFileName + ".sav");
                }
            };

            //Move all files found
            for( final File file : legacyAutoSavePath.listFiles(fileAutoSaveFilter) )
            {
                final DateFormat dateFormat = new SimpleDateFormat(GameDataManager.sFormatString, java.util.Locale.getDefault());
                final String dateAndTime = dateFormat.format(new Date());
                final String fileName = dateAndTime + ".sav";

                String targetPath = mGamePrefs.getAutoSaveDir() + "/" + fileName;
                File targetFile= new File(targetPath);

                if(!targetFile.exists())
                {
                    Log.i("GameDataManager", "Found legacy SAV file: " + file +
                            " Moving to " + targetFile.getPath());

                    if (!file.renameTo(targetFile)) {
                        Log.w("GameDataManager", "Error renaming SAV file: " + file +
                                " to " + targetFile.getPath());
                    }
                }
                else
                {
                    Log.i("GameDataManager", "Found legacy SAV file: " + file +
                            " but can't move");
                }
            }
        }
    }
}
