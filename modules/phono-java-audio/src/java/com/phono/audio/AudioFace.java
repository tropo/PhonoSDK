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

package com.phono.audio;

import com.phono.audio.NetStatsFace;
import com.phono.audio.codec.CodecFace;
import java.util.Properties;

/**
 * AudioFace is the interface to be implemented by each audio implementation.
 *
 * @see AudioReceiver#newAudioDataReady(AudioFace audioFace, int bytesAvailable)
 *
 */
public interface AudioFace {

    public String getCodecName();

    /**
     * Returns the optimum size of a frame. eg:
     * <ol>
     *  <li>33 bytes for GSM</li>
     *  <li>320 for SLIN</li>
     *  <li>160 for [ua]law</li>
     * </ol>
     * May not be called on an uninitialized instance.
     *
     * @return The frame size in bytes
     */
    public int getFrameSize();

    /**
     * Returns the frame interval in ms,
     * normally 20.
     * May not be called on an uninitialized instance.
     *
     * @return The frame interval in milliseconds
     */
    public int getFrameInterval();

    /**
     * Returns the current codec in use (int values - as specified in IAX RFC).
     * May not be called on an uninitialized instance.
     *
     * @return The codec, as specified in the IAX RFC
     */
    public long getCodec();

    /**
     * Returns an array of available codecs (int values - as specified in IAX RFC).
     * Must return all the codecs that can be supported by implementation.
     * May be called on an uninitialized instance.
     *
     * @return The array of available codecs
     */
    public long[] getCodecs();

    /**
     * Returns the percentage of frame that contains voice since you last asked.
     * @return The percentage of voice frames (0 - 100)
     */
    public int getVADpc();

    /*
     * Returns is this codec is available.
     */
    public boolean isCodecAvailable(long codec);

    public int getOutboundTimestamp();

    /**
     * Actually allocate resources.
     * The constructor should not allocate any resources.
     *
     * @param codec integer representing the codec to use.
     * Must be one of the values in the array returned by getCodecs().
     * May be called on an uninitialized instance.
     *
     * @param latency 
     * @throws AudioException 
     * @see #getCodecs()
     */
    public void init(long codec, int latency) throws AudioException;

    /**
     * Adds a class that will be notified of incomming audio
     * from the microphone.
     * Implementing class - eg IAXCall -
     * will call readStampedAudio() and then send data out over wire.
     * AudioReceiver can be null - in which case readStampedAudio()
     * should be called directly.
     *
     * @param r The AudioReceiver to be notified
     * @throws AudioException 
     * @see #readStampedAudio()
     */
    public void addAudioReceiver(AudioReceiver r) throws AudioException;

    /**
     * Reads data from the audio queue.
     * May block for upto 2x frame interval,
     * otherwise returns null.
     * May throw an exception if Audio Channel is not set up correctly.
     *
     * @return a StampedAudio containing a framesize worth of data.
     * @throws AudioException 
     * @see #getFrameSize()
     */
    public StampedAudio readStampedAudio() throws AudioException;

    public void updateRemoteStats(NetStatsFace r);

    /**
     * Sends audio data to the speakers.
     *
     * @param stampedAudio The stamped audio to play
     * @throws AudioException 
     */
    public void writeStampedAudio(StampedAudio stampedAudio) throws AudioException;

    /**
     * Obtains a 'cleaned' or unused StampedAudio,
     * used as a factory method in place of 'new'.
     * Implementations may choose to recycle StampedAudios after they
     * have been read.
     * Implementations may choose to assume that this method is _only_
     * ever called by the protocol stack in order to store data prior to
     * calling writeStampedAudio().
     *
     * @return A clean or unused instance of StampedAudio.
     * @see StampedAudio
     * @see #writeStampedAudio(StampedAudio)
     */
    public StampedAudio getCleanStampedAudio();

    /**
     * Returns if the audio is up.
     *
     * @return True if the audio is up, else false
     */
    public boolean isAudioUp();

    /**
     * Starts playing (to the speakers or headset).
     */
    public void startPlay();

    /**
     * Stops playing.
     */
    public void stopPlay();

    /**
     * Starts recording (from the microphone).
     */
    public void startRec();

    /**
     * Stops recording.
     */
    public void stopRec();

    /**
     * Frees resources allocted by init().
     *
     * @throws AudioException 
     * @see #init(int)
     */
    public void destroy() throws AudioException;

    /**
     * Returns all the available properties of this audio channnel.
     * eg:
     * <ol>
     *    <li>playLevel</li>
     *    <li>recLevel</li>
     *    <li>ec</li>
     *    <li>pitchShift</li>
     *    <li>field</li>
     * </ol>
     * @return 
     */
    public Properties getAudioProperties();

    /**
     * Sets the named property.
     *
     * @param name The name of the property
     * @param value The value of the property
     *
     * @return false if property unsupported or not settable,
     * true if property set.
     * @throws IllegalArgumentException if value invalid
     */
    public boolean setAudioProperty(String name, Object value)
        throws IllegalArgumentException;

    public double[] getEnergy();

    /**
     * Return is this audio is doing voice activity detection.
     * 
     * @return True if it is detecting, false otherwise
     */
    public boolean doVAD();

    public double getSampleRate();

    public void playDigit(char c);

    public void setMicGain(float f);

    public void setVolume(double d);

    public void muteMic(boolean v);

    public boolean callHasECon();

    public CodecFace getCodec(long l);

    public void unInit();

}
