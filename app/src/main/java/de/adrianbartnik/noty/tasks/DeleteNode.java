package de.adrianbartnik.noty.tasks;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.fragment.NavigationDrawerFragment;

public class DeleteNode extends AsyncTask<Void, Long, Boolean> {

    private static final String TAG = DeleteNode.class.getName();

    private FragmentActivity mFragmentActivity;
    private DropboxAPI<?> mDBApi;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private DropboxAPI.Entry mEntry;


    private String mErrorMsg;

    public DeleteNode(FragmentActivity activity, DropboxAPI<?> api, NavigationDrawerFragment fragment, DropboxAPI.Entry entry) {

        // TODO Check if deleted file was a folder

        mFragmentActivity = activity;

        mDBApi = api;
        mNavigationDrawerFragment = fragment;
        mEntry = entry;
    }

    // TODO Rename Parameter
    @Override
    protected Boolean doInBackground(Void... params) {
        try {

            Log.d(TAG, "Deleting Node: " + mEntry.fileName() + " from " + mEntry.parentPath() + ". Textnote: " + mEntry.isDir);

            mDBApi.delete(mEntry.path);

            Log.d(TAG, "Deleting file: Done");

            return true;

        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
            e.printStackTrace();
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            e.printStackTrace();
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            e.printStackTrace();

            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {

        if (result) {
            (new ShowFolderStructure(mDBApi, mNavigationDrawerFragment)).execute(mEntry.parentPath());

            final View coordinatorLayoutView = mNavigationDrawerFragment.getActivity().findViewById(R.id.coordinator);

            Snackbar
                    .make(coordinatorLayoutView, "Deleted " + mEntry.fileName(), Snackbar.LENGTH_INDEFINITE)
                    .setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(mNavigationDrawerFragment.getActivity(), "Undo", Toast.LENGTH_LONG).show();
                        }
                    }).show();

        } else {
            // Couldn't download it, so show an error
            Toast.makeText(mFragmentActivity, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }
}
