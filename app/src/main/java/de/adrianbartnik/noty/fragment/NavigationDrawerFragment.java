package de.adrianbartnik.noty.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.adapter.NoteAdapter;
import de.adrianbartnik.noty.application.MyApplication;
import de.adrianbartnik.noty.tasks.CreateNewItem;
import de.adrianbartnik.noty.tasks.DeleteNode;
import de.adrianbartnik.noty.tasks.DownloadFile;
import de.adrianbartnik.noty.tasks.MoveNode;
import de.adrianbartnik.noty.tasks.ShowFolderStructure;
import de.adrianbartnik.noty.tasks.UploadFile;
import de.adrianbartnik.noty.util.SerializableEntry;

public class NavigationDrawerFragment extends Fragment {

    private static final String TAG = NavigationDrawerFragment.class.getName();
    /**
     * Used for temporary files on the internal storage
     */
    private static final String RandomValue = "ffca4c4f-1ee8-4965-84c1-f9f761da966p";
    public SerializableEntry mCurrentEntry;
    private NavigationDrawerCallbacks mCallbacks;
    private ActionBarDrawerToggle mDrawerToggle;
    /**
     * Stores the hashes of all files to check if a new version is available
     */
    private HashMap<String, HashMap<SerializableEntry, String>> mVersions;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;
    private FloatingActionButton fab;
    private String mCurrentFolder = "/";
    private boolean mInSubfolder = false;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private ActionMode mActionMode;
    private boolean mNodeDirty = false;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.drawer_contextual, menu);
            return true;
        }

        // Called each time the action mode is shown.
        // Always called after onCreateActionMode, but may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.d(TAG, "onPrepareActionMode");

            MenuItem item = menu.findItem(R.id.action_drawer_cab_edit);

            if (mDrawerListView.getCheckedItemIds().length == 1) {
                item.setVisible(true);
                return true;
            } else {
                item.setVisible(false);
                return true;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            long[] ids = mDrawerListView.getCheckedItemIds();

            switch (item.getItemId()) {
                case R.id.action_drawer_cab_edit:

                    SerializableEntry entry = ((NoteAdapter) mDrawerListView.getAdapter()).getItemById(ids[0]);

                    createMoveDialog(getActivity(), entry);

                    mode.finish();
                    return true;

                case R.id.action_drawer_cab_delete:

                    createConfirmDeleteDialog(getActivity());

                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

            for (int i = 0; i < mDrawerListView.getAdapter().getCount(); i++)
                mDrawerListView.setItemChecked(i, false);

            mDrawerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            mActionMode = null;

        }
    };

    public void invalidateNote() {
        mNodeDirty = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBApi = ((MyApplication) getActivity().getApplication()).getDropboxAPI();

        mDrawerListView = (ListView) getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_navigation_drawer, null, false);
        mDrawerListView.setLongClickable(true);
        mDrawerListView.setItemsCanFocus(false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    selectItem(position);
                } else {

                    int currentCheckItems = mDrawerListView.getCheckedItemIds().length;

                    // Refresh menu to display edit_name action item if necessary. Only check for transitions between one and two selected items
                    if (currentCheckItems == 0)
                        mActionMode.finish();
                    else if (currentCheckItems == 1 || currentCheckItems == 2)
                        mActionMode.invalidate();
                }
            }
        });
        mDrawerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                mDrawerListView.clearChoices();
                mDrawerListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
                    mDrawerListView.setItemChecked(position, true);
                } else {
                    mActionMode.finish();
                }

                return true;
            }
        });
        NoteAdapter noteAdapter = new NoteAdapter(getActivity(), new ArrayList<SerializableEntry>());
        mDrawerListView.setAdapter(noteAdapter);

        readVersions();
        showLocalFiles(mCurrentFolder);
    }

    private void showLocalFiles(String path) {

        Log.d(TAG, "Showing local files for path: " + path);
        Log.d(TAG, "DEBUG: " + mVersions.containsKey("/Folder with Space/Folder with spaces 2"));
        Log.d(TAG, "DEBUG: " + mVersions.containsKey("/Folder with Space"));
        Log.d(TAG, "DEBUG: " + mVersions.containsKey("/"));


        setHomeButton();

        ArrayList<SerializableEntry> directory = new ArrayList<>(mVersions.get(path).keySet());

        int size = directory.size();
        SerializableEntry entry;

        ((NoteAdapter) mDrawerListView.getAdapter()).clear();

        Collections.sort(directory);

        // First show folders and then files.
        // First add all folders, remove them from entries and then add remaining files
        for (int i = 0; i < size; i++) {
            entry = directory.get(i);
            if (entry.isDir) {
                ((NoteAdapter) mDrawerListView.getAdapter()).add(entry);
                directory.remove(i);
                i -= 1;
                size -= 1;
            }
        }

        for (SerializableEntry e : directory)
            ((NoteAdapter) mDrawerListView.getAdapter()).add(e);

        mDrawerListView.invalidateViews();
    }

    private void readVersions() {
        Log.d(TAG, "Loading Hashmaps for Versions");
        try {
            ObjectInputStream inputStream = new ObjectInputStream(getActivity().openFileInput(RandomValue + "Versions"));
            mVersions = (HashMap<String, HashMap<SerializableEntry, String>>) inputStream.readObject();
            inputStream.close();
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        } catch (FileNotFoundException exception) {
            Log.d(TAG, "Not found. Creating new Hashmaps for Versions.");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (mVersions == null){
            mVersions = new HashMap<>();
            mVersions.put(mCurrentFolder, new HashMap<SerializableEntry, String>());
        }
    }

    private void writeVersions() {
        Log.d(TAG, "Writing Hashmaps for files and folders");
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(getActivity().openFileOutput(RandomValue + "Versions", Context.MODE_PRIVATE));
            outputStream.writeObject(mVersions);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException exception) {
            Log.d(TAG, "Hashmaps for Versions not found.");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        fab = (FloatingActionButton) getActivity().findViewById(R.id.floating_action_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                createNodeCreateDialog(getActivity(), true);

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    private void createFolderInStorage(HashMap<SerializableEntry, String> map, SerializableEntry entry){
        map.put(entry, (entry.hash == null ? "" : entry.hash));

        Log.d(TAG, "Directory " + entry.path + " Hash: " + (entry.hash == null ? "" : entry.hash));
    }

    public void saveEntriesToStorage(List<SerializableEntry> entries) {

        // TODO Check for deleted files from entries. Delete in local storage too

        HashMap<SerializableEntry, String> currentDirectory = mVersions.get(mCurrentFolder);

        Log.d(TAG, "mFileVersions: " + mVersions);
        boolean reloadListView = false;

        for (SerializableEntry entry : entries) {

            if (entry.isDir){
                if (currentDirectory.containsKey(entry))
                    Log.d(TAG, "Directory " + entry.path + " exist and is up to date");
                else {
                    createFolderInStorage(currentDirectory, entry);
                    reloadListView = true;
                }
            } else if (currentDirectory.containsKey(entry)) {
                if (currentDirectory.get(entry).equals(entry.rev)) {
                    Log.d(TAG, "File " + entry.path + " exist and is up to date");
                } else {
                    Log.d(TAG, "File " + entry.fileName() + " exist but is NOT up to date. Downloading new Version.");
                    new DownloadFile(getActivity(), mDBApi, entry, currentDirectory).execute();
                }
            } else {
                Log.d(TAG, "File " + entry.fileName() + " does not exist. Creating now.");

                new DownloadFile(getActivity(), mDBApi, entry, currentDirectory).execute();
                reloadListView = true;
            }
        }

        if(reloadListView){
            Log.d(TAG, "New files available. Reload ListView");
            addEntriesListView(entries);
        }
    }

    private void setHomeButton(){
        if (!mInSubfolder) {
            getActionBar().setHomeButtonEnabled(false);
            getActivity().supportInvalidateOptionsMenu();
        } else
            getActionBar().setHomeButtonEnabled(true);
    }

    private void addEntriesListView(List<SerializableEntry> entries) {

        ((ArrayAdapter) mDrawerListView.getAdapter()).clear();

        int size = entries.size();
        SerializableEntry entry;

        // First show folders and then files.
        // First add all folders, remove them from entries and then add remaining files
        for (int i = 0; i < size; i++) {
            entry = entries.get(i);
            if (entry.isDir) {
                ((NoteAdapter) mDrawerListView.getAdapter()).add(entry);
                entries.remove(i);
                i -= 1;
                size -= 1;
            }
        }

        for (SerializableEntry e : entries)
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
                fab.setVisibility(View.GONE);

                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setDisplayHomeAsUpEnabled(false);
                fab.setVisibility(View.VISIBLE);

                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
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

    public void openNavigationDrawer() {
        if (!isDrawerOpen())
            mDrawerLayout.openDrawer(mFragmentContainerView);
    }

    private void selectItem(int position) {

        if (position < 0)
            return;

        if (mCallbacks != null) {

            SerializableEntry entry = ((NoteAdapter) mDrawerListView.getAdapter()).getItem(position);

            Log.d(TAG, "Subfolder: " + mInSubfolder + " Path: " + entry.path);

            if (entry.isDir) {
                mCurrentFolder = entry.path;
                mInSubfolder = true;
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                if(mVersions.get(mCurrentFolder) == null)
                    mVersions.put(mCurrentFolder, new HashMap<SerializableEntry, String>());
                showLocalFiles(mCurrentFolder);
                new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder);

            } else {

                // Current node is dirty?

                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                mDrawerLayout.closeDrawer(mFragmentContainerView);

                // Return if selected node is the current open one
                if (mCurrentEntry != entry) {

                    uploadCurrentFile();

                    mNodeDirty = false;

                    mCurrentEntry = entry;

                    mCallbacks.onNavigationDrawerItemSelected(entry);
                }
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
            if (mInSubfolder)
                getActionBar().setDisplayHomeAsUpEnabled(true);
            else
                getActionBar().setDisplayHomeAsUpEnabled(false);
            inflater.inflate(R.menu.drawer, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void uploadCurrentFile() {

        // Checks if node has been modified. If not, uploading new version is unnecessary
        if (!mNodeDirty)
            return;

        Log.d(TAG, "Note was modified. Upload new version");

        EditText editText = ((EditText) getActivity().findViewById(R.id.note_content));

        String content = "";

        if (editText != null)
            content = editText.getText().toString();

        new UploadFile(getActivity(), mDBApi, mCurrentEntry).execute(content);
    }

    public String parentPath(String path) {
        int ind = path.lastIndexOf("/");

        if (path.equals("/")) {
            return "/";
        } else if (ind == 0){
            return "/";
        } else {
            return path.substring(0, ind);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isDrawerOpen())
                    mDrawerLayout.openDrawer(mFragmentContainerView);
                else {
                    if (mCurrentFolder.lastIndexOf('/') == 0) {
                        mCurrentFolder = "/";
                        mInSubfolder = false;
                    } else
                        mCurrentFolder = parentPath(mCurrentFolder);

                    showLocalFiles(mCurrentFolder);
                    new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder);
                }
                return true;

            case R.id.action_sync:
                new ShowFolderStructure(mDBApi, this).execute(mCurrentFolder);

                uploadCurrentFile();

                return true;

            case R.id.action_sign_out:
                mCallbacks.onClickedSignOut();
                return true;

            case R.id.action_create_folder:
                createNodeCreateDialog(getActivity(), false);
                return true;
        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void createNodeCreateDialog(final Activity context, final boolean note) {

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

                        (new CreateNewItem(getActivity(), mDBApi, NavigationDrawerFragment.this, mCurrentFolder, noteName)).execute(note);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setCancelable(true);
                AlertDialog dialog = builder.create();

                dialog.show();
            }
        });
    }

    private void createMoveDialog(final Activity context, final SerializableEntry note) {

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);
                } else {
                    builder = new AlertDialog.Builder(context);
                }

                LayoutInflater inflater = context.getLayoutInflater();

                View dialogView = inflater.inflate(R.layout.dialog_create_item, null);
                builder.setView(dialogView);
                final EditText noteNameEdit = (EditText) dialogView.findViewById(R.id.note_name);

                noteNameEdit.setHint(note.fileName());

                builder.setTitle("Enter " + (note.isDir ? "note" : "folder") + " title");

                builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String noteName = noteNameEdit.getText().toString();

                        if (noteName.equals(""))
                            noteName = noteNameEdit.getHint().toString();

                        (new MoveNode(getActivity(), mDBApi, NavigationDrawerFragment.this, note, noteName)).execute();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setCancelable(true);
                AlertDialog dialog = builder.create();

                dialog.show();
            }
        });
    }

    private void createConfirmDeleteDialog(final Activity context) {

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);

                builder.setTitle("Are you sure?");

                final long[] ids = mDrawerListView.getCheckedItemIds();

                String message = "Delete ";
                if (ids.length == 1)
                    message += ((NoteAdapter) mDrawerListView.getAdapter()).getItemById(ids[0]).fileName();
                else
                    message += ids.length + " files";

                builder.setMessage(message);

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        ArrayList<SerializableEntry> entries = new ArrayList<>(ids.length);
                        for (long entryID : ids)
                            entries.add(((NoteAdapter) mDrawerListView.getAdapter()).getItemById(entryID));

                        (new DeleteNode(getActivity(), mDBApi, NavigationDrawerFragment.this, entries)).execute();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setCancelable(true);
                AlertDialog dialog = builder.create();

                dialog.show();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        writeVersions();
        super.onDestroy();
    }

    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(SerializableEntry file);

        void onClickedSignOut();
    }
}