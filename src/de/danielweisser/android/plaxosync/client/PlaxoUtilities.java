package de.danielweisser.android.plaxosync.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.async.json.JSONArray;
import org.async.json.JSONObject;
import org.async.json.in.JSONParser;
import org.async.json.in.JSONReader;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import de.danielweisser.android.plaxosync.Constants;
import de.danielweisser.android.plaxosync.authenticator.PlaxoAuthenticatorActivity;

/**
 * Provides utility methods for communicating with the server.
 */
public class PlaxoUtilities {
	private static final String TAG = "PlaxoUtilities";

	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network operations to be executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
				}
			}
		};
		t.start();
		return t;
	}

	private static DefaultHttpClient getPlaxoConnection(String username, String password) throws PlaxoLoginException {
		DefaultHttpClient httpclient = null;
		try {
			httpclient = new DefaultHttpClient();
			httpclient.setRedirectHandler(new RedirectHandler() {
				public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
					return null;
				}

				public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
					return false;
				}
			});
			httpclient.getCredentialsProvider().setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(username, password));
			HttpGet httpget = new HttpGet("http://www.plaxo.com/pdata/contacts/@me/@all?count=1");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			Log.d(TAG, "Login form get: " + response.getStatusLine());
			if (entity != null) {
				entity.consumeContent();
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				if (response.getStatusLine().getStatusCode() == 401) {
					throw new PlaxoLoginException("Wrong username or password");
				} else {
					throw new PlaxoLoginException("Error on connecting to Plaxo. Error code: " + response.getStatusLine().getStatusCode());
				}
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new PlaxoLoginException(e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new PlaxoLoginException(e.getMessage());
		}
		return httpclient;
	}

	/**
	 * Connects to Plaxo, authenticates the provided username and password.
	 * 
	 * @param username
	 *            The user's username
	 * @param handler
	 *            The handler instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return boolean The boolean result indicating whether the user was successfully authenticated.
	 */
	public static boolean authenticate(String username, String password, Handler handler, final Context context) {
		DefaultHttpClient httpclient = null;
		try {
			httpclient = getPlaxoConnection(username, password);
			if (httpclient != null) {
				sendResult(true, handler, context, null);
				return true;
			}
		} catch (PlaxoLoginException e) {
			Log.e(TAG, e.getMessage(), e);
			sendResult(false, handler, context, e.getMessage());
			return false;
		} finally {
			if (httpclient != null) {
				httpclient.getConnectionManager().shutdown();
			}
		}
		return false;
	}

	/**
	 * Sends the authentication response from server back to the caller main UI thread through its handler.
	 * 
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 * @param message
	 */
	private static void sendResult(final Boolean result, final Handler handler, final Context context, final String message) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((PlaxoAuthenticatorActivity) context).onAuthenticationResult(result, message);
			}
		});
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param username
	 *            The user's username
	 * @param password
	 *            The password
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final String username, final String password, final Handler handler, final Context context) {
		return PlaxoUtilities.performOnBackgroundThread(new Runnable() {
			public void run() {
				authenticate(username, password, handler, context);
			}
		});
	}

	public static List<Contact> fetchContacts(String username, String password, Date mLastUpdated, final Context context) {
		final ArrayList<Contact> friendList = new ArrayList<Contact>();

		DefaultHttpClient httpclient = null;
		try {
			httpclient = getPlaxoConnection(username, password);
			if (httpclient != null) {
				HttpGet httpget = new HttpGet("http://www.plaxo.com/pdata/contacts/@me/@all");
				HttpResponse response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					InputStream contentStream = entity.getContent();
					File jsonFile = writedataToSD(contentStream);
					entity.consumeContent();

					parseJSON(friendList, jsonFile);
					Log.d(TAG, "Number of contacts: " + friendList.size());
				}
			}
		} catch (PlaxoLoginException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			if (httpclient != null) {
				httpclient.getConnectionManager().shutdown();
			}
		}

		return friendList;
	}

	/**
	 * Parses the JSON file from the SD card asynchronously.
	 * 
	 * @param friendList
	 *            List with contacts
	 * @param jsonFile
	 *            JSON file on SD card
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void parseJSON(final ArrayList<Contact> friendList, File jsonFile) throws FileNotFoundException, IOException {
		BufferedReader inJson = new BufferedReader(new FileReader(jsonFile));

		JSONParser jp = new JSONParser();
		JSONReader jr = new JSONReader(inJson);
		JSONObject jo = jp.parse(jr);

		@SuppressWarnings("unchecked")
		JSONArray<JSONObject> entries = (JSONArray<JSONObject>) jo.getArray("entry");
		for (JSONObject entry : entries) {
			Contact u = Contact.valueOf(entry);
			if (u != null && u.getFirstName() != null && u.getLastName() != null) {
				friendList.add(u);
			}
		}
		jr.close();
		inJson.close();
	}

	/**
	 * Obtains the raw data and writes it to the SD card.
	 * 
	 * @param contentStream
	 *            ContentStream to read from (HTTP access of Plaxo)
	 * @return File handle to the written JSON file on the SD card
	 * @throws IOException
	 */
	private static File writedataToSD(InputStream contentStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(contentStream));
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + Constants.SDCARD_FOLDER);
		dir.mkdirs();
		File jsonFile = new File(dir, "plaxosync.json");
		BufferedWriter f = new BufferedWriter(new FileWriter(new File(dir, "plaxosync.json")));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			f.write(inputLine + "\n");
		}
		f.close();
		in.close();
		return jsonFile;
	}
}
