package de.adrianbartnik.noty.tasks;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
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
import java.io.PrintWriter;

public class UploadFile extends AsyncTask<String, Long, Boolean> {

    private static final String TAG = UploadFile.class.getName();
    private final ProgressDialog mDialog;
    private FragmentActivity mFragmentActivity;
    private DropboxAPI<?> mDBApi;
    private DropboxAPI.Entry mEntry;

    private FileOutputStream mFos;

    private boolean mCanceled;
    private Long mFileLen;
    private String mErrorMsg;

    public UploadFile(FragmentActivity activity, DropboxAPI<?> api, DropboxAPI.Entry entry) {

        mFragmentActivity = activity;
        mDBApi = api;
        mEntry = entry;

        mDialog = new ProgressDialog(activity);
        mDialog.setMessage("Uploading File");
        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCanceled = true;
                mErrorMsg = "Canceled";

                // This will cancel the getThumbnail operation by closing its stream
                if (mFos != null) {
                    try {
                        mFos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mDialog.show();
    }

    // TODO Create Javadoc
    @Override
    protected Boolean doInBackground(String... params) {
        try {
            if (mCanceled) {
                return false;
            }

            String cachePath = mFragmentActivity.getCacheDir().getAbsolutePath() + "/" + mEntry.fileName();
            File mFile = new File(cachePath);

            FileUtils.writeStringToFile(mFile, params[0], "UTF-8");

            mFileLen = mFile.length();

            Log.d(TAG, "Uploading file: " + mEntry.fileName() + " to " + mEntry.parentPath());

            FileInputStream fis = new FileInputStream(mFile);

            DropboxAPI.Entry newEntry = mDBApi.putFile(mEntry.path, fis, mFile.length(), mEntry.rev, new ProgressListener() {
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
    protected void onProgressUpdate(Long... progress) {
        int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            Toast.makeText(mFragmentActivity, "UploadSuccessful", Toast.LENGTH_SHORT).show();
        } else {
            // Couldn't download it, so show an error
            Toast.makeText(mFragmentActivity, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }
}


