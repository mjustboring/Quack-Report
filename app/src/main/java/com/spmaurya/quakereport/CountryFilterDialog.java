package com.spmaurya.quakereport;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CountryFilterDialog {

    private static final String TAG = "___" + CountryFilterDialog.class.getSimpleName();
    private static final Calendar CALENDAR = Calendar.getInstance();

    private Calendar mCalendar;

    private final Context mContext;
    private final EarthquakeActivity mActivity;
    private final AlertDialog mDialog;
    private final SharedPreferences mSharedPreferences;

    private String mStartDate = null;
    private String mEndDate = null;
    private String mCountryName = null;

    private AutoCompleteTextView mSearchBar;

    private final SimpleDateFormat mDateFormatter =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final SimpleDateFormat mDateFormatterStd =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private boolean mIsDataChanged = false;

    public CountryFilterDialog(@NonNull Context context) {

        mContext = context;
        Country.loadAll(mContext);
        mActivity = ((EarthquakeActivity) mContext);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        restoreData();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setView(R.layout.country_filter_dialog);
        builder.setTitle(R.string.filter_dialog_title);

        builder.setNegativeButton(android.R.string.cancel, (di, id) -> restoreData());
        builder.setPositiveButton(android.R.string.ok, (di, id) -> {
            String countryName = mSearchBar.getText().toString().trim();
            if (countryName.isEmpty()) return;
            if (!countryName.equals(mCountryName) && Country.contains(countryName)) {
                mIsDataChanged = true;
                mCountryName = countryName;
                mSharedPreferences.edit().putString("country", countryName).apply();
            }
            mSharedPreferences.edit().putString("endDate", mEndDate).apply();
            mSharedPreferences.edit().putString("startDate", mStartDate).apply();
            if (mIsDataChanged) {
                mActivity.refresh();
            }
        });
        builder.setCancelable(true);

        mDialog = builder.create();

        mDialog.setOnShowListener(dialogInterface -> {

            restoreData();

            @NonNull final ImageView resetSearchBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.country_filter_reset_search_bar));
            mSearchBar = Objects.requireNonNull(mDialog.findViewById(R.id.country_filter_search_bar));
            @NonNull final EditText startDate =
                    Objects.requireNonNull(mDialog.findViewById(R.id.country_filter_start_date));
            @NonNull final EditText endDate =
                    Objects.requireNonNull(mDialog.findViewById(R.id.country_filter_end_date));
            @NonNull final ImageView resetRange =
                    Objects.requireNonNull(mDialog.findViewById(R.id.country_filter_reset_date_range));

            resetSearchBar.setOnClickListener(view -> mSearchBar.setText(""));
            endDate.setText(mEndDate);
            startDate.setText(mStartDate);

            getCalender();
            startDate.setOnClickListener(view -> new DatePickerDialog(
                    mContext, (datePicker, year, month, day) -> {
                        mCalendar.set(Calendar.YEAR, year);
                        mCalendar.set(Calendar.MONTH, month);
                        mCalendar.set(Calendar.DAY_OF_MONTH, day);
                        Date date = mCalendar.getTime();
                        String dateString = mDateFormatter.format(date);
                        if (!dateString.equals(mStartDate)) {
                            mIsDataChanged = true;
                            try {
                                if (date.before(mDateFormatter.parse(endDate.getText().toString()))) {
                                    startDate.setText(mStartDate = dateString);
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "showFilterDialog: " + e.getMessage(), e);
                            }
                        }
                    },
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MARCH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)).show()
            );

            getCalender();
            endDate.setOnClickListener(view -> new DatePickerDialog(
                    mContext, (datePicker, year, month, day) -> {
                        mCalendar.set(Calendar.YEAR, year);
                        mCalendar.set(Calendar.MONTH, month);
                        mCalendar.set(Calendar.DAY_OF_MONTH, day);
                        Date date = mCalendar.getTime();
                        String dateString = mDateFormatter.format(date);
                        if (!dateString.equals(mEndDate)) {
                            mIsDataChanged = true;
                            try {
                                if (date.after(mDateFormatter.parse(startDate.getText().toString()))) {
                                    endDate.setText(mEndDate = dateString);
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "showFilterDialog: " + e.getMessage(), e);
                            }
                        }
                    },
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MARCH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)).show()
            );

            resetRange.setOnClickListener(view -> {
                getCalender();
                String str = endDate.getText().toString();
                endDate.setText(mEndDate = mDateFormatter.format(mCalendar.getTime()));
                mIsDataChanged |= !str.equals(mEndDate);
                mCalendar.add(Calendar.DAY_OF_MONTH, -30);
                str = startDate.getText().toString();
                startDate.setText(mStartDate = mDateFormatter.format(mCalendar.getTime()));
                mIsDataChanged |= !str.equals(mStartDate);
                mSharedPreferences.edit().putString("endDate", mEndDate).apply();
                mSharedPreferences.edit().putString("startDate", mStartDate).apply();
            });

            mSearchBar.setText(mCountryName);
            mSearchBar.setAdapter(new ArrayAdapter<>(
                    mContext, android.R.layout.simple_spinner_item, Country.getCountryNames()));
        });
    }

    public void show() { mDialog.show(); }

    @NonNull
    public String getStartDate() {
        if (mStartDate != null) {
            try {
                return mDateFormatterStd.format(
                        Objects.requireNonNull(mDateFormatter.parse(mStartDate)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        getCalender().add(Calendar.DAY_OF_MONTH, -30);
        return mDateFormatterStd.format(mCalendar);
    }

    @NonNull
    public String getEndDate() {
        if (mEndDate != null) {
            try {
                return mDateFormatterStd.format(
                        Objects.requireNonNull(mDateFormatter.parse(mEndDate)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return mDateFormatterStd.format(getCalender());
    }

    @NonNull
    public String getCountryName() {
        if (mCountryName == null) return "India";
        return mCountryName;
    }

    private void restoreData() {
        mIsDataChanged = false;
        mCountryName = mSharedPreferences.getString("country", "India");
        getCalender();
        mEndDate = mSharedPreferences.getString("endDate", mDateFormatter.format(mCalendar.getTime()));
        mCalendar.add(Calendar.DAY_OF_MONTH, -30);
        mStartDate = mSharedPreferences.getString("startDate", mDateFormatter.format(mCalendar.getTime()));
    }

    private Calendar getCalender() {
        return mCalendar = (Calendar) CALENDAR.clone();
    }
}
