package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.LinkedList;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.CheatFile;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatSection;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class CheatEditorActivity extends ListActivity {

	private ArrayList<String> cheats_name = new ArrayList<String>();
	private ArrayList<String> cheats_desc = new ArrayList<String>();
	private ArrayList<String> cheats_code = new ArrayList<String>();
	private ArrayList<String> cheats_option = new ArrayList<String>();
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private ArrayAdapter<String> cheatList = null;
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		mAppData = new AppData( this );
	    mUserPrefs = new UserPrefs( this );
	    mUserPrefs.enforceLocale( this );
		setContentView(R.layout.cheat_editor);
		reload(mUserPrefs.selectedGameHeader.crc);
		final ImageButton add = (ImageButton) findViewById(R.id.imgBtnChtAdd);
		add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	cheats_name.add("Empty Cheat");
            	cheats_desc.add("(no notes available for this cheat)");
            	cheats_code.add("");
            	cheats_option.add("");
                cheatList = new ArrayAdapter<String>(CheatEditorActivity.this,R.layout.cheat_row,cheats_name);
        		setListAdapter(cheatList);
        		Toast t = Toast.makeText(CheatEditorActivity.this, "Empty Cheat Added at Bottom of List", Toast.LENGTH_SHORT);
        		t.show();
            }
		});
		final ImageButton save = (ImageButton) findViewById(R.id.imgBtnChtSave);
		save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	save(mUserPrefs.selectedGameHeader.crc);
            	CheatEditorActivity.this.finish();
            }
		});
		ListView lv = getListView();
		lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, final int pos, long id) 
			{
				final AlertDialog parentDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
				parentDialog.setTitle("Cheat Config");
				parentDialog.setMessage("Edit Cheat Config");
			    LinearLayout ll = new LinearLayout(CheatEditorActivity.this);
			    ll.setOrientation(LinearLayout.VERTICAL);
			    Button en = new Button(CheatEditorActivity.this);
			    en.setText("Edit Cheat Name");
			    en.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
					    alertDialog.setTitle("Cheat Title");
					    alertDialog.setMessage("Edit Cheat Title");
					    final EditText i = new EditText(CheatEditorActivity.this);
					    i.setText(cheats_name.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_name.set(pos, i.getText().toString());
			    	        	cheatList = new ArrayAdapter<String>(CheatEditorActivity.this,R.layout.cheat_row,cheats_name);
			    	    		setListAdapter(cheatList);
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button ed = new Button(CheatEditorActivity.this);
			    ed.setText("Edit Cheat Notes");
			    ed.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
					    alertDialog.setTitle("Cheat Notes");
					    alertDialog.setMessage("Edit Cheat Notes");
					    final EditText i = new EditText(CheatEditorActivity.this);
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
			    Button ec = new Button(CheatEditorActivity.this);
			    ec.setText("Edit Cheat Code");
			    ec.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
					    alertDialog.setTitle("Cheat Code");
					    alertDialog.setMessage("Edit Cheat Code");
					    final EditText i = new EditText(CheatEditorActivity.this);
					    i.setText(cheats_code.get(pos));
					    alertDialog.setView(i);
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_code.set(pos, i.getText().toString());
			    	        }

			    	    });
					    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	
			    	        }

			    	    });
					    alertDialog.show();
					}
			    	
			    });
			    Button eo = new Button(CheatEditorActivity.this);
			    eo.setText("Edit Cheat Options");
			    eo.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
					    alertDialog.setTitle("Cheat Options");
					    alertDialog.setMessage("Edit Cheat Options");
					    final EditText i = new EditText(CheatEditorActivity.this);
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
			    Button de = new Button(CheatEditorActivity.this);
			    de.setText("Delete Cheat");
			    de.setOnClickListener(new OnClickListener()
			    {

					@Override
					public void onClick(View v) {
						parentDialog.dismiss();
						AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
					    alertDialog.setTitle("Delete Cheat");
					    alertDialog.setMessage("Are you sure?");
					    
					    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {
			    	    	
			    	        public void onClick(DialogInterface dialog, int which) {
			    	            // Clicked
			    	        	cheats_name.remove(pos);
			    	        	cheats_desc.remove(pos);
			    	        	cheats_code.remove(pos);
			    	        	cheats_option.remove(pos);
			    	        	cheatList = new ArrayAdapter<String>(CheatEditorActivity.this,R.layout.cheat_row,cheats_name);
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
	private void reload( String crc )
	{
		Log.v( "CheatEditorActivity", "building from CRC = " + crc );
        
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "CheatEditorActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        
        // Set the title of the menu to the game name, if available
        // String ROM_name = configSection.get( "Name" );
        // if( !TextUtils.isEmpty( ROM_name ) )
        // setTitle( ROM_name );
        
        // Layout the menu, populating it with appropriate cheat options
        CheatBlock cheat;
        for( int i = 0; i<cheatSection.size(); i++ )
        {
            cheat = cheatSection.get( i );
            if( cheat!=null )
            {
                // Get the short title of the cheat (shown in the menu)
                String title;
                if( cheat.name==null )
                {
                    // Title not available, just use a default string for the menu
                    title = getString( R.string.cheats_defaultName, i );
                }
                
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    title = cheat.name;
                }
                cheats_name.add(title);
                // Get the descriptive note for this cheat (shown on long-click)
                final String notes = cheat.description;
                if(notes==null)
                {
                	//TODO: Translate/Turn into R.string
                	cheats_desc.add("(no notes available for this cheat)");
                }else{
                	cheats_desc.add(notes);
                }
                // Get the options for this cheat
                LinkedList<CheatCode> codes = new LinkedList<CheatCode>();
                LinkedList<CheatOption> options = new LinkedList<CheatOption>();
                for(int o=0; o<cheat.size(); o++)
                {
                	codes.add(cheat.get(o));
                }
                for(int o=0; o<codes.size(); o++)
                {
                	if(codes.get(o).options!=null)
                	{
                		options=codes.get(o).options;
                	}
                	
                }
                String codesAsString="";
                if(codes!=null)
                {
                	if(!codes.isEmpty())
                	{
                		for(int o = 0; o<codes.size(); o++)
                		{
                			String y="";
                			if(o!=codes.size()-1)
                			{
                				y="\n";
                			}
                			codesAsString+=codes.get(o).address+" "+codes.get(o).code+y;
                		}
                	}
                }
                cheats_code.add(codesAsString);
                String optionsAsString="";
                if(options!=null)
                {
                	if(!options.isEmpty())
                	{
                		for(int o = 0; o<options.size(); o++)
                		{
                			String y="";
                			if(o!=options.size()-1)
                			{
                				y="\n";
                			}
                			optionsAsString+=options.get(o).name+" "+options.get(o).code+y;
                		}
                	}
                }
                cheats_option.add(optionsAsString);
                String[] optionStrings = null;
                if( options!=null )
                {
                	if(!options.isEmpty())
                	{
                    // This is a multi-choice cheat

                    optionStrings = new String[options.size()];
                    
                    // Each element is a key-value pair
                    for( int z = 0; z < options.size(); z++ )
                    {
                        // The first non-leading space character is the pair delimiter
                        optionStrings[z] = options.get(z).name;
                        if( optionStrings[z].isEmpty() || optionStrings[z] == null )
                        	optionStrings[z] = getString( R.string.cheats_longPress );
                            
                    }
                	}
                }
                cheatList = new ArrayAdapter<String>(this,R.layout.cheat_row,cheats_name);
        		setListAdapter(cheatList);
            }
        }
	}
	
	private void save(String crc)
	{
		CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
		CheatSection c = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
		if(cheats_name.size()==cheats_desc.size()&&cheats_desc.size()==cheats_code.size()&&cheats_code.size()==cheats_option.size())
		{
			for(int i = 0; i<c.size(); i++)
			{
				c.remove(i);
			}
			for(int i = 0; i<cheats_name.size(); i++)
			{
				CheatBlock b = new CheatBlock(cheats_name.get(i),cheats_desc.get(i));
				LinkedList<CheatOption> ops = new LinkedList<CheatOption>();
				if(cheats_option.get(i)!=null)
				{
					if(!cheats_option.get(i).isEmpty())
					{
						String[] tmp_ops = cheats_option.get(i).split("\n");
						for(int o = 0; o<tmp_ops.length; o++)
						{
							ops.add(new CheatOption(tmp_ops[o].substring(0,tmp_ops[o].indexOf(' ')),tmp_ops[o].substring(tmp_ops[o].indexOf(' ')+1)));
						}
					}
				}
				String[] tmp_lines = cheats_code.get(i).split("\n");
				for(int o = 0; o<tmp_lines.length; o++)
				{
					if(tmp_lines[o].contains("?"))
					{
						b.add(new CheatCode(tmp_lines[o].substring(0, tmp_lines[o].lastIndexOf(' ')),tmp_lines[o].substring(tmp_lines[o].lastIndexOf(' ')+1),ops));
					}else{
						b.add(new CheatCode(tmp_lines[o].substring(0, tmp_lines[o].lastIndexOf(' ')),tmp_lines[o].substring(tmp_lines[o].lastIndexOf(' ')+1),null));
					}
				}
				c.add(b);
			}
			mupencheat_txt.save();
		}
	}
	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(CheatEditorActivity.this).create();
	    alertDialog.setTitle("Cheat Information");
	    String msg="Title:\n"+cheats_name.get(position)+"\nNotes:\n"+cheats_desc.get(position)+"\nCode(s):\n"+cheats_code.get(position);
	    if(cheats_option.get(position)!=null&&!cheats_option.get(position).isEmpty()&&cheats_code.get(position).contains("?"))
	    {
	    	msg+="\nCheat options:\n";
	    	msg+=cheats_option.get(position);
	    }
	    alertDialog.setMessage(msg);
	    alertDialog.show();
		
	}

	
	
}
