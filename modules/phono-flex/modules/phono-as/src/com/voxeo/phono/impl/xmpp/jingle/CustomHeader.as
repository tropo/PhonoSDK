package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class CustomHeader extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "custom-header";
		public static var NAMESPACE_URI:String = "http://voxeo.com/gordon/ext/header";
		public var name:String;
		public var data:String;
				
		public function CustomHeader(name:String="", data:String="")
		{
			if (name != "") this.name = name;
			if (data != "") this.data = data;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			if (element.@name) name = element.@name;
			if (element.@data) data = element.@data;
		}

		override public function toXML():XML
		{
			var x:XML = <custom-header/>
			if (name) x.@name = name;
			if (data) x.@data = data;
			return x;
		}		
	}
}