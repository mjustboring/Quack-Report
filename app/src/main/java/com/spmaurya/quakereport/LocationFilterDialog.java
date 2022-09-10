package com.spmaurya.quakereport;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class LocationFilterDialog {

    private static final String TAG = "___" + LocationFilterDialog.class.getSimpleName();
    private static final Calendar CALENDAR = Calendar.getInstance();

    private Calendar mCalendar;

    private final Context mContext;
    private final AlertDialog mDialog;
    private final EarthquakeActivity mActivity;
    private final LocationFilterMapDialog mSubDialog;
    private final SharedPreferences mSharedPreferences;

    private String mStartDate = null;
    private String mEndDate = null;
    private String mLocationName = null;
    private String mRadius = null;
    private LatLng mLatLng = null;
    private LatLngBounds mLatLngBounds = null;

    private final DecimalFormat mDecimalFormat = new DecimalFormat("0.000");

    private final SimpleDateFormat mDateFormatter =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final SimpleDateFormat mDateFormatterStd =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private boolean mIsDataChanged = false;

    public LocationFilterDialog(@NonNull Context context) {

        mContext = context;
        MapsInitializer.initialize(mContext);
        mActivity = ((EarthquakeActivity) mContext);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        restoreData();
        mSubDialog = new LocationFilterMapDialog();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setView(R.layout.location_filter_dialog);
        builder.setTitle(R.string.filter_dialog_title);

        builder.setNegativeButton(android.R.string.cancel, (di, id) -> restoreData());
        builder.setPositiveButton(android.R.string.ok, (di, id) -> {

            mSharedPreferences.edit().putString("location", mLocationName).apply();
            mSharedPreferences.edit().putString("radius", mRadius).apply();
            mSharedPreferences.edit().putString("endDate", mEndDate).apply();
            mSharedPreferences.edit().putString("startDate", mStartDate).apply();

            mSharedPreferences.edit().putString(
                    "latitude", mLatLng.latitude + "").apply();
            mSharedPreferences.edit().putString(
                    "longitude", mLatLng.longitude + "").apply();
            mSharedPreferences.edit().putString(
                    "minLatitude", mLatLngBounds.southwest.latitude + "").apply();
            mSharedPreferences.edit().putString(
                    "minLongitude", mLatLngBounds.southwest.longitude + "").apply();
            mSharedPreferences.edit().putString(
                    "maxLatitude", mLatLngBounds.northeast.latitude + "").apply();
            mSharedPreferences.edit().putString(
                    "maxLongitude", mLatLngBounds.northeast.longitude + "").apply();

            if (mIsDataChanged) {
                mActivity.refresh();
            }
        });
        builder.setCancelable(true);

        mDialog = builder.create();

        mDialog.setOnShowListener(dialogInterface -> {

            restoreData();

            @NonNull final EditText searchBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_search_bar));
            @NonNull final TextView radius =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_radius));
            @NonNull final View radiusContainer =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_radius_container));
            @NonNull final EditText startDate =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_start_date));
            @NonNull final EditText endDate =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_end_date));
            @NonNull final ImageView resetRange =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_reset_date_range));

            searchBar.setText(mLocationName);
            radius.setText(mDecimalFormat.format(Double.parseDouble(mRadius) / 1000));
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

            searchBar.setOnClickListener(et -> mSubDialog.showDialog(searchBar, radius));

            radiusContainer.setOnClickListener(tv -> mSubDialog.showDialog(searchBar, radius));
        });
    }

    public void showDialog() { mDialog.show(); }

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
    public String getLocationName() {
        return mLocationName;
    }

    @NonNull
    public LatLng getLocation() {
        return mLatLng;
    }

    @NonNull
    public LatLngBounds getBounds() {
        return mLatLngBounds;
    }

    @NonNull
    public String getRadius() {
        return mRadius;
    }

    private void restoreData() {
        mIsDataChanged = false;
        mLocationName = mSharedPreferences.getString("location", "India");
        mLatLng = new LatLng(
                Double.parseDouble(mSharedPreferences.getString("latitude", "22.3511148")),
                Double.parseDouble(mSharedPreferences.getString("longitude", "78.6677428"))
        );
        LatLng southwest = new LatLng(
                Double.parseDouble(mSharedPreferences.getString("minLatitude", "6.5531169")),
                Double.parseDouble(mSharedPreferences.getString("minLongitude", "67.9544415"))
        );
        LatLng northeast = new LatLng(
                Double.parseDouble(mSharedPreferences.getString("maxLatitude", "35.6745457")),
                Double.parseDouble(mSharedPreferences.getString("maxLongitude", "97.395561"))
        );
        mLatLngBounds = new LatLngBounds(southwest, northeast);
        mRadius = mSharedPreferences.getString("radius", "50000");
        getCalender();
        mEndDate = mSharedPreferences.getString("endDate", mDateFormatter.format(mCalendar.getTime()));
        mCalendar.add(Calendar.DAY_OF_MONTH, -30);
        mStartDate = mSharedPreferences.getString("startDate", mDateFormatter.format(mCalendar.getTime()));
    }

    private Calendar getCalender() {
        return mCalendar = (Calendar) CALENDAR.clone();
    }

    private final class LocationFilterMapDialog
            implements DialogInterface.OnShowListener, OnMapReadyCallback {

        private final AlertDialog mDialog;
        private String mTempLocationName;
        private String mTempRadius;
        private LatLng mTempLatLng;
        private LatLngBounds mTempLatLngBounds;
        private GoogleMap mMap;

        private Marker mMarker = null;
        private Circle mCircle = null;

        private EditText mSearchBarView = null;
        private TextView mRadiusView = null;

        public LocationFilterMapDialog() {

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

            builder.setView(R.layout.location_filter_map_dialog);
            builder.setTitle(R.string.location_filter_map_dialog_title);

            builder.setNegativeButton(android.R.string.cancel, (di, id) -> {
                mTempLocationName = mLocationName;
                mTempRadius = mRadius;
                mTempLatLng = mLatLng;
                mTempLatLngBounds = mLatLngBounds;
            });
            builder.setPositiveButton(android.R.string.ok, (di, id) -> {
                if (mTempLocationName != null && !mTempLocationName.equals(mLocationName)) {
                    mIsDataChanged = true;
                    mLocationName = mTempLocationName;
                    if (mSearchBarView != null) {
                        mSearchBarView.setText(mLocationName);
                    }
                }
                if (mTempRadius != null && !mTempRadius.equals(mRadius)) {
                    mIsDataChanged = true;
                    mRadius = mTempRadius;
                    if (mRadiusView != null) {
                        mRadiusView.setText(mDecimalFormat.format(Double.parseDouble(mRadius) / 1000));
                    }
                }
                if (mTempLatLng != null && !mTempLatLng.equals(mLatLng)) {
                    mIsDataChanged = true;
                    mLatLng = mTempLatLng;
                }
                if (mTempLatLngBounds != null && !mTempLatLngBounds.equals(mLatLngBounds)) {
                    mIsDataChanged = true;
                    mLatLngBounds = mTempLatLngBounds;
                }
            });

            mDialog = builder.create();
            mDialog.setOnShowListener(this);
            mDialog.setCanceledOnTouchOutside(true);
            mDialog.setOnCancelListener(di -> {
                mTempLocationName = mLocationName;
                mTempRadius = mRadius;
                mTempLatLng = mLatLng;
                mTempLatLngBounds = mLatLngBounds;
            });
        }

        public void showDialog(EditText searchBar, TextView radius) {
            mSearchBarView = searchBar;
            mRadiusView = radius;
            mDialog.show();
        }

        @Override
        public void onShow(DialogInterface dialogInterface) {

            mTempLocationName = mLocationName;
            mTempRadius = mRadius;
            mTempLatLng = mLatLng;
            mTempLatLngBounds = mLatLngBounds;

            @NonNull final MapView mapView =
                    Objects.requireNonNull(mDialog.findViewById(R.id.map));
            @NonNull final AutoCompleteTextView searchBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_search_bar));
            @NonNull final ImageView resetSearchBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_reset_search_bar));
            @NonNull final ImageView seekBarIncrease =
                    Objects.requireNonNull(mDialog.findViewById(R.id.seek_increase));
            @NonNull final ImageView seekBarDecrease =
                    Objects.requireNonNull(mDialog.findViewById(R.id.seek_decrease));
            @NonNull final SeekBar seekBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.radius_seek_bar));
            @NonNull final TextView seekBarValue =
                    Objects.requireNonNull(mDialog.findViewById(R.id.radius_seek_bar_value));
            @NonNull final ListView listView =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_search_bar_list));
            @NonNull final ProgressBar progressBar =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_search_bar_progress));
            @NonNull final ImageView controlToggle =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_control_toggle));
            @NonNull final RelativeLayout controls =
                    Objects.requireNonNull(mDialog.findViewById(R.id.location_filter_map_controls));
            @NonNull final ImageView recenter =
                    Objects.requireNonNull(mDialog.findViewById(R.id.map_recenter));

            @NonNull final Location.Adapter adapter = new Location.Adapter(mContext);
            listView.setAdapter(adapter);

            mapView.onCreate(mDialog.onSaveInstanceState());
            mapView.onResume();

            mapView.getMapAsync(this);

            final TextWatcher textWatcher = new TextWatcher() {

                @Override
                public void afterTextChanged(Editable editable) {

                    String text = editable.toString().trim();
                    if (text.isEmpty() || text.equals(mTempLocationName)) return;

                    Location.Request request;
                    try {
                        request = Location.newRequest(text);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    request.setOnFinishListener(locations -> {
                        adapter.clear();
                        adapter.addAll(locations);
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    });

                    request.setOnStartListener(() -> progressBar.setVisibility(View.VISIBLE));
                    request.setOnTerminateListener(() -> progressBar.setVisibility(View.GONE));

                    request.start();
                }

                public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {}
                public void onTextChanged(CharSequence cs, int i, int i1, int i2) {}
            };

            listView.setOnItemClickListener((adapterView, view, i, l) -> {
                searchBar.removeTextChangedListener(textWatcher);
                Location location = (Location)listView.getItemAtPosition(i);
                updateLocation(location);
                mTempRadius = seekBar.getProgress() + "";
                searchBar.setText(mTempLocationName);
                hideKeyboard(searchBar.getContext(), searchBar);
                searchBar.clearFocus();
                listView.setVisibility(View.GONE);
                searchBar.addTextChangedListener(textWatcher);
            });

            searchBar.setText(mLocationName);
            seekBar.setProgress(Integer.parseInt(mRadius));
            seekBarValue.setText(
                    mDecimalFormat.format(Double.parseDouble(mRadius) / 1000));

            searchBar.addTextChangedListener(textWatcher);

            resetSearchBar.setOnClickListener(iv -> searchBar.setText(""));

            seekBarIncrease.setOnClickListener(iv -> {

                int progress = seekBar.getProgress() / 1000 + 1;
                if (progress <= 10000) {
                    seekBar.setProgress(progress * 1000);
                }
            });

            seekBarDecrease.setOnClickListener(iv -> {

                int progress = seekBar.getProgress() / 1000;
                if (progress * 1000 == seekBar.getProgress()) {
                    progress -= 1;
                }
                if (progress >= 1) {
                    seekBar.setProgress(progress * 1000);
                }
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    mCircle.setRadius(progress);
                    mTempRadius = progress + "";
                    seekBarValue.setText(mDecimalFormat.format(progress / 1000.0));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            controlToggle.setOnClickListener(iv -> {
                if (controls.getVisibility() == View.VISIBLE) {
                    controlToggle.setAlpha(controlToggle.getAlpha() / 2);
                    controls.setVisibility(View.GONE);
                } else {
                    controlToggle.setAlpha(controlToggle.getAlpha() * 2);
                    controls.setVisibility(View.VISIBLE);
                }
            });

            recenter.setOnClickListener(iv -> mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(mTempLatLngBounds, 0)));
        }

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mMap = googleMap;
            mMarker = mMap.addMarker(new MarkerOptions().position(mLatLng));
            mCircle = mMap.addCircle(new CircleOptions().radius(Double.parseDouble(mRadius)).
                    center(mLatLng).strokeColor(0xC0FF0000).strokeWidth(5));
            updateLocation();
        }

        private void updateLocation(Location location) {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMarker.setTitle(location.getName());
            mMarker.setPosition(latLng);

            double pd = Math.ceil(Math.abs(location.getMaxLatitude() - location.getMinLatitude())) / 4;

            LatLng southwest = new LatLng(
                    location.getMinLatitude()-pd, location.getMinLongitude()-pd);
            LatLng northeast = new LatLng(
                    location.getMaxLatitude()+pd, location.getMaxLongitude()+pd);

            LatLngBounds latLngBounds = new LatLngBounds(southwest, northeast);

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,0));

            mCircle.setCenter(latLng);

            mTempLocationName = location.getName();
            mTempLatLng = latLng;
            mTempLatLngBounds = latLngBounds;
        }

        private void updateLocation() {

            updateLocation(new Location(
                    mTempLocationName, mTempLatLng.longitude, mTempLatLng.latitude,
                    mTempLatLngBounds.southwest.longitude, mTempLatLngBounds.southwest.latitude,
                    mTempLatLngBounds.northeast.longitude, mTempLatLngBounds.northeast.latitude));
        }
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
