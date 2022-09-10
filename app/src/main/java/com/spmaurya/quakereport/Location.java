package com.spmaurya.quakereport;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class Location {

    private final static String TAG = Location.class.getSimpleName();

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/";

    private static final String METHOD_SEARCH = "search?";
    private static final String METHOD_REVERSE = "reverse?";

    private static final String SEARCH_TYPE_DEFAULT = "q=";
    private static final String SEARCH_TYPE_STREET = "street="; // <housenumber> <streetname>
    private static final String SEARCH_TYPE_CITY = "city="; // <city>
    private static final String SEARCH_TYPE_COUNTY = "county="; // <county>
    private static final String SEARCH_TYPE_STATE = "state="; // <state>
    private static final String SEARCH_TYPE_COUNTRY = "country="; // <country>
    private static final String SEARCH_TYPE_POSTAL_CODE = "postalcode="; // <postalcode>

    private static final String ZOOM_TYPE_COUNTRY = "3";
    private static final String ZOOM_TYPE_STATE = "5";
    private static final String ZOOM_TYPE_COUNTY = "8";
    private static final String ZOOM_TYPE_CITY = "10";
    private static final String ZOOM_TYPE_SUBURB = "14";
    private static final String ZOOM_TYPE_MAJOR_STREETS = "16";
    private static final String ZOOM_TYPE_MINOR_STREETS = "17";
    private static final String ZOOM_TYPE_BUILDING = "18";

    private final String mName;
    private final LatLng mLatLng;
    private final LatLngBounds mBounds;

    public enum SearchType {
        DEFAULT, STREET, CITY, COUNTY, STATE, COUNTRY, POSTAL_CODE
    }

    public Location(String name, double longitude, double latitude,
                    double minLongitude, double minLatitude, double maxLongitude, double maxLatitude) {

        this.mName = name != null ? name : "";
        this.mLatLng = new LatLng(latitude, longitude);
        this.mBounds = new LatLngBounds(
                new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, maxLongitude));
    }

    @Override
    @NonNull public String toString() { return mName; }
    @NonNull public String getName() { return mName; }
    @NonNull public LatLng getLatLng() { return mLatLng; }
    @NonNull public LatLngBounds getBounds() { return mBounds; }

    public double getLongitude() { return mLatLng.longitude; }
    public double getLatitude() { return mLatLng.latitude; }
    public double getMinLongitude() { return mBounds.southwest.longitude; }
    public double getMinLatitude() { return mBounds.southwest.latitude; }
    public double getMaxLongitude() { return mBounds.northeast.longitude; }
    public double getMaxLatitude() { return mBounds.northeast.latitude; }

    public static final class Adapter extends BaseAdapter {

        private final Context mContext;
        private final ArrayList<Location> mList;
        private final HashMap<String, Location> mMap;

        Adapter(final Context context) {
            mList = new ArrayList<>();
            mMap = new HashMap<>();
            mContext = context;
        }

        Adapter(final Context context, ArrayList<Location> locations) {
            mList = new ArrayList<>();
            mMap = new HashMap<>();
            mContext = context;
            addAll(locations);
        }

        public void add(Location location) {
            if (location != null) {
                String key = keyOf(location.getName());
                if (!mMap.containsKey(key)) {
                    mMap.put(key, location);
                    mList.add(location);
                }
            }
        }

        public void addAll(Location... locations) {
            for (Location location : locations) {
                add(location);
            }
        }

        public void addAll(Collection<? extends Location> locations) {
            for (Location location : locations) {
                add(location);
            }
        }

        public Location get(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            String key = keyOf(name);
            if (mMap.containsKey(key)) {
                return mMap.get(key);
            }
            return null;
        }

        public void clear() {
            mList.clear();
            mMap.clear();
        }

        public static String keyOf(String text) {
            return text.replace(" ", "").toLowerCase();
        }

        @NonNull
        @Override
        public String toString() {
            return mList.toString();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Location getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = LayoutInflater.from(mContext)
                        .inflate(R.layout.location_item, viewGroup, false);
            }

            final Location location = getItem(position);
            final TextView nameTV = view.findViewById(R.id.location_name);
            nameTV.setText(location.getName());

            return view;
        }
    }

    public static Request newRequest(String text) throws IOException {
        return newRequest(text, SearchType.DEFAULT);
    }

    public static Request newRequest(String text, SearchType type) throws IOException {

        String url = BASE_URL + METHOD_SEARCH;

        switch (type) {
            case STREET: url += SEARCH_TYPE_STREET; break;
            case CITY: url += SEARCH_TYPE_CITY; break;
            case COUNTY: url += SEARCH_TYPE_COUNTY; break;
            case STATE: url += SEARCH_TYPE_STATE; break;
            case COUNTRY: url += SEARCH_TYPE_COUNTRY; break;
            case POSTAL_CODE: url += SEARCH_TYPE_POSTAL_CODE; break;
            default: url += SEARCH_TYPE_DEFAULT;
        }

        url += text.trim().replace(" ", "+");
        url += "&limit=4";
        url += "&format=geojson";
        url += "&accept-language=en-us";

        return Request.newRequest(createURL(url));
    }

    public static Request newRequest(double latitude, double longitude) throws IOException {
        return newRequest(latitude, longitude, ZOOM_TYPE_CITY);
    }

    public static Request newRequest(double latitude, double longitude, String zoomType) throws IOException {

        String url = BASE_URL + METHOD_REVERSE;

        url += "lat=" + latitude;
        url += "&lon=" + longitude;
        url += "&zoom=" + zoomType;
        url += "&format=geojson";
        url += "&accept-language=en-us";

        return Request.newRequest(createURL(url));
    }

    public static Request newRequest(LatLng latLng) throws IOException {
        return newRequest(latLng.latitude, latLng.longitude);
    }

    public static final class Request {

        private final ExecutorService mExecutor;
        private final HttpURLConnection mConnection;
        private OnStartListener mStartListener;
        private OnFinishListener mFinishListener;
        private OnTerminateListener mTerminateListener;
        private String mJsonResponse;
        private ArrayList<Location> mLocations;
        private boolean mTerminated;
        private boolean mFinished;

        private static Request mRequest = null;

        private Request(URL url) throws IOException {
            mExecutor = Executors.newSingleThreadExecutor();
            mConnection = (HttpURLConnection) url.openConnection();
            mConnection.setConnectTimeout(5000);
            mConnection.setReadTimeout(5000);
            mConnection.setRequestMethod("GET");
            mStartListener = null;
            mFinishListener = null;
            mTerminateListener = null;
            mJsonResponse = null;
            mLocations = null;
            mTerminated = false;
            mFinished = false;
        }

        private static Request newRequest(URL url) throws IOException {
            if (mRequest != null) {
                mRequest.terminate();
            }
            return mRequest = new Request(url);
        }

        public void start() {
            if (mStartListener != null) {
                mStartListener.onStart();
            }
            mExecutor.submit(() -> {
                try {
                    mConnection.connect();
                    if (mConnection.getResponseCode() == 200) {
                        InputStream inputStream = mConnection.getInputStream();
                        if (inputStream != null) {
                            mJsonResponse = getJsonResponse(inputStream);
                            inputStream.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (mConnection != null) {
                        mConnection.disconnect();
                    }
                    if (!mTerminated) {
                        mLocations = extractLocations(mJsonResponse);
                        mFinished = true;
                        if (mFinishListener != null) {
                            new Handler(Looper.getMainLooper()).post(
                                    () -> mFinishListener.onFinish(mLocations));
                        }
                    }
                }
            });
            mExecutor.shutdown();
        }

        public void terminate() {
            mConnection.disconnect();
            mExecutor.shutdownNow();
            mTerminated = true;
            if (mTerminateListener != null) {
                mTerminateListener.onTerminate();
            }
        }

        public boolean isTerminated() {
            return mTerminated;
        }

        public boolean isFinished() {
            return mFinished;
        }

        public interface OnStartListener {
            void onStart();
        }

        public interface OnFinishListener {
            void onFinish(ArrayList<Location> locations);
        }

        public interface OnTerminateListener {
            void onTerminate();
        }

        public void setOnStartListener(OnStartListener onStartListener) {
            this.mStartListener = onStartListener;
        }

        public void setOnFinishListener(OnFinishListener onFinishListener) {
            this.mFinishListener = onFinishListener;
        }

        public void setOnTerminateListener(OnTerminateListener onTerminateListener) {
            this.mTerminateListener = onTerminateListener;
        }
    }

    private static ArrayList<Location> extractLocations(String json_response) {

        ArrayList<Location> locations = new ArrayList<>();

        if (json_response == null || json_response.isEmpty()) {
            return locations;
        }

        try {
            JSONObject root = new JSONObject(json_response);
            JSONArray features = root.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");

                String name = properties.optString("display_name", "");

                JSONArray coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates");

                double longitude = coordinates.getDouble(0);
                double latitude = coordinates.getDouble(1);

                JSONArray boundBox = feature.getJSONArray("bbox");

                double minLongitude = boundBox.getDouble(0);
                double minLatitude = boundBox.getDouble(1);
                double maxLongitude = boundBox.getDouble(2);
                double maxLatitude = boundBox.getDouble(3);

                locations.add(new Location(name,
                        longitude, latitude, minLongitude, minLatitude, maxLongitude, maxLatitude));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return locations;
    }

    private static String getJsonResponse(InputStream inputStream) throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
}
