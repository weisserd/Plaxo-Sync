package de.danielweisser.android.plaxosync.platform;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.ContentProviderOperation.Builder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.text.TextUtils;
import de.danielweisser.android.plaxosync.client.Address;
import de.danielweisser.android.plaxosync.client.Contact;
import de.danielweisser.android.plaxosync.syncadapter.Logger;

/**
 * A helper class that merges the fields of existing contacts with the fields of new contacts.
 */
public class ContactMerger {

	private final long rawContactId;
	private final Contact newC;
	private final Contact existingC;
	private final ArrayList<ContentProviderOperation> ops;
	private final Logger l;

	public ContactMerger(long rawContactId, Contact newContact, Contact existingContact, ArrayList<ContentProviderOperation> ops, Logger l) {
		this.rawContactId = rawContactId;
		this.newC = newContact;
		this.existingC = existingContact;
		this.ops = ops;
		this.l = l;
	}

	public void updateName() {
		if (TextUtils.isEmpty(existingC.getFirstName()) || TextUtils.isEmpty(existingC.getLastName())) {
			l.d("Set name to: " + newC.getFirstName() + " " + newC.getLastName());
			ContentValues cv = new ContentValues();
			cv.put(StructuredName.PREFIX, newC.getNamePrefix());
			cv.put(StructuredName.GIVEN_NAME, newC.getFirstName());
			cv.put(StructuredName.FAMILY_NAME, newC.getLastName());
			cv.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!newC.getFirstName().equals(existingC.getFirstName()) || !newC.getLastName().equals(existingC.getLastName())
				|| (newC.getNamePrefix() != null && existingC.getNamePrefix() != null && !newC.getNamePrefix().equals(existingC.getNamePrefix()))) {
			l.d("Update name to: " + newC.getFirstName() + " " + newC.getLastName());
			ContentValues cv = new ContentValues();
			cv.put(StructuredName.PREFIX, newC.getNamePrefix());
			cv.put(StructuredName.GIVEN_NAME, newC.getFirstName());
			cv.put(StructuredName.FAMILY_NAME, newC.getLastName());
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(
					Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] { rawContactId + "", StructuredName.CONTENT_ITEM_TYPE })
					.withValues(cv);
			ops.add(updateOp.build());
		}
	}

	private Builder createInsert(long rawContactId, ContentValues cv) {
		Builder insertOp = ContentProviderOperation.newInsert(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withValues(cv);
		if (rawContactId == -1) {
			insertOp.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		} else {
			insertOp.withValue(Data.RAW_CONTACT_ID, rawContactId);
		}
		return insertOp;
	}

	private Uri addCallerIsSyncAdapterFlag(Uri uri) {
		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		return b.build();
	}

	public void updateMail(int mailType) {
		String newMail = null;
		String existingMail = null;
		if (mailType == Email.TYPE_WORK) {
			newMail = newC.getWorkEmail();
			existingMail = existingC.getWorkEmail();
		} else if (mailType == Email.TYPE_HOME) {
			newMail = newC.getHomeEmail();
			existingMail = existingC.getHomeEmail();
		}
		updateMail(newMail, existingMail, mailType);
	}

	private void updateMail(String newMail, String existingMail, int mailType) {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Email.MIMETYPE + "=? AND " + Email.TYPE + "=?";
		if (TextUtils.isEmpty(newMail) && !TextUtils.isEmpty(existingMail)) {
			l.d("Delete mail data " + mailType + " (" + existingMail + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Email.CONTENT_ITEM_TYPE, mailType + "" }).build());
		} else if (!TextUtils.isEmpty(newMail) && TextUtils.isEmpty(existingMail)) {
			l.d("Add mail data " + mailType + " (" + newMail + ")");
			ContentValues cv = new ContentValues();
			cv.put(Email.DATA, newMail);
			cv.put(Email.TYPE, mailType);
			cv.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!newMail.equals(existingMail)) {
			l.d("Update mail data " + mailType + " (" + existingMail + " => " + newMail + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Email.CONTENT_ITEM_TYPE, mailType + "" }).withValue(Email.DATA, newMail);
			ops.add(updateOp.build());
		}
	}

	public void updatePhone(int phoneType) {
		String newPhone = null;
		String existingPhone = null;
		if (phoneType == Phone.TYPE_HOME) {
			newPhone = newC.getHomePhone();
			existingPhone = existingC.getHomePhone();
		} else if (phoneType == Phone.TYPE_MOBILE) {
			newPhone = newC.getCellHomePhone();
			existingPhone = existingC.getCellHomePhone();
		} else if (phoneType == Phone.TYPE_WORK_MOBILE) {
			newPhone = newC.getCellWorkPhone();
			existingPhone = existingC.getCellWorkPhone();
		} else if (phoneType == Phone.TYPE_WORK) {
			newPhone = newC.getWorkPhone();
			existingPhone = existingC.getWorkPhone();
		} else if (phoneType == Phone.TYPE_FAX_WORK) {
			newPhone = newC.getWorkFax();
			existingPhone = existingC.getWorkFax();
		} else if (phoneType == Phone.TYPE_FAX_HOME) {
			newPhone = newC.getHomeFax();
			existingPhone = existingC.getHomeFax();
		}
		updatePhone(newPhone, existingPhone, phoneType);
	}

	private void updatePhone(String newPhone, String existingPhone, int phoneType) {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Phone.MIMETYPE + "=? AND " + Phone.TYPE + "=?";
		if (TextUtils.isEmpty(newPhone) && !TextUtils.isEmpty(existingPhone)) {
			l.d("Delete phone data " + phoneType + " (" + existingPhone + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Phone.CONTENT_ITEM_TYPE, phoneType + "" }).build());
		} else if (!TextUtils.isEmpty(newPhone) && TextUtils.isEmpty(existingPhone)) {
			l.d("Add phone data " + phoneType + " (" + newPhone + ")");
			ContentValues cv = new ContentValues();
			cv.put(Phone.DATA, newPhone);
			cv.put(Phone.TYPE, phoneType);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!newPhone.equals(existingPhone)) {
			l.d("Update phone data " + phoneType + " (" + existingPhone + " => " + newPhone + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Phone.CONTENT_ITEM_TYPE, phoneType + "" }).withValue(Phone.DATA, newPhone);
			ops.add(updateOp.build());
		}
	}

	public void updateURL(int urlType) {
		String newURL = null;
		String existingURL = null;
		if (urlType == Website.TYPE_HOME) {
			newURL = newC.getHomeURL();
			existingURL = existingC.getHomeURL();
		} else if (urlType == Website.TYPE_WORK) {
			newURL = newC.getWorkURL();
			existingURL = existingC.getWorkURL();
		}
		updateURL(newURL, existingURL, urlType);
	}

	private void updateURL(String newURL, String existingURL, int urlType) {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Website.MIMETYPE + "=? AND " + Website.TYPE + "=?";
		if (TextUtils.isEmpty(newURL) && !TextUtils.isEmpty(existingURL)) {
			l.d("Delete url data " + urlType + " (" + existingURL + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Website.CONTENT_ITEM_TYPE, urlType + "" }).build());
		} else if (!TextUtils.isEmpty(newURL) && TextUtils.isEmpty(existingURL)) {
			l.d("Add url data " + urlType + " (" + newURL + ")");
			ContentValues cv = new ContentValues();
			cv.put(Website.DATA, newURL);
			cv.put(Website.TYPE, urlType);
			cv.put(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!TextUtils.isEmpty(newURL) && !newURL.equals(existingURL)) {
			l.d("Update url data " + urlType + " (" + existingURL + " => " + newURL + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Website.CONTENT_ITEM_TYPE, urlType + "" }).withValue(Website.DATA, newURL);
			ops.add(updateOp.build());
		}
	}

	public void updatePicture() {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
		if (newC.getImage() == null && existingC.getImage() != null) {
			l.d("Delete image");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Photo.CONTENT_ITEM_TYPE }).build());
		} else if (newC.getImage() != null && existingC.getImage() == null) {
			l.d("Add image");
			ContentValues cv = new ContentValues();
			cv.put(Photo.PHOTO, newC.getImage());
			cv.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!Arrays.equals(newC.getImage(), existingC.getImage())) {
			l.d("Update image");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Photo.CONTENT_ITEM_TYPE }).withValue(Photo.PHOTO, newC.getImage());
			ops.add(updateOp.build());
		}
	}

	public void updateCompanyInformation() {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Organization.MIMETYPE + "=?";
		if (TextUtils.isEmpty(newC.getCompany()) && TextUtils.isEmpty(newC.getTitle())
				&& (!TextUtils.isEmpty(existingC.getCompany()) || !TextUtils.isEmpty(existingC.getTitle()))) {
			l.d("Delete company data " + "(" + existingC.getCompany() + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Organization.CONTENT_ITEM_TYPE }).build());
		} else if (TextUtils.isEmpty(existingC.getCompany()) && TextUtils.isEmpty(existingC.getTitle())
				&& (!TextUtils.isEmpty(newC.getCompany()) || !TextUtils.isEmpty(newC.getTitle()))) {
			l.d("Add company data " + " (" + newC.getCompany() + " / " + newC.getTitle() + ")");
			ContentValues cv = new ContentValues();
			cv.put(Organization.COMPANY, newC.getCompany());
			cv.put(Organization.TITLE, newC.getTitle());
			cv.put(Organization.TYPE, Organization.TYPE_WORK);
			cv.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if ((existingC.getCompany() != null && !existingC.getCompany().equals(newC.getCompany()))
				|| (existingC.getTitle() != null && !existingC.getTitle().equals(newC.getTitle()))) {
			l.d("Update company data " + " (" + existingC.getCompany() + "/" + existingC.getTitle() + " => " + newC.getCompany() + "/" + newC.getTitle() + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Organization.CONTENT_ITEM_TYPE }).withValue(Organization.COMPANY, newC.getCompany()).withValue(
					Organization.TITLE, newC.getTitle());
			ops.add(updateOp.build());
		}
	}

	public void updateAddress(int adressType) {
		if (adressType == StructuredPostal.TYPE_HOME) {
			updateAddress(newC.getHomeAddress(), existingC.getHomeAddress(), adressType);
		} else if (adressType == StructuredPostal.TYPE_WORK) {
			updateAddress(newC.getWorkAddress(), existingC.getWorkAddress(), adressType);
		}
	}

	private void updateAddress(Address newAddress, Address existingAddress, int adressType) {
		final String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + StructuredPostal.TYPE + "=?";
		if ((newAddress == null || newAddress.isEmpty()) && existingAddress != null) {
			l.d("Delete address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", StructuredPostal.CONTENT_ITEM_TYPE, adressType + "" }).build());
		} else if (existingAddress == null && newAddress != null && !newAddress.isEmpty()) {
			l.d("Add address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ContentValues cv = new ContentValues();
			cv.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
			cv.put(StructuredPostal.TYPE, adressType);
			cv.put(StructuredPostal.STREET, newAddress.getStreet());
			cv.put(StructuredPostal.CITY, newAddress.getCity());
			cv.put(StructuredPostal.COUNTRY, newAddress.getCountry());
			cv.put(StructuredPostal.POSTCODE, newAddress.getZip());
			cv.put(StructuredPostal.REGION, newAddress.getState());
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (newAddress != null && !newAddress.isEmpty() && !newAddress.equals(existingAddress)) {
			l.d("Update address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ContentValues cv = new ContentValues();
			cv.put(StructuredPostal.STREET, newAddress.getStreet());
			cv.put(StructuredPostal.CITY, newAddress.getCity());
			cv.put(StructuredPostal.COUNTRY, newAddress.getCountry());
			cv.put(StructuredPostal.POSTCODE, newAddress.getZip());
			cv.put(StructuredPostal.REGION, newAddress.getState());
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", StructuredPostal.CONTENT_ITEM_TYPE, adressType + "" }).withValues(cv);
			ops.add(updateOp.build());
		}
	}
	
	public void updateBirthday() {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Event.MIMETYPE + "=?";
		if (TextUtils.isEmpty(newC.getDateOfBirth()) && !TextUtils.isEmpty(existingC.getDateOfBirth())) {
			l.d("Delete date of birth " + "(" + existingC.getDateOfBirth() + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Event.CONTENT_ITEM_TYPE }).build());
		} else if (TextUtils.isEmpty(existingC.getDateOfBirth()) && !TextUtils.isEmpty(newC.getDateOfBirth())) {
			l.d("Add date of birth " + " (" + newC.getDateOfBirth() + ")");
			ContentValues cv = new ContentValues();
			cv.put(Event.START_DATE, newC.getDateOfBirth());
			cv.put(Event.TYPE, Event.TYPE_BIRTHDAY);
			cv.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (existingC.getDateOfBirth() != null && !existingC.getDateOfBirth().equals(newC.getDateOfBirth())) {
			l.d("Update date of birth " + " (" + existingC.getDateOfBirth() + " => " + newC.getDateOfBirth() + "/" + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Event.CONTENT_ITEM_TYPE }).withValue(Event.START_DATE, newC.getDateOfBirth());
			ops.add(updateOp.build());
		}
	}
}
