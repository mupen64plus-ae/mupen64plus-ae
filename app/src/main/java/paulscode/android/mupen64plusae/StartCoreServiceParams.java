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
    private boolean isRestarting;
    private boolean useRaphnetDevicesIfAvailable;
    private int videoRenderWidth;
    private int videoRenderHeight;
    private boolean usingNetplay;
    private boolean resolutionReset;

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

    public boolean isRestarting() {
        return isRestarting;
    }

    public void setRestarting(boolean restarting) {
        isRestarting = restarting;
    }

    public boolean isUseRaphnetDevicesIfAvailable() {
        return useRaphnetDevicesIfAvailable;
    }

    public void setUseRaphnetDevicesIfAvailable(boolean useRaphnetDevicesIfAvailable) {
        this.useRaphnetDevicesIfAvailable = useRaphnetDevicesIfAvailable;
    }

    public int getVideoRenderWidth() {
        return videoRenderWidth;
    }

    public void setVideoRenderWidth(int videoRenderWidth) {
        this.videoRenderWidth = videoRenderWidth;
    }

    public int getVideoRenderHeight() {
        return videoRenderHeight;
    }

    public void setVideoRenderHeight(int videoRenderHeight) {
        this.videoRenderHeight = videoRenderHeight;
    }

    public boolean getResolutionReset() {
        return resolutionReset;
    }

    public void setResolutionReset(boolean resolutionReset) {
        this.resolutionReset = resolutionReset;
    }

    public void setUsingNetplay(boolean usingNetplay) {
        this.usingNetplay = usingNetplay;
    }

    public boolean isUsingNetplay() {
        return this.usingNetplay;
    }

}
