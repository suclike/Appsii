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

package com.appsimobile.appsii.module.home;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.module.weather.loader.CantGetWeatherException;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;
import com.appsimobile.appsii.preference.PreferencesFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog fragment that pops up when touching the preference.
 */
public class YahooLocationChooserDialogFragment extends DialogFragment implements
        TextWatcher, Handler.Callback,
        LoaderManager.LoaderCallbacks<List<YahooWeatherApiClient.LocationSearchResult>>,
        View.OnClickListener {

    public static final int LOCATION_REQUEST_RESULT_DISABLED = 0;

    public static final int LOCATION_REQUEST_RESULT_READY = 1;

    public static final int LOCATION_REQUEST_RESULT_UPDATING_LOCATION = 2;

    /**
     * Time between search queries while typing.
     */
    private static final int QUERY_DELAY_MILLIS = 500;

    @VisibleForTesting
    static LocationUpdateHelper sLocationUpdateHelper;

    boolean mHasUpdatedLocationInfo;

    String mCurrentTownName;

    String mCurrentCountry;

    String mCurrentTimezone;

    String mCurrentWoeid;

    TextView mSearchView;

    TextView mHeaderTextView;

    View mCurrentLocationContainer;

    View mEnableLocationContainer;

    LocationResultListener mLocationResultListener;

    SearchResultsListAdapter mSearchResultsAdapter;

    ListView mSearchResultsList;

    boolean mLocationDisabled;

    Button mApplyCurrentLocationButton;

    Button mCancelCurrentLocationButton;

    Button mCancelUseLocationButton;

    Button mConfirmUseLocationButton;

    Runnable mShowLocationRunnable = new Runnable() {
        @Override
        public void run() {
            Activity activity = getActivity();
            if (activity == null) return;

            SharedPreferences preferences = PreferencesFactory.getPreferences(activity);

            mCurrentTownName = preferences.getString("last_location_update_town", null);
            mCurrentWoeid = preferences.getString("last_location_update_woeid", null);
            mCurrentCountry = preferences.getString("last_location_update_country", null);
            mCurrentTimezone = preferences.getString("last_location_update_timezone", null);
            onLocationInfoAvailable(mCurrentCountry, mCurrentTownName);
        }
    };

    private Handler mHandler;

    private String mQuery;

    private Handler mRestartLoaderHandler;

    public YahooLocationChooserDialogFragment() {
    }

    public static YahooLocationChooserDialogFragment newInstance() {
        return new YahooLocationChooserDialogFragment();
    }

    @Override
    public boolean handleMessage(Message msg) {
        Bundle args = new Bundle();
        args.putString("query", mQuery);
        getLoaderManager().restartLoader(0, args, YahooLocationChooserDialogFragment.this);
        return true;
    }

    public void setLocationResultListener(LocationResultListener locationResultListener) {
        mLocationResultListener = locationResultListener;
        tryBindList();
    }

    private void tryBindList() {
        if (mLocationResultListener == null) {
            return;
        }

        if (isAdded() && mSearchResultsAdapter == null) {
            mSearchResultsAdapter = new SearchResultsListAdapter();
        }

        if (mSearchResultsAdapter != null && mSearchResultsList != null) {
            mSearchResultsList.setAdapter(mSearchResultsAdapter);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tryBindList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mRestartLoaderHandler = new Handler(this);
        if (sLocationUpdateHelper == null) {
            sLocationUpdateHelper = new DefaultLocationUpdate();
        }
        sLocationUpdateHelper.setFragment(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context layoutContext = new ContextThemeWrapper(getActivity(),
                R.style.Appsi_Sidebar_Material_Teal);

        LayoutInflater layoutInflater = LayoutInflater.from(layoutContext);
        View rootView = layoutInflater.inflate(R.layout.fragment_location_chooser, null);
        mSearchView = (TextView) rootView.findViewById(R.id.location_query);

        mCurrentLocationContainer = rootView.findViewById(R.id.location_search);
        mApplyCurrentLocationButton =
                (Button) rootView.findViewById(R.id.apply_current_location_button);
        mCancelCurrentLocationButton =
                (Button) rootView.findViewById(R.id.cancel_current_location_button);


        mEnableLocationContainer = rootView.findViewById(R.id.location_unavailable);
        mCancelUseLocationButton =
                (Button) rootView.findViewById(R.id.cancel_enable_location_button);
        mConfirmUseLocationButton = (Button) rootView.findViewById(R.id.enable_location_button);


        mHeaderTextView = (TextView) rootView.findViewById(R.id.your_location_title);

        mSearchView.addTextChangedListener(this);

        mApplyCurrentLocationButton.setOnClickListener(this);
        mCancelCurrentLocationButton.setOnClickListener(this);
        mCancelUseLocationButton.setOnClickListener(this);
        mConfirmUseLocationButton.setOnClickListener(this);

        // Set up apps
        mSearchResultsList = (ListView) rootView.findViewById(android.R.id.list);
        mSearchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                    int position, long itemId) {
                YahooWeatherApiClient.LocationSearchResult value =
                        (YahooWeatherApiClient.LocationSearchResult) mSearchResultsAdapter
                                .getItem(position);
                mLocationResultListener.onLocationSearchResult(value);
                dismiss();
            }
        });

        tryBindList();

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mHasUpdatedLocationInfo) return;

        int locationResult = startLocationUpdateIfNeeded();
        switch (locationResult) {
            case LOCATION_REQUEST_RESULT_DISABLED:
                mLocationDisabled = true;
                showLocationProviderDisabled();
                break;
            case LOCATION_REQUEST_RESULT_READY:
                mHasUpdatedLocationInfo = true;
                // act as if we are updating the location
                mHandler.postDelayed(mShowLocationRunnable, 1);
                break;
            case LOCATION_REQUEST_RESULT_UPDATING_LOCATION:
                break;
        }

    }

    private int startLocationUpdateIfNeeded() {
        Activity activity = getActivity();
        mHasUpdatedLocationInfo = true;
        return sLocationUpdateHelper.startLocationUpdateIfNeeded(activity);
    }

    void showLocationProviderDisabled() {
        mHasUpdatedLocationInfo = false;
        mEnableLocationContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (sLocationUpdateHelper != null) {
            sLocationUpdateHelper.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sLocationUpdateHelper = null;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        mQuery = charSequence.toString();
        if (mRestartLoaderHandler.hasMessages(0)) {
            return;
        }

        mRestartLoaderHandler.sendMessageDelayed(
                mRestartLoaderHandler.obtainMessage(0),
                QUERY_DELAY_MILLIS);
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public Loader<List<YahooWeatherApiClient.LocationSearchResult>> onCreateLoader(int id,
            Bundle args) {
        final String query = args.getString("query");
        return new ResultsLoader(query, getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<YahooWeatherApiClient.LocationSearchResult>> loader,
            List<YahooWeatherApiClient.LocationSearchResult> results) {
        mSearchResultsAdapter.changeArray(results);
    }

    @Override
    public void onLoaderReset(Loader<List<YahooWeatherApiClient.LocationSearchResult>> loader) {
        mSearchResultsAdapter.changeArray(null);
    }

    public void onCurrentLocationInfoReady(String woeid, String country, String town,
            String timezone) {
        // TODO: create a test for this case
        if (woeid == null || town == null) return;

        mHasUpdatedLocationInfo = true;
        mCurrentTownName = town;
        mCurrentWoeid = woeid;
        mCurrentCountry = country;
        mCurrentTimezone = timezone;

        Activity activity = getActivity();
        if (activity != null) {

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(activity);
            sharedPreferences.edit().
                    putString("last_location_update_town", town).
                    putString("last_location_update_woeid", woeid).
                    putString("last_location_update_country", country).
                    putLong("last_location_update_millis", System.currentTimeMillis()).
                    apply();
            onLocationInfoAvailable(country, town);
        }
    }

    void onLocationInfoAvailable(String country, String currentTownName) {
        updateCurrentLocationFields(currentTownName, country);
        mCurrentLocationContainer.setVisibility(View.VISIBLE);
    }

    private void updateCurrentLocationFields(String header, String summary) {
        mCurrentLocationContainer.setVisibility(View.VISIBLE);
        mHeaderTextView.setText(header);
        String text = getResources().getString(R.string.user_location, header, summary);
        CharSequence styled = Html.fromHtml(text);
        mHeaderTextView.setText(styled);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.apply_current_location_button:
                onCurrentLocationClicked();
                break;
            case R.id.cancel_current_location_button:
                onCurrentLocationCancelClicked();
                break;
            case R.id.enable_location_button:
                onEnableLocationClicked();
                break;
            case R.id.cancel_enable_location_button:
                onCancelEnableLocationClicked();
                break;
        }
    }

    void onCurrentLocationClicked() {
        // communicate the selected result back to the fragment
        YahooWeatherApiClient.LocationSearchResult result =
                new YahooWeatherApiClient.LocationSearchResult();
        result.country = mCurrentCountry;
        result.woeid = mCurrentWoeid;
        result.displayName = mCurrentTownName;
        result.timezone = mCurrentTimezone;
        mLocationResultListener.onLocationSearchResult(result);

        dismiss();
    }

    void onCurrentLocationCancelClicked() {
        mCurrentLocationContainer.setVisibility(View.GONE);
    }

    void onEnableLocationClicked() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(locationIntent);

        dismiss();
    }

    void onCancelEnableLocationClicked() {
        mEnableLocationContainer.setVisibility(View.GONE);
    }

    interface LocationUpdateHelper {

        int startLocationUpdateIfNeeded(Context context);

        void setFragment(YahooLocationChooserDialogFragment fragment);

        void onStop();
    }


    interface LocationReceiver {


        void onCurrentLocationInfoReady(String woeid, String country, String town, String timezone);
    }

    interface LocationResultListener {

        void onLocationSearchResult(YahooWeatherApiClient.LocationSearchResult result);
    }

    /**
     * Loader that fetches location search results from {@link YahooWeatherApiClient}.
     */
    private static class ResultsLoader
            extends AsyncTaskLoader<List<YahooWeatherApiClient.LocationSearchResult>> {

        private String mQuery;

        private List<YahooWeatherApiClient.LocationSearchResult> mResults;

        public ResultsLoader(String query, Context context) {
            super(context);
            mQuery = query;
        }

        @Override
        public List<YahooWeatherApiClient.LocationSearchResult> loadInBackground() {
            return YahooWeatherApiClient.findLocationsAutocomplete(mQuery);
        }

        @Override
        public void deliverResult(List<YahooWeatherApiClient.LocationSearchResult> apps) {
            mResults = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mResults != null) {
                deliverResult(mResults);
            }

            if (takeContentChanged() || mResults == null) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
        }
    }

    public static class LocationLoader implements LocationListener {

        final LocationReceiver mLocationReceiver;

        LocationManager mLocationManager;

        AsyncTask<Location, Void, YahooWeatherApiClient.LocationInfo> mTask;

        public LocationLoader(LocationReceiver locationReceiver) {
            mLocationReceiver = locationReceiver;
        }

        public void requestLocationUpdate(Context context) {
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location lastKnown =
                    mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastKnown != null) {
                onLocationChanged(lastKnown);
            }
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }

        @Override
        public void onLocationChanged(final Location location) {
            if (mTask != null) {
                mTask.cancel(true);
            }
            mTask = new AsyncTask<Location, Void, YahooWeatherApiClient.LocationInfo>() {
                @Override
                protected YahooWeatherApiClient.LocationInfo doInBackground(
                        Location... locations) {
                    Location location = locations[0];
                    try {
                        return YahooWeatherApiClient.getLocationInfo(location);
                    } catch (CantGetWeatherException e) {
                        Log.w("WeatherFragment", "Error getting locationInfo", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(
                        YahooWeatherApiClient.LocationInfo locationInfo) {
                    onLocationInfoLoaded(locationInfo);
                }
            };
            mTask.execute(location);
        }

        void onLocationInfoLoaded(@Nullable YahooWeatherApiClient.LocationInfo locationInfo) {
            List<String> woeids = locationInfo == null ?
                    Collections.<String>emptyList() : locationInfo.woeids;

            String town = locationInfo == null ? null : locationInfo.town;
            String country = locationInfo == null ? null : locationInfo.country;
            String timezone = locationInfo == null ? null : locationInfo.timezone;

            Log.i("WeatherFragment",
                    "town: " + town + " country: " + country + " woeids: " + woeids);
            if (!woeids.isEmpty() && town != null) {
                String woeid = woeids.get(0);
                mLocationReceiver.onCurrentLocationInfoReady(woeid, country, town, timezone);
            } else {
                mLocationReceiver.onCurrentLocationInfoReady(null, null, null, null);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

        public void destroy() {
            if (mTask != null) {
                mTask.cancel(true);
            }
        }
    }

    static abstract class AbstractDefaultLocationUpdate implements LocationUpdateHelper {


        @Override
        public final int startLocationUpdateIfNeeded(Context context) {
            // request location updates if needed
            SharedPreferences preferences = PreferencesFactory.getPreferences(context);

            long lastLocationUpdate = preferences.getLong("last_location_update_millis", 0);
            long passedMillis = System.currentTimeMillis() - lastLocationUpdate;
            int passedMinutes = (int) (passedMillis / DateUtils.MINUTE_IN_MILLIS);

            Log.i("WeatherFragment", "passed time since last location request: " + passedMinutes);

            return doStartLocationUpdateIfNeeded(context, passedMinutes);
        }

        protected abstract int doStartLocationUpdateIfNeeded(Context context, int passedMinutes);

    }

    static class DefaultLocationUpdate extends AbstractDefaultLocationUpdate
            implements LocationReceiver {

        LocationLoader mLocationLoader;

        YahooLocationChooserDialogFragment mFragment;

        DefaultLocationUpdate() {
        }

        @Override
        protected int doStartLocationUpdateIfNeeded(Context context, int passedMinutes) {

            LocationManager locationManager =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // mark the location as received otherwise the
                // ui will start the animation anyway
                return LOCATION_REQUEST_RESULT_DISABLED;
            }

            if (passedMinutes > 3) {
                mLocationLoader = new LocationLoader(this);
                mLocationLoader.requestLocationUpdate(context);
                return LOCATION_REQUEST_RESULT_UPDATING_LOCATION;
            }

            return LOCATION_REQUEST_RESULT_READY;

        }

        @Override
        public void onCurrentLocationInfoReady(String woeid, String country, String town,
                String timezone) {
            if (mFragment != null) {
                mFragment.onCurrentLocationInfoReady(woeid, country, town, timezone);
            }
        }

        @Override
        public void setFragment(YahooLocationChooserDialogFragment fragment) {
            mFragment = fragment;
        }


        @Override
        public void onStop() {
            if (mLocationLoader != null) {
                mLocationLoader.destroy();
            }
            mFragment = null;
        }


    }

    private class SearchResultsListAdapter extends BaseAdapter {

        private List<YahooWeatherApiClient.LocationSearchResult> mResults;

        SearchResultsListAdapter() {
            mResults = new ArrayList<>();
        }

        public void changeArray(List<YahooWeatherApiClient.LocationSearchResult> results) {
            if (results == null) {
                results = new ArrayList<>();
            }

            mResults = results;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return Math.max(1, mResults.size());
        }

        @Override
        public Object getItem(int position) {
            if (position == 0 && mResults.size() == 0) {
                return null;
            }

            return mResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0 && mResults.size() == 0) {
                return -1;
            }

            return mResults.get(position).woeid.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.list_item_weather_location_result, container, false);
            }
            if (position == 0 && mResults.size() == 0) {
                ((TextView) convertView.findViewById(android.R.id.text1))
                        .setText(R.string.weather_auto_location);
                ((TextView) convertView.findViewById(android.R.id.text2))
                        .setText(R.string.weather_auto_location_summary);
            } else {
                YahooWeatherApiClient.LocationSearchResult result = mResults.get(position);
                ((TextView) convertView.findViewById(android.R.id.text1))
                        .setText(result.displayName);
                ((TextView) convertView.findViewById(android.R.id.text2))
                        .setText(result.country);
            }

            return convertView;
        }

        public String getPrefValueAt(int position) {
            if (position == 0 && mResults.size() == 0) {
                return "";
            }

            YahooWeatherApiClient.LocationSearchResult result = mResults.get(position);
            return result.woeid + "," + result.displayName;
        }
    }

}