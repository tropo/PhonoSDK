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

package com.phono.audio.phone;

import com.phono.audio.AudioException;
import com.phono.audio.Log;
import com.phono.audio.phone.PhonoAudioPropNames;

public class EsupPhonoAudio extends com.phono.audio.phone.PhonoAudio {

    protected double _gain = 1.0;
    private boolean _doEc = true;
    private boolean _muteMic = false;
    protected double _gainHistory[];
    protected int _delay;
    private int _echoFloor = 4096; // is a bit harsh, so might be reduced via audio properties

    @Override
    public void init(long codec, int latency) throws AudioException {
        super.init(codec, latency);
        _delay = _deep;

        _gainHistory = new double[_delay*2];
        for (int i = 0; i < _gainHistory.length; i++) {
            _gainHistory[i] = _gain;
        }
    }

    /**
     * Take the mic data and scale it - supressing it if we have an 'active'
     * speaker.
     * @param in
     * @return
     */
    @Override
    public short[] effectIn(short[] in) {
        in = super.effectIn(in);
        double energy = 0;
        if (_muteMic) {
            for (int i = 0; i < in.length; i++) {
                in[i] = 0;
                energy = 0;
            }
            Log.verb("EsupPhonoAudio.effectIn(): mute");
        } else {
            if (_doEc) {
                int index = _pframes % _gainHistory.length;
                double again = _gainHistory[index];
                for (int i = 0; i < in.length; i++) {
                    in[i] = (short) (in[i] * again);
                    energy = energy + (Math.abs(in[i]));

                }
                Log.verb("EsupPhonoAudio.effectIn(): echo can gain = " + again + " (" + _pframes + ", " + index + ")");
            } else {
                for (int i = 0; i < in.length; i++) {
                    in[i] = (short) (in[i] * _gain);
                    energy = energy + (Math.abs(in[i]));

                }
                Log.verb("EsupPhonoAudio.effectIn(): NO echo can gain = " + _gain);
            }

            double inEnergy = 0;
            if (energy > 0) {
                inEnergy = energy / in.length;
            }
            Log.verb("EsupPhonoAudio.effectIn(): _inEnergy=" + _inEnergy + " --> inEnergy=" + inEnergy);
        }
        return in;
    }

    /**
     * Calculate the gain based on what is about to go out the speakers...
     * @param out
     * @return
     */
    @Override
    public short[] effectOut(short[] out) {
        out = super.effectOut(out);
        // max energy is MAX.VALUE
        // assume typical background is MAX.VALUE/4096.0
        // so...
        double again = _gain * Short.MAX_VALUE / (_outEnergy * _echoFloor);
        if (again > _gain) {
            again = _gain;
        }
        // the output was...
        int index = (_pframes + _delay) % _gainHistory.length;
        _gainHistory[index] = again;
        Log.verb("EsupPhonoAudio.effectOut(): echo can gain = " + again + " (" + _pframes + ", " + index + "), _echoFloor=" + _echoFloor);

        return out;
    }

    @Override
    public boolean setAudioProperty(String name, Object value)
        throws IllegalArgumentException {
        if (name.equalsIgnoreCase(PhonoAudioPropNames.DOEC)) {
            String v = (String) value;
            _doEc = Boolean.parseBoolean(v);
            Log.debug("EsupPhonoAudio.setAudioProperty(): _doEc=" + _doEc);
        } else if (name.equalsIgnoreCase(PhonoAudioPropNames.ECFLOOR)) {
            String v = (String) value;
            _echoFloor = Integer.parseInt(v);
            Log.debug("EsupPhonoAudio.setAudioProperty(): _echoFloor=" + _echoFloor);
        } else if (name.equalsIgnoreCase(PhonoAudioPropNames.GAIN)) {
            String v = (String) value;
            _gain = Double.parseDouble(v);
            Log.debug("EsupPhonoAudio.setAudioProperty(): _gain=" + _gain);
        }
        return super.setAudioProperty(name, value);
    }

    /**
     * Is there an echo cancellation running?
     *
     * @return boolean
     */
    public boolean callHasECon() {
        return _doEc;
    }

    public void muteMic(boolean mute) {
        Log.debug("EsupPhonoAudio.muteMic(): mute=" + mute);
        _muteMic = mute;
    }

    public void setGain(double gain) {
        Log.debug("EsupPhonoAudio.setGain(): gain=" + gain);
        _gain = gain;
    }
}
