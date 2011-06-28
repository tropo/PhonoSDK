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

package com.phono.audio.codec.gsm;

import com.phono.audio.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketPermission;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import javax.sound.sampled.*;

public class GSMPlayer implements Runnable {

    private boolean _running;
    private GSM_Decoder _dec;
    SourceDataLine _line = null;
    private Thread _th;
    InputStream _uin;
    private boolean _paused = true;
    AudioFormat _mono8k;
    int _bsz;

    public GSMPlayer() throws LineUnavailableException {
        audioInit();
    }

    public void load(URL url) throws IOException {
        if (_th != null) {
            astop();
        }
        InputStream in = null;

        String hostPort = url.getHost() + ":" + url.getDefaultPort();
        SocketPermission sPermission = new SocketPermission(hostPort, "connect,resolve");
        try {
            AccessController.checkPermission(sPermission);
            in = url.openStream();
        } catch (AccessControlException ex) {
            Log.error(this.getClass().getName() + ".load(): sorry, you don't have permission to connect & resolve '" + hostPort + "'. Cannot load url '" + url + "' : " + ex.getClass().getName() + " " + ex.getMessage());            
        } catch (Exception ex) {
            Log.error(this.getClass().getName() + ".load(" + url + "): " + ex.getClass().getName() + " " + ex.getMessage());
        }

        if (in != null) {
            _uin = new java.io.BufferedInputStream(in);
            if (url.getPath().toLowerCase().endsWith(".wav")) {
                byte[] wavhead = new byte[60]; // skip the wave header and assume gsm 6.10
                _uin.read(wavhead);
            }
            _th = new Thread(this);
            _dec = new GSM_Decoder(); // get a fresh decoder for a new file.

            try {
                if (!_line.isOpen()) {
                    _line.open(_mono8k, _bsz);
                }
                _line.start();
                _th.setDaemon(true);
                _th.start();
            } catch (Exception ex) {
                Log.error(this.getClass().getName() + ".load(" + url + "): " + ex.getClass().getName() + " " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public int getStatus() {
        int status = 0;
        if (_running) {
            status += 1;
        }
        if (_paused) {
            status += 2;
        }
        return status;
    }

    public synchronized void pause() {
        _paused = true;
        notifyAll();
    }

    public synchronized void play() {
        _paused = false;
        notifyAll();
    }

    public void astop() {
        try {
            _running = false;
            Thread t = _th;
            _th = null;
            if (t != null) {
                t.join(200);
            }
        } catch (InterruptedException ex) {
            ; // don't actually care
        }
    }

    void audioInit() throws LineUnavailableException {

        _mono8k = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            8000.0F, 16, 1, 2, 8000.0F, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, _mono8k);

        _line = (SourceDataLine) (DataLine) AudioSystem.getLine(info);
        _bsz = 320 * 50 / 10;

    }

    /**
     * audioCleanup
     */
    void audioCleanup() {
        astop();
        if (_line.isOpen()) {
            _line.close();
        }
    }

    /**
     * run
     */
    public void run() {
        byte[] gsmb = new byte[33];
        byte[] sbuff = new byte[320];
        _running = true;

        try {
            int c = 0;
            while ((_running) && (_uin.available() > 0)) {
                _uin.read(gsmb);
                short[] is = _dec.decode_frame(gsmb);
                if (is != null) {
                    for (int i = 0; i < is.length; i++) {
                        sbuff[i * 2] = (byte) ((is[i] >> 8));
                        sbuff[1 + i * 2] = (byte) (0xff & (is[i]));
                    }
                    while (_paused) {
                        synchronized (this) {
                            this.wait(100);
                        }
                        if (!_running) {
                            break;
                        }
                    }
                    _line.write(sbuff, 0, sbuff.length);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (_running) {
            _line.drain();
        } else {
            _line.flush();
        }
        _running = false;
        _paused = true;
        _th = null;
    }
}
