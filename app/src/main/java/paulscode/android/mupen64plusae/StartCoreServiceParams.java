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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.persistent.AudioPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DataPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DefaultsPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DisplayPrefsActivity;
import paulscode.android.mupen64plusae.persistent.GamePrefsActivity;
import paulscode.android.mupen64plusae.persistent.InputPrefsActivity;
import paulscode.android.mupen64plusae.persistent.LibraryPrefsActivity;
import paulscode.android.mupen64plusae.persistent.TouchscreenPrefsActivity;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivity;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivityBigScreen;
import paulscode.android.mupen64plusae.profile.EmulationProfileActivity;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.profile.TouchscreenProfileActivity;
import paulscode.android.mupen64plusae.task.CacheRomInfoService;
import paulscode.android.mupen64plusae.task.DeleteFilesService;
import paulscode.android.mupen64plusae.task.ExtractRomService;
import paulscode.android.mupen64plusae.task.ExtractTexturesService;
import paulscode.android.mupen64plusae.util.LogcatActivity;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Utility class that encapsulates the Core Service parameters
 */
public class StartCoreServiceParams
{
    private String romGoodName;
    private String romDisplayName;
    private String romPath;
    private String romMd5;
    private String romCrc;
    private String romHeaderName;
    private byte romCountryCode;
    private String romArtPath;
    private String romLegacySave;
    private String cheatOptions;
    private boolean isRestarting;
    private String saveToLoad;
    private String coreLib;
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

    public String getRomLegacySave() {
        return romLegacySave;
    }

    public void setRomLegacySave(String romLegacySave) {
        this.romLegacySave = romLegacySave;
    }

    public String getCheatOptions() {
        return cheatOptions;
    }

    public void setCheatOptions(String cheatOptions) {
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

    public String getCoreLib() {
        return coreLib;
    }

    public void setCoreLib(String coreLib) {
        this.coreLib = coreLib;
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
}
