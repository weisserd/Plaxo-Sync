package de.danielweisser.android.plaxosync.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.async.json.JSONArray;
import org.async.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

/**
 * Represents a PlaxoSyncAdapter contact
 */
public class Contact {
	public static final String STANDARD_ENCODING = "ISO-8859-1";
	private static final String TAG = "Contact";
	private String id = "";
	private String namePrefix = "";
	private String firstName = "";
	private String lastName = "";
	private String workEmail = "";
	private String homeEmail = "";
	private String imageURL = "";
	private String cellWorkPhone = "";
	private String workPhone = "";
	private String workFax = "";
	private String workURL = "";
	private String cellHomePhone = "";
	private String homePhone = "";
	private String homeFax = "";
	private String homeURL = "";
	private String company = "";
	private String title = "";
	private String dateOfBirth = "";
	private Address workAddress = null;
	private Address homeAddress = null;
	private byte[] image = null;

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getWorkEmail() {
		return workEmail;
	}

	public void setWorkEmail(String workEmail) {
		this.workEmail = workEmail;
	}

	public byte[] getImage() {
		if (image == null) {
			if (!TextUtils.isEmpty(imageURL)) {
				HttpClient httpclient = new DefaultHttpClient();
				try {
					HttpGet httpRequest = new HttpGet(imageURL);
					HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
					HttpEntity entity = response.getEntity();
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
					InputStream instream = bufHttpEntity.getContent();
					try {
						Bitmap bm = BitmapFactory.decodeStream(instream);
						if (bm != null) {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							bm.compress(Bitmap.CompressFormat.JPEG, 70, baos);
							image = baos.toByteArray();
						}
					} catch (OutOfMemoryError e) {
						// Do not set an image, when an OutOfMemoryError occurs
						image = null;
					} finally {
						instream.close();
						entity.consumeContent();
					}
				} catch (ClientProtocolException e) {
					Log.e(TAG, e.getMessage(), e);
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				} finally {
					httpclient.getConnectionManager().shutdown();
				}
			}
		}
		return image;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	public void setCellWorkPhone(String cellWorkPhone) {
		this.cellWorkPhone = cellWorkPhone;
	}

	public String getCellWorkPhone() {
		return cellWorkPhone;
	}

	public void setWorkPhone(String workPhone) {
		this.workPhone = workPhone;
	}

	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkFax(String workFax) {
		this.workFax = workFax;
	}

	public String getWorkFax() {
		return workFax;
	}

	public void setWorkURL(String workURL) {
		this.workURL = workURL;
	}

	public String getWorkURL() {
		return workURL;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompany() {
		return company;
	}

	public void setCellHomePhone(String cellHomePhone) {
		this.cellHomePhone = cellHomePhone;
	}

	public String getCellHomePhone() {
		return cellHomePhone;
	}

	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	public String getHomePhone() {
		return homePhone;
	}

	public void setHomeEmail(String homeEmail) {
		this.homeEmail = homeEmail;
	}

	public String getHomeEmail() {
		return homeEmail;
	}

	public void setHomeFax(String homeFax) {
		this.homeFax = homeFax;
	}

	public String getHomeFax() {
		return homeFax;
	}

	public void setHomeURL(String homeURL) {
		this.homeURL = homeURL;
	}

	public String getHomeURL() {
		return homeURL;
	}

	public void setWorkAddress(Address workAddress) {
		this.workAddress = workAddress;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public Address getWorkAddress() {
		return workAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	public Address getHomeAddress() {
		return homeAddress;
	}

	/**
	 * Creates and returns an instance of the contact from the provided JSON data.
	 * 
	 * @param entry
	 * 
	 * @return user The new instance of a contact created from the JSON data.
	 */
	@SuppressWarnings("unchecked")
	public static Contact valueOf(org.async.json.JSONObject entry) {
		Contact c = new Contact();

		// Name
		c.setID(entry.getString("id"));

		JSONObject jsonName = entry.getObject("name");
		c.setFirstName(jsonName.getString("givenName"));
		c.setLastName(jsonName.getString("familyName"));

		// Photo
		if (entry.contains("photos")) {
			JSONArray<JSONObject> jsonPictureArray = (JSONArray<JSONObject>) entry.getArray("photos");
			for (JSONObject jsonPicture : jsonPictureArray) {
				if (jsonPicture.contains("type") && jsonPicture.getString("type").equals("home")) {
					c.setImageURL(jsonPicture.getString("value"));
				}
			}
		}

		// E-Mails
		if (entry.contains("emails")) {
			JSONArray<JSONObject> jsonMailArray = (JSONArray<JSONObject>) entry.getArray("emails");
			for (JSONObject jsonMail : jsonMailArray) {
				if (jsonMail.contains("type") && jsonMail.getString("type").equals("work")) {
					c.setWorkEmail(jsonMail.getString("value"));
				} else if (jsonMail.contains("type") && jsonMail.getString("type").equals("home")) {
					c.setHomeEmail(jsonMail.getString("value"));
				}
			}
		}

		// URLs
		if (entry.contains("urls")) {
			JSONArray<JSONObject> jsonURLArray = (JSONArray<JSONObject>) entry.getArray("urls");
			for (JSONObject jsonURL : jsonURLArray) {
				if (jsonURL.contains("type") && jsonURL.getString("type").equals("work")) {
					c.setWorkURL(jsonURL.getString("value"));
				} else if (jsonURL.contains("type") && jsonURL.getString("type").equals("home")) {
					c.setHomeURL(jsonURL.getString("value"));
				}
			}
		}

		// Phone numbers
		if (entry.contains("phoneNumbers")) {
			JSONArray<JSONObject> jsonPhoneArray = (JSONArray<JSONObject>) entry.getArray("phoneNumbers");
			for (JSONObject jsonPhone : jsonPhoneArray) {
				if (jsonPhone.contains("type") && jsonPhone.getString("type").equals("work")) {
					c.setWorkPhone(jsonPhone.getString("value"));
				} else if (jsonPhone.contains("type") && jsonPhone.getString("type").equals("home")) {
					c.setHomePhone(jsonPhone.getString("value"));
				} else if (jsonPhone.contains("type") && jsonPhone.getString("type").equals("fax")) {
					c.setWorkFax(jsonPhone.getString("value"));
				} else if (jsonPhone.contains("type") && jsonPhone.getString("type").equals("mobile")) {
					c.setCellWorkPhone(jsonPhone.getString("value"));
				}
			}
		}

		// Company
		if (entry.contains("organizations")) {
			JSONArray<JSONObject> jsonOrganizationArray = (JSONArray<JSONObject>) entry.getArray("organizations");
			JSONObject jsonOrganization = jsonOrganizationArray.getObject(0);
			if (jsonOrganization.contains("name")) {
				c.setCompany(jsonOrganization.getString("name"));
			}
			if (jsonOrganization.contains("title")) {
				c.setTitle(jsonOrganization.getString("title"));
			}
		}

		// Addresses
		if (entry.contains("addresses")) {
			JSONArray<JSONObject> jsonAddressArray = (JSONArray<JSONObject>) entry.getArray("addresses");
			for (JSONObject jsonAddress : jsonAddressArray) {
				Address a = new Address();
				a.setStreet(jsonAddress.contains("streetAddress") ? jsonAddress.getString("streetAddress") : "");
				a.setCity(jsonAddress.contains("locality") ? jsonAddress.getString("locality") : "");
				a.setZip(jsonAddress.contains("postalCode") ? jsonAddress.getString("postalCode") : "");
				a.setState(jsonAddress.contains("region") ? jsonAddress.getString("region") : "");
				a.setCountry(jsonAddress.contains("country") ? jsonAddress.getString("country") : "");
				if (jsonAddress.contains("type")) {
					if (jsonAddress.getString("type").equals("work")) {
						c.setWorkAddress(a);
					} else if (jsonAddress.getString("type").equals("home")) {
						c.setHomeAddress(a);
					}
				}
			}
		}

		return c;
	}
}
