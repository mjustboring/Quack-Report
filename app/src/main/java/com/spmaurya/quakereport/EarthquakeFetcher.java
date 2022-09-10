package com.spmaurya.quakereport;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public final class EarthquakeFetcher {

    public static final Integer INITIAL_OFFSET = 1;
    private static final String TAG = "___" + EarthquakeFetcher.class.getSimpleName();
    public static final String REQUEST_STATUS_NOT_FOUND = "No Earthquake Found!";
    public static final String REQUEST_STATUS_TIMEOUT = "Request Timeout!";
    public static final String REQUEST_STATUS_CONNECTION_FAILED = "URL Connection Failed!";
    public static final String REQUEST_STATUS_NO_NETWORK = "No Network!";
    private static final String USGS_REQUEST_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query";
    private static String mRequestStatus = "";

    private static boolean mIsOccupied = false;

    private EarthquakeFetcher() {}

    public static boolean isOccupied() {
        return mIsOccupied;
    }

    private static String getUrl(final Context context, String startDate, String endDate, int offset) {

        Uri.Builder uriBuilder = Uri.parse(USGS_REQUEST_URL).buildUpon();

        uriBuilder.appendQueryParameter("format", "geojson");
        uriBuilder.appendQueryParameter("limit", Settings.getLimit(context)+"");
        uriBuilder.appendQueryParameter("minmagnitude", Settings.getMinMagnitude(context)+"");
        uriBuilder.appendQueryParameter("orderby", Settings.getOrderBy(context));
        uriBuilder.appendQueryParameter("offset", offset+"");
        uriBuilder.appendQueryParameter("starttime", startDate);
        uriBuilder.appendQueryParameter("endtime", endDate);

        return uriBuilder.toString();
    }

    private static String getUrl(final Context context, String countryName, String startDate, String endDate, int offset) {

        @NonNull final Country country =
                Objects.requireNonNull(Country.getCountry(countryName));

        Uri.Builder uriBuilder = Uri.parse(USGS_REQUEST_URL).buildUpon();

        uriBuilder.appendQueryParameter("format", "geojson");
        uriBuilder.appendQueryParameter("limit", Settings.getLimit(context)+"");
        uriBuilder.appendQueryParameter("minmagnitude", Settings.getMinMagnitude(context)+"");
        uriBuilder.appendQueryParameter("orderby", Settings.getOrderBy(context));
        uriBuilder.appendQueryParameter("offset", offset+"");
        uriBuilder.appendQueryParameter("starttime", startDate);
        uriBuilder.appendQueryParameter("endtime", endDate);
        uriBuilder.appendQueryParameter("minlatitude", country.getLatitudeMin()+"");
        uriBuilder.appendQueryParameter("minlongitude", country.getLongitudeMin()+"");
        uriBuilder.appendQueryParameter("maxlatitude", country.getLatitudeMax()+"");
        uriBuilder.appendQueryParameter("maxlongitude", country.getLongitudeMax()+"");

        return uriBuilder.toString();
    }

    private static String getUrl(final Context context, LatLng latLng, String radius, String startDate, String endDate, int offset) {

        Uri.Builder uriBuilder = Uri.parse(USGS_REQUEST_URL).buildUpon();

        uriBuilder.appendQueryParameter("format", "geojson");
        uriBuilder.appendQueryParameter("limit", Settings.getLimit(context)+"");
        uriBuilder.appendQueryParameter("minmagnitude", Settings.getMinMagnitude(context)+"");
        uriBuilder.appendQueryParameter("orderby", Settings.getOrderBy(context));
        uriBuilder.appendQueryParameter("offset", offset+"");
        uriBuilder.appendQueryParameter("starttime", startDate);
        uriBuilder.appendQueryParameter("endtime", endDate);
        uriBuilder.appendQueryParameter("latitude", latLng.latitude+"");
        uriBuilder.appendQueryParameter("longitude", latLng.longitude+"");
        uriBuilder.appendQueryParameter("maxradiuskm", Double.parseDouble(radius)/1000+"");

        Log.d("___", "getUrl: " + uriBuilder);

        return uriBuilder.toString();
    }

    private static ArrayList<Earthquake> extractEarthquakes(String json_response) {

        ArrayList<Earthquake> earthquakes = new ArrayList<>();

        if (json_response.isEmpty()) {
            return earthquakes;
        }

        try {
            JSONObject jsonObject = new JSONObject(json_response);
            JSONArray jsonArray = jsonObject.getJSONArray("features");

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonArrayObject = jsonArray.getJSONObject(i);

                JSONObject object = jsonArrayObject.getJSONObject("properties");
                double mag = object.optDouble("mag");
                String place = object.optString("place");
                long time = object.optLong("time");
                String url = object.optString("url");

                JSONArray coordinates = jsonArrayObject
                        .getJSONObject("geometry").getJSONArray("coordinates");

                earthquakes.add(new Earthquake(
                        mag, place, time, url, coordinates.optDouble(1),
                        coordinates.optDouble(0), coordinates.optDouble(2)));
            }
        } catch (JSONException e) {
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }

        mIsOccupied = false;

        return earthquakes;
    }

    public static ArrayList<Earthquake> getEarthquakes(final Context context, String startDate, String endDate, int offset) {

        mIsOccupied = true;

        String url = getUrl(context, startDate, endDate, offset);
        String json_response = null;
        try {
            json_response = makeHttpResponse(context, url);
        } catch (IOException e) {
            Log.e(TAG, "Getting Response Failed", e);
        }

        ArrayList<Earthquake> earthquakes =
                extractEarthquakes(json_response == null ? "" : json_response);

        if (earthquakes.isEmpty()) {
            mRequestStatus = REQUEST_STATUS_NOT_FOUND;
        } else {
            mRequestStatus = "";
        }

        return earthquakes;
    }

    public static ArrayList<Earthquake> getEarthquakes(final Context context, String countryName, String startDate, String endDate, int offset) {

        mIsOccupied = true;

        String url = getUrl(context, countryName, startDate, endDate, offset);

        String json_response = null;
        try {
            json_response = makeHttpResponse(context, url);
        } catch (IOException e) {
            Log.e(TAG, "Getting Response Failed", e);
        }
        ArrayList<Earthquake> rawEarthquakes =
                extractEarthquakes(json_response == null ? "" : json_response);

        countryName = countryName.toLowerCase(Locale.getDefault());

        ArrayList<Earthquake> earthquakes = new ArrayList<>();
        for (Earthquake earthquake : rawEarthquakes) {
            String [] place = earthquake.getPlace();
            if (place[1].toLowerCase(Locale.getDefault()).contains(countryName)) {
                earthquakes.add(earthquake);
            }
        }

        if (earthquakes.isEmpty()) {
            mRequestStatus = REQUEST_STATUS_NOT_FOUND;
        } else {
            mRequestStatus = "";
        }

        return earthquakes;
    }

    public static ArrayList<Earthquake> getEarthquakes(final Context context, LatLng latLng, String radius, String startDate, String endDate, int offset) {

        mIsOccupied = true;

        String url = getUrl(context, latLng, radius, startDate, endDate, offset);
        String json_response = null;
        try {
            json_response = makeHttpResponse(context, url);
        } catch (IOException e) {
            Log.e(TAG, "Getting Response Failed", e);
        }

        ArrayList<Earthquake> earthquakes =
                extractEarthquakes(json_response == null ? "" : json_response);

        if (earthquakes.isEmpty()) {
            mRequestStatus = REQUEST_STATUS_NOT_FOUND;
        } else {
            mRequestStatus = "";
        }

        return earthquakes;
    }

    private static String makeHttpResponse(final Context context, String url) throws IOException {
        if (url == null || url.isEmpty()) {
            return null;
        }
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        String jsonResponse = null;
        try {
            urlConnection = (HttpURLConnection) createURL(url).openConnection();
            urlConnection.setConnectTimeout(Settings.getTimeout(context));
            urlConnection.setReadTimeout(Settings.getTimeout(context));
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = getJsonResponse(inputStream);
            } else {
                Log.e(TAG, "Response Code : " + urlConnection.getResponseCode());
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Response Code : " + REQUEST_STATUS_TIMEOUT, e);
            mRequestStatus = REQUEST_STATUS_TIMEOUT;
        } catch (IOException e) {
            Log.e(TAG, "Response Code : " + REQUEST_STATUS_CONNECTION_FAILED, e);
            mRequestStatus = REQUEST_STATUS_CONNECTION_FAILED;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String getJsonResponse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            builder.append(line);
            line = reader.readLine();
        }
        return builder.toString();
    }

    private static URL createURL(String urlStr) {
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            Log.e(TAG, "URL Creation Failed!", e);
        }
        return url;
    }

    public static String getRequestStatus() {
        return mRequestStatus;
    }
}