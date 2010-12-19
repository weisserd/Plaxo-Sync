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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	 * @param jsonObject
	 * 
	 * @return user The new instance of a contact created from the JSON data.
	 */
	public static Contact valueOf(JSONObject jsonObject) {
		Contact c = new Contact();

		// Name
		try {
			c.setID(jsonObject.getString("id"));

			JSONObject jsonName = jsonObject.getJSONObject("name");
			c.setFirstName(jsonName.getString("givenName"));
			c.setLastName(jsonName.getString("familyName"));
		} catch (JSONException e) {
			return null;
		}

		// Photo
		try {
			JSONArray jsonPictureArray = jsonObject.getJSONArray("photos");
			for (int i = 0; i < jsonPictureArray.length(); i++) {
				JSONObject jsonPicture = jsonPictureArray.getJSONObject(i);
				if (jsonPicture.has("type") && jsonPicture.getString("type").equals("home")) {
					c.setImageURL(jsonPicture.getString("value"));
				}
			}
		} catch (JSONException e) {
			// No photo available
		}

		// E-Mails
		try {
			JSONArray jsonMailArray = jsonObject.getJSONArray("emails");
			for (int i = 0; i < jsonMailArray.length(); i++) {
				JSONObject jsonMail = jsonMailArray.getJSONObject(i);
				if (jsonMail.has("type") && jsonMail.getString("type").equals("work")) {
					c.setWorkEmail(jsonMail.getString("value"));
				} else if (jsonMail.has("type") && jsonMail.getString("type").equals("home")) {
					c.setHomeEmail(jsonMail.getString("value"));
				}
			}
		} catch (JSONException e) {
			// No e-mail available
		}

		// URLs
		try {
			JSONArray jsonURLArray = jsonObject.getJSONArray("urls");
			for (int i = 0; i < jsonURLArray.length(); i++) {
				JSONObject jsonURL = jsonURLArray.getJSONObject(i);
				if (jsonURL.has("type") && jsonURL.getString("type").equals("work")) {
					c.setWorkURL(jsonURL.getString("value"));
				} else if (jsonURL.has("type") && jsonURL.getString("type").equals("home")) {
					c.setHomeURL(jsonURL.getString("value"));
				}
			}
		} catch (JSONException e) {
			// No URL available
		}

		// Phone numbers
		try {
			JSONArray jsonPhoneArray = jsonObject.getJSONArray("phoneNumbers");
			for (int i = 0; i < jsonPhoneArray.length(); i++) {
				JSONObject jsonPhone = jsonPhoneArray.getJSONObject(i);
				if (jsonPhone.has("type") && jsonPhone.getString("type").equals("work")) {
					c.setWorkPhone(jsonPhone.getString("value"));
				} else if (jsonPhone.has("type") && jsonPhone.getString("type").equals("home")) {
					c.setHomePhone(jsonPhone.getString("value"));
				} else if (jsonPhone.has("type") && jsonPhone.getString("type").equals("fax")) {
					c.setWorkFax(jsonPhone.getString("value"));
				} else if (jsonPhone.has("type") && jsonPhone.getString("type").equals("mobile")) {
					c.setCellWorkPhone(jsonPhone.getString("value"));
				}
			}
		} catch (JSONException e) {
			// No phone numbers available
		}

		// TODO Company
		
		// Addresses
		try {
			JSONArray jsonAddressArray = jsonObject.getJSONArray("addresses");
			for (int i = 0; i < jsonAddressArray.length(); i++) {
				JSONObject jsonAddress = jsonAddressArray.getJSONObject(i);
				Address a = new Address();
				a.setStreet(jsonAddress.has("streetAddress") ? jsonAddress.getString("streetAddress") : "");
				a.setCity(jsonAddress.has("locality") ? jsonAddress.getString("locality") : "");
				a.setZip(jsonAddress.has("postalCode") ? jsonAddress.getString("postalCode") : "");
				a.setState(jsonAddress.has("region") ? jsonAddress.getString("region") : "");
				a.setCountry(jsonAddress.has("country") ? jsonAddress.getString("country") : "");
				if (jsonAddress.has("type")) {
					if (jsonAddress.getString("type").equals("work")) {
						c.setWorkAddress(a);
					} else if (jsonAddress.getString("type").equals("home")) {
						c.setHomeAddress(a);
					}
				}
			}
		} catch (JSONException e) {
			// No addresses available
		}

		return c;
	}
}
