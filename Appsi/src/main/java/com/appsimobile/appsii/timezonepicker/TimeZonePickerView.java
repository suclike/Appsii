/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii.timezonepicker;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;

public class TimeZonePickerView extends LinearLayout implements TextWatcher, OnItemClickListener,
        OnClickListener {

    private static final String TAG = "TimeZonePickerView";

    final TimeZoneResultAdapter mResultAdapter;

    private final Context mContext;

    private final AutoCompleteTextView mAutoCompleteTextView;

    private final TimeZoneFilterTypeAdapter mFilterAdapter;
    InputMethodManager mInputMethodManager;
    private boolean mHideFilterSearchOnStart = false;
    private boolean mFirstTime = true;

    public TimeZonePickerView(Context context, AttributeSet attrs,
            String timeZone, long timeMillis, OnTimeZoneSetListener l,
            boolean hideFilterSearch) {
        super(context, attrs);
        mInputMethodManager = AppInjector.provideInputMethodManager();
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tzp_timezonepickerview, this, true);

        mHideFilterSearchOnStart = hideFilterSearch;

        TimeZoneData tzd = new TimeZoneData(mContext, timeZone, timeMillis);

        mResultAdapter = new TimeZoneResultAdapter(mContext, tzd, l);
        ListView timeZoneList = (ListView) findViewById(R.id.timezonelist);
        timeZoneList.setAdapter(mResultAdapter);
        timeZoneList.setOnItemClickListener(mResultAdapter);

        mFilterAdapter = new TimeZoneFilterTypeAdapter(mContext, tzd, mResultAdapter);

        mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.searchBox);
        mAutoCompleteTextView.addTextChangedListener(this);
        mAutoCompleteTextView.setOnItemClickListener(this);
        mAutoCompleteTextView.setOnClickListener(this);

        updateHint(R.string.hint_time_zone_search, R.drawable.ic_search_grey600_24dp);
    }

    private void updateHint(int hintTextId, int imageDrawableId) {
        String hintText = getResources().getString(hintTextId);
//        Drawable searchIcon = getResources().getDrawable(imageDrawableId);

//        SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
//        ssb.append(hintText);
//        int textSize = (int) (mAutoCompleteTextView.getTextSize() * 1.25);
//        searchIcon.setBounds(0, 0, textSize, textSize);
//        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mAutoCompleteTextView.setHint(hintText);
    }

    public void showFilterResults(int type, String string, int time) {
        if (mResultAdapter != null) {
            mResultAdapter.onSetFilter(type, string, time);
        }
    }

    public boolean hasResults() {
        return mResultAdapter != null && mResultAdapter.hasResults();
    }

    public int getLastFilterType() {
        return mResultAdapter != null ? mResultAdapter.getLastFilterType() : -1;
    }

    public String getLastFilterString() {
        return mResultAdapter != null ? mResultAdapter.getLastFilterString() : null;
    }

    public int getLastFilterTime() {
        return mResultAdapter != null ? mResultAdapter.getLastFilterType() : -1;
    }

    public boolean getHideFilterSearchOnStart() {
        return mHideFilterSearchOnStart;
    }

    // Implementation of TextWatcher
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    // Implementation of TextWatcher
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mFirstTime && mHideFilterSearchOnStart) {
            mFirstTime = false;
            return;
        }
        filterOnString(s.toString());
    }

    // Implementation of TextWatcher
    @Override
    public void afterTextChanged(Editable s) {
    }

    // This method will set the adapter if no adapter has been set.  The adapter is initialized
    // here to prevent the drop-down from appearing uninvited on orientation change, as the
    // AutoCompleteTextView.setText() will trigger the drop-down if the adapter has been set.
    private void filterOnString(String string) {
        if (mAutoCompleteTextView.getAdapter() == null) {
            mAutoCompleteTextView.setAdapter(mFilterAdapter);
        }
        mHideFilterSearchOnStart = false;
        mFilterAdapter.getFilter().filter(string);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Hide the keyboard since the user explicitly selected an item.
        InputMethodManager manager = mInputMethodManager;
        manager.hideSoftInputFromWindow(mAutoCompleteTextView.getWindowToken(), 0);
        // An onClickListener for the view item because I haven't figured out a
        // way to update the AutoCompleteTextView without causing an infinite loop.
        mHideFilterSearchOnStart = true;
        mFilterAdapter.onClick(view);
    }

    @Override
    public void onClick(View v) {
        if (mAutoCompleteTextView != null && !mAutoCompleteTextView.isPopupShowing()) {
            filterOnString(mAutoCompleteTextView.getText().toString());
        }
    }

    public interface OnTimeZoneSetListener {

        void onTimeZoneSet(TimeZoneInfo tzi);
    }


}
