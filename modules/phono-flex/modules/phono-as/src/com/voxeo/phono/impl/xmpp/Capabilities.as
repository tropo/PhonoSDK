package com.voxeo.phono.impl.xmpp
{
	import com.adobe.crypto.SHA1;
	
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class Capabilities extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "c";
		public static var NAMESPACE_URI:String = "http://jabber.org/protocol/caps";
		public var node:String;
		public var hash:String = "sha-1";
		public var category:String;
		public var type:String;
		public var identity:String;
		public var ext:String;
		public var features:Array = new Array();
		
		public function Capabilities( category:String="", type:String="", identity:String="", node:String="", ext:String="", ...rest )
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
			
			this.category = category;
			this.type = type;
			this.identity = identity;
			this.node = node;
			this.ext = ext;
			
			for(var i:uint = 0; i < rest.length; i++)
			{
				features.push(rest[i]);
			}
		}

		public function computeHash():String
		{
			var input:String;
			input = category + "/" + type + "//" + identity + "<";
			
			features.sort();
			
			for each (var f:String in features) 
			{
				input += f + "<";
			}
	
			return SHA1.hashToBase64(input);
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var capNs:Namespace = new Namespace( NAMESPACE_URI );
	
		}
			
		override public function toXML():XML
		{
			var x:XML = <c></c>
			x.@xmlns = NAMESPACE_URI;

			if (hash) x.@hash = hash;
			if (node) x.@node = node;
			if (ext) x.@ext = ext;			
			x.@ver = computeHash();
			return x;
		}						
	}
}