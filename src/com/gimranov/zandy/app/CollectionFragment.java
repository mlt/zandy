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

import android.support.v4.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.CollectionAdapter;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

/* Rework for collections only, then make another one for items */
public class CollectionFragment extends ListFragment {

	private static final String TAG = "com.gimranov.zandy.app.CollectionFragment";
	private ItemCollection collection;
	private Database db;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
        if (container == null) {
            // see http://developer.android.com/reference/android/app/Fragment.html
            return null;
        }
        */
        return inflater.inflate(R.layout.collections, container, false);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        
        db = new Database(getActivity()); // is context used somewhere? and what is it for?
        
        CollectionAdapter collectionAdapter;
        
        String collectionKey = getActivity().getIntent().getStringExtra("com.gimranov.zandy.app.collectionKey");
        if (collectionKey != null) {
            ItemCollection coll = ItemCollection.load(collectionKey, db);
            // We set the title to the current collection
            this.collection = coll;
            getActivity().setTitle(coll.getTitle());
            collectionAdapter = new CollectionAdapter(getActivity(), create(coll));
        } else {
            getActivity().setTitle(getResources().getString(R.string.collections));
            collectionAdapter = new CollectionAdapter(getActivity(), create());
        }
        setListAdapter(collectionAdapter);

        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CollectionAdapter adapter = (CollectionAdapter) parent.getAdapter();
                Cursor cur = adapter.getCursor();
                // Place the cursor at the selected item
                if (cur.moveToPosition(position)) {
                    // and replace the cursor with one for the selected collection
                    ItemCollection coll = ItemCollection.load(cur);
                    if (coll != null && coll.getKey() != null && coll.getSubcollections(db).size() > 0) {
                        Log.d(TAG, "Loading child collection with key: "+coll.getKey());
                        // We create and issue a specified intent with the necessary data
                        // TODO use fragment transaction to replace existing one?
                        Intent i = new Intent(getActivity().getBaseContext(), CollectionActivity.class);
                        i.putExtra("com.gimranov.zandy.app.collectionKey", coll.getKey());
                        startActivity(i);
                    } else {
                        Log.d(TAG, "Failed loading child collections for collection");
                        Toast.makeText(getActivity().getApplicationContext(),
                                getResources().getString(R.string.collection_no_subcollections), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // failed to move cursor-- show a toast
                    TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
                    Toast.makeText(getActivity().getApplicationContext(),
                            getResources().getString(R.string.collection_cant_open, tvTitle.getText()), 
                            Toast.LENGTH_SHORT).show();
                }
          }
        });
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                
                CollectionAdapter adapter = (CollectionAdapter) parent.getAdapter();
                Cursor cur = adapter.getCursor();
                // Place the cursor at the selected item
                if (cur.moveToPosition(position)) {
                    // and replace the cursor with one for the selected collection
                    ItemCollection coll = ItemCollection.load(cur);
                    if (coll != null && coll.getKey() != null) {
                        if (coll.getSize() == 0) {
                            Log.d(TAG, "Collection with key: "+coll.getKey()+ " is empty.");
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getResources().getString(R.string.collection_empty),
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Running a request to populate missing data for collection");
                            APIRequest req = new APIRequest(ServerCredentials.APIBASE
                                    + ServerCredentials.prep(getActivity().getBaseContext(), ServerCredentials.COLLECTIONS)
                                    +"/"+coll.getKey()+"/items", "get", null);
                            req.disposition = "xml";
                            // TODO Introduce a callback to update UI when ready
                            new ZoteroAPITask(getActivity().getBaseContext(), (CursorAdapter) getListAdapter()).execute(req);
                        }
                        Log.d(TAG, "Loading items for collection with key: "+coll.getKey());
                        // We create and issue a specified intent with the necessary data
                        Intent i = new Intent(getActivity().getBaseContext(), ItemActivity.class);
                        i.putExtra("com.gimranov.zandy.app.collectionKey", coll.getKey());
                        startActivity(i);
                    } else {
                        // collection loaded was null. why?
                        Log.d(TAG, "Failed loading items for collection at position: "+position);
                        return true;
                    }
                } else {
                    // failed to move cursor-- show a toast
                    TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
                    Toast.makeText(getActivity().getApplicationContext(),
                            getResources().getString(R.string.collection_cant_open, tvTitle.getText()), 
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return true;
          }
        });
//        onResume();
    }

    /** Called when the fragment is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // necessary for a fragment
    }
/*    
    public void onResume() {
		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
		// XXX This may be too agressive-- fix if causes issues
		Cursor newCursor = (collection == null) ? create() : create(collection);
		adapter.changeCursor(newCursor);
		adapter.notifyDataSetChanged();
		if (db == null) db = new Database(getActivity());
    	super.onResume();
    }
*/    
    public void onDestroy() {
		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
		Cursor cur = adapter.getCursor();
		if(cur != null) cur.close();
		if (db != null) db.close();
		super.onDestroy();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.zotero_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getActivity().getApplicationContext())) {
            	Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Making sync request for all collections");
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE 
        			+ ServerCredentials.prep(getActivity().getBaseContext(), ServerCredentials.COLLECTIONS),
        			"get", null);
			req.disposition = "xml";
			new ZoteroAPITask(getActivity().getBaseContext(), (CursorAdapter) getListAdapter()).execute(req);	
        	Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.sync_collection), 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_new:
        	Log.d(TAG, "Can't yet make new collections");
        	// XXX no i18n for temporary string
        	Toast.makeText(getActivity().getApplicationContext(), "Sorry, new collection creation is not yet possible. Soon!", 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
	/**
	 * Gives a cursor for top-level collections
	 * @return
	 */
	public Cursor create() {
	    Log.d(TAG, "create() called");
		String[] args = { "false" };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}

		return cursor;
	}

	/**
	 * Gives a cursor for child collections of a given parent
	 * @param parent
	 * @return
	 */
	public Cursor create(ItemCollection parent) {
        Log.d(TAG, "create(parent) is called");
		String[] args = { parent.getKey() };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}

		return cursor;
	}   
}
