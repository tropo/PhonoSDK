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
    
    import mx.collaboration.xmpp.protocol.*;
    
    /**
     * Base authenticator for Digest, Plaintext and SASL authenticators. Each of
     * these authentication mechanisms requires a JID and password for the server
     * to authenticate against.
     * 
     * The JID of the user being authenticated is gathered from the XMPPStream object
     * associated with this authenticator. If the authenticator needs to update the JID that
     * the stream references (in the case of resource binding) it will update the JID in the
     * XMPPStream class before throwing an 'authenticationSuccess' event.     */
    public class PasswordAuthenticator extends Authenticator
    {
        private var _password:String;
        private var _jid:JID;
     
    	/**
    	 * @param pass The password of the user being authenticated.    	 */
    	public function PasswordAuthenticator(pass:String)
        {           
            super();
            password = pass;
        }
    	
    	public function set userJID(jid:JID):void
        {
            _jid = jid;
        }
    	public function get userJID():JID
        {
            return _jid;
        }
    	
    	public function set password(pass:String):void
        {
            _password = pass;
        }
    	public function get password():String
        {
            return _password;
        }
    }
    
}