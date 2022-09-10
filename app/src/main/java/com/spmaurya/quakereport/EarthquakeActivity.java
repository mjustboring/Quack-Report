package com.spmaurya.quakereport;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;

public class EarthquakeActivity extends AppCompatActivity
        implements ListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    @SuppressWarnings("unused")
    private static final String TAG = "___" + EarthquakeActivity.class.getSimpleName();
    private ListView mListView = null;
    private EarthquakeAdapter mAdapter = null;
    private TextView mEmptyView = null;
    private TextView mListViewVisibleItemsRange = null;
    private EarthquakeViewModel mModel = null;

    private ProgressBar mProgressBar = null;
    private ProgressBar mProgressBarBottom = null;
    private View mMainLayout = null;
    private SwipeRefreshLayout mSwipeRefreshLayout = null;
    private boolean mBottomReached = true;

    private String [] mFilters;
    private CountryFilterDialog mCountryFilterDialog;
    private DateFilterDialog mDateFilterDialog;
    private LocationFilterDialog mLocationFilterDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.earthquake_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.launcher, new LauncherFragment())
                .addToBackStack("EarthquakeActivity").commit();

        mMainLayout = findViewById(R.id.main_layout);
        setViewAndChildrenEnabled(mMainLayout, false);

        mFilters = getResources().getStringArray(R.array.settings_filter_by_entry_values);

        new Handler().postDelayed(() -> {
            getSupportFragmentManager().popBackStackImmediate();
            Objects.requireNonNull(getSupportActionBar()).show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setViewAndChildrenEnabled(mMainLayout, true);
        }, 3000);

        mModel = new ViewModelProvider(this).get(EarthquakeViewModel.class);
        mModel.getData().observe(this, earthquakes -> {
            for (int i = mAdapter.getCount(); i < earthquakes.size(); i++) {
                mAdapter.add(earthquakes.get(i));
            }
            mAdapter.notifyDataSetChanged();
        });

        mEmptyView = findViewById(R.id.empty_view);
        mListView = findViewById(R.id.listView);
        mListView.setEmptyView(mEmptyView);

        mListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String url = ((Earthquake) adapterView.getItemAtPosition(i)).getUrl();
            Intent intent = new Intent(EarthquakeActivity.this, BrowserActivity.class);
            intent.putExtra("urlString", url);
            startActivity(intent);
            return true;
        });

        mListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Earthquake earthquake = ((Earthquake) adapterView.getItemAtPosition(i));
            Intent intent = new Intent(EarthquakeActivity.this, MapsActivity.class);
            intent.putExtra("quakeBundle", Earthquake.makeBundle(earthquake));
            startActivity(intent);
        });

        findViewById(R.id.map).setOnClickListener(view -> {
            EarthquakeAdapter adapter = (EarthquakeAdapter) mListView.getAdapter();
            if (!adapter.isEmpty()) {
                Intent intent = new Intent(EarthquakeActivity.this, MapsActivity.class);
                intent.putExtra("quakeBundle", Earthquake.makeBundle(adapter.getAll()));
                startActivity(intent);
            }
        });

        mAdapter = new EarthquakeAdapter(this, mModel.getAll());
        mListView.setAdapter(mAdapter);

        mListViewVisibleItemsRange = findViewById(R.id.list_view_visible_items_range);

        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBarBottom = findViewById(R.id.progress_bar_bottom);
        mSwipeRefreshLayout = findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mCountryFilterDialog = new CountryFilterDialog(this);
        mDateFilterDialog = new DateFilterDialog(this);
        mLocationFilterDialog = new LocationFilterDialog(this);

        if (!isConnected()) {
            findViewById(R.id.progress_bar).setVisibility(View.GONE);
            mEmptyView.setText(EarthquakeFetcher.REQUEST_STATUS_NO_NETWORK);
            if (!mAdapter.isEmpty()) {
                Toast.makeText(this,
                        EarthquakeFetcher.REQUEST_STATUS_NO_NETWORK, Toast.LENGTH_SHORT).show();
            }
        } else if (mModel.getAll().isEmpty()) {
            fetchEarthquakes();
        } else {
            mProgressBar.setVisibility(View.GONE);
        }

        mListView.setOnScrollListener(this);
    }

    private void fetchEarthquakes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ArrayList<Earthquake> earthquakes;
            String filterBy = Settings.getFilterBy(this);
            if (filterBy.equals(mFilters[0])) {
                earthquakes = EarthquakeFetcher.
                        getEarthquakes(EarthquakeActivity.this,
                                mDateFilterDialog.getStartDate(),
                                mDateFilterDialog.getEndDate(), mModel.getOffset());
            } else if (filterBy.equals(mFilters[1])) {
                earthquakes = EarthquakeFetcher.
                        getEarthquakes(EarthquakeActivity.this,
                                mCountryFilterDialog.getCountryName(),
                                mCountryFilterDialog.getStartDate(),
                                mCountryFilterDialog.getEndDate(), mModel.getOffset());
            } else {
                earthquakes = EarthquakeFetcher.
                        getEarthquakes(EarthquakeActivity.this, mLocationFilterDialog.getLocation(),
                                mLocationFilterDialog.getRadius(), mLocationFilterDialog.getStartDate(),
                                mLocationFilterDialog.getEndDate(), mModel.getOffset());
            }
            mModel.addAllPost(earthquakes);
            runOnUiThread(() -> {
                mProgressBar.setVisibility(View.GONE);
                mProgressBarBottom.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
                mListView.setOnScrollListener(this);
                String status = EarthquakeFetcher.getRequestStatus();
                if (!status.isEmpty()) {
                    if (mAdapter.isEmpty()) {
                        mEmptyView.setText(status);
                    } else if (mAdapter.getCount() >
                            (1 + mListView.getLastVisiblePosition() - mListView.getFirstVisiblePosition())) {
                        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchAction = menu.findItem(R.id.action_search);
        MenuItem filterAction = menu.findItem(R.id.action_filter);
        searchAction.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                filterAction.setEnabled(false);
                invalidateOptionsMenu();
                mListView.setOnScrollListener(null);
                mSwipeRefreshLayout.setEnabled(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                filterAction.setEnabled(true);
                invalidateOptionsMenu();
                mListView.setAdapter(mAdapter);
                mListView.setOnScrollListener(EarthquakeActivity.this);
                mSwipeRefreshLayout.setEnabled(true);
                return true;
            }
        });

        SearchView searchView = (SearchView) searchAction.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterAdapter(newText);
                return false;
            }
        });
        searchView.setQueryHint(getResources().getString(R.string.search_bar_hint));

        return true;
    }

    public void filterAdapter(String newText) {
        newText = newText.toLowerCase(Locale.getDefault()).trim();
        if (newText.isEmpty()) {
            if (mListView.getAdapter() != mAdapter) {
                mListView.setAdapter(mAdapter);
            }
        } else {
            EarthquakeAdapter adapter = new EarthquakeAdapter(this, new ArrayList<>());
            for (Earthquake earthquake : mModel.getAll()) {
                if (earthquake.getPlace()[1].toLowerCase(Locale.getDefault()).contains(newText)) {
                    adapter.add(earthquake);
                }
            }
            mListView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_filter) {
            final String filterBy = Settings.getFilterBy(this);
            if (filterBy.equals(mFilters[0])) {
                mDateFilterDialog.show();
            } else if (filterBy.equals(mFilters[1])) {
                mCountryFilterDialog.show();
            } else {
                mLocationFilterDialog.showDialog();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {

        if (scrollState == SCROLL_STATE_IDLE) {
            mListViewVisibleItemsRange.setVisibility(View.GONE);
        } else {
            mListViewVisibleItemsRange.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        visibleItemCount += firstVisibleItem++;
        String range = firstVisibleItem + " - " + visibleItemCount;
        mListViewVisibleItemsRange.setText(range);

        if (!mBottomReached && !Settings.isLimitByItemsEnabled(EarthquakeActivity.this) &&
                !mListView.canScrollVertically(1) && visibleItemCount == mAdapter.getCount() &&
                mModel.updateOffsetBy(Settings.getLimit(EarthquakeActivity.this)) &&
                !mAdapter.isEmpty()
        ) {
            if (mAdapter.getCount() >
                    (1 + mListView.getLastVisiblePosition() - mListView.getFirstVisiblePosition())) {
                mProgressBarBottom.setVisibility(View.VISIBLE);
            }
            fetchEarthquakes();
            mBottomReached = true;
        } else if (mListView.canScrollVertically(1)) {
            mBottomReached = false;
        }

        if (!mListView.canScrollVertically(1) || !mListView.canScrollVertically(-1)) {
            mListViewVisibleItemsRange.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRefresh() {
        mListView.setOnScrollListener(null);
        mSwipeRefreshLayout.setRefreshing(true);
        mEmptyView.setText("");
        mListViewVisibleItemsRange.setVisibility(View.GONE);
        mProgressBarBottom.setVisibility(View.GONE);
        mModel.reset();
        mAdapter.clear();
        fetchEarthquakes();
    }
    public void refresh() {
        mListView.setOnScrollListener(null);
        mEmptyView.setText("");
        mListViewVisibleItemsRange.setVisibility(View.GONE);
        mProgressBarBottom.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mModel.reset();
        mAdapter.clear();
        fetchEarthquakes();
    }
}