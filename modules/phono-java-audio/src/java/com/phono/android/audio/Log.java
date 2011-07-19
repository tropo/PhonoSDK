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
package com.phono.android.audio;

/**
 * A simple logger.
 */
public class Log {

    /**
     * Log all text
     */
    final public static int ALL = 9;
    /**
     * Log verbose text (and down)
     */
    final public static int VERB = 5;
    /**
     * Log debug text (and down)
     */
    final public static int DEBUG = 4;
    /**
     * Log info text (and down)
     */
    final public static int INFO = 3;
    /**
     * Log warning text (and down)
     */
    final public static int WARN = 2;
    /**
     * Log error text (and down)
     */
    final public static int ERROR = 1;
    /**
     * Log nothing
     */
    public static int NONE = 0;
    private static int _level = 1;

    /**
     * Constructor for the Log object
     */
    public Log() {
    }

    /**
     * Sets the level attribute of the Log class
     *
     * @param level The new level value
     */
    public static void setLevel(int level) {
        _level = level;
    }

    /**
     * Gets the level attribute of the Log class
     *
     * @return The level value
     */
    public static int getLevel() {
        return _level;
    }

    /**
     * error
     *
     * @param string String
     */
    public static void error(String string) {
        if (_level >= ERROR) {
            log(ERROR, string);
        }
    }

    /**
     * warn
     *
     * @param string String
     */
    public static void warn(String string) {
        if (_level >= WARN) {
            log(WARN, string);
        }
    }

    /**
     * info
     *
     * @param string String
     */
    public static void info(String string) {
        if (_level >= INFO) {
            log(INFO, string);
        }
    }

    /**
     * debug
     *
     * @param string Description of Parameter
     */
    public static void debug(String string) {
        if (_level >= DEBUG) {
            log(DEBUG, string);
        }
    }

    /**
     * verbose
     *
     * @param string Description of Parameter
     */
    public static void verb(String string) {
        if (_level >= VERB) {
            log(VERB, string);
        }
    }

    /**
     * where
     */
    public static void where() {
        Exception x = new Exception("Called From");
        x.printStackTrace();
    }

    private static void log(int level, String string) {
        String message = "" + System.currentTimeMillis() + " " + Thread.currentThread().getName() + "->" + string;
        switch (level) {
            case ERROR: {
                android.util.Log.e("Phono", message);
                break;
            }
            case DEBUG: {
                android.util.Log.d("Phono", message);
                break;
            }
            case WARN: {
                android.util.Log.w("Phono", message);
                break;
            }
            case VERB: {
                android.util.Log.v("Phono", message);
                break;
            }
            case INFO: {
                android.util.Log.i("Phono", message);
                break;
            }
        }
    }
}
