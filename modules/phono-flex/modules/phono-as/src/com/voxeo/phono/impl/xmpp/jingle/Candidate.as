package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
/*	      <candidate component='1'
                   generation='0'
                   id='a9j3mnbtu1'
                   ip='10.1.1.104'
                   port='13540'/>
          <candidate component='1'
                   foundation='1'
                   generation='0'
                   id='el0747fg11'
                   ip='10.0.1.1'
                   network='1'
                   port='8998'
                   priority='2130706431'
                   protocol='udp'
                   type='host'/>
*/	
	public class Candidate extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "candidate";
		public  var id:String;
		public var component:String;
		public var foundation:String;
		public var generation:String;
		public var priority:String;
		public var protocol:String;
		public var type:String;
		public var ip:String;
		public var port:String;
		// and now for rtmp
		public var rtmpUri:String;
		public var playName:String;
		public var publishName:String;
				
		public function Candidate(id:String="", component:String="", foundation:String="", generation:String="", 
								  priority:String="", protocol:String="", type:String="", ip:String="", port:String="", 
								  rtmpUri:String="", playName:String="", publishName:String="")
		{
			if (id != "") this.id = id;
			if (component != "") this.component = component;
			if (foundation != "") this.foundation = foundation;
			if (generation != "") this.generation = generation;
			if (priority != "") this.priority = priority;
			if (protocol != "") this.protocol = protocol;
			if (type != "") this.type = type;
			if (ip != "") this.ip = ip;
			if (port != "") this.port = port;
			if (rtmpUri != "") this.rtmpUri = rtmpUri;
			if (playName != "") this.playName = playName;
			if (publishName != "") this.publishName = publishName;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			if (element.@id) id = element.@id;
			if (element.@component) component = element.@component;
			if (element.@foundation) foundation = element.@foundation;
			if (element.@generation) generation = element.@generation;
			if (element.@priority) priority = element.@priority;
			if (element.@protocol) protocol = element.@protocol;
			if (element.@type) type = element.@type;
			if (element.@ip) ip = element.@ip;
			if (element.@port) port = element.@port;
			if (element.@rtmpUri) rtmpUri = element.@rtmpUri;
			if (element.@playName) playName = element.@playName;
			if (element.@publishName) publishName = element.@publishName;			
		}

		override public function toXML():XML
		{
			var x:XML = <candidate/>
			if (id) x.@id = id;
			if (component) x.@component = component;
			if (foundation) x.@foundation = foundation;
			if (generation) x.@generation = generation;
			if (priority) x.@priority = priority;
			if (type) x.@type = type;				
			if (ip) x.@ip = ip;
			if (port) x.@port = port;	
			if (rtmpUri) x.@rtmpUri = rtmpUri;
			if (playName) x.@playName = playName;
			if (publishName) x.@publishName = publishName;		
			return x;
		}		

	}
}