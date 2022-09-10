package com.spmaurya.quakereport;

import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public class LauncherFragment extends Fragment {

    private final ImageView[] waves = new ImageView[5];
    private int maxDim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        maxDim = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);

        maxDim -= maxDim >> 2;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        View view = inflater.inflate(R.layout.fragment_launcher, container, false);

        waves[0] = view.findViewById(R.id.wave0);
        waves[1] = view.findViewById(R.id.wave1);
        waves[2] = view.findViewById(R.id.wave2);
        waves[3] = view.findViewById(R.id.wave3);
        waves[4] = view.findViewById(R.id.wave4);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        animateWaves();
    }

    private void animateWaves() {
        for (int i = 0; i < 5; i++) {
            animateWave(waves[i], i * 300);
            waves[i].setVisibility(View.VISIBLE);
        }
    }

    private void animateWave(final ImageView wave, long delay) {

        ValueAnimator animator = new ValueAnimator();

        animator.setDuration(1500);
        animator.setStartDelay(delay);
        animator.setIntValues(0, maxDim);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setEvaluator(new IntEvaluator());
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(valueAnimator -> {

            float animatedFraction = valueAnimator.getAnimatedFraction();
            int dim = (int) (maxDim * animatedFraction);

            ViewGroup.LayoutParams params = wave.getLayoutParams();

            params.width = dim;
            params.height = dim;

            wave.setLayoutParams(params);

            Drawable background = wave.getBackground();

            int color = 0x00FF0000;
            int A = (int)(0xFF * (1 - animatedFraction));
            int G = (int)(0x50 * animatedFraction) + 0x40;
            int B = (int)(0x30 * animatedFraction);

            color |= (A << 24);
            color |= (G << 8);
            color |= (B);

            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            } else if (background instanceof ColorDrawable) {
                ((ColorDrawable) background).setColor(color);
            }
        });

        animator.start();
    }
}