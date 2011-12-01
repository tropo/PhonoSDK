package mx.collaboration.xmpp.protocol.authenticators
{
	import mx.collaboration.xmpp.protocol.Authenticator;
	
	  
	 /**
	 * A token authenticator takes advantage of support for single sign on and
	 * other mechanisms where a token is provided that the client can use to
	 * authenticate and gain access to the user's actual JID.
	 * 
	 * Once token authentication is complete and the node of the user is discovered
	 * (as part of the token authentication process), the TokenAuthenticator will update
	 * the JID of the XMPPStream class being authenticated against. It will then broadcast
	 * an "authenticationSuccess" event.	 */
	public class TokenAuthenticator extends Authenticator
	{
		public function TokenAuthenticator(token:String) {};
		
		public function set token(tokenarg:String):void {};
		public function get token():String { return null };
	}

}