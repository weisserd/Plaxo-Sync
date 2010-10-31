package de.danielweisser.android.plaxosync.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class PlaxoAuthenticationService extends Service {
    private PlaxoAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new PlaxoAuthenticator(this);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
