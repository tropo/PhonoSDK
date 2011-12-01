package com.voxeo.phono.impl.xmpp.jingle
{	
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	import com.voxeo.phono.impl.xmpp.jingle.*;
	
	public class DTMF extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "dtmf";
		public static var NAMESPACE_URI:String = "urn:xmpp:jingle:dtmf:0";
		public var code:String;
		public var duration:Number;
		public var volume:Number;
		
		public function DTMF(code:String="", duration:Number=400, volume:Number=37)
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
			this.code = code;
			this.duration = duration;
			this.volume = volume;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
						
			if (element.@code) code = element.@code;
			if (element.@duration) duration = element.@duration;
			if (element.@volume) volume = element.@volume;
		}
			
		override public function toXML():XML
		{
			var x:XML = <dtmf></dtmf>
			x.@xmlns = NAMESPACE_URI;
			
			if (code) x.@code = code;
			if (duration) x.@duration = duration;
			if (volume) x.@volume = volume;
						
			return x;
		}						
	}
}