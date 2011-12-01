package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
/*
 * <payload-type id="110" name="SPEEX" clockrate="16000"/>
 */
 
	public class Payload extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "payload-type";
		public var id:String;
		public var name:String;
		public var clockrate:String;
		
		public function Payload(id:String="", name:String="", clockrate:String="")
		{
			this.id = id;
			this.name = name;
			this.clockrate = clockrate;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			if (element.@id) id = element.@id;
			if (element.@name) name = element.@name;
			if (element.@clockrate) clockrate = element.@clockrate;				
		}

		override public function toXML():XML
		{
			var x:XML = <payload-type/>
			if (id) x.@id = id;
			if (name) x.@name = name;
			if (clockrate) x.@clockrate = clockrate;
			return x;
		}		

	}
}