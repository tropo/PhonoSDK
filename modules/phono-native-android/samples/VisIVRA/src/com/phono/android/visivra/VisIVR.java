package com.phono.android.visivra;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

import com.phono.android.audio.Play;
import com.phono.android.rtp.DroidPhonoAudioShim;
import com.phono.api.PlayFace;
import com.phono.audio.AudioFace;
import com.phono.jingle.PhonoCall;
import com.phono.jingle.PhonoMessage;
import com.phono.jingle.PhonoMessaging;
import com.phono.jingle.PhonoNative;
import com.phono.jingle.PhonoPhone;

public class VisIVR extends Activity {

	PhonoNative _pn;
	private Context _ctx;
	private PhonoMessaging _mess;
	private PhonoPhone _phone;
	private PhonoCall _call;
	protected String _sessionId;
	protected AudioTrack _audioTrack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vis_ivr);
		_ctx = this.getApplicationContext();
		AudioManager as = (AudioManager) (_ctx
				.getSystemService(Context.AUDIO_SERVICE));
		if (as.isWiredHeadsetOn()){
			as.setSpeakerphoneOn(false);
		} else {
			as.setSpeakerphoneOn(true);
		}
		startPhono();
	}

	private void startPhono() {

		_phone = new PhonoPhone() {

			@Override
			public PhonoCall newCall() {
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
		_mess = new PhonoMessaging() {

			@Override
			public void onMessage(PhonoMessage arg0) {
				setMessage(arg0.getBody());
			}

		};
		_pn = new PhonoNative() {

			@Override
			public AudioFace newAudio() {
				DroidPhonoAudioShim das = new DroidPhonoAudioShim();
				_audioTrack = das.getAudioTrack();
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
			public void onError() {
				setConnState("Error");
			}

			@Override
			public void onReady() {
				_pn.setMessaging(_mess);
				_pn.setPhone(_phone);
				_sessionId = this.getSessionID();
				setConnState(_sessionId);

			}

			@Override
			public void onUnready() {
				setConnState("Disconnected");
			}
		};
		_phone.setRingTone("http://s.phono.com/ringtones/Diggztone_Piano.mp3");
		_phone.setRingbackTone("http://s.phono.com/ringtones/ringback-uk.mp3");
	}

	private void setConnState(final String string) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				((TextView) findViewById(R.id.conStatusTextView))
						.setText(string);
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

	public void digit(View view) {
		if (_call != null) {
			Button b = (Button) view;
			Character dt = b.getText().charAt(0);
			_call.digit(dt);
		}
	}

	public void dial(View view) {
		TextView jidTextView = (TextView) findViewById(R.id.addressTextView);
		String jid = jidTextView.getText().toString() + "@app";
		if (_call == null) {
			_call = _phone.dial(jid, null);
		} else {
			if (_call.isRinging()){
				_call.answer();
			}
		}
	}

	public void hangup(View view) {
		if (_call != null) {
			_call.hangup();
		}
	}

	public void connect(View view) {
		_pn.connect();
	}

	public void disconnect(View view) {
		_pn.disconnect();
	}
}
