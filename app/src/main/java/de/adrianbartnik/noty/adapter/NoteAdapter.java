package de.adrianbartnik.noty.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import java.util.ArrayList;

import de.adrianbartnik.noty.R;

public class NoteAdapter extends ArrayAdapter<DropboxAPI.Entry> {

    private Context context;
    private ArrayList<DropboxAPI.Entry> entries;

    public NoteAdapter(Context context, ArrayList<DropboxAPI.Entry> entries) {
        super(context, R.layout.layout_item_note, entries);
        this.context = context;
        this.entries = entries;
    }

    public static class ViewHolder{
        TextView title;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public DropboxAPI.Entry getItem(int i) {
        return entries.get(i);
    }

    @Override
    public long getItemId(int i) {
        return entries.get(i).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        DropboxAPI.Entry entry = entries.get(position);

        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.layout_item_note, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.note_title);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // TODO Change Font, etc. if new synchronized

        viewHolder.title.setText(entry.fileName());

        return convertView;
    }
}
