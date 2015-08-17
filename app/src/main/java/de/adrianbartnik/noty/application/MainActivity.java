package de.adrianbartnik.noty.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.config.Constants;
import de.adrianbartnik.noty.config.DropboxCredentials;
import de.adrianbartnik.noty.fragment.NavigationDrawerFragment;
import de.adrianbartnik.noty.fragment.NoteFragment;
import de.adrianbartnik.noty.tasks.DownloadFile;
import de.adrianbartnik.noty.tasks.ShowFolderStructure;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = MainActivity.class.getName();

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppKeyPair appKeys = new AppKeyPair(DropboxCredentials.APP_KEY, DropboxCredentials.APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        if (!loadAuth(session))
            mDBApi.getSession().startOAuth2Authentication(MainActivity.this);

        setContentView(R.layout.activity_main);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        mNavigationDrawerFragment.setmDPApi(mDBApi);

        new ShowFolderStructure(mDBApi, mNavigationDrawerFragment).execute("/");
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mDBApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                // Sets the access token on the session
                session.finishAuthentication();
                storeAuth(session);

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                mDBApi.getSession().unlink();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(Entry file) {
        DownloadFile task = new DownloadFile(this, mDBApi, file);
        task.execute();
    }

    public void onFragmentAttached(String title) {
        mTitle = title;
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen if the drawer is not showing.
            // Otherwise, let the drawer decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.text, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button,
        // so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sign_out) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void storeAuth(AndroidAuthSession session) {
        String oauth2AccessToken = session.getOAuth2AccessToken();

        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Constants.ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
        }
    }

    private boolean loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);

        if (secret == null || secret.length() == 0)
            return false;

        session.setOAuth2AccessToken(secret);
        return true;
    }

    private void logOut() {
        mDBApi.getSession().unlink();

        clearKeys();
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
}
