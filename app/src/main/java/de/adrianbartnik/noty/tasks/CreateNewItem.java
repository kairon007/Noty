package de.adrianbartnik.noty.tasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.fragment.NavigationDrawerFragment;
import de.adrianbartnik.noty.fragment.NoteFragment;

public class CreateNewItem extends AsyncTask<Boolean, Long, Boolean> {

    private static final String TAG = CreateNewItem.class.getName();

    private FragmentActivity mFragmentActivity;
    private DropboxAPI<?> mDBApi;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private String mPath;
    private String mName;

    private FileOutputStream mFos;

    private boolean mCanceled;
    private String mErrorMsg;

    public CreateNewItem(FragmentActivity activity, DropboxAPI<?> api, NavigationDrawerFragment fragment, String path, String name) {

        mFragmentActivity = activity;

        mDBApi = api;
        mNavigationDrawerFragment = fragment;
        mPath = path;
        mName = name;
    }

    // TODO Rename Parameter
    @Override
    protected Boolean doInBackground(Boolean... params) {
        try {
            if (mCanceled) {
                return false;
            }

            Log.d(TAG, "Creating Node: " + mName + " to " + mPath + ". Textnote: " + params[0]);

            // Folder. File otherwise
            if(params[0]){

                String cachePath = mFragmentActivity.getCacheDir().getAbsolutePath() + "/" + mName;
                File mFile = new File(cachePath);

                mFile.createNewFile();

                Log.d(TAG, "Uploading file: " + mFile.getName() + " to " + mFile.getPath());

                FileInputStream fis = new FileInputStream(mFile);

                DropboxAPI.Entry newEntry = mDBApi.putFile(mPath + mName, fis, mFile.length(), null, new ProgressListener() {
                    @Override
                    public void onProgress(long l, long l1) {
                        publishProgress(l);
                    }
                });

                if (mCanceled) {
                    return false;
                }

                Log.d(TAG, "Uploading file: Done");

                fis.close();
            } else {
                DropboxAPI.Entry newEntry = mDBApi.createFolder(mPath + mName);
            }

            return true;

        } catch (IOException e){
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
            (new ShowFolderStructure(mDBApi, mNavigationDrawerFragment)).execute(mPath);

        } else {
            // Couldn't download it, so show an error
            Toast.makeText(mFragmentActivity, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }
}