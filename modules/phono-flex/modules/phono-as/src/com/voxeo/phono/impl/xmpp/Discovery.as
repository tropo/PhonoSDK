package com.voxeo.phono.impl.xmpp
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class Discovery extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "query";
		public static var NAMESPACE_URI:String = "http://jabber.org/protocol/disco#info";
		public var caps:Capabilities;
		
		public function Discovery()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var discoNs:Namespace = new Namespace( NAMESPACE_URI );
						
			caps = new Capabilities();
						
			for each (var f:String in element.discoNs::feature.@["var"]) {
				//trace("Found feature: " + f);
				caps.features.push(f);
			}	
		}
			
		override public function toXML():XML
		{
			var x:XML = <query></query>
			x.@xmlns = NAMESPACE_URI;
			
			if (caps) {
				//trace("caps.features: " + caps.features);
				x.identity.@category = caps.category;
				x.identity.@type = caps.type;
				x.@node = caps.node + '#' + caps.computeHash();
				for each (var f:String in caps.features) {
					var feature:XML = <feature></feature>
					feature.@["var"] = f;
					x.appendChild(feature);
				}
			}
			return x;			
		}						
	}
}