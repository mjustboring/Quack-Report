package com.spmaurya.quakereport;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class EarthquakeViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<Earthquake>> mData = new MutableLiveData<>(new ArrayList<>());
    private Integer mOffset = EarthquakeFetcher.INITIAL_OFFSET;

    public int getOffset() {
        return mOffset;
    }

    public boolean updateOffsetBy(int increase) {
        if (EarthquakeFetcher.isOccupied()) {
            return false;
        }
        mOffset += increase;
        return true;
    }

    public MutableLiveData<ArrayList<Earthquake>> getData() {
        return mData;
    }

    public ArrayList<Earthquake> getAll() {
        return mData.getValue();
    }

    public Earthquake get(int index) {
        return Objects.requireNonNull(mData.getValue()).get(index);
    }

    public void add(Earthquake earthquake) {
        Objects.requireNonNull(mData.getValue()).add(earthquake);
        mData.setValue(mData.getValue());
    }

    public void addAll(ArrayList<Earthquake> earthquakes) {
        Objects.requireNonNull(mData.getValue()).addAll(earthquakes);
        mData.setValue(mData.getValue());
    }

    public void addPost(Earthquake earthquake) {
        Objects.requireNonNull(mData.getValue()).add(earthquake);
        mData.postValue(mData.getValue());
    }

    public void addAllPost(ArrayList<Earthquake> earthquakes) {
        Objects.requireNonNull(mData.getValue()).addAll(earthquakes);
        mData.postValue(mData.getValue());
    }

    public void reset() {
        Objects.requireNonNull(mData.getValue()).clear();
        mOffset = EarthquakeFetcher.INITIAL_OFFSET;
    }
}
