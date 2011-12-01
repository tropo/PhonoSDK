package mx.collaboration.xmpp.protocol.packets
{
	import flash.events.IEventDispatcher;
	import mx.collaboration.xmpp.protocol.xmpp_internal;
	import mx.collaboration.xmpp.protocol.packets.Packet;
	
	use namespace xmpp_internal;
	
	public class PacketExtension
	{
		protected var _elementName:String;
		protected var _namespaceUri:String;
		protected var element:XML;
		
		private var _packet:Packet;
		
		public function get namespaceUri():String
		{
			return _namespaceUri;
		}
		
		public function get elementName():String
		{
			return _elementName;
		}
					
		public function processExtension( element:XML ):void
		{
			this.element = element;
			
			_namespaceUri = element.@xmlns;
			_elementName = element.name();
		}
		
		public function toXML():XML
		{
			return element;
		}
			
		xmpp_internal function set packet( value:Packet ):void
		{
			_packet = value;
		}
		
		xmpp_internal function get packet():Packet
		{
			return _packet;
		}
	}
}