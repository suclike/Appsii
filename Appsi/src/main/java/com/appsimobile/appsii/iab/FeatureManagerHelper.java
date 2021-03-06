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

package com.appsimobile.appsii.iab;

import android.content.Context;

import com.appsimobile.appsii.preference.ObfuscatedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class that helps determining access to certain features.
 * <p/>
 * <p/>
 * Created by nick on 04/02/15.
 */
@Singleton
public class FeatureManagerHelper {

    ObfuscatedPreferences mPrefs;

    @Inject
    public FeatureManagerHelper(ObfuscatedPreferences prefs) {
        mPrefs = prefs;
    }

    /**
     * Returns true is the purchase was actually purchased
     */
    private static boolean isPurchased(Purchase feature) {
        return feature != null && feature.isPurchased();
    }

    /**
     * Returns true when the Appsi calendar plugin was successfully validated
     */
    public boolean legacyAgendaUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_AGENDA_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.AGENDA_FEATURE, null));
    }

    /**
     * Returns true when the Appsi contacts plugin was successfully validated
     */
    public boolean legacyPeopleUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_PEOPLE_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.PEOPLE_FEATURE, null));
    }

    /**
     * Returns true when the Appsi calls plugin was successfully validated
     */
    public boolean legacyCallsUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_CALLS_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.CALLS_FEATURE, null));
    }

    /**
     * Returns true when the Appsi sms plugin was successfully validated
     */
    public boolean legacySmsUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_SMS_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.SMS_FEATURE, null));
    }

    /**
     * Returns true when the Appsi settings plugin was successfully validated
     */
    public boolean legacySettingsUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_SETTINGS_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.SETTINGS_FEATURE, null));
    }

    /**
     * Returns true when the Appsi powerpack plugin was successfully validated
     */
    public boolean legacyPowerPackUnlocked(Context context) {
        return FeatureManager.APPSI_PLUGIN_POWERPACK_UNLOCKED.
                equals(mPrefs.getString(FeatureManager.SETTINGS_AGENDA_FEATURE, null));
    }

    /**
     * Returns true when the Appsii should allow agenda access. This can either be because
     * the Appsi calendar plugin (or powerpack) was unlocked or because it was unlocked
     * through an in-app purchase.
     */
    public boolean hasAgendaAccess(Context context, FeatureManager featureManager) {
        if (legacyPowerPackUnlocked(context)) return true;
        if (legacyAgendaUnlocked(context)) return true;

        Purchase all = featureManager.getPurchaseForSku(FeatureManager.ALL_FEATURE);
        Purchase feature = featureManager.getPurchaseForSku(FeatureManager.AGENDA_FEATURE);
        Purchase powerPack =
                featureManager.getPurchaseForSku(FeatureManager.SETTINGS_AGENDA_FEATURE);
        if (all == null && feature == null && powerPack == null) return false;
        if (isPurchased(all)) return true;
        if (isPurchased(feature)) return true;
        return isPurchased(powerPack);
    }

    /**
     * Returns true when the Appsii should allow people page access. This can either be because
     * the Appsi contacts plugin was unlocked or because it was unlocked through an in-app
     * purchase.
     */
    public boolean hasPeopleAccess(Context context, FeatureManager featureManager) {
        if (legacyPeopleUnlocked(context)) return true;

        Purchase all = featureManager.getPurchaseForSku(FeatureManager.ALL_FEATURE);
        Purchase feature = featureManager.getPurchaseForSku(FeatureManager.PEOPLE_FEATURE);
        Purchase pack = featureManager.getPurchaseForSku(FeatureManager.SMS_CALLS_PEOPLE_FEATURE);
        if (all == null && feature == null && pack == null) return false;
        if (isPurchased(all)) return true;
        if (isPurchased(feature)) return true;
        return isPurchased(pack);
    }

    /**
     * Returns true when the Appsii should allow calls page access. This can either be because
     * the Appsi calls plugin was unlocked or because it was unlocked through an in-app
     * purchase.
     */
    public boolean hasCallsAccess(Context context, FeatureManager featureManager) {
        if (legacyCallsUnlocked(context)) return true;

        Purchase all = featureManager.getPurchaseForSku(FeatureManager.ALL_FEATURE);
        Purchase feature = featureManager.getPurchaseForSku(FeatureManager.CALLS_FEATURE);
        Purchase pack = featureManager.getPurchaseForSku(FeatureManager.SMS_CALLS_PEOPLE_FEATURE);
        if (all == null && feature == null && pack == null) return false;
        if (isPurchased(all)) return true;
        if (isPurchased(feature)) return true;
        return isPurchased(pack);
    }

    /**
     * Returns true when the Appsii should allow sms-page access. This can either be because
     * the Appsi sms plugin was unlocked or because it was unlocked through an in-app
     * purchase.
     */
    public boolean hasSmsAccess(Context context, FeatureManager featureManager) {
        if (legacySmsUnlocked(context)) return true;

        Purchase all = featureManager.getPurchaseForSku(FeatureManager.ALL_FEATURE);
        Purchase feature = featureManager.getPurchaseForSku(FeatureManager.SMS_FEATURE);
        Purchase pack = featureManager.getPurchaseForSku(FeatureManager.SMS_CALLS_PEOPLE_FEATURE);
        if (all == null && feature == null && pack == null) return false;
        if (isPurchased(all)) return true;
        if (isPurchased(feature)) return true;
        return isPurchased(pack);
    }

    /**
     * Returns true when the Appsii should allow settings-page access. This can either be
     * because the Appsi settings plugin (or powerpack) was unlocked or because it was
     * unlocked through an in-app purchase.
     */
    public boolean hasSettingsAccess(Context context, FeatureManager featureManager) {
        if (legacyPowerPackUnlocked(context)) return true;
        if (legacySettingsUnlocked(context)) return true;

        Purchase all = featureManager.getPurchaseForSku(FeatureManager.ALL_FEATURE);
        Purchase feature = featureManager.getPurchaseForSku(FeatureManager.SETTINGS_FEATURE);
        Purchase pack = featureManager.getPurchaseForSku(FeatureManager.SETTINGS_AGENDA_FEATURE);
        if (all == null && feature == null && pack == null) return false;
        if (isPurchased(all)) return true;
        if (isPurchased(feature)) return true;
        return isPurchased(pack);
    }


}
