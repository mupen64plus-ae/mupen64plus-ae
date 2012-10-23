package paulscode.android.mupen64plusae.util;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.NativeMethods;
import paulscode.android.mupen64plusae.R;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class Audio
{
    public static Thread mAudioThread = null;
    public static AudioTrack mAudioTrack = null;
    public static Object buf;
    
    public static Object init( int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames )
    {
        int channelConfig = isStereo
                ? AudioFormat.CHANNEL_OUT_STEREO
                : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit
                ? AudioFormat.ENCODING_PCM_16BIT
                : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = ( isStereo
                ? 2
                : 1 ) * ( is16Bit
                ? 2
                : 1 );
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math
                .max( desiredFrames,
                        ( AudioTrack.getMinBufferSize( sampleRate, channelConfig, audioFormat )
                                + frameSize - 1 )
                                / frameSize );
        
        Audio.mAudioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM );
        
        Audio.startThread();
        
        if( is16Bit )
        {
            Audio.buf = new short[desiredFrames * ( isStereo
                    ? 2
                    : 1 )];
        }
        else
        {
            Audio.buf = new byte[desiredFrames * ( isStereo
                    ? 2
                    : 1 )];
        }
        return Audio.buf;
    }
    
    public static void startThread()
    {
        Audio.mAudioThread = new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    Audio.mAudioTrack.play();
                    NativeMethods.runAudioThread();
                }
                catch( IllegalStateException ise )
                {
                    Log.e( "GameActivity", "audioStartThread IllegalStateException", ise );
                    if( Globals.gameInstance != null )
                        Notifier.showToast(
                                Globals.gameInstance.getString( R.string.illegal_audio_state ),
                                Globals.gameInstance );
                    else
                        // Static context, can't get the string in the correct locale, so just use
                        // English:
                        Notifier.showToast(
                                "Audio track illegal state.  Please report at paulscode.com",
                                Globals.gameInstance );
                }
            }
        } );
        
        // I'd take REALTIME if I could get it!
        Audio.mAudioThread.setPriority( Thread.MAX_PRIORITY );
        Audio.mAudioThread.start();
    }
    
    public static void writeShortBuffer( short[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = Audio.mAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                try
                {
                    Thread.sleep( 1 );
                }
                catch( InterruptedException e )
                {
                    // Nom nom
                }
            }
            else
            {
                Log.w( "GameActivity", "SDL audio: error return from write(short)" );
                return;
            }
        }
    }
    
    public static void writeByteBuffer( byte[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = Audio.mAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                try
                {
                    Thread.sleep( 1 );
                }
                catch( InterruptedException e )
                {
                    // Nom nom
                }
            }
            else
            {
                Log.w( "GameActivity", "SDL audio: error return from write(short)" );
                return;
            }
        }
    }
    
    public static void quit()
    {
        if( Audio.mAudioThread != null )
        {
            try
            {
                Audio.mAudioThread.join();
            }
            catch( Exception e )
            {
                Log.v( "GameActivity", "Problem stopping audio thread: " + e );
            }
            Audio.mAudioThread = null;
            
            // Log.v("SDL", "Finished waiting for audio thread");
        }
        
        if( Audio.mAudioTrack != null )
        {
            Audio.mAudioTrack.stop();
            Audio.mAudioTrack = null;
        }
    }
}