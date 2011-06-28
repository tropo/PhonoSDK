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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 * A simple logger.
 */
public class Log {

    /**
     * Log all text
     */
    public static int ALL = 9;
    
    
    /**
     * Log verbose text (and down)
     */
    public static int VERB = 5;
    
    /**
     * Log debug text (and down)
     */
    public static int DEBUG = 4;
    
    /**
     * Log info text (and down)
     */
    public static int INFO = 3;
    
    /**
     * Log warning text (and down)
     */
    public static int WARN = 2;
    
    /**
     * Log error text (and down)
     */
    public static int ERROR = 1;
    
    /**
     * Log nothing
     */
    public static int NONE = 0;
    
    private final static String POST_URL =
        "http://www.phono.com/audio_config_error";

    private static int _level = 1;
    private static boolean _doLogDatabase = true;
    private static URL _post_url = null;
    
    // To log in database:
    private static String _stack_version = "?";
    private static String _bind_host = "?";
    private static String _local_hostname = "?";
    private static String _http_agent = "?";
    private static String _java_version = "?";
    private static String _java_vendor = "?";
    private static String _os_arch = "?";
    private static String _os_name = "?";
    private static String _os_version = "?";
    private static String _user_country = "?";
    private static String _user_language = "?";
    
    
    /**
     * Constructor for the Log object
     */
    public Log() {
        
    }
    
    public static void setBindHost(String bind_host) {
        _bind_host = bind_host;
    }
    
    public static void setVersion(String version) {
        _stack_version = version;
    }
    
    
    /**
     * Sets the level attribute of the Log class
     *
     * @param level The new level value
     */
    public static void setLevel(int level) {
        _level = level;
        
//    IP address (browser)
//    http.agent = Mozilla/4.0 (Linux 2.6.13-15.18-smp)
//    java.version = 1.5.0_06
//    java.vendor = Sun Microsystems Inc.
//    os.arch = i386
//    os.name = Linux
//    os.version = 2.6.13-15.18-smp
//    user.country = GB
//    user.language = en
        
        try {
            InetAddress inetA = InetAddress.getLocalHost();
            _local_hostname = inetA.getHostName();
        } catch (UnknownHostException ex) {
        }
        
        try {
            _http_agent = System.getProperty("http.agent");
            _java_version = System.getProperty("java.version");
            _java_vendor = System.getProperty("java.vendor");
            _os_arch = System.getProperty("os.arch");
            _os_name = System.getProperty("os.name");
            _os_version = System.getProperty("os.version");
            _user_country = System.getProperty("user.country");
            _user_language = System.getProperty("user.language");
        } catch (java.lang.SecurityException exc) {
        }
        
        if (_post_url == null) {
            try {
                _post_url = new URL(POST_URL);
            } catch (MalformedURLException ex) {
                Log.debug("Log.setLevel(): MalformedURLException: " + ex.getMessage());
            }
        }
    }
    
    
    /**
     * Gets the level attribute of the Log class
     *
     * @return The level value
     */
    public static int getLevel() {
        return _level;
    }
    
    
    public static void setLogDatabase(boolean dolog) {
        _doLogDatabase = dolog;
    }
    
    public static boolean isLogDatabase() {
        return _doLogDatabase;
    }
    
    
    /**
     * error
     *
     * @param string String
     */
    public static void error(String string) {
        if (_level >= ERROR) {
            log("ERROR", string);
        }
    }
    
    
    /**
     * warn
     *
     * @param string String
     */
    public static void warn(String string) {
        if (_level >= WARN) {
            log("WARN", string);
        }
    }
    
    
    /**
     * info
     *
     * @param string String
     */
    public static void info(String string) {
        if (_level >= INFO) {
            log("INFO", string);
        }
    }
    
    
    /**
     * debug
     *
     * @param string Description of Parameter
     */
    public static void debug(String string) {
        if (_level >= DEBUG) {
            log("DEBUG", string);
        }
    }
    
    
    /**
     * verbose
     *
     * @param string Description of Parameter
     */
    public static void verb(String string) {
        if (_level >= VERB) {
            log("VERB", string);
        }
    }
    
    
    
    
    /**
     * where
     */
    public static void where() {
        Exception x = new Exception("Called From");
        x.printStackTrace();
    }
    
    
    public static void database(Throwable throwable, Object object,
            String method_name) {
        String class_name = object.getClass().getName();
        database(throwable, class_name, method_name);
    }
        
    static void sendMessage(Throwable throwable,String class_name, String method_name  ){
        String exception_class = throwable.getClass().getName();
    
            String exception_message = throwable.getMessage();
            
            Log.debug("Log.database():");
            Log.debug("Stack Version = " + _stack_version);
            Log.debug("Bind Host = " + _bind_host);
            Log.debug("Local Hostname = " + _local_hostname);
            Log.debug("Http Agent = " + _http_agent);
            Log.debug("Java Version = " + _java_version);
            Log.debug("Java Vendor = " + _java_vendor);
            Log.debug("Os Arch = " + _os_arch);
            Log.debug("Os Name = " + _os_name);
            Log.debug("Os Version = " + _os_version);
            Log.debug("User Country = " + _user_country);
            Log.debug("User Lang = " + _user_language);
            
            Log.debug("Exc Class = " + exception_class);
            Log.debug("Exc Message = " + exception_message);
            Log.debug("Class Name = " + class_name);
            Log.debug("Method Name = " + method_name);
            
            StringBuffer buf = new StringBuffer();
            buf.append("stack_version=").append(_stack_version);
            buf.append("&bind_host=").append(_bind_host);
            buf.append("&local_hostname=").append(_local_hostname);
            buf.append("&http_agent=").append(_http_agent);
            buf.append("&java_version=").append(_java_version);
            buf.append("&java_vendor=").append(_java_vendor);
            buf.append("&os_arch=").append(_os_arch);
            buf.append("&os_name=").append(_os_name);
            buf.append("&os_version=").append(_os_version);
            buf.append("&user_country=").append(_user_country);
            buf.append("&user_language=").append(_user_language);
            buf.append("&exception_class=").append(exception_class);
            buf.append("&exception_message=").append(exception_message);
            buf.append("&class_name=").append(class_name);
            buf.append("&method_name=").append(method_name);

            BufferedReader inStream = postToUrl(_post_url, buf.toString());
            String line;
            if (inStream != null) {
                try {
                    line = inStream.readLine();
                    Log.debug(line);
                    while (line != null) {
                        line = inStream.readLine();
                        Log.debug(line);
                    }
                } catch (IOException ex) {
                    Log.debug("Log.database(): IOException: " + ex.getMessage());
                }
                try {
                    inStream.close();
                } catch (IOException ex) {
                    Log.debug("Log.database(): IOException: " + ex.getMessage());
                }
            }
        }

    public static void database(Throwable throwable, String class_name,
            String method_name) {
        if (_doLogDatabase && _post_url != null) {
            // call sendMessge on a new thread some how....
            Runner r = new Runner(throwable,class_name,method_name);
            r.start();
        }

    }
    
    private static class Runner extends Thread {
        Throwable _throwable ;
        String _class_name;
        String _method_name;
        Runner(Throwable throwable, String class_name,
            String method_name ){
            _throwable = throwable;
            _class_name = class_name;
            _method_name = method_name;
        }
        public void run(){
            sendMessage(_throwable,_class_name,_method_name);
        }
    }
    
    /**
     * Sends a command to an URL via the POST method.
     * The command should have the following format:
     *      k1=v1&amp;k2=v2&amp;k3=v3
     *
     * @return the input stream from the URL to read the answer
     */
    private static BufferedReader postToUrl(URL url, String command) {
        BufferedReader inStream = null;
        
        try {
            URLConnection conn;
            conn = url.openConnection();
            
            // No cache may be used, else we might get an old value !!
            conn.setUseCaches(false);
            
            // After long fiddling with Netscape, I found out that the
            // content-type has to be set, else the servlet somehow cannot
            // decode the parameters!!!!
            conn.setRequestProperty("Content-type",
                    "application/x-www-form-urlencoded");
            
            // Use the POST method
            conn.setDoInput(true);
            conn.setDoOutput(true);
            
            // Write the parameters to the URL
            PrintWriter outStream;
            outStream = new PrintWriter(conn.getOutputStream(), true);
            outStream.write(command);
            outStream.close();
            
            // Read the answer
            inStream = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            Log.debug("Log.database(): IOException: " + e.getMessage());
            _doLogDatabase = false;
        }
        return inStream;
    }
    
    
    private static void log(String level, String string) {
        String message = level + ": " + System.currentTimeMillis() + " " + Thread.currentThread().getName() + "->" + string;
        System.out.println(message);
    }
    
}

