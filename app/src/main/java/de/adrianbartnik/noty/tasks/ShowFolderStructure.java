package de.adrianbartnik.noty.tasks;


import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import de.adrianbartnik.noty.fragment.NavigationDrawerFragment;

public class ShowFolderStructure extends AsyncTask<String, Void, DropboxAPI.Entry> {

    private static final String TAG = ShowFolderStructure.class.getName();

    private static DropboxAPI mDBApi;
    private static NavigationDrawerFragment mNavigationDrawerFragment;

    public static void setDropboxAPI(DropboxAPI api){
        mDBApi = api;
    }

    public static void setmNavigationDrawerFragment(NavigationDrawerFragment fragment){
        mNavigationDrawerFragment = fragment;
    }

    @Override
    protected DropboxAPI.Entry doInBackground(String... params) {

        Log.d(TAG, "Getting Dropbox content for path: " + params);

        try {
            return mDBApi.metadata(params[0], 200, null, true, null);
        } catch (DropboxException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(DropboxAPI.Entry result) {

        if (result == null)
            return;

        for (DropboxAPI.Entry e : result.contents) {
            if (!e.isDeleted) {
                Log.i("DropboxEntry", "DropboxEntry - " + String.valueOf(e.isDir) + " Itemname: " + e.fileName() + " ParentPath: " + e.parentPath() + " Path: " + e.path);
            }
        }

        mNavigationDrawerFragment.addEntries(result.contents);
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}