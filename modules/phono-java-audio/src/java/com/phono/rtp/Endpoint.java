package com.phono.rtp;

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



import com.phono.api.Share;
import com.phono.srtplight.Log;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class Endpoint {

    private String _initialuri;
    DatagramSocket _ds;
    Share _share;

    public Endpoint(String uri) {
        _initialuri = uri;
    }
    public static Endpoint allocate() throws SocketException{
        Endpoint end = new Endpoint();
        end.allocateSocket();
        return end;
    }
    protected Endpoint()  {
    }
    // no-op for base class

    public void release() {
        if (_ds != null ){
            _ds.close();
        }
    }

    public String getLocalURI() {
        return _initialuri;
    }
    DatagramSocket getSocket() throws SocketException{
        if (_ds == null){
            int lap = 0;
            _ds = new DatagramSocket(lap);
        }
        return _ds;
    }

    void buildJSONLine(String name, String val, StringBuffer target) {
        target.append('"').append(name).append('"').append(": ");
        target.append('"').append(val).append('"').append(",\n");
    }

    public void getJSONStatus(StringBuffer target) {
        buildJSONLine("uri", getLocalURI(), target);
        buildJSONLine("type", this.getClass().getSimpleName(),target);
        if (_share != null){
            buildJSONLine("sent", _share.getSent(),target);
            buildJSONLine("rcvd", _share.getRcvd(),target);

        }
    }

    private void allocateSocket() throws SocketException {
        _ds = new DatagramSocket();
        // note - we actually listen on _all_ the interfaces, but only
        // tell the upper layers about the one we want them to use.
        _initialuri = "rtp://"+findMyLocalIPAddress()+":"+_ds.getLocalPort() ;
    }

    public void setShare(Share s) {
        if (_share != null){
            Log.error("Duplicate share on (stopping old one) "+_initialuri);
            _share.stop();
        }
        if (_ds == null){
            InetSocketAddress near = s.getNear();
            try {
                _ds = new DatagramSocket(near);
            } catch (SocketException ex) {
                Log.error("Can't allocate specified local socket "+near.toString()+" message: "+ ex);
            }
        }
        s.setSocket(_ds);

        _share = s;
    }

    public Share getShare(){
        return _share;
    }

    private String findMyLocalIPAddress() {
        Inet4Address home = null;
        Inet6Address home6 = null;
        String localAdd = null;

        // we need to be _lots_ smarter about v6 here 
        // we might chose to use the ipv6 address if it is routable
        // when the v4 address isn't
        // but for now we choose the last non-loopback v4 address
        // or if none the last v6 address.
        // which will at least work if we are in pure v6

        try {
            Enumeration nifs = NetworkInterface.getNetworkInterfaces();
            int i = 0;
            while (nifs.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nifs.nextElement();
                Enumeration ipads = ni.getInetAddresses();

                while (ipads.hasMoreElements()) {
                    InetAddress ipad = (InetAddress) ipads.nextElement();
                    if (!ipad.isLoopbackAddress()) {
                        if (ipad instanceof Inet4Address) {
                            if (home == null) {
                                home = (Inet4Address) ipad;
                                System.out.println("Using address: " + ipad.getHostAddress());
                            }
                        }
                        if (ipad instanceof Inet6Address) {
                            if (home6 == null) {
                                home6 = (Inet6Address) ipad;
                                System.out.println("Using address: " + ipad.getHostAddress());
                            }
                        }
                    }
                }

            }
        } catch (SocketException x) {
        }
        if (home != null) {
            localAdd = home.getHostAddress();
        } else {
            if (home6 != null) {
                localAdd = "["+home6.getHostAddress()+"]";
            }
        }

        return localAdd;
    }
}
