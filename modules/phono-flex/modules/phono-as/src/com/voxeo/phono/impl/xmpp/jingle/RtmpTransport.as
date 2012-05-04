package com.voxeo.phono.impl.xmpp.jingle
{	
	import com.voxeo.phono.impl.xmpp.jingle.*;
	
	public class RtmpTransport extends Transport
	{
		public static var ELEMENT_NAME:String = "transport";
		public static var NAMESPACE_URI:String = "http://voxeo.com/gordon/transports/rtmp";

		
		public function RtmpTransport()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}
			
		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var rawNs:Namespace = new Namespace( NAMESPACE_URI );
			
			for each (var c:XML in element.rawNs::candidate) {
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