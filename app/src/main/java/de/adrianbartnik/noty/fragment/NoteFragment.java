package de.adrianbartnik.noty.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.application.MainActivity;

public class NoteFragment extends Fragment {

    private static final String TAG = NoteFragment.class.getName();

    private static final String titleKey = "titleKey";
    private static final String contentKey = "contentKey";

    public NoteFragment() {
    }

    public static NoteFragment newInstance(String title, String content) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putString(titleKey, title);
        args.putString(contentKey, content);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String title = this.getArguments().getString(titleKey);
        String content = this.getArguments().getString(contentKey);
        Log.d(TAG, "Title: " + title + " Content: " + content);

        EditText editText = (EditText) rootView.findViewById(R.id.note_content);
        editText.setText(content);

        ((MainActivity) getActivity()).onFragmentAttached(title);

        return rootView;
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
