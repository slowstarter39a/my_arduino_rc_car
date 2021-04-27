package com.singpung.my_arduino_rc_car;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.app.Activity;

import java.util.ArrayList;
import android.view.LayoutInflater;

public class BTDeviceItemAdapter extends BaseAdapter {
    private ArrayList<String> items;
    private Activity activity;

    public BTDeviceItemAdapter(Activity activity) {
        this.activity = activity;
        items = new ArrayList<String>();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public void addItem(String item) {
        items.add(item);
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clearAll() {
        items.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListViewHolder holder = null;
        TextView name;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.dialog_textview_layout, parent, false);
            name = (TextView) convertView.findViewById(R.id.textView);

            holder = new ListViewHolder();
            holder.name = name;

            convertView.setTag(holder);
            name.setVisibility(View.VISIBLE);
        }
        else {
            holder = (ListViewHolder) convertView.getTag();
            name = holder.name;
        }
        name.setText(items.get(position));

        return convertView;

    }

    private class ListViewHolder {
        TextView name;
    }
}
