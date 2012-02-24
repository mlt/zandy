/*******************************************************************************
 * This file is part of Zandy.
 * 
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gimranov.zandy.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.ListFragment;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.support.v4.view.Menu;
import android.view.MenuInflater;
import android.support.v4.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemAdapter;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class ItemFragment extends ListFragment {

	private static final String TAG = "com.gimranov.zandy.app.ItemFragment";
	
	static final int DIALOG_VIEW = 0;
	static final int DIALOG_NEW = 1;
	static final int DIALOG_SORT = 2;
	static final int DIALOG_IDENTIFIER = 3;
	static final int DIALOG_PROGRESS = 6;
	
//	static final String
/*
	static final String[] SORTS = {
		"item_year, item_title",
		"item_creator, item_year",
		"item_title, item_year",
		"timestamp ASC, item_title"
	};

	// XXX i8n
	static final String[] SORTS_EN = {
		"Year, then title",
		"Creator, then year",
		"Title, then year",
		"Date modified, then title"
	};	
*/	
	private String collectionKey;
	private String query;
	private String tag;
	private Database db;
		
	private ProgressDialog mProgressDialog;
	private ProgressThread progressThread;
	
	public String sortBy = "item_year, item_title";
	
    public ItemFragment () {
        tag = null;
        query = null;
        collectionKey = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        if (container == null) {
            // see http://developer.android.com/reference/android/app/Fragment.html
            return null;
        }
        */
        return inflater.inflate(R.layout.items, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        db = new Database(getActivity());
        
        prepareAdapter();
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If we have a click on an item, do something...
                ItemAdapter adapter = (ItemAdapter) parent.getAdapter();
                Cursor cur = adapter.getCursor();
                // Place the cursor at the selected item
                if (cur.moveToPosition(position)) {
                    // and load an activity for the item
                    Item item = Item.load(cur);
                    
                    Log.d(TAG, "Loading item data with key: "+item.getKey());
                    // We create and issue a specified intent with the necessary data
                    Intent i = new Intent(getActivity().getBaseContext(), ItemDataActivity.class);
                    i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
                    i.putExtra("com.gimranov.zandy.app.itemDbId", item.dbId);
                    startActivity(i);
                } else {
                    // failed to move cursor-- show a toast
                    TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
                    Toast.makeText(getActivity().getApplicationContext(),
                            getResources().getString(R.string.cant_open_item, tvTitle.getText()), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // necessary for a fragment
    }

	public void onResume() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		adapter.changeCursor(prepareCursor());
    	super.onResume();
    }
    
    public void onDestroy() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Cursor cur = adapter.getCursor();
		if(cur != null) cur.close();
		if (db != null) db.close();
		super.onDestroy();
    }
    
    private void prepareAdapter() {
		ItemAdapter adapter = new ItemAdapter(getActivity(), prepareCursor());
        setListAdapter(adapter);
    }
    
    private Cursor prepareCursor() {
    	Cursor cursor;
    	// TODO move necessary functionality to ItemActivity
        Intent intent = getActivity().getIntent();
        // Be ready for a search
/*

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	query = intent.getStringExtra(SearchManager.QUERY);
        	cursor = getCursor(query);
        	getActivity().setTitle(getResources().getString(R.string.search_results, query));
        } else */ 
        if (query != null) {
           	cursor = getCursor(query);
           	getActivity().setTitle(getResources().getString(R.string.search_results, query));
//        } else if (intent.getStringExtra("com.gimranov.zandy.app.tag") != null) {
        } else if (tag != null) {
//        	String tag = intent.getStringExtra("com.gimranov.zandy.app.tag");
        	Query q = new Query();
        	q.set("tag", tag);
        	cursor = getCursor(q);
        	getActivity().setTitle(getResources().getString(R.string.tag_viewing_items, tag));
     	} else {
     	    if (null == collectionKey) // workaround until fragment is properly created by ItemActivity
     	       collectionKey = intent.getStringExtra("com.gimranov.zandy.app.collectionKey");
	        if (collectionKey != null) {
	        	ItemCollection coll = ItemCollection.load(collectionKey, db);
	        	cursor = getCursor(coll);
	        	getActivity().setTitle(coll.getTitle());
	        } else {
	        	cursor = getCursor();
	        	getActivity().setTitle(getResources().getString(R.string.all_items));
	        }
        }
        return cursor;
    }
    
	protected void onPrepareDialog(int id, Dialog dialog, Bundle b) {
		switch(id) {
		case DIALOG_PROGRESS:
			Log.d(TAG, "_____________________dialog_progress_prepare");
		}
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.items_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DialogFragment newFragment = null;
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_new_item:
            newFragment = new AlertDialogFragment(DIALOG_NEW);
            newFragment.show(getFragmentManager(), "DIALOG_NEW");
            return true;
        case R.id.do_identifier:
            newFragment = new AlertDialogFragment(DIALOG_IDENTIFIER);
            newFragment.show(getFragmentManager(), "DIALOG_IDENTIFIER");
            return true;
        case R.id.do_search:
        	getActivity().onSearchRequested(); // FIXME add search back to items list
            return true;
        case R.id.do_sort:
            newFragment = new AlertDialogFragment(DIALOG_SORT);
            newFragment.show(getFragmentManager(), "DIALOG_SORT");
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
    /* Sorting */
	public void setSortBy(String sort) {
		this.sortBy = sort;
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		String[] s = sort.split("[\\s,]+");
		adapter.setField(s[0]);
		// http://stackoverflow.com/questions/3898749/re-index-refresh-a-sectionindexer
		// The following does not work
		// TODO We shall replace fragment with new one without adding to stack
        getListView().setFastScrollEnabled(false);
        getListView().setFastScrollEnabled(true);
	}
	
	/* Handling the ListView and keeping it up to date */
	public Cursor getCursor() {
		Cursor cursor = db.query("items", Database.ITEMCOLS, null, null, null, null, this.sortBy, null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}

	public Cursor getCursor(ItemCollection parent) {
		String[] args = { parent.dbId };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
				"items._id, item_key, item_year, item_creator, timestamp, item_children " +
				" FROM items, itemtocollections WHERE items._id = item_id AND collection_id=? ORDER BY "+this.sortBy,
				args);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}

	public Cursor getCursor(String query) {
		String[] args = { "%"+query+"%", "%"+query+"%" };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
				"_id, item_key, item_year, item_creator, timestamp, item_children " +
				" FROM items WHERE item_title LIKE ? OR item_creator LIKE ?" +
				" ORDER BY "+this.sortBy,
				args);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}
	
	public Cursor getCursor(Query query) {
		return query.query(db);
	}

	/* Thread and helper to run lookups */
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d(TAG, "_____________________on_activity_result");
		
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
		    // handle scan result
			Bundle b = new Bundle(); // FIXME make use of bundle?
			b.putString("mode", "isbn");
			b.putString("identifier", scanResult.getContents());
			if (scanResult != null
					&& scanResult.getContents() != null) {
				Log.d(TAG, b.getString("identifier"));
				progressThread = new ProgressThread(handler, b);
				progressThread.start();
	            DialogFragment newFragment = new AlertDialogFragment(DIALOG_PROGRESS);
	            newFragment.show(getFragmentManager(), "DIALOG_PROGRESS"); // , b
			} else {
				// XXX i18n
				Toast.makeText(getActivity().getApplicationContext(),
						"Scan canceled or failed", 
	    				Toast.LENGTH_SHORT).show();
			}
		} else {
			// XXX i18n
			Toast.makeText(getActivity().getApplicationContext(),
					"Scan canceled or failed", 
    				Toast.LENGTH_SHORT).show();
		}
	}
	
    /**
     * Shows given collection when shown along with collections list
     * @param collectionKey
     * @return
     */
	public void showCollection(String collectionKey) {
	    this.collectionKey = collectionKey;
        ItemAdapter adapter = (ItemAdapter) getListAdapter();
        adapter.changeCursor(prepareCursor());
	}
	
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Log.d(TAG, "______________________handle_message");
			if (ProgressThread.STATE_DONE == msg.arg2) {
				Bundle data = msg.getData();
				String itemKey = data.getString("itemKey");
				if (itemKey != null) {
					if (collectionKey != null) {
						Item item = Item.load(itemKey, db);
						ItemCollection coll = ItemCollection.load(collectionKey, db);
						coll.add(item);
						coll.saveChildren(db);
					}
					
					mProgressDialog.dismiss();
					mProgressDialog = null;
					
					Log.d(TAG, "Loading new item data with key: "+itemKey);
    				// We create and issue a specified intent with the necessary data
    		    	Intent i = new Intent(getActivity().getBaseContext(), ItemDataActivity.class);
    		    	i.putExtra("com.gimranov.zandy.app.itemKey", itemKey);
    		    	startActivity(i);
				}
				return;
			}
			
			if (ProgressThread.STATE_PARSING == msg.arg2) {
				mProgressDialog.setMessage("Parsing item data...");
				return;
			}
			
			if (ProgressThread.STATE_ERROR == msg.arg2) {
			    getActivity().dismissDialog(DIALOG_PROGRESS);
				// XXX i18n
				Toast.makeText(getActivity().getBaseContext(), "Error fetching metadata", 
	    				Toast.LENGTH_SHORT).show();
				progressThread.setState(ProgressThread.STATE_DONE);
				return;
			}
		}
	};
	
	private class ProgressThread extends Thread {
		Handler mHandler;
		Bundle arguments;
		final static int STATE_DONE = 5;
		final static int STATE_FETCHING = 1;
		final static int STATE_PARSING = 6;
		final static int STATE_ERROR = 7;
		
		int mState;
		
		ProgressThread(Handler h, Bundle b) {
			mHandler = h;
			arguments = b;
			Log.d(TAG, "_____________________thread_constructor");
		}
		
		public void run() {
			Log.d(TAG, "_____________________thread_run");
			mState = STATE_FETCHING;
			
			// Setup
			String identifier = arguments.getString("identifier");
			String mode = arguments.getString("mode");
			URL url;
			String urlstring;
			
			String response = "";
			
			if ("isbn".equals(mode)) {
				urlstring = "http://xisbn.worldcat.org/webservices/xid/isbn/"
							+ identifier
							+ "?method=getMetadata&fl=*&format=json&count=1";
			} else {
				urlstring = "";
			}
			
			try {
				Log.d(TAG, "Fetching from: "+urlstring);
				url = new URL(urlstring);
                
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();                
                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is, 16000);

                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                
                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
    			while (mState == STATE_FETCHING 
    					&& (current = bis.read()) != -1) {
                        baf.append((byte) current);
                }
                response = new String(baf.toByteArray());
    			Log.d(TAG, response);
    			
    			
	        } catch (IOException e) {
	                Log.e(TAG, "Error: ",e);
	        }

			Message msg = mHandler.obtainMessage();
        	msg.arg2 = STATE_PARSING;
        	mHandler.sendMessage(msg);
        	
        	/*
        	 * {
 "stat":"ok",
 "list":[{
	"url":["http://www.worldcat.org/oclc/177669176?referer=xid"],
	"publisher":"O'Reilly",
	"form":["BA"],
	"lccn":["2004273129"],
	"lang":"eng",
	"city":"Sebastopol, CA",
	"author":"by Mark Lutz and David Ascher.",
	"ed":"2nd ed.",
	"year":"2003",
	"isbn":["0596002815"],
	"title":"Learning Python",
	"oclcnum":["177669176",
..
	 "748093898"]}]}
        	 */
        	
        	// This is OCLC-specific logic
        	try {
				JSONObject result = new JSONObject(response);
				
				if (!result.getString("stat").equals("ok")) {
					Log.e(TAG, "Error response received");
					msg = mHandler.obtainMessage();
		        	msg.arg2 = STATE_ERROR;
		        	mHandler.sendMessage(msg);
		        	return;
				}
				
				result = result.getJSONArray("list").getJSONObject(0);
				String form = result.getJSONArray("form").getString(0);
				String type;
				
				if ("AA".equals(form)) type = "audioRecording";
				else if ("VA".equals(form)) type = "videoRecording";
				else if ("FA".equals(form)) type = "film";
				else type = "book";
				
				// TODO Fix this
				type = "book";
				
				Item item = new Item(getActivity().getBaseContext(), type);
				
				JSONObject content = item.getContent();
				
				if (result.has("lccn")) {
					String lccn = "LCCN: " + result.getJSONArray("lccn").getString(0);
					content.put("extra", lccn);
				}
				
				if (result.has("isbn")) {
					content.put("ISBN", result.getJSONArray("isbn").getString(0));
				}
				
				content.put("title", result.optString("title", ""));
				content.put("place", result.optString("city", ""));
				content.put("edition", result.optString("ed", ""));
				content.put("language", result.optString("lang", ""));
				content.put("publisher", result.optString("publisher", ""));
				content.put("date", result.optString("year", ""));
				
				item.setTitle(result.optString("title", ""));
				item.setYear(result.optString("year", ""));
				
				String author = result.optString("author", "");
				
				item.setCreatorSummary(author);
				JSONArray array = new JSONArray();
				JSONObject member = new JSONObject();
				member.accumulate("creatorType", "author");
				member.accumulate("name", author);
				array.put(member);
				content.put("creators", array);
				
				item.setContent(content);
				item.save(db);
								
				msg = mHandler.obtainMessage();
				Bundle data = new Bundle();
				data.putString("itemKey", item.getKey());
				msg.setData(data);
				msg.arg2 = STATE_DONE;
	        	mHandler.sendMessage(msg);
				return;
			} catch (JSONException e) {
				Log.e(TAG, "exception parsing response", e);
				msg = mHandler.obtainMessage();
	        	msg.arg2 = STATE_ERROR;
	        	mHandler.sendMessage(msg);
	        	return;
			}
		}
		
		public void setState(int state) {
			mState = state;
		}
	}
	
	public class AlertDialogFragment extends DialogFragment {
	    private int id;
	    public AlertDialogFragment (int id) {
	        this.id = id;
	        Bundle args = new Bundle();
//	        args.putInt("title", title);
	        this.setArguments(args);
	    }

	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        switch (id) {
	        case DIALOG_NEW:
	            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	            builder.setTitle(getResources().getString(R.string.item_type))
	                    // XXX i18n
	                    .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int pos) {
	                            Item item = new Item(getActivity().getBaseContext(), Item.ITEM_TYPES[pos]);
	                            item.dirty = APIRequest.API_DIRTY;
	                            item.save(db);
	                            if (collectionKey != null) {
	                                ItemCollection coll = ItemCollection.load(collectionKey, db);
	                                if (coll != null) {
	                                    coll.loadChildren(db);
	                                    coll.add(item);
	                                    coll.saveChildren(db);
	                                }
	                            }
	                            Log.d(TAG, "Loading item data with key: "+item.getKey());
	                            // We create and issue a specified intent with the necessary data
	                            Intent i = new Intent(getActivity().getBaseContext(), ItemDataActivity.class);
	                            i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
	                            startActivity(i);
	                        }
	                    });
	            AlertDialog dialog = builder.create();
	            return dialog;
	        case DIALOG_SORT:
	            AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
	            builder2.setTitle(getResources().getString(R.string.set_sort_order))
	                    // XXX i18n
	                    .setItems(R.array.sorts, new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int pos) {
	                            Cursor cursor;
	                            Configuration conf = getResources().getConfiguration();
	                            conf.locale = Locale.ROOT;
	                            DisplayMetrics metrics = new DisplayMetrics();
	                            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
	                            Resources resources = new Resources(getActivity().getAssets(), metrics, conf);
	                            String sort = resources.obtainTypedArray(R.array.sorts).getString(pos);
	                            setSortBy(sort);//SORTS[pos]);
	                            if (collectionKey != null)
	                                cursor = getCursor(ItemCollection.load(collectionKey, db));
	                            else if (query != null)
	                                cursor = getCursor(query);
	                            else
	                                cursor = getCursor();
	                            ItemAdapter adapter = (ItemAdapter) getListAdapter();
	                            adapter.changeCursor(cursor);
	                            Log.d(TAG, "Re-sorting by: "+sort);//SORTS[pos]);
	                        }
	                    });
	            AlertDialog dialog2 = builder2.create();
	            return dialog2;
	        case DIALOG_PROGRESS:
	            Log.d(TAG, "_____________________dialog_progress");
	            mProgressDialog = new ProgressDialog(getActivity());
	            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	            mProgressDialog.setIndeterminate(true);
	            // XXX i18n
	            mProgressDialog.setMessage("Looking up item...");
	            return mProgressDialog;
	        case DIALOG_IDENTIFIER:
	            final EditText input = new EditText(getActivity());
	            // XXX i18n
	            input.setHint("Enter identifier");
	            
	            dialog = new AlertDialog.Builder(getActivity())
	                // XXX i18n
	                .setTitle("Look up item by identifier")
	                .setView(input)
	                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                        Editable value = input.getText();
	                        // run search
	                        Bundle c = new Bundle(); // FIXME make use of bundle?
	                        c.putString("mode", "isbn");
	                        c.putString("identifier", value.toString());
	                        DialogFragment newFragment = new AlertDialogFragment(DIALOG_PROGRESS);
	                        newFragment.show(getFragmentManager(), "DIALOG_PROGRESS"); // ,c
	                    }
	                }).setNeutralButton("Scan", new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                            IntentIntegrator integrator = new IntentIntegrator(getActivity());
	                            integrator.initiateScan();
	                        }
	                }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                        // do nothing
	                    }
	                }).create();
	            return dialog;
	        default:
	            return null;
	        }
	    }

	}

}