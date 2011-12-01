package com.voxeo.phono.impl.xmpp.jingle
{	
	import com.voxeo.phono.impl.xmpp.jingle.*;
	
	public class RawTransport extends Transport
	{
		public static var ELEMENT_NAME:String = "transport";
		public static var NAMESPACE_URI:String = "urn:xmpp:jingle:transports:raw-udp:1";

		
		public function RawTransport()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}
			
		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var rawNs:Namespace = new Namespace( NAMESPACE_URI );
			
			for each (var c:XML in element.candidate) {
				var candidate:Candidate = new Candidate();
				candidate.processExtension(c);
				candidates[candidate.id] = candidate;
			}	
		}
			
		override public function toXML():XML
		{
			var x:XML = <transport></transport>
			x.@xmlns = NAMESPACE_URI;
			// Add the candidates
			for(var id:String in candidates)
            {
                x.candidate = candidates[id].toXML();
            }	
			
			return x;
		}						
	}
}