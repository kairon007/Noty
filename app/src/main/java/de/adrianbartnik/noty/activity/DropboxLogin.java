package de.adrianbartnik.noty.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import de.adrianbartnik.noty.R;
import de.adrianbartnik.noty.application.MyApplication;
import de.adrianbartnik.noty.config.Constants;
import de.adrianbartnik.noty.config.DropboxCredentials;

public class DropboxLogin extends Activity {

    private static final String TAG = DropboxLogin.class.getName();

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private AndroidAuthSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_login);

        MyApplication myApp = (MyApplication) getApplication();

        AppKeyPair appKeys = new AppKeyPair(DropboxCredentials.APP_KEY, DropboxCredentials.APP_SECRET);
        session = new AndroidAuthSession(appKeys);
        myApp.setDropboxAPI(new DropboxAPI<>(session));
        mDBApi = myApp.getDropboxAPI();

        if (loadAuth(session)){
            Log.d(TAG, "User already logged in. Starting MainActivity");
            startMain();
        }

        Button loginButton = (Button) findViewById(R.id.button_dropbox_login);
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Starting OAuth2 Authentication");
                mDBApi.getSession().startOAuth2Authentication(DropboxLogin.this);
            }
        });
    }

    private boolean loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);

        if (secret == null || secret.length() == 0)
            return false;

        session.setOAuth2AccessToken(secret);
        return true;
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

//                String accessToken = mDBApi.getSession().getOAuth2AccessToken();

                startMain();

            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    private void startMain() {
        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(main);
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
}

