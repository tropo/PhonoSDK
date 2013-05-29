package com.phono.android.visivra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

import com.phono.android.audio.Play;
import com.phono.android.rtp.DroidPhonoAudioShim;
import com.phono.api.PlayFace;
import com.phono.api.DeviceInfoFace;
import com.phono.android.DeviceInfo;
import com.phono.audio.AudioFace;
import com.phono.jingle.PhonoCall;
import com.phono.jingle.PhonoMessage;
import com.phono.jingle.PhonoMessaging;
import com.phono.jingle.PhonoNative;
import com.phono.jingle.PhonoPhone;
import com.phono.srtplight.Log;
/**
 * Minimalist Phono native client for Android.
 * Illustrates most of the features of Phono Native in the
 * simplest possible way.
 * 
 * @author tim
 *
 */
public class VisIVR extends Activity {

	/* pretty much any phono native usage will need these */
	PhonoNative _pn;
	private Context _ctx;
	private PhonoMessaging _mess;
	private PhonoPhone _phone;
	private PhonoCall _call;
	protected String _sessionId;
	
	/* app specific */
	protected AudioTrack _audioTrack;
	protected String _googleId;
	protected String _otherGuy;

	protected String _knownNames[];
	private VisIVR _act;

	/*
	 * Standard Android stuff... 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.setLevel(Log.DEBUG);
		setContentView(R.layout.activity_vis_ivr);
		_act = this;
		_ctx = this.getApplicationContext();
		
		/* we set some audio UX behaviour here - adjust as needed */
		AudioManager as = (AudioManager) (_ctx
				.getSystemService(Context.AUDIO_SERVICE));
		if (as.isWiredHeadsetOn()) {
			as.setSpeakerphoneOn(false);
		} else {
			as.setSpeakerphoneOn(true);
		}
		startPhono();
	}


/*
 * Phono native config and setup.
 */
	private void startPhono() {

		// we will need an implementation of the (Abstract) PhonoPhone class
		// Our needs are simple enough to do that inline in an Anon class.
		_phone = new PhonoPhone() {

			@Override
			// invoked when a new call is created 
			public PhonoCall newCall() {
				// implement the abstract PhonoCall class with our behaviours
				// again simple enough to do inline.
				PhonoCall acall = new PhonoCall(_phone) {

					@Override
					public void onAnswer() {
						setStatus("Answered");
					}

					@Override
					public void onError() {
						setStatus("Call Error");
					}

					@Override
					public void onHangup() {
						setStatus("Hung up");
						_call = null;
					}

					@Override
					public void onRing() {
						setStatus("Ringing");
					}

				};
				// set other initialization of the call - default volume etc.
				acall.setGain(100);
				acall.setVolume(100);
				return acall;
			}

			@Override
			public void onError() {
				setStatus("Phone Error");
			}

			@Override
			public void onIncommingCall(PhonoCall arg0) {
				setStatus("Incomming call");
				_call = arg0;
			}

		};
		// and we need an implementation of the (Abstract) PhonoMessaging class 
		_mess = new PhonoMessaging() {

			@Override
			public void onMessage(PhonoMessage arg0) {
				setMessage(arg0.getBody());
			}

		};
		// Likewise PhonoNative - optionally set the address of the phono server
		_pn = new PhonoNative() {

			// 3 boilerplate android methods - we return platform specific implementations of the AudioFace and PlayFace and DeviceInfoFace
			// interfaces.
			
			@Override
			public AudioFace newAudio() {
				DroidPhonoAudioShim das = new DroidPhonoAudioShim();
				_audioTrack = das.getAudioTrack(); // in theory you might want to manipulate the audio track.
				return das;
			}

			@Override
			public PlayFace newPlayer(String arg0) {
				PlayFace f = null;
				try {
					f = new Play(arg0, _ctx);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return f;
			}
			@Override
                        public DeviceInfoFace newDeviceInfo(){
				return new DeviceInfo();
			}


			
			// What to do when an error occurs
			@Override
			public void onError() {
				setConnState("Error");
			}

			// we have connected to the Phono server so we now set the UI into motion.
			@Override
			public void onReady() {
				// once we are connected, apply the messaging and phone instances we built.
				_pn.setMessaging(_mess);
				_pn.setPhone(_phone);
				// This is where your Id mapping code would go
				_sessionId = this.getSessionID();
				_googleId = myGoogleId();
				_knownNames = notmyGoogleId(getKnownNames());
				
				// set up the user interface
				setConnState(_googleId);
				setBuddyNames(_knownNames);
				// update the database
				setMapping();
				// display who is (was) online
			}

			// we got disconnected from the phono server - 
			// retry logic goes here.
			@Override
			public void onUnready() {
				setConnState("Disconnected");
			}
		};
		// set the ringtones (ideally these are local resources - not remote URLS.)
		_phone.setRingTone("http://s.phono.com/ringtones/Diggztone_Piano.mp3");
		_phone.setRingbackTone("http://s.phono.com/ringtones/ringback-uk.mp3");
		// and request a connection.
		// phono native will ensure that this (and all other network activity) 
		// occurs on a non UI thread.
		_pn.connect();
	}

	
	/*
	 * GUI
	 * Methods to change screen display based on network events task is to flip
	 * changes onto the UI thread
	 */

	private void setConnState(final String string) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				((TextView) findViewById(R.id.conStatusTextView))
						.setText(string);
			}
		});
	}

	private void setBuddyNames(final String[] budds) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Spinner spinner = (Spinner) findViewById(R.id.buddyList);
				ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
						_act, android.R.layout.simple_spinner_item, budds);
				spinnerArrayAdapter
						.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner.setAdapter(spinnerArrayAdapter);
				setSpinnerCallback(spinner);
			}
		});
	}

	private void setStatus(final String string) {
		this.runOnUiThread(new Runnable() {
			public void run() {

				((TextView) findViewById(R.id.statusTextView)).setText(string);
			}
		});
	}

	private void setMessage(final String string) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				WebView w = ((WebView) findViewById(R.id.webView1));
				w.loadData(string, "text/html", null);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_vis_ivr, menu);
		return true;
	}

	/*
	 * User Actions
	 * callbacks from buttons on the screen
	 */
	void setSpinnerCallback(Spinner spinner) {
		OnItemSelectedListener spinlisten = new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				_otherGuy = _knownNames[pos];
				Log.debug("Other guy is now " + _otherGuy);
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		};
		spinner.setOnItemSelectedListener(spinlisten);
	}

	public void digit(View view) {
		if (_call != null) {
			Button b = (Button) view;
			Character dt = b.getText().charAt(0);
			_call.digit(dt);
		}
	}

	public void hangup(View view) {
		if (_call != null) {
			_call.hangup();
		}
	}

	public void dial(View view) {
		Runnable pd = new Runnable() {
			public void run() {
				String rjid = getMapping(_otherGuy);
				Log.debug("calling " + rjid);
				if (_call == null) {
					setStatus("dialing");
					_call = _phone.dial(rjid, null);
				} else {
					if (_call.isRinging()) {
						setStatus("answering");
						_call.answer();
					}
				}
			}
		};
		// because the 'getMapping()' method does network IO, we ask PhonoNative to run this
		// Runnable on a non-UI thread for us.
		_pn.submit(pd);
		
	}

	
	/*
	 * Identity - try and work out a human name for this user/phone
	 * we use the first google Id on the phone we can find.
	 * or the device type if we must.
	 */
		private String myGoogleId() {
			AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
			String ret = android.os.Build.MODEL;
;
			Account[] list = manager.getAccounts();
			if (list.length > 0) {
				ret = list[0].name;
			}
			return ret;
		}

		// remove my id from the list - don't want to ring myself.
		private String[] notmyGoogleId(String[] kn) {
			String[] ret = kn;
			Vector v = new Vector();
			for (String s : kn) {
				if (!s.equals(_googleId)) {
					v.add(s);
				}
			}
			if (v.size() != ret.length) {
				ret = new String[v.size()];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = (String) v.elementAt(i);
				}
			}
			return ret;
		}
	
	
	/*
	 * DB management
	 * Functions to access a simple id<->name cloud database
	 */

	String baseURL = "http://71.6.135.115/bm2012/";

	/* get a mapping from a name to their current session id - if any */
	private String getMapping(String name) {
		String surl = baseURL + "/getJid.groovy?name=" + name;
		String s[] = { "" };
		try {
			s = readStream(surl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (s.length > 0) ? s[0] : "9996160714\\40sip.tropo.com@sip";
	}

	/* Shove the mapping into a redis DB */
	private void setMapping() {
		String bits[] = _sessionId.split("@");
		String rjid = bits[0] + "\\40" + bits[1];
		String surl = baseURL + "/setJid.groovy?name=" + _googleId + "&jid="
				+ rjid + "@sip";
		String s[] = { "" };
		try {
			s = readStream(surl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Try pulling a list of users from our simple redis cloud DB */
	private String[] getKnownNames() {
		String[] ret = { "bill", "ben", "littleweed" };
		try {
			ret = readStream(baseURL + "allusers.groovy");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	/* dumb url slurper - assumes results are non-blank lines. */
	private String[] readStream(String slurl) throws Exception {
		String ret[] = null;
		URL url = new URL(slurl);
		Vector lines = new Vector();
		Log.debug("url =" + slurl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ret = new String[lines.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (String) lines.elementAt(i);
			Log.debug("-> " + ret[i]);
		}
		return ret;
	}

}
