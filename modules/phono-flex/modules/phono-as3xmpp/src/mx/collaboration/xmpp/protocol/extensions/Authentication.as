package mx.collaboration.xmpp.protocol.extensions
{
	import mx.collections.ArrayCollection;
	import mx.collections.IViewCursor;
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;

	public class Authentication extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "query";
		public static var NAMESPACE_URI:String = "jabber:iq:auth";
		
		public var username:String;
		public var password:String;
		public var digest:String;
		public var resource:String;
		
		public function Authentication()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}
		
		override public function processExtension( element:XML ):void
		{
			this.element = element;
			 
			var auth:Namespace = new Namespace( NAMESPACE_URI );
			
			if( element.auth::username.length() > 0 ) username = element.auth::username;
			if( element.auth::password.length() > 0 ) password = element.auth::password;
			if( element.auth::digest.length() > 0 )   digest = element.auth::digest;
			if( element.auth::resource.length() > 0 ) resource = element.auth::resource;
		}
		
		override public function toXML():XML
		{
			var x:XML = <query></query>
				x.@xmlns = NAMESPACE_URI;
			if( username ) x.username = username;
			if( password ) x.password = password;
			if( digest ) x.digest = digest;
			if( resource ) x.resource = resource;
			
			return x;
		}
						
	}
}