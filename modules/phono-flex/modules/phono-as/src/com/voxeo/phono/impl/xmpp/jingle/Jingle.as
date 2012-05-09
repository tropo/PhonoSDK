package com.voxeo.phono.impl.xmpp.jingle
{
	import mx.collaboration.xmpp.protocol.packets.PacketExtension;
	
	public class Jingle extends PacketExtension
	{
		public static var ELEMENT_NAME:String = "jingle";
		public static var NAMESPACE_URI:String = "urn:xmpp:jingle:1";
		public static var INFO_URI:String = "urn:xmpp:jingle:apps:rtp:1:info";
		public static var ACTION_CONTENT_ACCEPT:String = "content-accept";
		public static var ACTION_CONTENT_ADD:String = "content-add";
		public static var ACTION_CONTENT_MODIFY:String = "content-modify";
		public static var ACTION_CONTENT_REJECT:String = "content-reject";
		public static var ACTION_DESCRIPTION_INFO:String = "description-info";
		public static var ACTION_SECURITY_INFO:String = "security-info";
		public static var ACTION_SESSION_ACCEPT:String = "session-accept";
		public static var ACTION_SESSION_INFO:String = "session-info";
		public static var ACTION_SESSION_INITIATE:String = "session-initiate";
		public static var ACTION_SESSION_TERMINATE:String = "session-terminate";
		public static var ACTION_TRANSPORT_ACCEPT:String = "transport-accept";
		public static var ACTION_TRANSPORT_INFO:String = "transport-info";
		public static var ACTION_TRANSPORT_REJECT:String = "transport-reject";
		public static var ACTION_TRANSPORT_REPLACE:String = "transport-replace";

		public static var REASON_ALTERNATIVE_SESSION:String = "alternative-session";
		public static var REASON_BUSY:String = "busy";
		public static var REASON_CANCEL:String = "cancel";
		public static var REASON_CONNECTIVITY_ERROR:String = "connectivity-error";
		public static var REASON_DECLINE:String = "decline";
		public static var REASON_EXPIRED:String = "expired";
		public static var REASON_FAILED_APPLICATION:String = "failed-application";
		public static var REASON_FAILED_TRANSPORT:String = "failed-transport";
		public static var REASON_GENERAL_ERROR:String = "general-error";
		public static var REASON_GONE:String = "gone";
		public static var REASON_INCOMPATIBLE_PARAMETERS:String = "incompatible-parameters";
		public static var REASON_MEDIA_ERROR:String = "media-error";
		public static var REASON_SECURITY_ERROR:String = "security-error";
		public static var REASON_SUCCESS:String = "success";
		public static var REASON_TIMEOUT:String = "timeout";
		public static var REASON_UNSUPPORTED_APPLICATIONS:String = "unsupported-applications";
		public static var REASON_UNSUPPORTED_TRANSPORTS:String = "unsupported-transports";

		public var action:String = "";
		public var initiator:String = "";
		public var sid:String = "";
		public var description:Description;
		public var transport:Transport;
		public var dtmf:DTMF;
		
		public var content_creator:String = "";
		public var content_disposition:String = "";
		public var content_name:String = "";
		public var content_senders:String = "";

		public var custom_headers:Array = new Array();
		
		public var ringing:Boolean = false;
		public var reason:String = "";
		
		public function Jingle()
		{
			_elementName = ELEMENT_NAME;
			_namespaceUri = NAMESPACE_URI;
		}

		override public function processExtension( element:XML ):void
		{
			this.element = element;
			
			var jingleNs:Namespace = new Namespace(NAMESPACE_URI);
			var infoNs:Namespace = new Namespace(INFO_URI);
			var rtpDescNs:Namespace = new Namespace(RtpDescription.NAMESPACE_URI);
			var rtmpDescNs:Namespace = new Namespace(RtmpDescription.NAMESPACE_URI);
			var iceNs:Namespace = new Namespace(IceTransport.NAMESPACE_URI);
			var rawNs:Namespace = new Namespace(RawTransport.NAMESPACE_URI);
			var rtmpNs:Namespace = new Namespace(RtmpTransport.NAMESPACE_URI);
			var dtmfNs:Namespace = new Namespace(DTMF.NAMESPACE_URI);
			var headerNs:Namespace = new Namespace(CustomHeader.NAMESPACE_URI);
			default xml namespace = jingleNs;
			
			trace("Jingle: processExtension:" + element);
			
			if( element.@action.length() > 0 ) action = element.@action;
			if( element.@initiator.length() > 0 ) initiator = element.@initiator;
			if( element.@sid.length() > 0 ) sid = element.@sid;
			
			if (element.dtmf.length() > 0) {
				var dtmf:DTMF = new DTMF();
				dtmf.processExtension(element.dtmf);
				trace("Found DTMF: " + element.dtmf);
				this.dtmf = dtmf;
			}
			
			for each (var header:XML in element["custom-header"]) {
				var customHeader:CustomHeader = new CustomHeader();
				customHeader.processExtension(header);
				custom_headers.push(customHeader);
			}	
			
			if (element.content.creator.length() > 0) {
				content_creator = element.content.creator;
			}
			if (element.content.name.length() > 0) {
				content_name = element.content.name;
			}
			if (element.content.senders.length() > 0) {
				content_senders = element.content.senders;
			}
						
			if (element.infoNs::ringing.length() > 0) {
				ringing = true;
			}
			
			if (element.reason.length() > 0) {
				this.reason = element.reason.elements()[0].localName.toString();
				var r:XML = new XML(element.reason.elements()[0]);
				this.reason = r.localName();
				trace("reason: " + this.reason);
			}
						
			if (element.content.rtpDescNs::description.@media == "audio") {
				description = new RtpDescription();
				description.processExtension(element.content.rtpDescNs::description[0]);
				description.media = "audio";
			}
			
			if (element.content.rtmpDescNs::description.@media == "audio") {
				description = new RtmpDescription();
				description.processExtension(element.content.rtmpDescNs::description[0]);
				description.media = "audio";
			}
			
			if (element.content.iceNs::transport.length() > 0) {
				var iceTransport:IceTransport = new IceTransport();
				iceTransport.processExtension(element.content.iceNs::transport[0]);
				trace("Found Ice Transport: " + element.content.iceNs::transport);
				this.transport = iceTransport;
			}

			if (element.content.rawNs::transport.length() > 0) {
				var rawTransport:RawTransport = new RawTransport();
				rawTransport.processExtension(element.content.rawNs::transport[0]);
				trace("Found Raw Transport: " + element.content.rawNs::transport);
				this.transport = rawTransport;
			}
			
			if (element.content.rtmpNs::transport.length() > 0) {
				var rtmpTransport:RtmpTransport = new RtmpTransport();
			    rtmpTransport.processExtension(element.content.rtmpNs::transport[0]);
				trace("Found RTMP Transport: " + element.content.rtmpNs::transport);
				this.transport = rtmpTransport;
			}
						
		}
			
		override public function toXML():XML
		{
			var x:XML = <jingle></jingle>;
			var jingleNs:Namespace = new Namespace(NAMESPACE_URI);
			default xml namespace = jingleNs;
			x.setNamespace(jingleNs);
//			x.@xmlns = NAMESPACE_URI;
			if (action != "") x.@action = action;
			if (initiator != "") x.@initiator = initiator;
			if (sid != "") x.@sid = sid;
			if (content_creator != "") {
				var c:XML = <content></content>;
				c.@creator = content_creator;
				c.@name = content_name;	
				c.@senders = content_senders;
				if (description) c.appendChild(description.toXML());
				if (transport) c.appendChild(transport.toXML());
				x.appendChild(c);	
			}
			
			if (dtmf) x.appendChild(dtmf.toXML());
			
			if (ringing) {
				var r:XML = <ringing/>
				r.@xmlns = INFO_URI;
				x.appendChild(r);
			}
			
			if (reason) {
				var e:XML = <reason/>
				e.appendChild(new XML("<"+reason+"/>"));
				x.appendChild(e);
			}
			
			// Add the custom headers
			for(var id:String in custom_headers)
            {
            	var p:XML = custom_headers[id].toXML();
            	x.appendChild(p);
            }
			
			return x;
		}						
	}
}