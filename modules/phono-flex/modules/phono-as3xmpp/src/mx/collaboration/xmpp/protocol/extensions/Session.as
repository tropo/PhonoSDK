package mx.collaboration.xmpp.protocol.extensions
{
	import mx.collaboration.xmpp.protocol.JID;
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;

	public class Session extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "session";
		public static var NAMESPACE_URI:String = "urn:ietf:params:xml:ns:xmpp-session";	
		
		public function Session()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}
		
		override public function processExtension( element:XML ):void
		{
			this.element = element;
		}
		
		override public function toXML():XML
		{
			var x:XML = <session></session>
				x.@xmlns = NAMESPACE_URI;			
			return x;
		}						
	}
}