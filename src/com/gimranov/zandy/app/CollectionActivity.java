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

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

import android.app.FragmentBreadCrumbs;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.MenuInflater;
import android.widget.Toast;

public class CollectionActivity extends FragmentActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "com.gimranov.zandy.app.CollectionActivity";
    public String collectionKey;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collections_activity);
        /**
         * We cannot use fragments in the layout
         * if we want to be able to replace those during transaction.
         * http://stackoverflow.com/questions/5658675/replacing-a-fragment-with-another-fragment-inside-activity-group
         * Other options are:
         * 1) Use new activity as before. It doesn't provide smooth feeling during navigation.
         * 2) Don't use transactions at all.
         *    In this case by pushing back we will likely get back to home and activity is destroyed.
         *    There are workarounds, but creation of fragments from code seems to be the proper one.
         * Note that ActionBar is related to Activity. Thus we should maintain breadcrumbs between activities.
         * Keeping just a single activity for navigation makes everything easier. 
         */
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        String collectionKey = getIntent().getStringExtra("com.gimranov.zandy.app.collectionKey");
        CollectionFragment fragment = new CollectionFragment(collectionKey);
        fragmentTransaction.add(R.id.collections, fragment);
        fragmentTransaction.commit();
        
        // FIXME How to use it?
        // I can't compile https://github.com/JakeWharton/HanselAndGretel either :(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setCustomView(new FragmentBreadCrumbs(this));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.common_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, CollectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.do_sync:
            if (!ServerCredentials.check(getBaseContext())) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.sync_log_in_first), 
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            // Make this a collection-specific sync, preceding by de-dirtying
            // De-dirtying
            Database db = new Database(this);
            Item.queue(db);
            db.close();
            APIRequest[] reqs = new APIRequest[Item.queue.size() + 1];
            for (int j = 0; j < Item.queue.size(); j++) {
                Log.d(TAG, "Adding dirty item to sync: "+Item.queue.get(j).getTitle());
                reqs[j] = ServerCredentials.prep(getBaseContext(), APIRequest.update(Item.queue.get(j)));
            }
            if (collectionKey == null) {
                Log.d(TAG, "Adding sync request for all items");
                APIRequest req = new APIRequest(ServerCredentials.APIBASE 
                        + ServerCredentials.prep(getBaseContext(), ServerCredentials.ITEMS +"/top"),
                        "get", null);
                req.disposition = "xml";
                reqs[Item.queue.size()] = req;
            } else {
                Log.d(TAG, "Adding sync request for collection: " + collectionKey);
                APIRequest req = new APIRequest(ServerCredentials.APIBASE
                            + ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS)
                            +"/"
                            + collectionKey + "/items",
                        "get",
                        null);
                req.disposition = "xml";
                reqs[Item.queue.size()] = req;
            }
            // XXX Am I doing it right?
            new ZoteroAPITask(getBaseContext()).execute(reqs);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
                    Toast.LENGTH_SHORT).show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
