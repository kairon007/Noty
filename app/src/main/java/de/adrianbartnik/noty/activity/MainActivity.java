package de.adrianbartnik.noty.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.application.MyApplication;
import de.adrianbartnik.noty.config.Constants;
import de.adrianbartnik.noty.fragment.NavigationDrawerFragment;
import de.adrianbartnik.noty.fragment.NoteFragment;
import de.adrianbartnik.noty.tasks.DownloadFile;
import de.adrianbartnik.noty.tasks.ShowFolderStructure;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, NoteFragment.NoteFragmentCallbacks {

    private static final String TAG = MainActivity.class.getName();

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBApi = ((MyApplication) getApplication()).getAndroidAPI();

        setContentView(R.layout.activity_main);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        mNavigationDrawerFragment.setmDBApi(mDBApi);

        new ShowFolderStructure(mDBApi, mNavigationDrawerFragment).execute("/");
    }

    @Override
    protected void onStop() {
        super.onStop();

        mNavigationDrawerFragment.uploadCurrentFile();

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
    public void onNavigationDrawerItemSelected(Entry entry) {
        DownloadFile task = new DownloadFile(this, mDBApi, entry, mNavigationDrawerFragment);
        task.execute();
    }

    @Override
    public void onClickedSignOut(){
        logOut();
        Intent login = new Intent(this, DropboxLogin.class);
        startActivity(login);
    }

    public void onFragmentAttached(String title) {
        mTitle = title;
        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen if the drawer is not showing.
            // Otherwise, let the drawer decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.note, menu);
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

        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        mDBApi.getSession().unlink();

        clearKeys();

        Intent intent = new Intent(this, DropboxLogin.class);
        startService(intent);
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.apply();
    }

    @Override
    public void onBackPressed() {
        if(mNavigationDrawerFragment.isDrawerOpen())
            finish();
        else
            mNavigationDrawerFragment.openNavigationDrawer();
    }

    @Override
    public void noteModified() {
        mNavigationDrawerFragment.invalidateNote();
    }
}
