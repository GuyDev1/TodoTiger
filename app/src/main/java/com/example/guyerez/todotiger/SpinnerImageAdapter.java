package com.example.guyerez.todotiger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.guyerez.todotiger.R;

public class SpinnerImageAdapter extends ArrayAdapter<Integer> {

    private Integer[] images;
    private String[] text;
    private Context context;

    public SpinnerImageAdapter(Context context, Integer[] images,String[] text) {
        super(context, android.R.layout.simple_spinner_item, images);
        this.images = images;
        this.text=text;
        this.context=context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getImageForPosition(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getImageForPosition(position, convertView, parent);
    }

    private View getImageForPosition(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_priority_layout, parent, false);
        TextView textView = (TextView) row.findViewById(R.id.spinnerTextView);
        textView.setText(text[position]);
        ImageView imageView = (ImageView)row.findViewById(R.id.spinnerImages);
        imageView.setImageResource(images[position]);
        return row;
    }
}
