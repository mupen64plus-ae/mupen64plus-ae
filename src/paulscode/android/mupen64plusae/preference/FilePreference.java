package paulscode.android.mupen64plusae.preference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.ListPreference;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;

public class FilePreference extends ListPreference
{
    String mPath;
    
    public FilePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        refreshItems();
        super.onPrepareDialogBuilder( builder );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        File file = new File(mPath);
        if (positiveResult && file.isDirectory())
            onClick();
    }
    
    public void refreshItems()
    {
        // Restore the persisted state
        mPath = getPersistedString( Settings.paths.storageDir );
        populate( new File( mPath ) );        
    }
    
    private void populate( File startPath )
    {
        if( startPath.isFile() )
            startPath = startPath.getParentFile();
        
        // Get all files in this folder
        // TODO: add filter
        File[] fileList = startPath.listFiles();
        List<File> files = new ArrayList<File>();
        for( File file : fileList )
            if( !file.isHidden() )
                files.add( file );
        Collections.sort( files, new FileComparer() );
        
        // Construct the key-value pairs for the list entries
        boolean hasParent = startPath.getParentFile() != null;
        ArrayList<Spanned> entries = new ArrayList<Spanned>();
        ArrayList<String> values = new ArrayList<String>();
        if( hasParent )
        {
            entries.add( Html.fromHtml( ".." ) );
            values.add( startPath.getParentFile().getAbsolutePath() );
        }
        for( File file : files )
        {
            if( file.isDirectory() )
                entries.add( Html.fromHtml( "<i>" + file.getName() + "</i>" ) );
            else
                entries.add( Html.fromHtml( "<b>" + file.getName() + "</b>" ) );
            values.add( file.getAbsolutePath() );
        }
        
        // Populate the list
        setEntries( entries.toArray( new Spanned[entries.size()] ) );
        setEntryValues( values.toArray( new String[values.size()] ) );
        
        // Update the menu text
        setDialogTitle( startPath.getName() );
        //setSummary( getEntry() + " (" + getValue() + ")" );
        notifyChanged();
    }
    
    public class FileComparer implements Comparator<File>
    {
        public int compare( File lhs, File rhs )
        {
            if( lhs.isDirectory() && rhs.isFile() )
                return -1;
            else if( lhs.isFile() && rhs.isDirectory() )
                return 1;
            else
                return lhs.getName().toLowerCase().compareTo( rhs.getName().toLowerCase() );
        }
    }
}
