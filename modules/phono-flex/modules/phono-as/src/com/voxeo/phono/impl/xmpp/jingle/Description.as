package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class Description extends PacketExtension
	{
		public var payloads:Array = new Array();
		public var media:String = "audio";
	}
}