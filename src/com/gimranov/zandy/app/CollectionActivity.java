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

public class CollectionActivity extends FragmentActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "com.gimranov.zandy.app.CollectionActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collections_activity);
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

}
