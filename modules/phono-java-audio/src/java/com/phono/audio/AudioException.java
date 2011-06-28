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
 * The AudioException that can be thrown as part of the audio implementation of 
 * this stack.
 *
 */
@SuppressWarnings("serial")
public class AudioException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>AudioException</code> without
     * detail message.
     */
    public AudioException() {
    }
    
    
    /**
     * Constructs an instance of <code>AudioException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public AudioException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause
     */
    public AudioException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param cause the cause
     */
    public AudioException(Throwable cause) {
        super(cause);
    }    
}
