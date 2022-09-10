package com.spmaurya.quakereport;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.spmaurya.quakereport.databinding.ActivityMapsBinding;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    private GoogleMap mMap;
    private static final int DURATION = 3000;
    private static final int DURATION_GAP = 500;
    private static final int ANIMATE_CIRCLE_THRESHOLD = 70;
    private static final int ANIMATE_CIRCLES_THRESHOLD = 7;

    private final ArrayList<Circle> mCircles = new ArrayList<>();
    private final ArrayList<Float> mCircleStrokes = new ArrayList<>();
    private float mZoom = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.spmaurya.quakereport.databinding.ActivityMapsBinding
                binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mMapFragment = new SupportMapFragment();

        getSupportFragmentManager().beginTransaction().replace(R.id.map, mMapFragment).commit();

        mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        Bundle bundle = getIntent().getBundleExtra("quakeBundle");

        double [] latitudes = bundle.getDoubleArray("latitudes");
        double [] longitudes = bundle.getDoubleArray("longitudes");
        String [] titles = bundle.getStringArray("titles");
        int [] colors = bundle.getIntArray("colors");
        int [] radii = bundle.getIntArray("radii");
        int [] hues = bundle.getIntArray("hues");

        int count = hues.length;

        if (count == 1) {

            LatLng latLng = new LatLng(latitudes[0], longitudes[0]);

            MarkerOptions marker = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(hues[0]))
                    .position(latLng).title(titles[0]);

            mMap.addMarker(marker);
            animateCircles(latLng, radii[0], colors[0]);

            mMap.animateCamera(CameraUpdateFactory
                    .newLatLngZoom(new LatLng(latitudes[0], longitudes[0]),
                            (int) (21 - Math.log(radii[0] / 5f) / Math.log(2))));
        } else {

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (int i = 0; i < count; i++) {

                LatLng latLng = new LatLng(latitudes[i], longitudes[i]);

                builder.include(latLng);

                MarkerOptions marker = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(hues[i]))
                        .position(latLng).title(titles[i]);

                mMap.addMarker(marker);

                if (count > ANIMATE_CIRCLE_THRESHOLD) {
                    makeCircle(latLng, radii[i], colors[i]);
                } else
                    if (count > ANIMATE_CIRCLES_THRESHOLD) {
                    animateCircle(latLng, radii[i], colors[i], 0);
                } else {
                    animateCircles(latLng, radii[i], colors[i]);
                }
            }
            mMap.animateCamera(CameraUpdateFactory
                    .newLatLngBounds(builder.build(), 2));
        }

        mMap.setOnCameraMoveListener(this);
    }

    private void animateCircles(final LatLng latLng, int radius, int color) {

        for (int i = 0; i < DURATION; i += DURATION_GAP) {
            animateCircle(latLng, radius, color, i);
        }
    }

    private void animateCircle(final LatLng latLng, int radius, int color, long delay) {

        final float stroke = radius * 0.000001f;

        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeColor(color)
                .fillColor(Color.TRANSPARENT)
                .strokeWidth(stroke * mMap.getCameraPosition().zoom);

        Circle circle = mMap.addCircle(circleOptions);
        mCircles.add(circle);
        mCircleStrokes.add(stroke);
        ValueAnimator valueAnimator = new ValueAnimator();

        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
        valueAnimator.setIntValues(0, radius);
        valueAnimator.setDuration(DURATION);
        valueAnimator.setStartDelay(delay);
        valueAnimator.setEvaluator(new IntEvaluator());
        valueAnimator.setInterpolator(new LinearInterpolator());

        valueAnimator.addUpdateListener(animator -> {

            float animatedFraction = animator.getAnimatedFraction();
            circle.setRadius(animatedFraction * radius);

            int clr = circle.getStrokeColor();
            clr = ((int)((1 - animatedFraction) * 255) << 24) | (clr & 0xFFFFFF);
            circle.setStrokeColor(clr);
        });

        valueAnimator.start();
    }

    private void makeCircle(final LatLng latLng, int radius, int color) {

        final float stroke = radius * 0.000001f;

        final CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeColor(color)
                .fillColor(Color.TRANSPARENT)
                .strokeWidth(stroke * mMap.getCameraPosition().zoom);

        mCircles.add(mMap.addCircle(circleOptions));
        mCircleStrokes.add(stroke);
    }

    @Override
    public void onCameraMove() {

        float zoom = mMap.getCameraPosition().zoom;
        if (zoom != mZoom) {

            int count = mCircles.size();
            for (int i = 0; i < count; i++) {
                mCircles.get(i).setStrokeWidth((1 << (int) zoom) * mCircleStrokes.get(i));
            }
            mZoom = zoom;
        }
    }
}