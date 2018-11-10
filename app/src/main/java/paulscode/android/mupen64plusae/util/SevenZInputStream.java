/*
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
 * Authors:
 */

package paulscode.android.mupen64plusae.util;

import androidx.annotation.NonNull;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.IOException;
import java.io.InputStream;

public final class SevenZInputStream extends InputStream {

    private SevenZFile mZipFile;

    public SevenZInputStream(SevenZFile zipFile)
    {
        mZipFile = zipFile;
    }

    @Override
    public int read() throws IOException
    {
        return mZipFile.read();
    }

    public int read(@NonNull byte b[], int off, int len) throws IOException
    {
        return mZipFile.read(b, off, len);
    }

    @Override
    public void close() throws IOException
    {
        //Nothing to do here, entries can't be closed
    }
}
