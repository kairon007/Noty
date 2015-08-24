package de.adrianbartnik.noty.adapter;


import android.content.Context;
import android.graphics.Typeface;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;

import java.util.ArrayList;

import de.adrianbartnik.noty.R;

public class NoteAdapter extends ArrayAdapter<DropboxAPI.Entry> {

    private static final String TAG = NoteAdapter.class.getName();

    private Context context;
    private ArrayList<DropboxAPI.Entry> entries;

    public NoteAdapter(Context context, ArrayList<DropboxAPI.Entry> entries) {
        super(context, R.layout.layout_item_note, entries);
        this.context = context;
        this.entries = entries;
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
    public long getItemId(int position) {
        if (position >= entries.size()) {
//            Log.d(TAG, "Error accesing element at position " + position + " while entries only holds " + entries.size() + " entries");
            return position;
        }
        return entries.get(position).hashCode();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    public ArrayList<DropboxAPI.Entry> getEntries() {
        return entries;
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
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.note_icon);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        if (entry.isDir) {
            viewHolder.icon.setImageResource(R.drawable.folder);
        } else {
            viewHolder.icon.setImageResource(R.drawable.note);
        }

        // TODO Change Font, etc. if new synchronized
        //  viewHolder.title.setTypeface(null, Typeface.ITALIC); Typeface.NORMAL

        viewHolder.title.setText(entry.fileName());

        return convertView;
    }

    public static class ViewHolder {
        TextView title;
        ImageView icon;
    }
}
