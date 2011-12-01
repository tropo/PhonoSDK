/*
	Adobe Systems Incorporated(r) Source Code License Agreement
	Copyright(c) 2005 Adobe Systems Incorporated. All rights reserved.
	
	Please read this Source Code License Agreement carefully before using
	the source code.
	
	Adobe Systems Incorporated grants to you a perpetual, worldwide, non-exclusive, 
	no-charge, royalty-free, irrevocable copyright license, to reproduce,
	prepare derivative works of, publicly display, publicly perform, and
	distribute this source code and such derivative works in source or 
	object code form without any attribution requirements.  
	
	The name "Adobe Systems Incorporated" must not be used to endorse or promote products
	derived from the source code without prior written permission.
	
	You agree to indemnify, hold harmless and defend Adobe Systems Incorporated from and
	against any loss, damage, claims or lawsuits, including attorney's 
	fees that arise or result from your use or distribution of the source 
	code.
	
	THIS SOURCE CODE IS PROVIDED "AS IS" AND "WITH ALL FAULTS", WITHOUT 
	ANY TECHNICAL SUPPORT OR ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING,
	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
	FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  ALSO, THERE IS NO WARRANTY OF 
	NON-INFRINGEMENT, TITLE OR QUIET ENJOYMENT.  IN NO EVENT SHALL MACROMEDIA
	OR ITS SUPPLIERS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
	OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
	WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOURCE CODE, EVEN IF
	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package mx.collaboration.xmpp.protocol
{
    
    import flash.events.*;
    import flash.net.*;
    import flash.utils.*;
    
    import mx.collaboration.xmpp.protocol.channels.*;
    import mx.collaboration.xmpp.protocol.events.*;
    import mx.collaboration.xmpp.protocol.extensions.Bind;
    import mx.collaboration.xmpp.protocol.extensions.Session;
    import mx.collaboration.xmpp.protocol.extensions.ExtensionManager;
    import mx.collaboration.xmpp.protocol.packets.*;
    
    use namespace xmpp_internal;
    
    /**
     * A stream error has occured.     */
    [Event("error")]
    
    /**
     * You have connected to the server and been authenticated using the
     * mechanism specified in setAuthenticator.
     */
    [Event("connect")]
    
    /**
     * This connection to the XMPP server has been closed.
     */
    [Event("disconnect")]
    
    /**
     * A new packet has been received. There is no filter on this
     * event, all packets that are received will throw a data event.
     * To get a filtered data collection mechanism see XMPPStream.createPacketCollector
     * and xmpp.PacketCollector.
     */
    [Event("data")]	
    	
    /**
     * An XMPPStream represents a connection to an XMPP server and allows receiving
     * and sending of packets to that server.
	   	   <pre>
       var stream = new XMPPStream();
       // replace [AT] with the real symbol, ASDocs freaked out
       stream.authenticator = new DigestAuthenticator("ddura[AT]jabberserver.com/Trillian","secretpass");
       stream.channel = new SocketChannel("jabberserver.com",5222);
       
       stream.addEventListener(StreamEvent.CONNECT_SUCCESS,onConnectSucceed);
       stream.addEventListener(StreamEvent.CONNECT_FAILURE,onConnectFail);
       stream.addEventListener(StreamEvent.DISCONNECT,onDisconnect);

       var collector = stream.createPacketCollector( new filters.IDFilter("myid") );
       collector.addEventListener(PacketCollectorEvent.DATA, onReceivePacketWithMyID);
       
       stream.connect("jabberserver.com",5222);
       
       function onConnectSucceed(event:StreamEvent)
       {
       		trace("We are connected and authenticated!");
       }
       
       function onConnectFail(event:StreamEvent)
       {
       		trace("Connection has failed. Either we can't connect to the server, or couldn't authenticate.");
       }
       
       function onDisconnect(event:StreamEvent)
       {
       		trace("We were disconnected from the server.");
       }
       
       function onReceivePacketWithMyID(event:PacketCollectorEvent)
       {
       		trace("I got a packet with my ID!");
       		var packetwithid:Packet = event.packet;
       }
       </pre>
     */
    public class XMPPStream extends EventDispatcher
    {	
    	private var _channel:Channel;
        private var _connected:Boolean;
    	private var _host:String;
    	private var _port:uint;
        private var _jid:JID;	
        private var _availableChannels:Array;
        private var _registry:ChannelRegistry;
        private var _authenticator:Authenticator;
        private var _callbackMap:Array;
        private var _sessionID:String;
        private var _isAuthenticated:Boolean = false;
            	
        private var bindNs:Namespace = new Namespace("urn:ietf:params:xml:ns:xmpp-bind");
        private var sessionNs:Namespace = new Namespace("urn:ietf:params:xml:ns:xmpp-session");
            	
    	private static var _streamCount:Number = 0;
    	
    	public var _streamID:Number;
    	public var extensionManager:ExtensionManager;
    
    	public function XMPPStream():void
    	{
    		_streamCount++;
    		_streamID = _streamCount;
            _callbackMap = new Array();

			extensionManager = new ExtensionManager();
    	}
    	
    	/**
    	 * Connect to the XMPP server using the available channel. The default channels
    	 * are specified in the setChannel description. Other channels may be specified
    	 * by using the setChannel method.
    	 */
    	public function connect():void
    	{
//    		trace(_streamID+": XMPPStream.connect()");
            _channel.connect();
    	}        
        
        /**
         * @private         */
        public function authenticate(auth:Authenticator):void
        {
//        	trace(_streamID+": XMPPStream.authenticate()");
            _authenticator = auth;
            _authenticator.stream = this;
            _authenticator.channel = _channel;
            _authenticator.addEventListener( AuthenticatorEvent.SUCCESS, onAuthenticationSuccess );
            _authenticator.addEventListener( AuthenticatorEvent.FAILURE, onAuthenticationFailure );
            _authenticator.authenticate();
        }
        
    	/**
    	 * Returns true if the connection is active.
    	 */
    	public function isConnected():Boolean
    	{
    		return _connected;
    	}
    	
    	/**
    	 * Disconnects from the XMPP server, closing the stream.
    	 */
    	public function disconnect():void
    	{
    		_channel.disconnect();
    	}
    	
    	/**
    	 * Sends a packet to the XMPP Server.
    	 */
    	public function sendPacket(packet:Packet, callbackFunction:Function=null ):void
    	{
            if(callbackFunction != null)
                _callbackMap[packet.id] = callbackFunction;
    		_channel.sendData(packet.toXML());
    	}
        
        /**
         * Returns the callback function that correlates with this packet id.
         */
        private function getCallbackFunction(id:String):Function
        {
            return _callbackMap[id];
        }
    	
    	/**
    	 * Sets the specified authenticatication class for the connection. When connect is
    	 * called, and the stream is established with the server, the authenticator takes
    	 * over and authenticates the client using the credentials provided in the authenticator.
    	 * We might default to a certian set of authenticators. XMPP supports numerous authentication
    	 * mechanisms, including:
    	 * - Plain Text
    	 * - MD5
    	 * - SASL (Digest-MD5, Anonymous, External, etc.)
    	 */
    	public function set authenticator(auth:Authenticator):void
    	{
    		_authenticator = auth;
    	}
    	
    	public function get authenticator():Authenticator
    	{
    		return _authenticator;
    	}

		/**
		 * Sets the channel that this stream will use.		 */
    	public function set channel(channelArg:Channel):void
    	{
    		_channel = channelArg;
    		
      		_channel.addEventListener(ChannelEvent.CONNECT,onChannelConnect);
            _channel.addEventListener(ChannelEvent.DISCONNECT, onChannelDisconnect);
            _channel.addEventListener(ChannelEvent.DATA, onChannelData);
            _channel.addEventListener(ChannelEvent.ERROR, onChannelError);
    	}
    	
    	public function get channel():Channel
    	{
    		return _channel;
    	}

    	/*
        public function set host(hostArg:String)
        {
            _host = hostArg;            
        }
        
        public function get host():String
        {
            return _host;
        }
        
        public function set port(portArg:uint)
        {
            _port = portArg;
        }
        
        public function get port():uint
        {
            return _uint;
        }
		*/
		
        /**
         * @private
         */
        public function set sessionID(sessionIDArg:String):void
        {
            this._sessionID = sessionIDArg;
        }
        
        /**
         * @private
         */
        public function get sessionID():String
        {
            return _sessionID;
        }
        
    	/**
    	 * The JID of the user connecting to the XMPP server. Sometimes, when authenticating, the
    	 * full JID is not known until authentication and resource binding are complete. If the
    	 * JID is changed after these steps, then this JID will be updated at that time by the
    	 * authenticator.    	 */
    	public function set userJID(jid:JID):void
        {
            _jid = jid;
        }
        
    	public function get userJID():JID
        {
            return _jid;
        }
    	
        /**
         * @private
         */    	
    	public function onChannelConnect(event:ChannelEvent):void
    	{
            _sessionID = _channel.streamInfo.@id.toString();
            
            // If the authenticator is already specified, then lets
            // go ahead and begin authentication.
            if(_authenticator != null && !_isAuthenticated)
            {            	
                authenticate(_authenticator);
            }

			// Do we need to ask for a resource binding?
			if(channel.streamFeatures.bindNs::bind.length() > 0)
			{
				var bindIq:IQPacket = new IQPacket();
				bindIq.type = IQPacket.TYPE_SET;
				bindIq.id = "bind:1";
				var bindExt:Bind = new Bind();
				if (_jid && _jid.resource) bindExt.resource = _jid.resource;
				bindIq.addExtension(bindExt);
				sendPacket(bindIq, onBind);
			}
    	}
    	
    	private function onBind(packet:Packet):void
    	{
    		if (packet.hasExtensionType(Bind))
    		{
    			var bind:Bind = packet.getExtensionByType(Bind) as Bind;
    			if (bind.jid) {
    				_jid = bind.jid;
       			}
    			
    			// Do we need to ask for a session?
    			if(channel.streamFeatures.sessionNs::session.length() > 0) {
    				var sessionIq:IQPacket = new IQPacket();
					sessionIq.type = IQPacket.TYPE_SET;
					sessionIq.id = "session:1";
					var sessionExt:Session = new Session();
					sessionIq.addExtension(sessionExt);
					sendPacket(sessionIq, onSession);
    			}
				else 
				{
					// All done
					dispatchEvent( new XMPPStreamEvent(XMPPStreamEvent.CONNECT) );
				}
    		}
    	}
    	
    	private function onSession(packet:Packet):void
    	{
    		if (packet.type == IQPacket.TYPE_RESULT) {
    			// All done
				dispatchEvent( new XMPPStreamEvent(XMPPStreamEvent.CONNECT) );
    		} else {
    			dispatchEvent( new XMPPStreamEvent(XMPPStreamEvent.ERROR) );
    		}
    	}
    	
        /**
         * @private
         */    	
    	public function onChannelDisconnect(event:ChannelEvent):void
    	{
            _connected = false;
    		dispatchEvent( new XMPPStreamEvent(XMPPStreamEvent.DISCONNECT) );
    	}
        
        /**
         * @private
         */        
        public function onChannelData( event:ChannelEvent ):void
        {
            var packet:Packet = Packet.createPacket( event.data, this );
            
            if(packet != null)
            {
                var callback:Function = getCallbackFunction(packet.id);
                if(callback != null)
                    callback(packet);
                    
                if( _connected )
                	dispatchEvent( new XMPPStreamEvent( XMPPStreamEvent.DATA, true, false, packet ) );
            }
        }
        
        /**
         * @private         */
        public function onChannelError( event:ChannelEvent ):void
        {
        	var error:* = event.data;
        	
        	dispatchEvent( new XMPPStreamEvent( XMPPStreamEvent.ERROR, true, false, null, error ) );
        }
        
        /**
         * @private
         */        
        public function onAuthenticationSuccess(event:AuthenticatorEvent):void
        {
            _connected = true;
            _isAuthenticated = true;
            
            // Will become connected on session or bind success
            //if (_jid) dispatchEvent( new XMPPStreamEvent(XMPPStreamEvent.CONNECT) );
        }
        
        /**
         * @private
         */        
        public function onAuthenticationFailure(event:AuthenticatorEvent):void
        {
            _isAuthenticated = false;
        }
    }

} //package mx.collaboration.xmpp.protocol