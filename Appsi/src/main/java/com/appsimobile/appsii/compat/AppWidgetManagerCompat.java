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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.appsimobile.appsii.appwidget.AppWidgetIconCache;

import java.util.List;

public abstract class AppWidgetManagerCompat {

    final Context mContext;
    AppWidgetManager mAppWidgetManager;

    AppWidgetManagerCompat(Context context, AppWidgetManager appWidgetManager) {
        mContext = context;
        mAppWidgetManager = appWidgetManager;
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId) {
        return mAppWidgetManager.getAppWidgetInfo(appWidgetId);
    }

    public abstract List<AppWidgetProviderInfo> getAllProviders();

    public abstract String loadLabel(AppWidgetProviderInfo info);

    public abstract boolean bindAppWidgetIdIfAllowed(
            int appWidgetId, AppWidgetProviderInfo info, Bundle options);

    public abstract UserHandleCompat getUser(AppWidgetProviderInfo info);

    public abstract void startConfigActivity(AppWidgetProviderInfo info, int widgetId,
            Activity activity, AppWidgetHost host, int requestCode);

    public abstract Drawable loadPreview(AppWidgetProviderInfo info);

    public abstract Drawable loadIcon(AppWidgetProviderInfo info, AppWidgetIconCache cache);

    public abstract Bitmap getBadgeBitmap(AppWidgetProviderInfo info, Bitmap bitmap);

}
