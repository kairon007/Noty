package de.adrianbartnik.noty.tasks;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class DownloadFile extends AsyncTask<Void, Long, Boolean> {

    private static final String TAG = DownloadFile.class.getName();

    private FragmentActivity mFragmentActivity;
    private DropboxAPI<?> mApi;
    private Entry mEntry;
    private HashMap<String, String> mFileVersions;

    private FileOutputStream mFos;
    private String mErrorMsg;

    public DownloadFile(FragmentActivity activity, DropboxAPI<?> api, Entry entry, HashMap<String, String> fileVersions) {
        // We set the context this way so we don't accidentally leak activities
        mFragmentActivity = activity;

        mApi = api;
        mEntry = entry;
        mFileVersions = fileVersions;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {

            Log.d(TAG, "Path: " + mFragmentActivity.getFilesDir() + mEntry.path + " for node " + mEntry.parentPath());
            boolean success = new File(mFragmentActivity.getFilesDir() + mEntry.parentPath() + "/").mkdirs();

            mFos = new FileOutputStream(new File(mFragmentActivity.getFilesDir() + mEntry.path));

            DropboxAPI.DropboxFileInfo info = mApi.getFile(mEntry.path, mEntry.rev, mFos, new ProgressListener() {
                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });

            mFileVersions.put(mEntry.path, (mEntry.rev == null ? "" : mEntry.rev));

            Log.d(TAG, "File " + mEntry.path + " Size: " + info.getFileSize() + " Revision: " + (mEntry.rev == null ? "" : mEntry.rev));

            mFos.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
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
            Log.d(TAG, "Creating file " + mEntry.fileName() + " successful");
        } else {
            // Couldn't download it, so show an error
            Toast.makeText(mFragmentActivity, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }
}


