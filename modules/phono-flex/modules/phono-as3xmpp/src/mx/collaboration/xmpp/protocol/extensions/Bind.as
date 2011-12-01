package mx.collaboration.xmpp.protocol.extensions
{
	import mx.collaboration.xmpp.protocol.JID;
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;

	public class Bind extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "bind";
		public static var NAMESPACE_URI:String = "urn:ietf:params:xml:ns:xmpp-bind";
		public var jid:JID;
		public var resource:String;
		
		public function Bind()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}
		
		override public function processExtension( element:XML ):void
		{
			this.element = element;
			 
			var bindNs:Namespace = new Namespace( NAMESPACE_URI );			
			if( element.bindNs::jid.length() > 0 ) jid = new JID(element.bindNs::jid);
			
		}
		
		override public function toXML():XML
		{
			var x:XML = <bind></bind>
				x.@xmlns = NAMESPACE_URI;
			if( resource ) x.resource = resource;
			
			return x;
		}						
	}
}