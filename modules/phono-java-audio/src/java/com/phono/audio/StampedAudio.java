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

/**
 * Provides a holder class to contain a single sample set of
 * audio data. Typically 20ms worth, but that is codec dependent.
 *
 * Warning - you can't set the synchronized keyword on interfaces.
 * All users should synchronize access to implementations of this interface
 */
public interface StampedAudio {
    
    /**
     * Sets all the attributes of this sample set.
     * <em>access must be synchronized</em>
     *
     * @param b The audio data
     * @param offset The offset into the audio data (b)
     * @param length The length of the audio data to copy
     * @param stamp The timestamp of this sample
     */
    void setStampAndBytes(byte []b, int offset, int length, int stamp);
    
    /**
     * Creates a new byte buffer and fills it with all available valid
     * data.
     *
     * @return A copy of all available data
     */
    byte [] copyData();
    
    
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
    void copyData(byte []data, int offset, int length);
    
    /**
     * Returns the raw data buffer.
     * Use getOffset to determine where the valid data starts,
     * use getLength to determine how much valid data there is.
     * <em>access must be synchronized</em>
     *
     * @return The raw data buffer
     *
     * @see #getOffset()
     * @see #getLength()
     */
    byte [] getData();
    
    /**
     * Returns the offset to the start of valid data.
     * <em>access must be synchronized</em>
     *
     * @return The offset
     */
    int getOffset();
    
    /**
     * Returns the length of valid data.
     * <em>access must be synchronized</em>
     *
     * @return The length
     */
    int getLength();
    
    /**
     * Returns the time stamp of this sample.
     * <em>access must be synchronized</em>
     *
     * @return The timestamp
     */
    int getStamp();
}
