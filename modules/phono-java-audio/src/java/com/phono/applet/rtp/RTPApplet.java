/*
 * Copyright 2011 Voxeo Corp.
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

package com.phono.applet.rtp;

import com.phono.audio.AudioException;
import com.phono.api.Codec;
import com.phono.api.CodecList;
import com.phono.api.Share;
import com.phono.applet.audio.phone.PhonoAudioShim;
import com.phono.applet.audio.phone.Play;
import com.phono.rtp.Endpoint;
import com.phono.srtplight.Log;
import java.applet.Applet;
import java.io.IOException;
import java.io.StringReader;

import java.net.SocketException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import netscape.javascript.*;

public class RTPApplet extends Applet {

    private PhonoAudioShim _audio;
    final private Hashtable _endpoints = new Hashtable();
    private CodecList _codecList;
    private boolean _userClickedTrust = false;
    private String _deviceList;

    /**
     * Initialization method that will be called after the applet is loaded
     * into the browser.
     */
    @Override
    public void init() {
        Log.setLevel(Log.DEBUG);
        Permission all = new AllPermission();
        try {
            AccessController.checkPermission(all);
            _userClickedTrust = true;
        } catch (AccessControlException ace) {
            _userClickedTrust = false;
            Log.error("permission problem " + ace.getMessage());
        }
        printSigners(this.getClass());
        StringBuffer bret = new StringBuffer("{\n");
        PhonoAudioShim.getMixersJSON(bret);
        bret.append("}\n");
        _deviceList =  bret.toString();
        _audio = new PhonoAudioShim();
        _codecList = new CodecList(_audio);

        // Call the callback that we have been given to say we are ready
        String callback = this.getParameter("callback");
        if (callback != null) {
            JSObject jso = JSObject.getWindow(this);
            try {
            jso.call(callback, new String[]{_deviceList});
            } catch (Throwable t){
                Log.error(t.getMessage());
            }
        }
    }
    @Override
    public void start(){
        if (!_userClickedTrust){
            Log.error("User does not trust us. Can't continue.");
        }
    }
    @Override
    public void stop(){
            Log.debug("Applet stopped");
    }
    @Override
    public void destroy(){
            Log.debug("Applet destroyed");
 //           if (_audio != null){
 //               _audio.destroy();
 //           }
    }
    public String allocateEndpoint() {
        String ret = null;
        // strictly we supposedly want to actually allocate a socket here,
        // but there is _really_ no point so we make plausible something up
        synchronized (_endpoints) {
            /*
            if (_localAdd == null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
            findAnIPAddress();
            return null; // nothing to return
            }
            });
            }

             */
            Endpoint r = AccessController.doPrivileged(new PrivilegedAction<Endpoint>() {

                public Endpoint run() {
                    Endpoint end = null;
                    try {
                        end = Endpoint.allocate();
                    } catch (SocketException ex) {
                        Log.error("Problem allocating a socket " + ex.getMessage());
                    }
                    return end;

                }
            });
            /*            String u = "rtp://" + _localAdd + ":" + _portNo++;
            Endpoint r = new Endpoint(u);
             *
             */
            _endpoints.put(r.getLocalURI(), r);
            ret = r.getLocalURI();
        }
        return ret;
    }

    public void freeEndpoint(String uri) {
        synchronized (_endpoints) {
            Endpoint e = (Endpoint) _endpoints.get(uri);
            if (e != null) {
                e.release();
            }
            _endpoints.remove(e);
        }
    }

    // see http://download.oracle.com/javase/7/docs/api/java/security/AccessController.html
    // for doc on priv escalation
    public Codec[] codecs() {
        return _codecList.getCodecs();
    }

    public Share share(String uri, final Codec codec, boolean autoStart) {
        return share(uri, codec, autoStart, null, null);
    }

    /**
     *
     * @param uri rtp://localhost:port<:remotehost:remoteport>
     * @param autoPlay start immediatly
     * @param codec Selected codec.
     * @param srtp crypto params
     * @return
     */
    public Share share(String uri, final Codec codec, boolean autoStart, String srtpPropsl, String srtpPropsr) {
        Share ret = null;
        Share s = null;
        Properties spl = null;
        Properties spr = null;

        if ((srtpPropsl != null) && (srtpPropsl.length() > 0)) {
            StringReader reader = new StringReader(srtpPropsl);
            spl = new Properties();
            try {
                spl.load(reader);
            } catch (IOException ex) {
                Log.error("srtp Props invalid format" + ex.toString());
            }
        }
        if ((srtpPropsr != null) && (srtpPropsr.length() > 0)) {
            StringReader reader = new StringReader(srtpPropsr);
            spr = new Properties();
            try {
                spr.load(reader);
            } catch (IOException ex) {
                Log.error("srtp Props invalid format" + ex.toString());
            }
        }
        Log.verb("in share() codec = " + codec.name);
        Log.verb("in share() uri = " + uri);
        try {
            s = new Share(uri, _audio, codec.pt, spl, spr);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                public Void run() {
                    try {
                        _audio.init(codec.iaxcn, 100);
                    } catch (AudioException ex) {
                        Log.error(ex.toString());
                    }
                    return null; // nothing to return

                }
            });
            String luri = s.getLocalURI();
            synchronized (_endpoints) {
                Endpoint e = (Endpoint) _endpoints.get(luri);
                if (e != null) {
                    e.setShare(s);
                } else {
                    e = new Endpoint(luri);
                    e.setShare(s);
                    Log.warn("Unexpected local endpoint used :" + luri);
                    _endpoints.put(luri, e);

                }
            }
            // should check auto start here...
            if (autoStart) {
                s.start();
            }
            ret = s; // only return the share if no exceptions ....
        } catch (Exception ex) {
            if (s != null) {
                s.stop(); // minimal cleanup on errors.
            }
            Log.error(ex.toString());                // do something useful here....
        }
        return ret;
    }
    // TODO overwrite start(), stop() and destroy() methods
    /*
    allocateEndpoint() -> uri
    Allocate a local RTP endpoint uri, rtp://ipaddress:port
    freeEndpoint(uri)
    Free a given local RTP endpoint
    share(uri, autoPlay, codec) -> Share
    Start to send audio from the localUri to the remoteUri {bi-directionally? - thp}
    play(uri, autoPlay) -> Play
    Start to play audio received on the local Uri,
    or from a remote http Uri (used for mp3 ringtones etc).
    codecs() -> Codec[]
    Return an array of codecs supported by the plugin {JSON array of property objects thp}
     *
     */

    public Play play(final String uri, boolean autoStart) {
        Play s = null;
        Log.debug("in play() uri = " + uri);
        try {
            s = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Play>() {

                        public Play run() {
                            Play id = new Play(uri);
                            return id;
                        }
                    });
            if (autoStart) {
                s.start();
            }
        } catch (Exception ex) {
            Log.error(ex.toString());                // do something useful here....
        }
        return s;
    }

    public String getJSONStatus() {
        StringBuffer ret = new StringBuffer("{\n");
        ret.append("userTrust").append(" : ");
        ret.append(_userClickedTrust?"true":"false").append(",\n");
        Enumeration rat = _endpoints.elements();
        ret.append("endpoints").append(" : ");
        ret.append("[");
        while (rat.hasMoreElements()) {
            Endpoint r = (Endpoint) rat.nextElement();
            r.getJSONStatus(ret);
            ret.append(",");
        }
        ret.append("]\n");
        ret.append("}\n");
        // Log.debug("Phonefromhere.getCallStatus(): " + ret);
        return ret.toString();
    }

    void printSigners(Class cl) {
        Object[] signers = cl.getSigners();
        if (signers != null) {
            int len = signers.length;
            for (int i = 0; i < len; i++) {
                Object o = signers[i];
                if (o instanceof java.security.cert.X509Certificate) {
                    java.security.cert.X509Certificate cert =
                        (java.security.cert.X509Certificate) o;
                    Log.debug(cl.getName() + ": signer " + i
                              + " = " + cert.getSubjectX500Principal().getName());
                }
            }
        }
        else {
            Log.debug(cl.getName() + " is not signed (has no signers)");
        }
    }

    public String getAudioDeviceList(){
        return _deviceList;
    }

    public void setAudioIn(String ain){
        if (_audio != null){
            _audio.setAudioInName(ain);
            Log.debug("Set audio input device preference to "+ ain);
        }
    }

}
