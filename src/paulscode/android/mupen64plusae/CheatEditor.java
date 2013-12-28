package paulscode.android.mupen64plusae;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class CheatEditor extends ListActivity {

	private ArrayList<String> cheats_name = new ArrayList<String>();
	private ArrayList<String> cheats_desc = new ArrayList<String>();
	private ArrayList<String> cheats_code = new ArrayList<String>();
	private ArrayList<String> cheats_option = new ArrayList<String>();
    private AppData mAppData = null;
    private ArrayAdapter<String> cheatList = null;
    private String crc = null;
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		mAppData = new AppData( this );
			
		setContentView(R.layout.cheat_editor);
		reload();
		ListView lv = getListView();
		lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, final int pos, long id) 
			{
				final AlertDialog parentDialog = new AlertDialog.Builder(CheatEditor.this).create();
				parentDialog.setTitle("Cheat Config");
				parentDialog.setMessage("Edit Cheat Config");
			    LinearLayout ll = new LinearLayout(CheatEditor.this);
			    ll.setOrientation(LinearLayout.VERTICAL);
			    Button en = new Button(CheatEditor.this);
			    en.setText("Edit Cheat Name");
			    en.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
					    alertDialog.setTitle("Cheat Title");
					    alertDialog.setMessage("Edit Cheat Title");
					    final EditText i = new EditText(CheatEditor.this);
					    i.setText(cheats_name.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_name.set(pos, i.getText().toString());
			    	        	cheatList = new ArrayAdapter<String>(CheatEditor.this,R.layout.cheat_row,cheats_name);
			    	    		setListAdapter(cheatList);
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button ed = new Button(CheatEditor.this);
			    ed.setText("Edit Cheat Notes");
			    ed.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
					    alertDialog.setTitle("Cheat Notes");
					    alertDialog.setMessage("Edit Cheat Notes");
					    final EditText i = new EditText(CheatEditor.this);
					    i.setText(cheats_desc.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_desc.set(pos, i.getText().toString());
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button ec = new Button(CheatEditor.this);
			    ec.setText("Edit Cheat Code");
			    ec.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
					    alertDialog.setTitle("Cheat Code");
					    alertDialog.setMessage("Edit Cheat Code");
					    final EditText i = new EditText(CheatEditor.this);
					    i.setText(cheats_code.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_code.set(pos, i.getText().toString());
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button eo = new Button(CheatEditor.this);
			    eo.setText("Edit Cheat Options");
			    eo.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
					    alertDialog.setTitle("Cheat Options");
					    alertDialog.setMessage("Edit Cheat Options");
					    final EditText i = new EditText(CheatEditor.this);
					    i.setText(cheats_option.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_option.set(pos, i.getText().toString());
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button de = new Button(CheatEditor.this);
			    de.setText("Delete Cheat");
			    de.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
					    alertDialog.setTitle("Delete Cheat");
					    alertDialog.setMessage("Are you sure?");
					    
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_name.remove(pos);
			    	        	cheats_desc.remove(pos);
			    	        	cheats_code.remove(pos);
			    	        	cheats_option.remove(pos);
			    	        	cheatList = new ArrayAdapter<String>(CheatEditor.this,R.layout.cheat_row,cheats_name);
			    	    		setListAdapter(cheatList);
			    	        }

			    	    });
					    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    ll.addView(en);
			    ll.addView(ed);
			    ll.addView(ec);
			    if(cheats_code.get(pos).contains("?"))
			    {
			    	ll.addView(eo);
			    }
			    ll.addView(de);
			    parentDialog.setView(ll);
			    parentDialog.show();
				return true;
			}
		}); 
	}
	private void reload()
	{
		ConfigFile mupen64plus_cht = new ConfigFile( mAppData.mupen64plus_cht );
        ConfigSection configSection = mupen64plus_cht.match( "^" + crc.replace( ' ', '.' ) + ".*" );
        String cheat = " ";
		for( int i = 0; !TextUtils.isEmpty( cheat ); i++ )
        {
            cheat = configSection.get( "Cheat" + i );
            if( !TextUtils.isEmpty( cheat ) )
            {
                // Get the short title of the cheat (shown in the menu)
            	
                int x = cheat.indexOf( ",", cheat.lastIndexOf("\"") );
                String title;
                if( x < 3 || x >= cheat.length() )
                {
                    // Title not available, just use a default string for the menu
                    title = getString( R.string.cheats_defaultName, i );
                }
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    title = cheat.substring( 1, x - 1 );
                }
                String code=cheat.substring(x+1);
                code=code.replaceAll(",", "\n");
                cheats_code.add(code);
                if(title!=null){
                	cheats_name.add(title);
                }
                // Get the descriptive note for this cheat (shown on long-click)
                final String notes = configSection.get( "Cheat" + i + "_N" );
                if(notes!=null&&!notes.isEmpty()){
                	cheats_desc.add(notes);
                }else{
                	cheats_desc.add("(no notes available for this cheat)");
                }
                
                // Get the options for this cheat
                String val_O = configSection.get( "Cheat" + i + "_O" );
                
                if( !TextUtils.isEmpty( val_O ) )
                {
                    val_O = val_O.replaceAll(",","\n");
                    cheats_option.add(val_O);   
                }else{
                	cheats_option.add(null);
                }
            }
        }
		cheatList = new ArrayAdapter<String>(this,R.layout.cheat_row,cheats_name);
		setListAdapter(cheatList);
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(CheatEditor.this).create();
	    alertDialog.setTitle("Cheat Information");
	    String msg="Title:\n"+cheats_name.get(position)+"\nNotes:\n"+cheats_desc.get(position)+"\nCode:\n"+cheats_code.get(position);
	    if(cheats_option.get(position)!=null&&!cheats_option.get(position).isEmpty()&&cheats_code.get(position).contains("?"))
	    {
	    	msg+="\nCheat options:\n";
	    	msg+=cheats_option.get(position);
	    }
	    alertDialog.setMessage(msg);
	    alertDialog.show();
		
	}

	
	
}
