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

package mx.collaboration.xmpp.protocol.tests
{
    
    import flash.events.Event;
    import flash.events.ProgressEvent;
    import flash.net.*;
    import flash.utils.*;
    
    import flexunit.framework.*;
    
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.authenticators.NonSASLAuthenticator;
    import mx.collaboration.xmpp.protocol.channels.SocketChannel;
    import mx.collaboration.xmpp.protocol.events.*;
    import mx.collaboration.xmpp.protocol.tests.authenticators.*;;
    
    public class TestXMPPStream extends TestCase
    {
    
        public function TestXMPPStream( method : String = null )
        {
            super(method);
        }
    
        public function testCreateXMPPStream():void
        {
        	var x:XMPPStream = new XMPPStream();
        	
        	assertNotNull(x);
        	assertNotUndefined(x);	
        }
        
        public function testSetJID():void
        {
            var x:XMPPStream = new XMPPStream();
            x.userJID = new JID("node@domain.com/resource");
            var clone:JID = new JID("node@domain.com/resource");
            
            assertEquals(x.userJID,"node@domain.com/resource");
            if(x.userJID == clone)
            {
                assertEquals(true,true);
            }
        }
        
        private var _socket1:Socket;
        private var _socket2:Socket;
        private var _socket1init:Boolean;
        private var _socket2init:Boolean;
        
        public function testSocket1():void
        {
        	_socket1init = false;        	
        	        	
            _socket1 = new Socket();
            _socket1.addEventListener( "connect", 
            						   addAsync(onSocket1Connect, 15000) );
            _socket1.connect( TestConfig.GOOD_SERVER, 5222 );               
        }
        
        public function testSocket2():void
        {
        	_socket2init = false;
        	
            _socket2 = new Socket();

            _socket2.addEventListener( "connect", 
            						   addAsync(onSocket2Connect, 15000) );
            _socket2.connect( TestConfig.GOOD_SERVER, 5222 );    
        }
        
        public function onSocket1Connect( event:Event ):void
        {
        	assertTrue(true);
        	
	        _socket1.addEventListener( "socketData", 
						   			   addAsync(onSocket1Data, 15000) );
        	_socket1.writeUTFBytes("<stream:stream to='"+TestConfig.GOOD_SERVER+"' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>");
        	_socket1.flush();   		        	       
        }
        
        public function onSocket1Data( event:Event ):void
        {        	
        	assertTrue(true);
        }

        public function onSocket2Connect( event:Event ):void
        {
        	assertTrue(true);
        	
        	_socket2.addEventListener( "socketData", 
            						   addAsync(onSocket2Data, 15000) );
        	_socket2.writeUTFBytes("<stream:stream to='"+TestConfig.GOOD_SERVER+"' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' version='1.0'>");
        	_socket2.flush();
        }
        
        public function onSocket2Data( event:Event ):void
        {
        	assertTrue(true);
        }
    	
        public function testConnectXMPPStream():void
        {
            var stream:XMPPStream = new XMPPStream();
            
            stream.userJID = new JID( TestConfig.GOOD_USER );
            stream.channel = new SocketChannel( TestConfig.GOOD_SERVER, TestConfig.GOOD_SERVER, TestConfig.GOOD_PORT );
            stream.authenticator = new NonSASLAuthenticator( TestConfig.GOOD_PASS, 
                                                             NonSASLAuthenticator.DIGEST );          
            stream.addEventListener( XMPPStreamEvent.CONNECT, 
                                     addAsync( checkConnectThenClose, TestConfig.DEFAULT_TIMEOUT) );            
            stream.connect();
        }
        
        public function checkConnectThenClose( e:Event ):void
        {
        	assertTrue( e.target.isConnected() );
    		
    		//e.target.addEventListener( XMPPStreamEvent.DISCONNECT, 
    		//                           addAsync( checkDisconnect, 3000) );
    		e.target.disconnect();
        }
    	
    	public function checkDisconnect( e:Event ):void
    	{
    		assertTrue( e.target.isConnected() );
    	}
    }
    
}