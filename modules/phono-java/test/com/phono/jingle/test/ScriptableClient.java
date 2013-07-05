/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.jingle.test;

import com.phono.api.DeviceInfoFace;
import com.phono.api.PlayFace;
import com.phono.audio.AudioFace;
import com.phono.jingle.PhonoCall;
import com.phono.jingle.PhonoMessage;
import com.phono.jingle.PhonoMessaging;
import com.phono.jingle.PhonoNative;
import com.phono.jingle.PhonoPhone;
import com.phono.srtplight.Log;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author tim
 */
public class ScriptableClient {

    PhonoNative _pn;
    PhonoPhone _phone;
    PhonoCall _call;
    Thread _console;
    String _gateway;
    ScriptEngine _js;
    com.phono.applet.audio.phone.PhonoAudioShim _shim;
    boolean _done = false;

    public static void main(String args[]) throws Exception {
        //Log.setLevel(Log.DEBUG);
        Log.setLevel(Log.NONE);
        new ScriptableClient(args);
    }

    void invokeFun(String f) {
        try {
            Log.debug("invoking " + f + "On " + Thread.currentThread().getName());
            ((Invocable) _js).invokeFunction(f);
        } catch (Exception ex) {
            Log.error("Cant invoke " + f + " because " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadScript() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine js = manager.getEngineByName("JavaScript");
        Log.debug("Creating script On " + Thread.currentThread().getName());

        _js = js;

        // invoke the global function named "hello"
//        inv.invokeFunction("hello", "Scripting!!" );


        Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("call", _call);
        bindings.put("phono", _pn);
        bindings.put("phone", _phone);
        bindings.put("audio", _shim);
        bindings.put("done", _done);

        InputStream script = this.getClass().getResourceAsStream("/autotest.js");
        try {
            js.eval(new InputStreamReader(script));
        } catch (ScriptException ex) {
            Log.error("reader error" + ex);
        }

    }

    ScriptableClient(String[] args) {

        Log.debug("Starting scriptable client");
        if (args.length > 0) {
            _gateway = args[0];
            Log.debug("using " + _gateway + " as xmpp/phono server.");
        }


        final PhonoPhone phone = new PhonoPhone() {
            /* implement the abstract methods in PhonoPhone to give UI feedback */
            @Override
            public void onIncommingCall(PhonoCall c) {
                _call = c;
                _call.answer();
                invokeFun("onIncommingCall");
            }

            @Override
            public void onError() {
                System.out.println("Phone error");
            }

            /* implement the Phono Phone method that created a new PhonoCall Object */
            @Override
            public PhonoCall newCall() {
                return new PhonoCall(this) {
                    /* implement the abstract methods in PhonoCall to provide UI feedback */
                    @Override
                    public void onRing() {
                       // System.out.println("Phone ringing ");
                    }

                    @Override
                    public void onAnswer() {
                        invokeFun("onAnswer");
                    }

                    @Override
                    public void onHangup() {
                        invokeFun("onHangup");
                        _call = null;
                    }

                    @Override
                    public void onError() {
                        System.out.println("Phone error ");
                        _call = null;

                    }

                    public void digit(String ds) {
                        digit(ds.charAt(0));
                    }
                };
            }
        };
        final PhonoMessaging messing = new PhonoMessaging() {
            /* implement the abstract method in PhonoMessaging to provide UI feedback */
            @Override
            public void onMessage(PhonoMessage message) {
                try {
                    Log.debug("message: " + message.getBody());
                    _js.eval(message.getBody());
                } catch (ScriptException ex) {
                    Log.error("problem evaling message: " + ex);
                    ex.printStackTrace();
                }
            }
        };
        _pn = new PhonoNative(_gateway) {
            /* implement the abstract methods in PhonoNative to provide UI feedback */
            @Override
            public void onReady() {
                /* attach the UI to the PhonoNative stack*/
                _pn.setPhone(phone);
                _pn.setMessaging(messing);
                loadScript();
                invokeFun("onReady");
            }

            @Override
            public void onUnready() {
                _console = null; // thread will die soonish
            }

            @Override
            public void onError() {
                System.out.println("Connection Error! ");
            }
            /* platform specific stuff here */
            /* different classes are used for android. */

            @Override
            public AudioFace newAudio() {
                _shim = new com.phono.applet.audio.phone.PhonoAudioShim();
                return _shim;
            }

            @Override
            public PlayFace newPlayer(String tone) {
                return new com.phono.applet.audio.phone.Play(tone);
            }

            @Override
            public DeviceInfoFace newDeviceInfo() {
                return new DeviceInfoFace() {
                    public String getName() {
                        return System.getProperty("user.name");
                    }

                    public String getSystemName() {
                        return System.getProperty("os.name");
                    }

                    public String getSystemVersion() {
                        return System.getProperty("os.version");
                    }

                    public String getPhonoPlatform() {
                        return "java-scriptable";
                    }
                };
            }
        };

        /* connect to voxeo's cloud */
        _pn.connect();
        /* configure the ringtones */

        phone.setRingTone("http://s.phono.com/ringtones/Diggztone_Piano.mp3");
        phone.setRingbackTone("http://s.phono.com/ringtones/ringback-uk.mp3");
        /* save our phone for later access */
        _phone = phone;
        int maxTestSecs = 90;
        while (!_done) {
            try {
                Thread.sleep(1000);
                maxTestSecs --;
                if (maxTestSecs < 0){
                    _done = true;
                }
                if (_call != null){
                    invokeFun("pollEnergy");
                }
            } catch (Exception ex) {
                ;
            }
        }
        System.exit(0);
    }
}
