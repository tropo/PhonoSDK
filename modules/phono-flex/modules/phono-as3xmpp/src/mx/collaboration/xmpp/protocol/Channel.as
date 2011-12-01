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
    import flash.utils.*;
    
    import mx.collaboration.xmpp.protocol.events.*;
    
    /**
     * Event generated when a new data has been received
     * by the channel.
     */
    [Event(ChannelEvent.DATA)]
    
    /* The channel has successfully connected to the server. This
     * does not imply that strem initiation, authentication, or 
     * resource binding has occured.
     */
    [Event(ChannelEvent.CONNECT)]
    
    /**
     * The channel has disconnected with the XMPP server.
     */
    [Event(ChannelEvent.DISCONNECT)]    
    
    /**
     * Channels are used to define how traffic is sent and received by the
     * server. By default we will support an XMPP socket channel (RFC 3920,
     * based upon the new Socket in Maelstrom) and an HTTP Binding channel 
     * (JEP-0124:HTTP Binding). Other channels may be supported in the
     * future such as HTTP tunneling (JEP-0025:Jabber HTTP Polling).
     * 
     * This is an abstract class and it is required that it be extended to
     * be useful.
     * 
     * @see xmpp.channels.SocketChannel
     * @see xmpp.channels.httpbinding.HTTPBindingChannel     */
    public class Channel extends EventDispatcher
    {	
                        
        private var _host:String;
        private var _domain:String;
        private var _port:uint;
        private var _availableChannels:Array;
        private var _connected:Boolean;
        private var _streamInfo:XML;
        private var _streamFeatures:XML;
        private var _waitForFeatures:Boolean;
    	//public var stream:XMPPStream;
    
    	public var tracePackets:Boolean;
    
        public function Channel(host:String, domain:String="", port:uint=5222)
        {
            super();
            _connected = false;
            _host = host;
            _port = port;
            _domain = domain;
        }
    	
        /**
    	 * Sends a data packet to the XMPP server    	 */
    	public function sendData(data:XML):void
        {
        	if( tracePackets )
        	{
		        trace("\nOUTGOING:");
	            trace(data.toXMLString());            
	        }
        }
        
		/**
         * @private
         */                
        public function dispatchDataEvent(data:XML):void
        {
            dispatchEvent( new ChannelEvent(ChannelEvent.DATA, true, false, data) );
        }
        
        /**
         * @private         */
        public function dispatchErrorEvent(data:XML):void
        {
        	dispatchEvent( new ChannelEvent(ChannelEvent.ERROR, true, false, data) );
        }
        
        /**
         * Connect the channel to the XMPP server
         */
        public function connect():void
        {
        }
        
        /**
         * @private
         */
        public function onStreamInitiation():void
        {       
            if(streamInfo.@version == "1.0")
            {                
                _waitForFeatures = true;
            }
            else
            {
                _waitForFeatures = false;
                connectSuccess();
            }
        }
        
        /**
         * @private
         */
        public function onStreamFeatures():void
        {           
            if(_waitForFeatures)
            {
                _waitForFeatures = false;
                connectSuccess();
            }
        }
        
        /**
         * @private         */
        public function onStreamErrors(data:XML):void
        {
        	
        }
        
        /**
         * @private
         */
        public function processStanza(data:String):void
        {            
            var streamNS:Namespace = new Namespace("http://etherx.jabber.org/streams");        
            var dataXML:XML;
            
            if( data.indexOf("<stream:stream") >= 0 ) //The opening stream tag
            {
                data = data + "</stream:stream>";
                streamInfo = new XML(data);
                
                if( tracePackets )
                {
	                trace("\nINCOMING:");
	                trace(streamInfo.toXMLString());
                }
                
                onStreamInitiation();               
            }
            else if( data.indexOf("</stream:stream>") >= 0 ) //The closing stream tag
            {
                data = "<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>"+data;
                dataXML = new XML( data );
				
				if( tracePackets )
				{
                	trace("\nINCOMING:");
	                trace(dataXML.toXMLString());
	   			}
	   
				disconnect();
            }          
            else if( data.indexOf("<stream:features") >= 0 ) //The stream features tag
            {
                //TODO Fix this hackery here that adds the stream namespace to the features tag.
                dataXML = new XML("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>"+data+"</stream:stream>");               
                streamFeatures = new XML( dataXML.streamNS::features[0].toString() ); 
                
                if( tracePackets )
                {
                	trace("\nINCOMING:");
                	trace(dataXML.toXMLString());
                }
                
                onStreamFeatures();              
            }
            else if( data.indexOf("<stream:error") >= 0 ) //The stream error tag
            {
                var wrappedXML:XML = new XML("<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>"+data+"</stream:stream>");
                dataXML = new XML( wrappedXML.streamNS::error[0].toString() );
                
                if( tracePackets )
                {
                	trace("\nINCOMING:");
                	trace(dataXML.toXMLString());
                }
                
                dispatchErrorEvent( dataXML );
            }
            else //All other tags: message, presence, iq
            {                 
                dataXML = new XML( data );
                
                if( tracePackets )
                {
                	trace("\nINCOMING:");
                	trace(dataXML.toXMLString());
               	}
               	
                dispatchDataEvent( dataXML );
            }        	
        }
        
        /**
         * Internal method that is called by classes that extend channel. Signals the
         * initiation of a stream.
         * 
         * @param data The opening tag of the stream that contains the session id and
         * supported version number.
         */
        public function connectSuccess():void
        {
            _connected = true;
            dispatchEvent( new ChannelEvent( ChannelEvent.CONNECT ) );
        }
        
        /**
         * Disconnects the channel from the XMPP server.
         */
        public function disconnect():void
        {
        }
        
        /**
         * @private
         */
        public function disconnectSuccess():void
        {        
        	if( isConnected())
        	{
        		_connected = false;
            	dispatchEvent( new ChannelEvent( ChannelEvent.DISCONNECT ) );
        	}
        }
        
    	/**
    	 * The host name of the XMPP server    	 */
        public function get host():String
    	{
            return _host;
        }
    	
    	/**
    	 * The domain of the XMPP server
    	 */
        public function get domain():String
    	{
            return _domain;
        }
    	
    	/**
    	 * The port of the XMPP server    	 */
        public function get port():uint
        {
            return _port;
        }
        
        /**
         * This is the representation of the initial stream
         * tag that is sent during stream initiation         */
        public function set streamInfo( streamInfoArg:XML ):void
        {
            _streamInfo = streamInfoArg;
        }
        
        public function get streamInfo():XML
        {
            return _streamInfo;
        }
        
        /**
         * This is the representation of stream features that
         * this stream supports. This information may not always
         * be available and is dependent upon server features
         * that are supported.         */
        public function set streamFeatures( streamFeaturesArg:XML ):void
        {
            _streamFeatures = streamFeaturesArg;            
        }
        
        public function get streamFeatures():XML
        {
            return _streamFeatures;
        }
        
        /**
         * Is the current channel connected to the server.
         */
        public function isConnected():Boolean
        {
            return _connected;
        }
        
        /**
         * Get the opening stream tag
         */
        public function getOpeningStreamTag():String
        {
            return String("<stream:stream to='"+_domain+"' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>");
        }
        
        public function newStream():void
        {
        	
        }

    }
    
}