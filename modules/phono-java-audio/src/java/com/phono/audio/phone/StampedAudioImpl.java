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

import com.phono.audio.StampedAudio;

/**
 * StampedAudioImpl
 *
 * Provides a holder class to contain a single sample set of
 * audio data. Typically 20ms worth, but that is codec dependent.
 * All users should synchronize access to implementations of this interface
 *
 */
public class StampedAudioImpl implements StampedAudio {
    
    private byte[] _bytes;
    private int _length;
    private int _timestamp;
    
    /** Creates a new instance of StampedAudioImpl */
    public StampedAudioImpl() {
    }
    
    
    /**
     * Sets all the attributes of this sample set.
     *
     * @param b The audio data
     * @param offset The offset into the audio data (b)
     * @param length The length of the audio data to copy
     * @param stamp The timestamp of this sample*
     */
    public synchronized void setStampAndBytes(byte []b, int offset, int length, int stamp) {
        _length = length;
        _timestamp = stamp;
        
        _bytes = new byte[_length];
        System.arraycopy(b, offset, _bytes, 0, _length);
    }
    
    
    /**
     * Creates a new byte buffer and fills it with all available valid
     * data.
     *
     * @return A copy of all available data
     */
    public synchronized byte [] copyData() {
        byte[] copyData = new byte[_length];
        System.arraycopy(_bytes, 0, copyData, 0, _length);
        return copyData;
    }
    
    
    /**
     * Copies all available valid data into the provided buffer,
     * starting at the 'offset' upto a maximum of 'length' bytes.
     *
     * @param data The provided buffer
     * @param offset The offset into  data
     * @param length The maximum length of data to copy
     *
     * @see #getOffset()
     * @see #getLength()
     */
    public synchronized void copyData(byte []data, int offset, int length) {
        if (length > _length) {
            length = _length;
        }
        System.arraycopy(_bytes, 0, data, offset, length);
    }
    
    
    /**
     * Returns the raw data buffer.
     * Use getOffset to determine where the valid data starts,
     * use getLength to determine how much valid data there is.
     *
     * @return The raw data buffer (not a copy)
     *
     * @see #getOffset()
     * @see #getLength()
     */
    public synchronized byte [] getData() {
        return _bytes;
    }
    
    
    /**
     * Returns the offset to the start of valid data.
     *
     * @return The offset
     */
    public synchronized int getOffset() {
        return 0;
    }
    
    
    /**
     * Returns the length of valid data.
     *
     * @return The length
     */
    public synchronized int getLength() {
        return _length;
    }
    
    
    /**
     * Returns the time stamp of this sample.
     *
     * @return The timestamp
     */
    public synchronized int getStamp() {
        return _timestamp;
    }
    

    public synchronized String toString() {
        StringBuffer buf = new StringBuffer("StampedAudio: ");
        buf.append("offset=").append(0);
        buf.append(", length=").append(_length);
        buf.append(", stamp=").append(_timestamp);
        return buf.toString();
    }
}
