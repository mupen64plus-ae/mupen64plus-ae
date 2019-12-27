/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */

package paulscode.android.mupen64plusae;
import android.util.SparseArray;

import java.util.ArrayList;
import paulscode.android.mupen64plusae.persistent.GamePrefs;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Utility class that encapsulates the Core Service parameters
 */
@SuppressWarnings("WeakerAccess")
public class StartCoreServiceParams
{
    private String romGoodName;
    private String romDisplayName;
    private String romPath;
    private String zipPath;
    private String romMd5;
    private String romCrc;
    private String romHeaderName;
    private byte romCountryCode;
    private String romArtPath;
    private String cheatPath;
    private ArrayList<GamePrefs.CheatSelection> cheatOptions;
    private boolean isRestarting;
    private String saveToLoad;
    private String rspLib;
    private String gfxLib;
    private String audioLib;
    private String inputLib;

    private boolean useHighPriorityThread;
    private ArrayList<Integer> pakTypes;
    private boolean[] isPlugged;
    private boolean isFrameLimiterEnabled;

    private String coreUserDataDir;
    private String coreUserCacheDir;
    private String coreUserConfigDir;
    private String userSaveDir;
    private boolean useRaphnetDevicesIfAvailable;

    private SparseArray<String> mGbRomPaths = new SparseArray<>(4);
    private SparseArray<String> mGbRamPaths = new SparseArray<>(4);
    private String mDdRom = null;
    private String mDdDisk = null;

    public String getRomGoodName() {
        return romGoodName;
    }

    public void setRomGoodName(String romGoodName) {
        this.romGoodName = romGoodName;
    }

    public String getRomDisplayName() {
        return romDisplayName;
    }

    public void setRomDisplayName(String romDisplayName) {
        this.romDisplayName = romDisplayName;
    }

    public String getRomPath() {
        return romPath;
    }

    public void setRomPath(String romPath) {
        this.romPath = romPath;
    }

    public String getZipPath() {
        return zipPath;
    }

    public void setZipPath(String zipPath) {
        this.zipPath = zipPath;
    }

    public String getRomMd5() {
        return romMd5;
    }

    public void setRomMd5(String romMd5) {
        this.romMd5 = romMd5;
    }

    public String getRomCrc() {
        return romCrc;
    }

    public void setRomCrc(String romCrc) {
        this.romCrc = romCrc;
    }

    public String getRomHeaderName() {
        return romHeaderName;
    }

    public void setRomHeaderName(String romHeaderName) {
        this.romHeaderName = romHeaderName;
    }

    public byte getRomCountryCode() {
        return romCountryCode;
    }

    public void setRomCountryCode(byte romCountryCode) {
        this.romCountryCode = romCountryCode;
    }

    public String getRomArtPath() {
        return romArtPath;
    }

    public void setRomArtPath(String romArtPath) {
        this.romArtPath = romArtPath;
    }

    public String getCheatPath() {
        return cheatPath;
    }

    public void setCheatPath(String cheatPath) {
        this.cheatPath = cheatPath;
    }

    public ArrayList<GamePrefs.CheatSelection> getCheatOptions() {
        return cheatOptions;
    }

    public void setCheatOptions(ArrayList<GamePrefs.CheatSelection> cheatOptions) {
        this.cheatOptions = cheatOptions;
    }

    public boolean isRestarting() {
        return isRestarting;
    }

    public void setRestarting(boolean restarting) {
        isRestarting = restarting;
    }

    public String getSaveToLoad() {
        return saveToLoad;
    }

    public void setSaveToLoad(String saveToLoad) {
        this.saveToLoad = saveToLoad;
    }

    public String getRspLib() {
        return rspLib;
    }

    public void setRspLib(String rspLib) {
        this.rspLib = rspLib;
    }

    public String getGfxLib() {
        return gfxLib;
    }

    public void setGfxLib(String gfxLib) {
        this.gfxLib = gfxLib;
    }

    public String getAudioLib() {
        return audioLib;
    }

    public void setAudioLib(String audioLib) {
        this.audioLib = audioLib;
    }

    public String getInputLib() {
        return inputLib;
    }

    public void setInputLib(String inputLib) {
        this.inputLib = inputLib;
    }

    public boolean isUseHighPriorityThread() {
        return useHighPriorityThread;
    }

    public void setUseHighPriorityThread(boolean useHighPriorityThread) {
        this.useHighPriorityThread = useHighPriorityThread;
    }

    public ArrayList<Integer> getPakTypes() {
        return pakTypes;
    }

    public void setPakTypes(ArrayList<Integer> pakTypes) {
        this.pakTypes = pakTypes;
    }

    public boolean[] getIsPlugged() {
        return isPlugged;
    }

    public void setIsPlugged(boolean[] isPlugged) {
        this.isPlugged = isPlugged;
    }

    public boolean isFrameLimiterEnabled() {
        return isFrameLimiterEnabled;
    }

    public void setFrameLimiterEnabled(boolean frameLimiterEnabled) {
        isFrameLimiterEnabled = frameLimiterEnabled;
    }

    public String getCoreUserDataDir() {
        return coreUserDataDir;
    }

    public void setCoreUserDataDir(String coreUserDataDir) {
        this.coreUserDataDir = coreUserDataDir;
    }

    public String getCoreUserCacheDir() {
        return coreUserCacheDir;
    }

    public void setCoreUserCacheDir(String coreUserCacheDir) {
        this.coreUserCacheDir = coreUserCacheDir;
    }

    public String getCoreUserConfigDir() {
        return coreUserConfigDir;
    }

    public void setCoreUserConfigDir(String coreUserConfigDir) {
        this.coreUserConfigDir = coreUserConfigDir;
    }

    public String getUserSaveDir() {
        return userSaveDir;
    }

    public void setUserSaveDir(String userSaveDir) {
        this.userSaveDir = userSaveDir;
    }

    public boolean isUseRaphnetDevicesIfAvailable() {
        return useRaphnetDevicesIfAvailable;
    }

    public void setUseRaphnetDevicesIfAvailable(boolean useRaphnetDevicesIfAvailable) {
        this.useRaphnetDevicesIfAvailable = useRaphnetDevicesIfAvailable;
    }

    public String getGbRomPath(int player) {
        return mGbRomPaths.get(player);
    }

    public void setGbRomPath(int player, String path) {
        mGbRomPaths.put(player, path);
    }

    public String getGbRamPath(int player) {
        return mGbRamPaths.get(player);
    }

    public void setGbRamPath(int player, String path) {
        mGbRamPaths.put(player, path);
    }

    public String getDdRomPath() {
        return mDdRom;
    }

    public void setDdRomPath(String ddRomPath) {
        this.mDdRom = ddRomPath;
    }

    public String getDdDiskPath() {
        return mDdDisk;
    }

    public void setDdDiskPath(String ddDiskPath) {
        this.mDdDisk = ddDiskPath;
    }
}
