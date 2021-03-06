package de.danielweisser.android.plaxosync.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.util.Log;
import de.danielweisser.android.plaxosync.Constants;
import de.danielweisser.android.plaxosync.client.Address;
import de.danielweisser.android.plaxosync.client.Contact;

/**
 * Class for managing contacts sync related operations
 */
public class ContactManager {
	private static final String TAG = "ContactManager";

	/**
	 * Synchronize raw contacts
	 * 
	 * @param context
	 *            The context
	 * @param accountName
	 *            The account name
	 * @param contacts
	 *            The list of retrieved Plaxo contacts
	 */
	public synchronized void syncContacts(Context context, String accountName, List<Contact> contacts, SyncResult syncResult) {
		final ContentResolver resolver = context.getContentResolver();

		// Get all phone contacts for the Plaxo account
		HashMap<String, Long> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);

		// Update and create new contacts
		for (final Contact contact : contacts) {
			if (contactsOnPhone.containsKey(contact.getID())) {
				Long contactId = contactsOnPhone.get(contact.getID());
				Log.d(TAG, "Update contact: " + contact.getID());
				updateContact(resolver, contactId, contact);
				syncResult.stats.numUpdates++;
				contactsOnPhone.remove(contact.getID());
			} else {
				Log.d(TAG, "Add contact: " + contact.getFirstName() + " " + contact.getLastName());
				addContact(resolver, accountName, contact);
				syncResult.stats.numInserts++;
			}
		}

		// Delete contacts
		for (Entry<String, Long> contact : contactsOnPhone.entrySet()) {
			Log.d(TAG, "Delete contact: " + contact.getKey() + "(" + contact.getValue() + ")");
			deleteContact(resolver, contact.getValue());
			syncResult.stats.numDeletes++;
		}
	}

	private void updateContact(ContentResolver resolver, long rawContactId, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Contact existingContact = new Contact();

		final String selection = Data.RAW_CONTACT_ID + "=?";
		final String[] projection = new String[] { Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA7, Data.DATA8, Data.DATA9,
				Data.DATA10, Data.DATA15 };

		try {
			final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { rawContactId + "" }, null);

			if (c != null) {
				while (c.moveToNext()) {
					String mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
					if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
						existingContact.setFirstName(c.getString(c.getColumnIndex(Data.DATA2)));
						existingContact.setLastName(c.getString(c.getColumnIndex(Data.DATA3)));
						existingContact.setNamePrefix(c.getString(c.getColumnIndex(Data.DATA4)));
					} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
						int type = c.getInt(c.getColumnIndex(Data.DATA2));
						if (type == Email.TYPE_HOME) {
							existingContact.setHomeEmail(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Email.TYPE_WORK) {
							existingContact.setWorkEmail(c.getString(c.getColumnIndex(Data.DATA1)));
						}
					} else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
						int type = c.getInt(c.getColumnIndex(Data.DATA2));
						if (type == Phone.TYPE_WORK_MOBILE) {
							existingContact.setCellWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Phone.TYPE_WORK) {
							existingContact.setWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Phone.TYPE_HOME) {
							existingContact.setHomePhone(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Phone.TYPE_MOBILE) {
							existingContact.setCellHomePhone(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Phone.TYPE_FAX_HOME) {
							existingContact.setHomeFax(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Phone.TYPE_FAX_WORK) {
							existingContact.setWorkFax(c.getString(c.getColumnIndex(Data.DATA1)));
						}
					} else if (mimetype.equals(Website.CONTENT_ITEM_TYPE)) {
						int type = c.getInt(c.getColumnIndex(Data.DATA2));
						if (type == Website.TYPE_HOME) {
							existingContact.setHomeURL(c.getString(c.getColumnIndex(Data.DATA1)));
						} else if (type == Website.TYPE_WORK) {
							existingContact.setWorkURL(c.getString(c.getColumnIndex(Data.DATA1)));
						}
					} else if (mimetype.equals(Organization.CONTENT_ITEM_TYPE)) {
						int type = c.getInt(c.getColumnIndex(Data.DATA2));
						if (type == Organization.TYPE_WORK) {
							existingContact.setCompany(c.getString(c.getColumnIndex(Data.DATA1)));
							existingContact.setTitle(c.getString(c.getColumnIndex(Data.DATA4)));
						}
					} else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE)) {
						existingContact.setImage(c.getBlob(c.getColumnIndex(Photo.PHOTO)));
					} else if (mimetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
						int type = c.getInt(c.getColumnIndex(Data.DATA2));
						Address address = new Address();
						address.setStreet(c.getString(c.getColumnIndex(Data.DATA4)));
						address.setCity(c.getString(c.getColumnIndex(Data.DATA7)));
						address.setCountry(c.getString(c.getColumnIndex(Data.DATA10)));
						address.setZip(c.getString(c.getColumnIndex(Data.DATA9)));
						address.setState(c.getString(c.getColumnIndex(Data.DATA8)));
						if (type == StructuredPostal.TYPE_WORK) {
							existingContact.setWorkAddress(address);
						} else if (type == StructuredPostal.TYPE_HOME) {
							existingContact.setHomeAddress(address);
						}
					}
				}
			}

			prepareFields(rawContactId, contact, existingContact, ops, false);

			if (ops.size() > 0) {
				resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			}
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private void deleteContact(ContentResolver resolver, Long rawContactId) {
		try {
			resolver.delete(RawContacts.CONTENT_URI, RawContacts._ID + "=?", new String[] { "" + rawContactId });
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Retrieves all contacts that are on the phone for this account.
	 * 
	 * @return
	 */
	private static HashMap<String, Long> getAllContactsOnPhone(ContentResolver resolver, String accountName) {
		final String[] projection = new String[] { RawContacts._ID, RawContacts.SYNC1, RawContacts.SOURCE_ID };
		final String selection = RawContacts.ACCOUNT_NAME + "=?";

		final Cursor c = resolver.query(RawContacts.CONTENT_URI, projection, selection, new String[] { accountName }, null);
		HashMap<String, Long> contactsOnPhone = new HashMap<String, Long>();
		if (c != null) {
			while (c.moveToNext()) {
				contactsOnPhone.put(c.getString(c.getColumnIndex(RawContacts.SOURCE_ID)), c.getLong(c.getColumnIndex(Data._ID)));
			}
			c.close();
		}

		return contactsOnPhone;
	}

	private Uri addCallerIsSyncAdapterFlag(Uri uri) {
		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		return b.build();
	}

	/**
	 * Add a new contact to the RawContacts table.
	 * 
	 * @param resolver
	 * @param accountName
	 * @param contact
	 */
	private void addContact(ContentResolver resolver, String accountName, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Uri uri = addCallerIsSyncAdapterFlag(RawContacts.CONTENT_URI);

		ContentValues cv = new ContentValues();
		cv.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		cv.put(RawContacts.ACCOUNT_NAME, accountName);
		cv.put(RawContacts.SOURCE_ID, contact.getID());

		// This is the first insert into the raw contacts table
		ContentProviderOperation i1 = ContentProviderOperation.newInsert(uri).withValues(cv).build();
		ops.add(i1);

		prepareFields(-1, contact, new Contact(), ops, true);

		// Now create the contact with a single batch operation
		try {
			ContentProviderResult[] res = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			// The first insert is the one generating the ID for this contact
			long id = ContentUris.parseId(res[0].uri);
			Log.d(TAG, "The new contact has id: " + id);
		} catch (Exception e) {
			Log.e(TAG, "Cannot create contact ", e);
		}
	}

	private void prepareFields(long rawContactId, Contact newC, Contact existingC, ArrayList<ContentProviderOperation> ops, boolean isNew) {
		ContactMerger contactMerger = new ContactMerger(rawContactId, newC, existingC, ops);
		contactMerger.updateName();
		contactMerger.updateMail(Email.TYPE_WORK);
		contactMerger.updateMail(Email.TYPE_HOME);

		contactMerger.updatePhone(Phone.TYPE_HOME);
		contactMerger.updatePhone(Phone.TYPE_MOBILE);
		contactMerger.updatePhone(Phone.TYPE_WORK_MOBILE);
		contactMerger.updatePhone(Phone.TYPE_WORK);
		contactMerger.updatePhone(Phone.TYPE_FAX_WORK);
		contactMerger.updatePhone(Phone.TYPE_FAX_HOME);

		contactMerger.updateURL(Website.TYPE_HOME);
		contactMerger.updateURL(Website.TYPE_WORK);
		contactMerger.updateURL(Website.TYPE_PROFILE);

		contactMerger.updatePicture();
		contactMerger.updateBirthday();
		contactMerger.updateCompanyInformation();

		contactMerger.updateAddress(StructuredPostal.TYPE_WORK);
		contactMerger.updateAddress(StructuredPostal.TYPE_HOME);
	}

	public static void makeGroupVisible(String accountName, ContentResolver resolver) {
		try {
			ContentProviderClient client = resolver.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
			ContentValues cv = new ContentValues();
			cv.put(Groups.ACCOUNT_NAME, accountName);
			cv.put(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
			cv.put(Settings.UNGROUPED_VISIBLE, true);
			client.insert(Settings.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(), cv);
		} catch (RemoteException e) {
			Log.d(TAG, "Cannot make the Group Visible");
		}
	}
}
