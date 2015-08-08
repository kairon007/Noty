package de.adrianbartnik.noty.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.dropbox.client2.DropboxAPI;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.application.MainActivity;

import de.adrianbartnik.noty.adapter.NoteAdapter;

public class NoteFragment extends Fragment {

    private static final String TAG = NoteFragment.class.getName();

    private static final String entryPosition = "entry_position";
    private static NoteAdapter adapter;
    private DropboxAPI.Entry note;

    public NoteFragment() {
    }

    public static NoteFragment newInstance(int position) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putInt(entryPosition, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        int position = this.getArguments().getInt(entryPosition, -1);
        Log.d(TAG, "Position: " + position);

        note = adapter.getItem(position);
        EditText editText = (EditText) rootView.findViewById(R.id.note_content);
        editText.setText(note.fileName());

        ((MainActivity) getActivity()).onFragmentAttached(note);

        return rootView;
    }

    public static void setAdapter(NoteAdapter a) {
        adapter = a;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
