package com.spmaurya.quakereport;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class Settings extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private EditTextPreference mMinMagnitude = null;
    private ListPreference mOrderBy = null;
    private ListPreference mFilterBy = null;
    private SwitchPreferenceCompat mLimitByItem = null;
    private EditTextPreference mListItemLimit = null;
    private EditTextPreference mListFetchLimit = null;
    private EditTextPreference mTimeout = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
        initPreferences();
        refreshLimits();
        bindPreferenceSummaries();
    }

    public void restoreSettings() {
        mMinMagnitude.setText(getString(R.string.settings_minimum_magnitude_default_value));
        mOrderBy.setValue(getString(R.string.settings_order_by_default_value));
        mFilterBy.setValue(getString(R.string.settings_filter_by_default_value));
        mLimitByItem.setChecked(
                Boolean.parseBoolean(getString(R.string.settings_limit_by_item_default_value)));
        mListItemLimit.setText(getString(R.string.settings_list_item_limit_default_value));
        mListFetchLimit.setText(getString(R.string.settings_list_fetch_limit_default_value));
        mTimeout.setText(getString(R.string.settings_timeout_default_value));
        refreshLimits();
        bindPreferenceSummaries();
    }

    public void initPreferences() {
        mMinMagnitude = findPreference(getString(R.string.settings_minimum_magnitude_key));
        mOrderBy = findPreference(getString(R.string.settings_order_by_key));
        mFilterBy = findPreference(getString(R.string.settings_filter_by_key));
        mLimitByItem = findPreference(getString(R.string.settings_limit_by_item_key));
        if (mLimitByItem != null) {
            mLimitByItem.setOnPreferenceClickListener(preference -> {
                refreshLimits();
                return false;
            });
        }
        mListItemLimit = findPreference(getString(R.string.settings_list_item_limit_key));
        mListFetchLimit = findPreference(getString(R.string.settings_list_fetch_limit_key));
        mTimeout = findPreference(getString(R.string.settings_timeout_key));
    }

    public void bindPreferenceSummaries() {
        bindPreferenceSummary(mMinMagnitude);
        bindPreferenceSummary(mOrderBy);
        bindPreferenceSummary(mFilterBy);
        bindPreferenceSummary(mListItemLimit);
        bindPreferenceSummary(mListFetchLimit);
        bindPreferenceSummary(mTimeout);
    }

    private void bindPreferenceSummary(Preference preference) {
        if (preference == null) return;
        preference.setOnPreferenceChangeListener(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        String preferenceString = preferences.getString(preference.getKey(), "");
        onPreferenceChange(preference, preferenceString);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

        final String SETTINGS_ORDER_BY_KEY = getString(R.string.settings_order_by_key);
        final String SETTINGS_FILTER_BY_KEY = getString(R.string.settings_filter_by_key);
        final String SETTINGS_LIST_FETCH_LIMIT_KEY = getString(R.string.settings_list_fetch_limit_key);
        final String SETTINGS_LIST_ITEM_LIMIT_KEY = getString(R.string.settings_list_item_limit_key);
        final String SETTINGS_TIMEOUT_KEY = getString(R.string.settings_timeout_key);
        final String SETTINGS_MINIMUM_MAGNITUDE_KEY = getString(R.string.settings_minimum_magnitude_key);

        String stringValue = newValue.toString().trim();
        String key = preference.getKey();

        if (key.equals(SETTINGS_ORDER_BY_KEY) || key.equals(SETTINGS_FILTER_BY_KEY)) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                CharSequence[] labels = listPreference.getEntries();
                preference.setSummary(labels[prefIndex]);
            }
        } else if (key.equals(SETTINGS_LIST_FETCH_LIMIT_KEY) ||
                key.equals(SETTINGS_MINIMUM_MAGNITUDE_KEY) ||
                key.equals(SETTINGS_LIST_ITEM_LIMIT_KEY) ||
                key.equals(SETTINGS_TIMEOUT_KEY)) {

            double value = stringValue.isEmpty() ? -1 : Double.parseDouble(stringValue);

            if (key.equals(SETTINGS_LIST_FETCH_LIMIT_KEY)) {
                if (value < 12 || value > 20000) {
                    return false;
                }
            } else if (key.equals(SETTINGS_LIST_ITEM_LIMIT_KEY)) {
                if (value < 1 || value > 20000) {
                    return false;
                }
            } else if (key.equals(SETTINGS_MINIMUM_MAGNITUDE_KEY)) {
                if (value < 0 || value > 10) {
                    return false;
                }
            } else {
                if (value < 5 || value > 300) {
                    return false;
                }
            }

            preference.setSummary(stringValue);
        }
        return true;
    }

    public void refreshLimits() {
        if (mListItemLimit != null) {
            mListItemLimit.setEnabled(mLimitByItem.isChecked());
        }
        if (mListFetchLimit != null) {
            mListFetchLimit.setEnabled(!mLimitByItem.isChecked());
        }
    }

    public static int getMinMagnitude(final Context context) {
        return Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        context.getString(R.string.settings_minimum_magnitude_key), context.getString(R.string.settings_minimum_magnitude_default_value)));
    }

    public static String getOrderBy(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.settings_order_by_key),
                context.getString(R.string.settings_order_by_default_value)
        );
    }

    public static String getFilterBy(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.settings_filter_by_key),
                context.getString(R.string.settings_filter_by_default_value)
        );
    }

    public static int getLimit(final Context context) {
        return isLimitByItemsEnabled(context) ? getItemLimit(context) : getFetchLimit(context);
    }

    public static int getItemLimit(final Context context) {
        return Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        context.getString(R.string.settings_list_item_limit_key), context.getString(R.string.settings_list_item_limit_default_value)));
    }

    public static int getFetchLimit(final Context context) {
        return Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        context.getString(R.string.settings_list_fetch_limit_key), context.getString(R.string.settings_list_fetch_limit_default_value)));
    }

    public static boolean isLimitByItemsEnabled(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.settings_limit_by_item_key), Boolean.parseBoolean(context.getString(R.string.settings_limit_by_item_default_value)));
    }

    public static int getTimeout(final Context context) {
        return Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        context.getString(R.string.settings_timeout_key),
                        context.getString(R.string.settings_timeout_default_value)
                )) * 1000;
    }
}
