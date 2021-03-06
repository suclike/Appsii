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

package com.appsimobile.appsii.compat;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import com.appsimobile.appsii.appwidget.AppWidgetIconCache;
import com.appsimobile.appsii.appwidget.Utilities;

import java.util.List;

public class AppWidgetManagerCompatV16 extends AppWidgetManagerCompat {

    public AppWidgetManagerCompatV16(Context context, AppWidgetManager awm) {
        super(context, awm);
    }

    @Override
    public List<AppWidgetProviderInfo> getAllProviders() {
        return mAppWidgetManager.getInstalledProviders();
    }

    @Override
    public String loadLabel(AppWidgetProviderInfo info) {
        return info.label.trim();
    }

    @Override
    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, AppWidgetProviderInfo info,
            Bundle options) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider);
        } else {
            return mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider, options);
        }
    }

    @Override
    public UserHandleCompat getUser(AppWidgetProviderInfo info) {
        return UserHandleCompat.myUserHandle();
    }

    @Override
    public void startConfigActivity(AppWidgetProviderInfo info, int widgetId, Activity activity,
            AppWidgetHost host, int requestCode) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        intent.setComponent(info.configure);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        Utilities.startActivityForResultSafely(activity, intent, requestCode);
    }

    @Override
    public Drawable loadPreview(AppWidgetProviderInfo info) {
        try {
            return mContext.getPackageManager().getDrawable(
                    info.provider.getPackageName(), info.previewImage, null);
        } catch (OutOfMemoryError e) {
            Runtime.getRuntime().gc();
            return null;
        }
    }

    @Override
    public Drawable loadIcon(AppWidgetProviderInfo info, AppWidgetIconCache cache) {
        return cache.getFullResIcon(info.provider.getPackageName(), info.icon);
    }

    @Override
    public Bitmap getBadgeBitmap(AppWidgetProviderInfo info, Bitmap bitmap) {
        return bitmap;
    }
}
