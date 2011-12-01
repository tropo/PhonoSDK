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
    
    import flexunit.framework.*;
    import flash.events.*;
    import flash.net.*;
    import flash.utils.*;
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.channels.SocketChannel;
    import mx.collaboration.xmpp.protocol.authenticators.NonSASLAuthenticator;
    import mx.collaboration.xmpp.protocol.events.*;
    import mx.collaboration.xmpp.protocol.filters.PacketTypeFilter;
    import mx.collaboration.xmpp.protocol.packets.*;    
    import mx.collaboration.xmpp.protocol.tests.authenticators.*;
    
    public class TestPacketCollector extends TestCase
    {

		private var _stream1:XMPPStream;
		private var _stream2:XMPPStream;
		private var _pc:PacketCollector;
		
        public function testConnectXMPPStream():void
        {
            _stream1 = new XMPPStream();
            
            _stream1.userJID = new JID( TestConfig.GOOD_USER );
            _stream1.channel = new SocketChannel( TestConfig.GOOD_SERVER, TestConfig.GOOD_SERVER, TestConfig.GOOD_PORT );
            _stream1.authenticator = new NonSASLAuthenticator( TestConfig.GOOD_PASS, 
                                                              NonSASLAuthenticator.DIGEST );          
            _stream1.addEventListener( XMPPStreamEvent.CONNECT, 
                                      addAsync( checkConnectThenDisconnect, TestConfig.DEFAULT_TIMEOUT) );            
            _stream1.connect();
        }
        
        public function checkConnectThenDisconnect( e:Event ):void
        {
        	assertTrue( _stream1.isConnected() );
    		
    		_stream1.addEventListener( XMPPStreamEvent.DISCONNECT, 
    									onDisconnect );
//    		                           addAsync( checkDisconnect, 3000 ) );
    		_stream1.disconnect();

			//_pc = new PacketCollector( _stream1, new PacketTypeFilter( MessagePacket ) );
			//_pc.addEventListener( PacketCollectorEvent.PACKET,
			//					  addAsync( onPacketReceived, DEFAULT_TIMEOUT ) );
        }
        
        public function onDisconnect( event:Event ):void
        {
        	trace("DISCONNECTED");
       	}
    	
    	public function onPacketReceived( e:Event ):void
    	{
    		assertTrue(true);
    		trace("GOT FILTERED PACKET");
    	}
    	
    	
    	public function checkDisconnect( e:Event ):void
    	{
    		assertTrue( e.target.isConnected() );
    	}
    }
    
}