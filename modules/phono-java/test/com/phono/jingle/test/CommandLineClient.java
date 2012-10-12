/*
 * Copyright 2012 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.phono.jingle.test;

import com.phono.api.PlayFace;
import com.phono.applet.audio.phone.Play;
import com.phono.audio.AudioFace;
import com.phono.jingle.PhonoNative;
import com.phono.jingle.PhonoPhone;
import com.phono.jingle.PhonoCall;
import com.phono.jingle.PhonoMessage;
import com.phono.jingle.PhonoMessaging;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.util.Hashtable;

/**
 *
 * @author tim
 */
public class CommandLineClient {

    PhonoNative _pn;
    PhonoPhone _phone;
    PhonoCall _call;
    Thread _console;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Log.setLevel(0);
        CommandLineClient clc = new CommandLineClient();
    }

    CommandLineClient() {
        Log.debug("Starting command line client");
        final PhonoPhone phone = new PhonoPhone() {

            /* implement the abstract methods in PhonoPhone to give UI feedback */
            @Override
            public void onIncommingCall(PhonoCall c) {
                _call = c;
                System.out.println("Press a<ret> to answer");
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
                        System.out.println("Phone ringing ");
                    }

                    @Override
                    public void onAnswer() {
                        System.out.println("Phone answered ");
                    }

                    @Override
                    public void onHangup() {
                        System.out.println("Phone hungup ");
                        _call = null;
                    }

                    @Override
                    public void onError() {
                        System.out.println("Phone error ");
                        _call = null;

                    }
                };
            }
        };
        final PhonoMessaging messing = new PhonoMessaging() {
            /* implement the abstract method in PhonoMessaging to provide UI feedback */
            @Override
            public void onMessage(PhonoMessage message) {
                System.out.println("message from " + message.getFrom() + ": " + message.getBody());
            }
        };
        _pn = new PhonoNative() {
            /* implement the abstract methods in PhonoNative to provide UI feedback */

            @Override
            public void onReady() {
                        /* attach the UI to the PhonoNative stack*/
                _pn.setPhone(phone);
                _pn.setMessaging(messing);
                System.out.println("Connection Ready");
                if (_console == null) {
                    Runnable cli = new Runnable() {

                        public void run() {
                            consoleUI();
                        }
                    };
                    _console = new Thread(cli);
                    _console.start();
                }
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
                return new com.phono.applet.audio.phone.PhonoAudioShim();
            }

            @Override
            public PlayFace newPlayer(String tone) {
                return new com.phono.applet.audio.phone.Play(tone);
            }

        };

        /* connect to voxeo's cloud */
        _pn.connect();
        /* configure the ringtones */

        phone.setRingTone("http://s.phono.com/ringtones/Diggztone_Piano.mp3");
        phone.setRingbackTone("http://s.phono.com/ringtones/ringback-uk.mp3");
        /* save our phone for later access */
        _phone = phone;

    }

    /**
     * primitive (minimal) commandline UI
     */
    private void consoleUI() {
        while (_console != null) {
            try {
                if (_call != null) {
                    System.out.println("Press h<ret> to hangup or 0-9*# <ret> to send dtmf");
                } else {
                    System.out.println("Press d<ret> to dial");
                }
                int c = System.in.read();

                if ((c == 'h') || (c == 'H')) {
                    if (_call != null) {
                        _call.hangup();
                        _call = null;
                    } else {
                        System.out.println("No current call to hangup on...");
                    }
                }
                if ((c == 'd') || (c == 'D')) {
                    if (_call == null) {
                        _call = _phone.dial("9996160714@app", null);
                    } else {
                        System.out.println("Currently in a call, can't start a new one.");
                    }
                }
                if ("0123456789*#".indexOf(c) >= 0) {
                    _call.digit(new Character((char) c));
                }
                if ((c == 'a') || (c == 'A')) {
                    _call.answer();
                }
            } catch (IOException ex) {
                Log.warn(ex.getMessage());
            }
        }

    }
}
