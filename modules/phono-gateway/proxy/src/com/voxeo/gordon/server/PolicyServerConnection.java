/*
 * PolicyServerConnection.java
 *
 * This file is part of a tutorial on making a chat application using Flash
 * for the clients and Java for the multi-client server.
 * 
 * View the tutorial at http://www.broculos.net/
 */

package com.voxeo.gordon.server;

import java.io.*;
import java.net.*;

import org.apache.log4j.Logger;

/**
 * PolicyServerConnection reads policy requests from a client's socket and writes the server policy.
 *
 * @author Nuno Freitas (nunofreitas@gmail.com)
 */
public class PolicyServerConnection extends Thread {
    protected Socket socket;
    protected BufferedReader socketIn;
    protected PrintWriter socketOut;
    private static final Logger LOG = Logger.getLogger(PolicyServerConnection.class);
    
    /**
     * Creates a new instance of PolicyServerConnection.
     *
     * @param socket client's socket connection
     */
    public PolicyServerConnection(Socket socket) {
        this.socket = socket;
    }
    
    /**
     * Roots a debug message to the main application.
     * 
     * @param msg the debug message to be sent to the main application
     */
    protected void debug(String msg) {
        LOG.debug("PolicyServerConnection (" + this.socket.getRemoteSocketAddress() + ") " + msg);
    }

    /**
     * Create a reader and writer for the socket and call readPolicyRequest.
     */
    public void run() {
        try {
            this.socketIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.socketOut = new PrintWriter(this.socket.getOutputStream(), true);
            readPolicyRequest();
        }
        catch (Exception e) {
            debug("Exception (run): " + e.getMessage());
        }
    }
    
    /**
     * Reads a string from the client and if it is a policy request we write the policy, then we close the connection.
     */
    protected void readPolicyRequest() {
        try {
            String request = read();
            debug("client says '" + request + "'");
            
            if (request.equals(PolicyServer.POLICY_REQUEST)) {
               writePolicy();
            }
        }
        catch (Exception e) {
            debug("Exception (readPolicyRequest): " + e.getMessage());
        }
        finalize();
    }
    
    /**
     * Writes the policy of the server.
     */
    protected void writePolicy() {
        try {
            this.socketOut.write(PolicyServer.POLICY_XML + "\u0000");
            this.socketOut.close();
            debug("policy sent to client");
        }
        catch (Exception e) {
            debug("Exception (writePolicy): " + e.getMessage());
        }
    }

    /**
     * Safely read a string from the reader until a zero character is received or the 200 character is reached.
     *
     * @return the string read from the reader.
     */
    protected String read() {
        StringBuffer buffer = new StringBuffer();
        int codePoint;
        boolean zeroByteRead = false;
        
        try {
            do {
                codePoint = this.socketIn.read();

                if (codePoint == 0) {
                    zeroByteRead = true;
                }
                else if (Character.isValidCodePoint(codePoint)) {
                    buffer.appendCodePoint(codePoint);
                }
            }
            while (!zeroByteRead && buffer.length() < 200);
        }
        catch (Exception e) {
            debug("Exception (read): " + e.getMessage());
        }
        
        return buffer.toString();
    }

    /**
     * Closes the reader, the writer and the socket.
     */
    protected void finalize() {	 
        try {
            this.socketIn.close(); 
            this.socketOut.close();
            this.socket.close();
            debug("connection closed");
        }
        catch (Exception e) {
            debug("Exception (finalize): " + e.getMessage());
        }
    }
}
