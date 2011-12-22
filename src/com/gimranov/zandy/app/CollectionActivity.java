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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.FragmentTransaction;

// http://stackoverflow.com/questions/7499017/is-it-possible-to-display-tabs-without-using-fragments-in-android-3-0 

public class CollectionActivity extends FragmentActivity implements ActionBar.TabListener {
    @SuppressWarnings("unused")
    private static final String TAG = "com.gimranov.zandy.app.CollectionActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collections_activity);
        
        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText("zzz").setTabListener(this));
        bar.selectTab(bar.getTabAt(0));
        
        /* Programmatically create fragment
        // the following somehow works, but what about our IDs?
        // Create the list fragment and add it as our sole content.
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            CollectionFragment list = new CollectionFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, list).commit();
        }
        */

        
        
        /*
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            ItemFragment details = new ItemFragment();
            details.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
        }
        */
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        Log.d(TAG, "Position: " + tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }
}
