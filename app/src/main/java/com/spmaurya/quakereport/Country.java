package com.spmaurya.quakereport;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class Country {

    private final String mName;
    private final double mLatitudeMin;
    private final double mLatitudeMax;
    private final double mLongitudeMin;
    private final double mLongitudeMax;
    private static boolean isLoaded = false;

    private static final HashMap<String, Country> countriesMap = new HashMap<>();
    private static final ArrayList<String> countries = new ArrayList<>();

    public Country(String name, double latitudeMin, double latitudeMax, double longitudeMin, double longitudeMax) {
        this.mName = name;
        this.mLatitudeMin = latitudeMin;
        this.mLatitudeMax = latitudeMax;
        this.mLongitudeMin = longitudeMin;
        this.mLongitudeMax = longitudeMax;
    }

    public double getLatitudeMin() {
        return mLatitudeMin;
    }

    public double getLatitudeMax() {
        return mLatitudeMax;
    }

    public double getLongitudeMin() {
        return mLongitudeMin;
    }

    public double getLongitudeMax() {
        return mLongitudeMax;
    }

    public String getName() {
        return mName;
    }

    @NonNull
    @Override
    public String toString() {
        return mName;
    }

    private static void addCountry(String name, double latitudeMin, double latitudeMax, double longitudeMin, double longitudeMax) {
        name = name.trim();
        if (!countriesMap.containsKey(name)) {
            countriesMap.put(name, new Country(name, latitudeMin, latitudeMax, longitudeMin, longitudeMax));
            countries.add(name);
        }
    }

    public static Country getCountry(String name) {
        name = name.trim();
        if (countriesMap.containsKey(name)) {
            return countriesMap.get(name);
        }
        return null;
    }

    public static boolean contains(String name) {
        return countriesMap.containsKey(name);
    }

    public static ArrayList<String> getCountryNames() {
        return countries;
    }

    public static void loadAll(final Context context) {

        if (isLoaded) return;

        String jsonString = loadJson(context);

        if (jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    String name = jsonObject.getString("country");
                    double latitudeMin = jsonObject.optDouble("latitude_min");
                    double latitudeMax = jsonObject.optDouble("latitude_max");
                    double longitudeMin = jsonObject.optDouble("longitude_min");
                    double longitudeMax = jsonObject.optDouble("longitude_max");
                    addCountry(name, latitudeMin, latitudeMax, longitudeMin, longitudeMax);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        isLoaded = true;
    }

    private static String loadJson(final Context context) {

        try {
            InputStream inputStream = context.getAssets().open("countries.json");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            String line = reader.readLine();
            StringBuilder countries = new StringBuilder();

            while (line != null) {
                countries.append(line);
                line = reader.readLine();
            }

            return countries.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}