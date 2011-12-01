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

package mx.collaboration.xmpp.protocol.channels
{    
    import com.adobe.net.proxies.RFC2817Socket;
    
    import flash.events.Event;
    import flash.events.ProgressEvent;
    import flash.net.*;
    import flash.utils.*;
    
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.events.*;
    import mx.collaboration.xmpp.protocol.utils.*;
    import mx.messaging.AbstractConsumer;
    
    public class SocketChannel extends Channel
    {
        
        private var _socket:RFC2817Socket;
		private var _buffer:String;
        private var _bufferIndex:Number;
        
        public function SocketChannel(host:String, domain:String, port:uint=5222, useProxy:Boolean = false, proxyHost:String = null, proxyPort:uint = 80)
        {
            super(host, domain, port);
            
            _bufferIndex = 0;
            _buffer = new String();
            
//            _socket = new Socket();
			_socket = new RFC2817Socket();
			if( useProxy )
				_socket.setProxyInfo( proxyHost, proxyPort );
//				_socket.setProxyInfo( "sj-proxy.corp.adobe.com", 80 );
			
            _socket.addEventListener( Event.CONNECT, onSocketConnect );
            _socket.addEventListener( ProgressEvent.SOCKET_DATA, onSocketData );
        }
        
        private function addToBuffer( data:String ):void
        {
        	_buffer += data;
        	
        	parseBuffer();
        }       
        
        private function parseBuffer():void
        {
        	var stanza:String;        	
        	
        	if( _buffer.length == 0 )
        		return;
        	
        	if( _buffer.indexOf("<stream:stream") >= 0 )
        	{
        		var startIndex:Number = _buffer.indexOf("<stream:stream");
        		var endIndex:Number = _buffer.indexOf( ">", startIndex );
        		stanza = _buffer.substring( startIndex, endIndex + 1 );        		
        		_buffer = _buffer.substring( endIndex + 1, _buffer.length );
        		
                //trace(stream._streamID+":INCOMING:");
                //trace(stream._streamID+":"+stanza);
	                        		
        		processStanza( stanza );
        		parseBuffer();
        	}
        	else if( _buffer.indexOf("</stream:stream>") == 0 )
        	{
        		processStanza("</stream:stream>");
        	}
        	else if( _buffer.charAt(0) == "<" && _buffer.charAt(1) != "/" )
        	{
        		var spaceIndex:Number = _buffer.indexOf( " ", 1 );
        		var gtIndex:Number = _buffer.indexOf( ">", 1 );
        		var endTagNameIndex:Number = ( (gtIndex > -1) && (gtIndex < spaceIndex) ) ? gtIndex : spaceIndex;
        		var tagName:String = _buffer.substring( 1, endTagNameIndex );
        		var endTag:String = new String("</" + tagName + ">");
   	    		var endTagIndex:Number = _buffer.indexOf(endTag);
     		
        		//var nextGT:Number = _buffer.indexOf(">", endTagNameIndex);
        		
        		if( gtIndex >= 0 && _buffer.charAt( gtIndex - 1 ) == "/" )
        		{
        			stanza = _buffer.substring(0, gtIndex + 1 );
        			_buffer = _buffer.substring( gtIndex + 1, _buffer.length );
        			
	                //trace(stream._streamID+":INCOMING:");
	                //trace(stream._streamID+":"+stanza);
        			
        			processStanza( stanza );        			
        			parseBuffer();        			
        		}
        		else if( endTagIndex >= 0 )
        		{
        			stanza = _buffer.substring(0, endTagIndex + endTag.length);
        			_buffer = _buffer.substring( endTagIndex + endTag.length, _buffer.length );
        			
	                //trace(stream._streamID+":INCOMING:");
	                //trace(stream._streamID+":"+stanza);
        			
        			processStanza( stanza );        			
        			parseBuffer();
        		}

        		
        	}
        	       		
        }
        
        public function onSocketData(event:ProgressEvent):void
        {                      
            var data:String = _socket.readUTFBytes( _socket.bytesAvailable );
			addToBuffer( data );
        }
        
        public override function connect():void
        {
            super.connect();
            //trace(stream._streamID+": SocketChannel.connect() host:"+host+" port:"+port);
            try
            {
            	_socket.connect( host, port );
            }
            catch( e:Error )
            {
            	trace("ERROR CAUGHT: "+e.message);
            }
        }
        
        public function onSocketConnect(event:Event):void
        {
        	trace("SocketChannel.onSocketConnect");
            writeSocketData( getOpeningStreamTag() );
        }
                
        public override function disconnect():void
        {            
            super.disconnect();
            //writeSocketData( "</stream:stream>" );
            _socket.close();
            disconnectSuccess();
        }
        
        public override function sendData(data:XML):void
        {            
        	super.sendData( data );
            writeSocketData( data.toXMLString() );
        }
        
        public override function newStream():void
        {
        	writeSocketData(getOpeningStreamTag());
        }
        
        private function writeSocketData( data:String ):void
        {
        	// Debug.xmppOut(data.toString());
            //trace(stream._streamID+":OUTGOING:");
            //trace(stream._streamID+": "+data);
            
        	try
        	{
        		_socket.writeUTFBytes( data );
            	_socket.flush();
        	}
        	catch( e:Error )
        	{
//        		trace("ERROR WRITING DATA: "+e.message);
        	}
        }
        
    }

}