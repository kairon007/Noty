package de.adrianbartnik.noty.tasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.fragment.NoteFragment;

public class DownloadFile extends AsyncTask<Void, Long, Boolean> {

    private static final String TAG = DownloadFile.class.getName();

    private FragmentActivity mFragmentActivity;
    private final ProgressDialog mDialog;
    private DropboxAPI<?> mApi;
    private String mFileContent;
    private Entry mFile;

    private FileOutputStream mFos;

    private boolean mCanceled;
    private Long mFileLen;
    private String mErrorMsg;

    public DownloadFile(FragmentActivity activity, DropboxAPI<?> api, Entry file) {
        // We set the context this way so we don't accidentally leak activities
        mFragmentActivity = activity;

        mApi = api;
        mFile = file;

        mDialog = new ProgressDialog(activity);
        mDialog.setMessage("Downloading File");
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

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            if (mCanceled) {
                return false;
            }

            String path = mFile.path;
            mFileLen = mFile.bytes;

            String cachePath = mFragmentActivity.getCacheDir().getAbsolutePath() + "/" + mFile.fileName();

            try {
                mFos = new FileOutputStream(cachePath);

            } catch (FileNotFoundException e) {
                mErrorMsg = "Couldn't create a local file to store the image";
                return false;
            }

            DropboxAPI.DropboxFileInfo info = mApi.getFile(path, null, mFos, new ProgressListener() {
                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });

            if (mCanceled) {
                return false;
            }

            InputStream reader = new FileInputStream(cachePath);
            StringWriter writer = new StringWriter();
            IOUtils.copy(reader, writer, "UTF-8");
            mFileContent = writer.toString();

            reader.close();
            writer.close();

            Log.d(TAG, "Size: " + info.getFileSize() + " Content: " + mFileContent);

            if (mCanceled) {
                return false;
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
    protected void onProgressUpdate(Long... progress) {
        int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            FragmentManager fragmentManager = mFragmentActivity.getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, NoteFragment.newInstance(mFile.fileName(), mFileContent))
                    .commit();
        } else {
            // Couldn't download it, so show an error
            Toast.makeText(mFragmentActivity, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }
}

