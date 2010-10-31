package de.danielweisser.android.plaxosync.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.nullwire.trace.ExceptionHandler;

import de.danielweisser.android.plaxosync.Constants;
import de.danielweisser.android.plaxosync.R;
import de.danielweisser.android.plaxosync.client.PlaxoUtilities;
import de.danielweisser.android.plaxosync.platform.ContactManager;

/**
 * Activity which displays login screen to the user.
 */
public class PlaxoAuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final int ERROR_DIALOG = 1;
	private static final int PROGRESS_DIALOG = 0;

	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";

	private static final String TAG = "PlaxoAuthActivity";

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = true;

	/**
	 * If set we are just checking that the user knows their credentials, this doesn't cause the user's password to be changed on the device.
	 */
	private Boolean mConfirmCredentials = false;

	/** for posting authentication attempts back to UI thread */
	private final Handler mHandler = new Handler();

	private AccountManager mAccountManager;
	private Thread mAuthThread;

	private String mPassword;
	private EditText mPasswordEdit;
	private String mUsername;
	private EditText mUsernameEdit;
	private String message;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		ExceptionHandler.register(this, "http://www.danielweisser.de/android/server.php");

		mAccountManager = AccountManager.get(this);

		// Get data from Intent
		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		mPassword = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
		mRequestNewAccount = (mUsername == null);
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

		setContentView(R.layout.login_activity);

		// Find controls
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);

		// Set values from the intent
		mUsernameEdit.setText(mUsername);
		mPasswordEdit.setText(mPassword);
	}

	/**
	 * Called when response is received from the server for confirm credentials request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult which
	 * is sent back to the caller.
	 * 
	 * @param the
	 *            confirmCredentials result.
	 */
	protected void finishConfirmCredentials(boolean result) {
		Log.i(TAG, "finishConfirmCredentials()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
		mAccountManager.setPassword(account, mPassword);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Called when response is received from the server for authentication request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult which is
	 * sent back to the caller. Also sets the authToken in AccountManager for this account.
	 * 
	 * @param the
	 *            confirmCredentials result.
	 */
	protected void finishLogin() {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);

		if (mRequestNewAccount) {
			Bundle userData = new Bundle();
			mAccountManager.addAccountExplicitly(account, mPassword, userData);

			// Set contacts sync for this account.
			// ContentResolver.setSyncAutomatically(account,
			// ContactsContract.AUTHORITY, true);
			ContactManager.makeGroupVisible(account.name, getContentResolver());
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		intent.putExtra(AccountManager.KEY_AUTHTOKEN, mPassword);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Handles onClick event on the Next button. Sends username/password to the server for authentication.
	 * 
	 * @param view
	 *            The Next button for which this method is invoked
	 */
	public void doLogin(View view) {
		Log.i(TAG, "handleLogin");
		if (mRequestNewAccount) {
			mUsername = mUsernameEdit.getText().toString();
		}
		mPassword = mPasswordEdit.getText().toString();

		showDialog(PROGRESS_DIALOG);
		// Start authenticating...
		mAuthThread = PlaxoUtilities.attemptAuth(mUsername, mPassword, mHandler, PlaxoAuthenticatorActivity.this);
	}

	/**
	 * Call back for the authentication process. When the authentication attempt is finished this method is called.
	 * 
	 * @param message
	 */
	public void onAuthenticationResult(boolean result, String message) {
		Log.i(TAG, "onAuthenticationResult(" + result + ")");
		dismissDialog(PROGRESS_DIALOG);
		if (!result) {
			this.message = message;
			showDialog(ERROR_DIALOG);
			Log.e(TAG, "onAuthenticationResult: failed to authenticate");
		} else {
			if (!mConfirmCredentials) {
				finishLogin();
			} else {
				finishConfirmCredentials(true);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == PROGRESS_DIALOG) {
			final ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getText(R.string.ui_activity_authenticating));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Log.i(TAG, "dialog cancel has been invoked");
					if (mAuthThread != null) {
						mAuthThread.interrupt();
						finish();
					}
				}
			});
			return dialog;
		} else if (id == ERROR_DIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Connection error").setMessage("Could not connect to Plaxo: " + message).setCancelable(false);
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == ERROR_DIALOG) {
			((AlertDialog) dialog).setMessage("Could not connect to Plaxo: " + message);
		}
	}
}
