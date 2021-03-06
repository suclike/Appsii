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

package com.appsimobile.appsii.icontheme.iconpack;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by Nick Martens on 9/22/13.
 */
public class AdwIconPack extends AbstractIconPack {

    public static final String ADW_PACK_ID = "adw";

    AppFilterParser.AppFilterData mAppFilterData;

    private final String mPackageName;

    private Resources mResources;

    private final Uri mIconPackUri;

    private AppFilterDecorationHelper mAppFilterDecorationHelper;

    public AdwIconPack(String packageName) {
        mPackageName = packageName;
        mIconPackUri = createIconPackUri(ADW_PACK_ID, packageName);
    }

    public AdwIconPack(Uri uri, String packageName) {
        mPackageName = packageName;
        mIconPackUri = uri;
    }

    @Override
    public void initialize(Context context) {

        try {
            mResources = context.getPackageManager().getResourcesForApplication(mPackageName);
            Resources resources = mResources;

            mAppFilterData = AppFilterParser.parse(mPackageName, resources);

        } catch (PackageManager.NameNotFoundException e) {

        } catch (XmlPullParserException e) {
            Log.w("AdwIconPack", "error parsing adw theme", e);
        } catch (IOException e) {
            Log.w("AdwIconPack", "error while parsing adw theme", e);
        }
    }


    @Override
    public Bitmap loadIcon(Context context, ComponentName componentName, Uri fallback,
            boolean applyDecorations) {
        Bitmap result = loadIconImpl(context, componentName, fallback);
        if (result == null) {
            result = loadFallBack(context, fallback);
            result = applyDecorations(context, result, fallback);
        }
        return result;
    }

    public Bitmap loadIconImpl(Context context, ComponentName componentName, Uri fallback) {
        try {
            Resources themeResources =
                    context.getPackageManager().getResourcesForApplication(mPackageName);
            String className = componentName.getClassName();
            String resourceName;
            if (mAppFilterData != null && mAppFilterData.mIconNameMappings.containsKey(className)) {
                resourceName = mAppFilterData.mIconNameMappings.get(className);
            } else {
                resourceName = className.replace('.', '_').toLowerCase(Locale.ENGLISH);
            }
            int resId = themeResources.getIdentifier(resourceName, "drawable", mPackageName);
            if (resId == 0) {
                return null;
            }
            return BitmapFactory.decodeResource(themeResources, resId);
        } catch (PackageManager.NameNotFoundException e) {
            // the icon theme is not there, fall back to the uri
            Log.w("AdwIconPack", "error opening theme", e);
        }

        return null;
    }


    @Override
    public Bitmap applyDecorations(Context context, Bitmap original, Uri uri) {
        if (mAppFilterData == null) {
            return original;
        }
        if (mAppFilterDecorationHelper == null) {
            mAppFilterDecorationHelper =
                    AppFilterDecorationHelper.getInstance(mPackageName, mResources, mAppFilterData);
        }
        return mAppFilterDecorationHelper.applyDecorations(original, uri);

    }


    @Override
    public Uri getIconPackUri() {
        return mIconPackUri;
    }
}
