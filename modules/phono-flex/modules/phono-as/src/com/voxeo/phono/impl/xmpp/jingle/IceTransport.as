package com.voxeo.phono.impl.xmpp.jingle
{	
	import com.voxeo.phono.impl.xmpp.jingle.*;
	
	public class IceTransport extends Transport
	{
		public static var ELEMENT_NAME:String = "transport";
		public static var NAMESPACE_URI:String = "urn:xmpp:jingle:transports:ice-udp:1";

		public var pwd:String;
		public var ufrag:String;
		
		public function IceTransport(pwd:String="", ufrag:String="")
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
			this.pwd = pwd;
			this.ufrag = ufrag;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var iceNs:Namespace = new Namespace( NAMESPACE_URI );
			
			if (element.@pwd) pwd = element.@pwd;
			if (element.@ufrag) ufrag = element.@ufrag;
			for each (var c:XML in element.iceNs::candidate) {
				var candidate:Candidate = new Candidate();
				candidate.processExtension(c);
				candidates.push(candidate);
			}	
		}
			
		override public function toXML():XML
		{
			var x:XML = <transport></transport>
			x.@xmlns = NAMESPACE_URI;
			
			if (pwd) x.@pwd = pwd;
			if (ufrag) x.@ufrag = ufrag;
			
			// Add the candidates
			for(var id:String in candidates)
            {
                var c:XML = candidates[id].toXML();
            	x.appendChild(c);
            }	
			
			return x;
		}						
	}
}