/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.iab;

import android.content.Context;

import com.appsimobile.appsii.BuildConfig;

/**
 * Created by nick on 04/02/15.
 */
public class FeatureManagerFactory {

    private static FeatureManager sFeatureManager;

    public static FeatureManager getFeatureManager(Context context) {
        if (sFeatureManager == null) {
            sFeatureManager = new FeatureManagerDebugImpl(context);
        }
        return sFeatureManager;
    }

    public static IabPurchaseHelper.OnIabPurchaseFinishedListener wrapListener(
            final String sku,
            final IabPurchaseHelper.OnIabPurchaseFinishedListener purchaseListener) {

        // in case of test purchases, we want to override the result again
        // with the sku that was really clicked
        if (BuildConfig.TEST_PURCHASES) {
            return new IabPurchaseHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseSuccess(Purchase info) {
                    // override the test-purchase sku with the requested sku
                    info.mSku = sku;
                    purchaseListener.onIabPurchaseSuccess(info);
                    FeatureManagerDebugImpl.addPurchase(info);
                }
            };
        }
        return purchaseListener;

    }
}
