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

package mx.collaboration.xmpp.protocol.authenticators
{
    import com.adobe.crypto.SHA1;
    
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.events.*;
    import mx.collaboration.xmpp.protocol.packets.*;
    import mx.collaboration.xmpp.protocol.utils.*;
    import mx.collaboration.xmpp.protocol.extensions.Authentication;
    
    /**
     * Authenticates the XMPP Stream using the method described in JEP-0078:Non-SASL Authentication. That spec describes two
     * methods of authentication. PlaintextAuthenticator performs the plaintext based authentication described in that JEP.
     */
    public class NonSASLAuthenticator extends PasswordAuthenticator
    {
        public static var PLAINTEXT:String = "plaintext";
        public static var DIGEST:String = "digest";
        
        private var _mode:String;
        
        public function NonSASLAuthenticator(pass:String, modeArg:String)
        {
            super(pass);
            
            mode = modeArg;
        }
        
        public function set mode(modeArg:String):void
        {
            _mode = modeArg;
        }
        
        public function get mode():String
        {
            return _mode;
        }
        
        public function calculateDigest():String
        {
			return SHA1.hash( stream.sessionID + password );
        }
        
        override public function authenticate():void
        {
            if(stream.userJID != null)
            {
                var auth:Authentication = new Authentication()
                auth.username = stream.userJID.node;
                
                var iq:IQPacket = new IQPacket();
                iq.attachUUID();
                iq.jidTo = new JID(stream.channel.domain);
                iq.type = IQPacket.TYPE_GET;
                iq.addExtension( auth );
                
//               iq.stanza.query = <query xmlns="jabber:iq:auth">
//                                    <username>{stream.userJID.node}</username>
//                                  </query>;

                stream.sendPacket(iq, handleFieldRequest);
            }
            else
            {
                authenticationFailure();
            }    
        }
        
        public function handleFieldRequest( packet:Packet ):void
        {
//            var auth:Namespace = new Namespace("jabber:iq:auth");
            var jid:JID = stream.userJID;
            var auth:Authentication = Authentication( packet.getExtensionByType( Authentication ) );
            
            var iq:IQPacket = new IQPacket();
            iq.attachUUID();
            iq.jidTo = new JID( channel.domain );
            iq.type = IQPacket.TYPE_SET;
            
           	var a:Authentication = new Authentication();
           	
            if( auth && auth.password != null && _mode == PLAINTEXT)
            {
            	a.username = jid.node;
            	a.password = password;
            	a.resource = jid.resource;
                
                iq.addExtension( a );
                stream.sendPacket( iq, handleAuthenticationRequest );
            }
            else if( auth && auth.digest != null && _mode == DIGEST)
            {
            	a.username = jid.node;
            	a.digest = calculateDigest();
            	a.resource = jid.resource;
                
                iq.addExtension( a );
                stream.sendPacket( iq, handleAuthenticationRequest );
            }
            else
            {
                authenticationFailure();
            }

        }
        
        public function handleAuthenticationRequest( packet:Packet ):void
        {
            if( !packet.error )
            {
                authenticationSuccess();
            }
            else
            {
                authenticationFailure();
            }
        }        
        
    }
    
}