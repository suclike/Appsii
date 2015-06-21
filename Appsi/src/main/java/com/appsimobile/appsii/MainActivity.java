/*
 * Copyright 2015. Appsi Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.firstrun.FirstRunFragment;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.preference.PreferencesFactory;


public class MainActivity extends AppCompatActivity
        implements FirstRunFragment.OnFirstRunCompletedListener {

    private static final String PREF_FIRST_RUN = "first_run";

    private static final String FRAGMENT_FIRST_RUN = "first_run";
    private static final String FRAGMENT_PREFS = "prefs";


    boolean mFirstRun;

    private SharedPreferences mPreferences;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityUtils.setContentView(this, R.layout.activity_main);
        ActivityUtils.setupToolbar(this, R.id.toolbar);

        mPreferences = PreferencesFactory.getPreferences(this);
        // For now always show in debug builds.
        boolean firstRun = BuildConfig.DEBUG || mPreferences.getBoolean(PREF_FIRST_RUN, true);
        mFirstRun = firstRun;

        if (firstRun) {
            showFirstRunFragment();
        } else {
            showPreferencesFragment();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    private void showFirstRunFragment() {
        FragmentManager fm = getFragmentManager();
        FirstRunFragment fragment = (FirstRunFragment) fm.findFragmentByTag(FRAGMENT_FIRST_RUN);
        if (fragment == null) {
            fragment = new FirstRunFragment();
            fm.beginTransaction().
                    replace(R.id.container, fragment, FRAGMENT_FIRST_RUN).
                    commit();
        }
    }

    private void showPreferencesFragment() {
        FragmentManager fm = getFragmentManager();
        MainPreferencesFragment prefs =
                (MainPreferencesFragment) fm.findFragmentByTag(FRAGMENT_PREFS);

        if (prefs == null) {
            fm.beginTransaction().
                    replace(R.id.container, new MainPreferencesFragment(), FRAGMENT_PREFS).
                    commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!PermissionUtils.holdsPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            showSystemAlertWindowPermissionErrorIfNotFirstRun();
        } else if (!mFirstRun) {
            // when we are in the first run, Appsii is started on
            // page 2, just after granting permission to draw over
            // other apps.
            AppsiiUtils.startAppsi(this);
        }

        FragmentManager fm = getFragmentManager();
        FirstRunFragment fragment = (FirstRunFragment) fm.findFragmentByTag(FRAGMENT_FIRST_RUN);
        if (fragment != null) {
            fragment.setOnFirstRunCompletedListener(this);
        }

    }

    private void showSystemAlertWindowPermissionErrorIfNotFirstRun() {
        if (mFirstRun) return;
        PermissionUtils.requestPermission(this, 1, Manifest.permission.SYSTEM_ALERT_WINDOW);
        // TODO: implement better
    }

    @Override
    public void onFirstRunCompleted() {
        onFirstRunFragmentComplete();
    }

    private void onFirstRunFragmentComplete() {
        mPreferences.edit().putBoolean(PREF_FIRST_RUN, false).apply();
        mFirstRun = false;

        FragmentManager fm = getFragmentManager();

        // clear the listener
        FirstRunFragment prefs = (FirstRunFragment) fm.findFragmentByTag(FRAGMENT_FIRST_RUN);
        if (prefs != null) {
            prefs.setOnFirstRunCompletedListener(null);
        }

        // move forward to the main preferences fragment
        fm.beginTransaction().
                replace(R.id.container, new MainPreferencesFragment(), FRAGMENT_PREFS).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();
    }

    public static class MainPreferencesFragment extends PreferenceFragment {

        AppsiServiceStatusView mAppsiServiceStatusView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // There are two of these. One for each flavor.
            // The community version does not contain the pages option
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }


        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mAppsiServiceStatusView =
                    (AppsiServiceStatusView) view.findViewById(R.id.running_status);

            addPreferencesFromResource(R.xml.prefs_main);
        }
    }

}
