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
 * Authors: Paul Lamb, lioncash
 */
package emulator.android.mupen64plusae.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class which collects a bunch of commonly used methods into one class.
 */
public final class Utility
{
    /**
     * Clamps a value to the limit defined by min and max.
     * 
     * @param val The value to clamp to min and max.
     * @param min The lowest number val can be equal to.
     * @param max The largest number val can be equal to.
     * 
     * @return If the value is lower than min, min is returned. <br/>
     *         If the value is higher than max, max is returned.
     */
    public static<T extends Comparable<? super T>> T clamp( T val, T min, T max )
    {
        final T temp;

        //  val < max
        if ( val.compareTo(max) < 0 )
            temp = val;
        else
            temp = max;

        // temp > min
        if ( temp.compareTo(min) > 0 )
            return temp;
        else
            return min;
    }

    public static String executeShellCommand(String... args)
    {
        StringBuilder result = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(args);
            BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

            ExecutorService executor = Executors.newFixedThreadPool(1);

            // Read data with timeout
            Callable<String> readTask = reader::readLine;

            String line;

            do
            {
                Future<String> future = executor.submit(readTask);
                line = future.get(1000, TimeUnit.MILLISECONDS);

                result.append(line).append("\n");
            }
            while(line != null);

        }
        catch( InterruptedException | ExecutionException | TimeoutException| IOException e )
        {
            e.printStackTrace();
        }

        return result.toString();
    }
}
