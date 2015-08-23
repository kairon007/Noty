package de.adrianbartnik.noty.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.adapter.NoteAdapter;
import de.adrianbartnik.noty.tasks.ShowFolderStructure;
import de.adrianbartnik.noty.tasks.UploadFile;

public class NavigationDrawerFragment extends Fragment {

    private static final String TAG = NavigationDrawerFragment.class.getName();

    private NavigationDrawerCallbacks mCallbacks;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private String mCurrentFolder = "/";
    private Entry mCurrentEntry;
    private boolean mInSubfolder = false;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrawerListView = (ListView) getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_navigation_drawer, null, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        NoteAdapter noteAdapter = new NoteAdapter(getActivity(), new ArrayList<Entry>());
        mDrawerListView.setAdapter(noteAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void addEntries(List<Entry> entries) {

        if(!mInSubfolder){
            getActionBar().setHomeButtonEnabled(false);
            getActivity().supportInvalidateOptionsMenu();
        } else
            getActionBar().setHomeButtonEnabled(true);

        ((ArrayAdapter) mDrawerListView.getAdapter()).clear();

        int size = entries.size();
        Entry entry;

        // First show folders and then files.
        // First add all folders, remove them from entries and then add remaining files
        for(int i = 0; i < size; i++){
            entry = entries.get(i);
            if(entry.isDir){
                ((NoteAdapter) mDrawerListView.getAdapter()).add(entry);
                entries.remove(i);
                i -= 1;
                size -= 1;
            }
        }

        for (Entry e : entries)
            ((NoteAdapter) mDrawerListView.getAdapter()).add(e);

        mDrawerListView.invalidateViews();
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions between the navigation drawer and the action bar app icon.

        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActionBar().setDisplayHomeAsUpEnabled(true);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setDisplayHomeAsUpEnabled(false);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
        public void onDrawerSlide(View drawerView, float slideOffset){
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };

        mDrawerLayout.openDrawer(mFragmentContainerView);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {

        if (position < 0)
            return;

        if (mCallbacks != null) {

            Entry entry = ((NoteAdapter) mDrawerListView.getAdapter()).getItem(position);

            Log.d(TAG, "Subfolder: " + mInSubfolder  + " Entry: " + entry.fileName() + "Path: " + entry.path);

            if (entry.isDir) {
                mCurrentFolder = entry.parentPath();
                mInSubfolder = true;
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder + entry.fileName() + "/");

            } else {
                mCurrentEntry = entry;
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                mDrawerLayout.closeDrawer(mFragmentContainerView);
                mCallbacks.onNavigationDrawerItemSelected(entry);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar.
        // See also showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            if(mInSubfolder)
                getActionBar().setDisplayHomeAsUpEnabled(true);
            else
                getActionBar().setDisplayHomeAsUpEnabled(false);
            inflater.inflate(R.menu.drawer, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, "OptionMenu klicked. Item: " + item.getItemId() + " SignoutID: " + R.id.action_sign_out);

        switch (item.getItemId()){
            case android.R.id.home:
                if (!isDrawerOpen())
                    mDrawerLayout.openDrawer(mFragmentContainerView);
                else {
                    if(mCurrentFolder.lastIndexOf('/') == 0) {
                        mCurrentFolder = "/";
                        mInSubfolder = false;
                    }
                    else
                        mCurrentFolder = mCurrentFolder.substring(0, mCurrentFolder.lastIndexOf('/'));
                    new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder);
                }
                Toast.makeText(getActivity(), "Clicked up.", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_sync:
                new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder); // TODO Add check if file was modified

                // TODO Check if there is an EditText meaning one file has already been openened
                String content = ((EditText) getActivity().findViewById(R.id.note_content)).getText().toString();

                new UploadFile(getActivity(), mDBApi, mCurrentEntry).execute(content);
                Toast.makeText(getActivity(), "Klicked Sync", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_sign_out:
                mCallbacks.onClickedSignOut();
                return true;

            case R.id.action_create_folder:
                createNodeCreateDialog(getActivity(), false);
                return true;

            case R.id.action_create_note:
                createNodeCreateDialog(getActivity(), true);
                return true;
        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void createNodeCreateDialog(final Activity context, final boolean note){

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);
                LayoutInflater inflater = context.getLayoutInflater();

                View dialogView = inflater.inflate(R.layout.dialog_create_item, null);
                builder.setView(dialogView);
                final EditText noteNameEdit = (EditText) dialogView.findViewById(R.id.note_name);

                SimpleDateFormat format = new SimpleDateFormat();
                format.applyPattern("EEE, d MMM yyyy");
                noteNameEdit.setHint(format.format(new Date()) + ".txt");

                builder.setTitle("Enter " + (note ? "note" : "folder") + " title");

                builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String noteName = noteNameEdit.getText().toString();

                        if (noteName.equals(""))
                            noteName = noteNameEdit.getHint().toString();

                        Log.d(TAG, "Dialog: " + noteName);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setCancelable(true);
                AlertDialog dialog = builder.create();

//                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.accent));
//                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.accent));

                dialog.show();
            }
        });
    }


    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public void setmDBApi(DropboxAPI<AndroidAuthSession> mDBApi) {
        this.mDBApi = mDBApi;
    }

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(Entry file);
        void onClickedSignOut();
    }
}
