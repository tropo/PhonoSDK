package mx.collaboration.xmpp.protocol.authenticators
{
	import mx.collaboration.xmpp.protocol.events.ChannelEvent;
	import mx.utils.Base64Decoder;
	import mx.utils.Base64Encoder;
  
	/**
	 * Authenticates the stream using the SASL.	 */
	public class SASLAuthenticator extends PasswordAuthenticator
	{
		public static var TYPE_PLAIN:String = "PLAIN";
        public static var TYPE_DIGEST:String = "DIGEST-MD5";
        public static var TYPE_ANONYMOUS:String = "ANONYMOUS";
        public static var TYPE_ANY:String = "ANY";
        public static var NAMESPACE_URI:String = "urn:ietf:params:xml:ns:xmpp-sasl";
  
		private var saslNs:Namespace = new Namespace( NAMESPACE_URI );
		private var mode:String;
		
		public function SASLAuthenticator( password:String, mode:String="ANY" )
		{
			super( password );
			this.mode = mode;
		}
				
		override public function authenticate():void
        {
        	trace("Trying SASL");
            if(stream.userJID != null || mode == TYPE_ANONYMOUS)
            {
				// Check the stream features and pick a mechanism
				var mechanisms:XMLList = channel.streamFeatures.mechanisms.mechanism + channel.streamFeatures.saslNs::mechanisms.saslNs::mechanism;
				for each (var x:XML in mechanisms) 
				{
					var reply:XML = <auth/>
					if ((mode == TYPE_DIGEST || mode == TYPE_ANY) && x == TYPE_DIGEST)
					{
						//trace("Found DIGEST");
	                    /* <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5'/>*/
						
						reply.@mechanism = TYPE_DIGEST;
						reply.@xmlns = NAMESPACE_URI;
						channel.addEventListener(ChannelEvent.DATA, this.onChannelData,false,0);
						channel.sendData(reply);
						break;
					}
					if ((mode == TYPE_PLAIN || mode == TYPE_ANY) && x == TYPE_PLAIN)
					{
						//trace("Found PLAIN");
						var response:String = String.fromCharCode(0);
						response = response + stream.userJID.node + String.fromCharCode(0) + password;
						var encoder:Base64Encoder = new Base64Encoder();
                        encoder.encode(response);
    					
						reply.appendChild(encoder.flush());						
						reply.@mechanism = TYPE_PLAIN;
						reply.@xmlns = NAMESPACE_URI;
						channel.addEventListener(ChannelEvent.DATA, this.onChannelData,false,0);
						channel.sendData(reply);
						break;
					}
					if ((mode == TYPE_ANONYMOUS || mode == TYPE_ANY) && x == TYPE_ANONYMOUS)
					{
						trace("Found ANONYMOUS");
						reply.@mechanism = TYPE_ANONYMOUS;
						reply.@xmlns = NAMESPACE_URI;
						channel.addEventListener(ChannelEvent.DATA, this.onChannelData,false,0);
						channel.sendData(reply);
						break;
					}
				}
            }
            else
            {
                authenticationFailure();
            }    
        }
        
        override public function onChannelData( event:ChannelEvent ):void
        {
       		// Is this a challenge?
            //trace("SASL: onChannelData: " + event.data.toXMLString());
            var x:XML = event.data;
            
            if (x.name().localName == "challenge" && x.name().uri == NAMESPACE_URI)
            {
            	trace("SASL Challenge");
            	var decoder:Base64Decoder = new Base64Decoder();
                decoder.decode(x.text());
    			var challenge:String = decoder.flush().toString();		
				trace(challenge);
            }
            if (x.name().localName == "success" && x.name().uri == NAMESPACE_URI)
            {
         		// Start a new stream
				channel.removeEventListener(ChannelEvent.DATA,this.onChannelData,false);
         		channel.newStream();
              	authenticationSuccess();
            }
            if (x.name().localName == "failure" && x.name().uri == NAMESPACE_URI)
            {
              	authenticationFailure();
            }
        }		
	}
}