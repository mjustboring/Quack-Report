package com.spmaurya.quakereport;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeAdapter extends ArrayAdapter<Earthquake> {

    public EarthquakeAdapter(Context context, List<Earthquake> arrayList) {
        super(context, 0, arrayList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.earthquake, parent, false);
        }

        Earthquake item = getItem(position);

        double mag = item.getMagnitude();
        TextView magnitude = view.findViewById(R.id.magnitude);
        CardView magnitudeCircle = view.findViewById(R.id.magnitudeCircle);

        magnitude.setText(item.getMagnitudeStr());

        magnitudeCircle.setCardBackgroundColor(item.getColor());
        if (mag > 8) {
            magnitude.setTextColor(0xFFFFFFFF);
        } else {
            magnitude.setTextColor(0xFF000000);
        }

        String [] place = item.getPlace();
        ((TextView) view.findViewById(R.id.place1)).setText(place[0]);
        ((TextView) view.findViewById(R.id.place2)).setText(place[1]);
        String [] dateTime = item.getDateTime();
        ((TextView) view.findViewById(R.id.date)).setText(dateTime[0]);
        ((TextView) view.findViewById(R.id.time)).setText(dateTime[1]);

        return view;
    }

    public ArrayList<Earthquake> getAll() {
        int count = getCount();
        if (count == 0) {
            return null;
        }
        ArrayList<Earthquake> earthquakes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            earthquakes.add(getItem(i));
        }
        return earthquakes;
    }
}
