/*
 * Simple DirectMedia Layer Java source code (C) 2009-2011 Sergii Pylypenko
 * 
 * This software is provided 'as-is', without any express or implied warranty. In no event will the
 * authors be held liable for any damages arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, including commercial
 * applications, and to alter it and redistribute it freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim that you wrote the
 * original software. If you use this software in a product, an acknowledgment in the product
 * documentation would be appreciated but is not required. 2. Altered source versions must be
 * plainly marked as such, and must not be misrepresented as being the original software. 3. This
 * notice may not be removed or altered from any source distribution.
 */

// Taken from from Pelya's Android SDL port.
// THIS IS NOT THE ORIGINAL SOURCE, IT HAS BEEN ALTERED TO FIT THIS APP
// (05SEP2011, http://www.paulscode.com)

package paulscode.android.mupen64plusae.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.persistent.Paths;
import android.app.Activity;
import android.content.res.Resources;
import android.text.SpannedString;
import android.util.Log;
import android.widget.TextView;

public class DataDownloader extends Thread
{
    public interface Listener
    {
        void onDownloadComplete();
    }
    
    public StatusWriter mStatus;
    public boolean mDownloadComplete = false;
    public boolean mDownloadFailed = false;
    private Activity mActivity;
    private Listener mListener;
    private String outFilesDir = null;
    
    public DataDownloader( Activity parent, Listener listener, TextView status )
    {
        mActivity = parent;
        mListener = listener;
        mStatus = new StatusWriter( status, parent );
        outFilesDir = Globals.paths.dataDir;
        mDownloadComplete = false;
        start();
    }
    
    public void setStatusField( TextView view )
    {
        synchronized( this )
        {
            mStatus.setParent( view, mActivity );
        }
    }
    
    @Override
    public void run()
    {
        String[] downloadFiles = Paths.dataDownloadUrl.split( "\\^" );
        for( int i = 0; i < downloadFiles.length; i++ )
        {
            if( downloadFiles[i].length() > 0 )
                if( !downloadDataFile( downloadFiles[i],
                        "libsdl-DownloadFinished-" + String.valueOf( i ) + ".flag" ) )
                {
                    mDownloadFailed = true;
                    return;
                }
        }
        mDownloadComplete = true;
        synchronized( this )
        {
            if( mActivity != null )
            {
                mActivity.runOnUiThread( new Runnable()
                {
                    public void run()
                    {
                        if( mListener != null )
                            mListener.onDownloadComplete();
                    }
                } );
            }
        }
    }
    
    public boolean downloadDataFile( final String dataDownloadUrl, final String downloadFlagFileName )
    {
        Resources res = mActivity.getResources();
        
        // Get the download URLs, must be at least two
        String[] downloadUrls = dataDownloadUrl.split( "[|]" );
        if( downloadUrls.length < 2 )
            return false;
        
        // See if the download is even necessary
        String path = getOutFilePath( downloadFlagFileName );
        if( !isDownloadRequired( downloadUrls, path ) )
        {
            mStatus.setText( res.getString( R.string.download_unneeded ) );
            return true;
        }
        
        // Create output directory (not necessary for phone storage)
        createOutputDirectory();
        
        HttpResponse response = null;
        HttpGet request;
        long totalLen = 0;
        CountingInputStream stream;
        byte[] buf = new byte[16384];
        
        String url = "";
        boolean doNotUnzip = false;
        boolean fileInAssets = false;
        int downloadUrlIndex = 1;
        
        while( downloadUrlIndex < downloadUrls.length )
        {
            Log.i( "DataDownloader", "Processing download " + downloadUrls[downloadUrlIndex] );
            url = downloadUrls[downloadUrlIndex];
            doNotUnzip = false;
            if( url.indexOf( ':' ) == 0 )
            {
                url = url.substring( url.indexOf( ':', 1 ) + 1 );
                doNotUnzip = true;
            }
            mStatus.setText( res.getString( R.string.connecting_to, url ) );
            if( !url.contains( "http://" ) && !url.contains( "https://" ) ) // File inside assets
            {
                Log.i( "DataDownloader", "Fetching file from assets: " + url );
                fileInAssets = true;
                break;
            }
            else
            {
                Log.i( "DataDownloader", "Connecting to: " + url );
                request = new HttpGet( url );
                request.addHeader( "Accept", "*/*" );
                try
                {
                    DefaultHttpClient client = getHttpWithDisabledSslCertCheck();
                    client.getParams().setBooleanParameter( "http.protocol.handle-redirects", true );
                    response = client.execute( request );
                }
                catch( IOException e )
                {
                    Log.e( "DataDownloader", "Failed to connect to " + url );
                    downloadUrlIndex++;
                }
                
                if( response != null )
                {
                    if( response.getStatusLine().getStatusCode() != 200 )
                    {
                        response = null;
                        Log.e( "DataDownloader", "Failed to connect to " + url );
                        downloadUrlIndex++;
                    }
                    else
                        break;
                }
            }
        }
        
        if( fileInAssets )
        {
            try
            {
                stream = new CountingInputStream( mActivity.getAssets().open( url ), 8192 );
                while( stream.skip( 65536 ) > 0 )
                {
                }
                ;
                totalLen = stream.getBytesRead();
                stream.close();
                stream = new CountingInputStream( mActivity.getAssets().open( url ), 8192 );
            }
            catch( IOException e )
            {
                Log.e( "DataDownloader",
                        "Unpacking from assets '" + url + "' - error: " + e.toString() );
                mStatus.setText( res.getString( R.string.error_dl_from, url ) );
                return false;
            }
        }
        else
        {
            if( response == null )
            {
                Log.e( "DataDownloader", "Error connecting to " + url );
                mStatus.setText( res.getString( R.string.failed_connecting_to, url ) );
                return false;
            }
            mStatus.setText( res.getString( R.string.dl_from, url ) );
            totalLen = response.getEntity().getContentLength();
            try
            {
                stream = new CountingInputStream( response.getEntity().getContent(), 8192 );
            }
            catch( IOException e )
            {
                mStatus.setText( res.getString( R.string.error_dl_from, url ) );
                return false;
            }
        }
        
        if( doNotUnzip )
        {
            if( !saveRegularFiles( res, downloadUrls, totalLen, stream, buf, downloadUrlIndex ) )
                return false;
        }
        else
        {
            if( !saveZippedFiles( res, path, totalLen, stream, buf, url ) )
                return false;
        }
        
        if( !writeOutput( downloadFlagFileName, res, downloadUrls, downloadUrlIndex ) )
            return false;
        
        mStatus.setText( res.getString( R.string.dl_finished ) );
        try
        {
            stream.close();
        }
        catch( IOException e )
        {
        }
        return true;
    }
    
    private String getOutFilePath( final String filename )
    {
        return outFilesDir + "/" + filename;
    }
    
    private boolean isDownloadRequired( String[] downloadUrls, String path )
    {
        boolean isRequired = true;
        InputStream checkFile = null;
        try
        {
            checkFile = new FileInputStream( path );
        }
        catch( FileNotFoundException e )
        {
        }
        catch( SecurityException e )
        {
        }
        
        if( checkFile != null )
        {
            try
            {
                byte b[] = new byte[Paths.dataDownloadUrl.getBytes( "UTF-8" ).length + 1];
                int readed = checkFile.read( b );
                String compare = new String( b, 0, readed, "UTF-8" );
                for( int i = 1; i < downloadUrls.length; i++ )
                {
                    if( compare.compareTo( downloadUrls[i] ) == 0 )
                    {
                        isRequired = false;
                    }
                }
            }
            catch( IOException e )
            {
            }
        }
        try
        {
            checkFile.close();
        }
        catch( Exception e )
        {
        }
        checkFile = null;
        return isRequired;
    }
    
    private void createOutputDirectory()
    {
        Log.i( "DataDownloader", "Downloading data to: '" + outFilesDir + "'" );
        try
        {
            File outDir = new File( outFilesDir );
            if( !( outDir.exists() && outDir.isDirectory() ) )
                outDir.mkdirs();
            OutputStream out = new FileOutputStream( getOutFilePath( ".nomedia" ) );
            out.flush();
            out.close();
        }
        catch( SecurityException e )
        {
        }
        catch( FileNotFoundException e )
        {
        }
        catch( IOException e )
        {
        }
    }
    
    private boolean saveRegularFiles( Resources res, String[] downloadUrls, long totalLen,
            CountingInputStream stream, byte[] buf, int downloadUrlIndex )
    {
        String path;
        path = getOutFilePath( downloadUrls[downloadUrlIndex].substring( 1,
                downloadUrls[downloadUrlIndex].indexOf( ':', 1 ) ) );
        Log.i( "DataDownloader", "Saving file '" + path + "'" );
        OutputStream out = null;
        try
        {
            try
            {
                File outDir = new File( path.substring( 0, path.lastIndexOf( '/' ) ) );
                if( !( outDir.exists() && outDir.isDirectory() ) )
                    outDir.mkdirs();
            }
            catch( SecurityException e )
            {
            }
            out = new FileOutputStream( path );
        }
        catch( FileNotFoundException e )
        {
            Log.e( "DataDownloader", "Saving file '" + path + "' - error creating output file: "
                    + e.toString() );
        }
        catch( SecurityException e )
        {
            Log.e( "DataDownloader", "Saving file '" + path + "' - error creating output file: "
                    + e.toString() );
        }
        
        if( out == null )
        {
            mStatus.setText( res.getString( R.string.error_write, path ) );
            Log.e( "DataDownloader", "Saving file '" + path + "' - error creating output file" );
            return false;
        }
        try
        {
            int len = stream.read( buf );
            while( len >= 0 )
            {
                if( len > 0 )
                    out.write( buf, 0, len );
                len = stream.read( buf );
                float percent = 0.0f;
                if( totalLen > 0 )
                    percent = stream.getBytesRead() * 100.0f / totalLen;
                mStatus.setText( res.getString( R.string.dl_progress, percent, path ) );
            }
            out.flush();
            out.close();
            out = null;
        }
        catch( IOException e )
        {
            mStatus.setText( res.getString( R.string.error_write, path ) );
            Log.e( "DataDownloader", "Saving file '" + path + "' - error writing: " + e.toString() );
            return false;
        }
        Log.i( "DataDownloader", "Saving file '" + path + "' done" );
        return true;
    }
    
    private boolean saveZippedFiles( Resources res, String path, long totalLen,
            CountingInputStream stream, byte[] buf, String url )
    {
        Log.i( "DataDownloader", "Reading from zip file '" + url + "'" );
        ZipInputStream zip = new ZipInputStream( stream );
        while( true )
        {
            ZipEntry entry = null;
            try
            {
                entry = zip.getNextEntry();
                if( entry != null )
                    Log.i( "DataDownloader",
                            "Reading from zip file '" + url + "' entry '" + entry.getName() + "'" );
            }
            catch( IOException e )
            {
                mStatus.setText( res.getString( R.string.error_dl_from, url ) );
                Log.e( "DataDownloader",
                        "Error reading from zip file '" + url + "': " + e.toString() );
                return false;
            }
            if( entry == null )
            {
                Log.i( "DataDownloader", "Reading from zip file '" + url + "' finished" );
                break;
            }
            if( entry.isDirectory() )
            {
                Log.i( "DataDownloader", "Creating dir '" + getOutFilePath( entry.getName() ) + "'" );
                try
                {
                    File outDir = new File( getOutFilePath( entry.getName() ) );
                    if( !( outDir.exists() && outDir.isDirectory() ) )
                        outDir.mkdirs();
                }
                catch( SecurityException e )
                {
                }
                continue;
            }
            OutputStream out = null;
            path = getOutFilePath( entry.getName() );
            Log.i( "DataDownloader", "Saving file '" + path + "'" );
            try
            {
                File outDir = new File( path.substring( 0, path.lastIndexOf( '/' ) ) );
                if( !( outDir.exists() && outDir.isDirectory() ) )
                    outDir.mkdirs();
            }
            catch( SecurityException e )
            {
            }
            try
            {
                CheckedInputStream check = new CheckedInputStream( new FileInputStream( path ),
                        new CRC32() );
                while( check.read( buf, 0, buf.length ) > 0 )
                {
                }
                ;
                check.close();
                if( check.getChecksum().getValue() != entry.getCrc() )
                {
                    File ff = new File( path );
                    ff.delete();
                    throw new Exception();
                }
                Log.i( "DataDownloader", "File '" + path
                        + "' exists and passed CRC check - not overwriting it" );
                continue;
            }
            catch( Exception e )
            {
            }
            try
            {
                out = new FileOutputStream( path );
            }
            catch( FileNotFoundException e )
            {
                Log.e( "DataDownloader",
                        "Saving file '" + path + "' - cannot create file: " + e.toString() );
            }
            catch( SecurityException e )
            {
                Log.e( "DataDownloader",
                        "Saving file '" + path + "' - cannot create file: " + e.toString() );
            }
            
            if( out == null )
            {
                mStatus.setText( res.getString( R.string.error_write, path ) );
                Log.e( "DataDownloader", "Saving file '" + path + "' - cannot create file" );
                return false;
            }
            float percent = 0.0f;
            if( totalLen > 0 )
                percent = stream.getBytesRead() * 100.0f / totalLen;
            mStatus.setText( res.getString( R.string.dl_progress, percent, path ) );
            try
            {
                int len = zip.read( buf );
                while( len >= 0 )
                {
                    if( len > 0 )
                        out.write( buf, 0, len );
                    len = zip.read( buf );
                    percent = 0.0f;
                    if( totalLen > 0 )
                        percent = stream.getBytesRead() * 100.0f / totalLen;
                    mStatus.setText( res.getString( R.string.dl_progress, percent, path ) );
                }
                out.flush();
                out.close();
                out = null;
            }
            catch( IOException e )
            {
                mStatus.setText( res.getString( R.string.error_write, path ) );
                Log.e( "DataDownloader", "Saving file '" + path
                        + "' - error writing or downloading: " + e.toString() );
                return false;
            }
            try
            {
                CheckedInputStream check = new CheckedInputStream( new FileInputStream( path ),
                        new CRC32() );
                while( check.read( buf, 0, buf.length ) > 0 )
                {
                }
                ;
                check.close();
                if( check.getChecksum().getValue() != entry.getCrc() )
                {
                    File ff = new File( path );
                    ff.delete();
                    throw new Exception();
                }
            }
            catch( Exception e )
            {
                mStatus.setText( res.getString( R.string.error_write, path ) );
                Log.e( "DataDownloader", "Saving file '" + path + "' - CRC check failed" );
                return false;
            }
            Log.i( "DataDownloader", "Saving file '" + path + "' done" );
        }
        return true;
    }
    
    private boolean writeOutput( final String downloadFlagFileName, Resources res,
            String[] downloadUrls, int downloadUrlIndex )
    {
        String path;
        OutputStream out = null;
        path = getOutFilePath( downloadFlagFileName );
        try
        {
            out = new FileOutputStream( path );
            out.write( downloadUrls[downloadUrlIndex].getBytes( "UTF-8" ) );
            out.flush();
            out.close();
        }
        catch( FileNotFoundException e )
        {
        }
        catch( SecurityException e )
        {
        }
        catch( IOException e )
        {
            mStatus.setText( res.getString( R.string.error_write, path ) );
            return false;
        }
        return true;
    }
    
    private static DefaultHttpClient getHttpWithDisabledSslCertCheck()
    {
        /*
         * HostnameVerifier hostnameVerifier =
         * org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER; DefaultHttpClient
         * client = new DefaultHttpClient(); SchemeRegistry registry = new SchemeRegistry();
         * SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
         * socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
         * registry.register(new Scheme("https", socketFactory, 443)); SingleClientConnManager mgr =
         * new SingleClientConnManager(client.getParams(), registry); DefaultHttpClient http = new
         * DefaultHttpClient(mgr, client.getParams());
         * HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier); return http;
         */
        return new DefaultHttpClient();
    }
    
    class StatusWriter
    {
        private TextView pView;
        private Activity pActivity;
        private SpannedString mOldText = new SpannedString( "" );
        
        public StatusWriter( TextView view, Activity activity )
        {
            pView = view;
            pActivity = activity;
        }
        
        public void setParent( TextView view, Activity activity )
        {
            synchronized( DataDownloader.this )
            {
                pView = view;
                pActivity = activity;
                setText( mOldText.toString() );
            }
        }
        
        public void setText( final String str )
        {
            class Callback implements Runnable
            {
                public TextView Status;
                public SpannedString text;
                
                public void run()
                {
                    Status.setText( text );
                }
            }
            synchronized( DataDownloader.this )
            {
                Callback cb = new Callback();
                mOldText = new SpannedString( str );
                cb.text = new SpannedString( str );
                cb.Status = pView;
                if( pActivity != null && pView != null )
                    pActivity.runOnUiThread( cb );
            }
        }
    }
    
    class CountingInputStream extends BufferedInputStream
    {
        private long mBytesReadMark = 0;
        private long mBytesRead = 0;
        
        public CountingInputStream( InputStream in, int size )
        {
            super( in, size );
        }
        
        public CountingInputStream( InputStream in )
        {
            super( in );
        }
        
        public long getBytesRead()
        {
            return mBytesRead;
        }
        
        public synchronized int read() throws IOException
        {
            int read = super.read();
            if( read >= 0 )
            {
                mBytesRead++;
            }
            return read;
        }
        
        public synchronized int read( byte[] b, int off, int len ) throws IOException
        {
            int read = super.read( b, off, len );
            if( read >= 0 )
            {
                mBytesRead += read;
            }
            return read;
        }
        
        public synchronized long skip( long n ) throws IOException
        {
            long skipped = super.skip( n );
            if( skipped >= 0 )
            {
                mBytesRead += skipped;
            }
            return skipped;
        }
        
        public synchronized void mark( int readlimit )
        {
            super.mark( readlimit );
            mBytesReadMark = mBytesRead;
        }
        
        public synchronized void reset() throws IOException
        {
            super.reset();
            mBytesRead = mBytesReadMark;
        }
    }
}
