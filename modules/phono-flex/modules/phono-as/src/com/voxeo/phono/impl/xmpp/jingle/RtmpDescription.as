package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class RtmpDescription extends Description
	{
		public static var ELEMENT_NAME:String = "description";
		public static var NAMESPACE_URI:String = "http://voxeo.com/gordon/apps/rtmp";
				
		public function RtmpDescription()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var descNs:Namespace = new Namespace( NAMESPACE_URI );
			for each (var p:XML in element.descNs::["payload-type"]) {
				var payload:Payload = new Payload();
				payload.processExtension(p);
				payloads.push(payload);
			}	
		}
			
		override public function toXML():XML
		{
			var x:XML = <description></description>
				x.@xmlns = NAMESPACE_URI;
				x.@media = media;
			
			// Add the payloads
			for(var id:String in payloads)
            {
            	var p:XML = payloads[id].toXML();
            	x.appendChild(p);
            }	

			return x;
		}						
	}
}