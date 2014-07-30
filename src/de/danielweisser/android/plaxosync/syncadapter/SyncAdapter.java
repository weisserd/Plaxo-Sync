package de.danielweisser.android.plaxosync.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import de.danielweisser.android.plaxosync.Constants;
import de.danielweisser.android.plaxosync.client.Contact;
import de.danielweisser.android.plaxosync.client.PlaxoUtilities;
import de.danielweisser.android.plaxosync.platform.ContactManager;

/**
 * SyncAdapter implementation for synchronizing Plaxo contacts to the platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "PlaxoSyncAdapter";

	private final AccountManager mAccountManager;
	private final Context mContext;

	private Date mLastUpdated;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Log.d(TAG, "Start the sync.");
		List<Contact> users = new ArrayList<Contact>();
		String authtoken = null;
		try {
			// use the account manager to request the credentials
			authtoken = mAccountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			users = PlaxoUtilities.fetchContacts(account.name, authtoken, mLastUpdated, this.getContext());

			if (users == null) {
				syncResult.stats.numIoExceptions++;
				return;
			}
			// update the last synced date.
			mLastUpdated = new Date();
			// update platform contacts.
			Log.d(TAG, "Calling contactManager's sync contacts");
			ContactManager cm = new ContactManager();
			cm.syncContacts(mContext, account.name, users, syncResult);
			// ContactManager.syncContacts(mContext, account.name, users, syncResult, l);
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		}
	}
}
