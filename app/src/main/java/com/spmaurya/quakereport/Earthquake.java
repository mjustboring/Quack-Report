package com.spmaurya.quakereport;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Earthquake {

    private final double mMagnitude;
    private final String [] mPlace;
    private final String [] mDateTime;
    private final String mUrl;
    private final LatLng mLatLng;
    private final double mDepth;

    public Earthquake(double magnitude, String place, long dateTime, String url, double latitude, double longitude, double depth) {
        this.mMagnitude = magnitude;
        this.mPlace = extractPlace(place);
        this.mDateTime = extractTime(dateTime);
        this.mUrl = url;
        this.mLatLng = new LatLng(latitude, longitude);
        this.mDepth = depth;
    }

    public int getColor() {

        double magnitude = mMagnitude;

        if (magnitude >= 10) {
            return 0xFFFF0000;
        } else if (magnitude <= 0) {
            return 0xFF00FF00;
        } else if (magnitude == 5) {
            return 0xFFFFFF00;
        }

        int color = 0;

        if (magnitude < 5) {
            color |= (((int) (255 * magnitude / 5)) << 16);
            color |= 0xFF00FF00;
        } else {
            magnitude -= 5;
            color |= (((int) (255 * (1 - magnitude / 5))) << 8);
            color |= 0xFFFF0000;
        }

        return color;
    }

    public int getHue() {

        if (mMagnitude >= 10) {
            return 0;
        } else if (mMagnitude <= 0) {
            return 120;
        }

        return (int)(12 * (10 - mMagnitude));
    }

    @NonNull
    @Override
    public String toString() {
        String title = getMagnitudeStr() + ", ";
        title += mPlace[0] + " " + mPlace[1] + ", ";
        title += mDateTime[1] + " " + mDateTime[0];
        return title;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public double getDepth() {
        return mDepth;
    }

    private String [] extractPlace(String place) {
        String [] temp = new String[2];
        if (place.contains("of")) {
            int idx = place.indexOf("of");
            temp[0] = place.substring(0, idx+2).trim();
            temp[1] = place.substring(idx+2).trim();
        } else {
            temp[0] = "Near the";
            temp[1] = place;
        }
        return temp;
    }

    private String [] extractTime(long time) {
        Date date = new Date(time);
        String [] temp = new String[2];
        temp[0] = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
        temp[1] = new SimpleDateFormat(
                "hh:mm aa", Locale.getDefault()
        ).format(date).replace("am", "AM").replace("pm", "PM");
        return temp;
    }

    public double getMagnitude() {
        return mMagnitude;
    }

    public String getMagnitudeStr() {
        return new DecimalFormat("0.0").format(mMagnitude);
    }

    public String [] getPlace() {
        return mPlace;
    }

    public String [] getDateTime() {
        return mDateTime;
    }

    public String getUrl() {
        return mUrl;
    }

    public int getRadius() {
        int radius = (int)(Math.exp(mMagnitude) * 100);
        if (mDepth <= 70) {
            radius *= 3;
        } else if (mDepth <= 300) {
            radius *= 2;
        }
        return radius;
    }

    public static Bundle makeBundle(ArrayList<Earthquake> earthquakes) {

        final int count = earthquakes.size();

        double [] latitudes = new double[count];
        double [] longitudes = new double[count];
        String [] titles = new String[count];
        int [] colors = new int[count];
        int [] hues = new int[count];
        int [] radii = new int[count];

        for (int i = 0; i < count; i++) {
            Earthquake earthquake = earthquakes.get(i);
            latitudes[i] = earthquake.getLatLng().latitude;
            longitudes[i] = earthquake.getLatLng().longitude;
            titles[i] = earthquake.toString();
            colors[i] = earthquake.getColor();
            hues[i] = earthquake.getHue();
            radii[i] = earthquake.getRadius();
        }

        Bundle bundle = new Bundle();

        bundle.putDoubleArray("latitudes", latitudes);
        bundle.putDoubleArray("longitudes", longitudes);
        bundle.putStringArray("titles", titles);
        bundle.putIntArray("colors", colors);
        bundle.putIntArray("hues", hues);
        bundle.putIntArray("radii", radii);

        return bundle;
    }

    public static Bundle makeBundle(Earthquake earthquake) {

        ArrayList<Earthquake> earthquakes = new ArrayList<>();
        earthquakes.add(earthquake);

        return makeBundle(earthquakes);
    }
}
