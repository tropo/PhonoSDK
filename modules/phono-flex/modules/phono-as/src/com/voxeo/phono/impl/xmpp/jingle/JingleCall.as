package com.voxeo.phono.impl.xmpp.jingle
{
	import com.voxeo.phono.*;
	import com.voxeo.phono.CallState;
	import com.voxeo.phono.events.*;
	import com.voxeo.phono.impl.*;
	import com.voxeo.phono.impl.xmpp.*;
	
	import flash.events.*;
	
	import mx.collaboration.xmpp.protocol.*;
	import mx.collaboration.xmpp.protocol.authenticators.*;
	import mx.collaboration.xmpp.protocol.channels.*;
	import mx.collaboration.xmpp.protocol.events.*;
	import mx.collaboration.xmpp.protocol.packets.*;
	import mx.utils.*;
	
	public class JingleCall extends EnhancedEventDispatcher implements Call
	{
		private var _id:String;
		private var _to:String;
		private var _state:String;		
		private var _toJid:String;
		private var _fromJid:String;
		private var _remoteJid:String;
		private var _initiator:String;
		private var _stream:XMPPStream;
		private var _media:Media;
		private var _ptt:Boolean = false;
		private var _talking:Boolean = false;
		private var _volume:Number = 1;
		private var _hold:Boolean = false;
		private var _mute:Boolean = false;
		private var _cand:Candidate;
		private var _custom_headers:Array = new Array();
		private var _tones:Tones = null;
		private var _phone:Phone;
						
		private static var SPEEX_PT:String = "97";
		
		public function JingleCall(stream:XMPPStream, id:String=null)
		{
			_stream = stream;
			_media = new Media();
			_media.setVolume(_volume);
			_media.setMute(_mute);
			_media.addEventListener(MediaEvent.OPEN, mediaListener);
			_media.addEventListener(MediaEvent.CLOSE, mediaListener);
			
			// Allocate a unique ID for this call if we don't have one
			if (id == null)
				_id = UIDUtil.createUID();
			else _id = id;
			
			_state = CallState.STATE_INITIAL;
		}
		
		public function get id():String
		{
			return _id;
		}
		
		public function set pushToTalk(ptt:Boolean):void
		{
			_ptt = ptt;
			pushToTalkStateChanged();
		}
		
		public function get pushToTalk():Boolean
		{
			return _ptt;
		}

		public function set talking(talking:Boolean):void
		{
			_talking = talking;
			pushToTalkStateChanged();
		}
		
		private function pushToTalkStateChanged():void {
			if (_ptt) {
				if (_talking) {
					_media.setVolume(0.2);
					this.muted = false;
				} else {
					_media.setVolume(_volume);
					this.muted = true;
				}
			} else {
				_media.setVolume(_volume);
				this.muted = false;
			}
		}
		
		public function set tones(enableTones:Boolean):void
		{
			if (enableTones) {
				_tones = new Tones();
			}
			else
				_tones = null;
		}
		
		public function get tones():Boolean
		{
			if (_tones != null) return true;
			else return false;
		}
				
		public function set volume(vol:Number):void
		{
			_volume = vol;
			_media.setVolume(vol);
		}
		
		public function get volume():Number
		{
			return _volume;
		}
		
		public function set muted(mute:Boolean):void
		{
			_mute = mute;
			_media.setMute(mute);
		}
		
		public function get muted():Boolean
		{
			return _mute;
		}
		
		public function set hold(hold:Boolean):void
		{
			_hold = hold;
			_media.setHold(hold);
			if (!_hold) {
				_media.setMute(_mute);
				_media.setVolume(_volume);
			}
		}
		
		public function get hold():Boolean
		{
			return _hold;
		}
		
		public function get state():String
		{
			return _state;
		}
		
		public function get to():String
		{
			return _to;
		}

		public function set to(to:String):void
		{
			this._to = to;
		}

		public function reportIssue(body:String):void
		{
			var m:MessagePacket = new MessagePacket();
			m.jidTo = new JID("issue@log."+_stream.userJID.domain);
			m.body = "call:" + this._id + ":" + body;
			m.type="chat";
			_stream.sendPacket(m);
		}
		
		public function answer():void
		{
			if (_state != CallState.STATE_RINGING && _state != CallState.STATE_PROGRESS) return;
			
			var j:IQPacket = new IQPacket();
			var je:Jingle = new Jingle();
			j.addExtension(je);
			j.jidTo = new JID(_remoteJid);
			j.type = IQPacket.TYPE_SET;
			je.initiator = _initiator;
			je.action = Jingle.ACTION_SESSION_ACCEPT;
			je.sid = _id;
			j.id = UIDUtil.createUID();
			_stream.sendPacket(j,answerResult);
   			_media.startAudio(_cand.rtmpUri,_cand.playName,_cand.publishName);
   			this._state = CallState.STATE_CONNECTED;
			
			dispatchEvent(new CallEvent(CallEvent.ANSWERED, null, this));
		}
		
		public function hangup():void
		{
			if (_state != CallState.STATE_CONNECTED 
				&& _state != CallState.STATE_PROGRESS
				&& _state != CallState.STATE_RINGING) return; 
			
			// Tell the remote pary
			var j:IQPacket = new IQPacket();
			var je:Jingle = new Jingle();
			j.addExtension(je);
			j.jidTo = new JID(_remoteJid);
			j.type = IQPacket.TYPE_SET;
			je.initiator = _initiator;
			je.action = Jingle.ACTION_SESSION_TERMINATE;
			je.sid = _id;
			j.id = UIDUtil.createUID();
			_stream.sendPacket(j, hangupResult);		
			
			// Stop the audio
			_media.stopAudio();	
			this._state = CallState.STATE_DISCONNECTED;
			
			dispatchEvent(new CallEvent(CallEvent.HANGUP, null, this));
		}
		
		public function digit(digit:String, length:Number=400):void
		{
			if (_state != CallState.STATE_CONNECTED) return;
			
			if (_tones != null) _tones.play(digit);
			
			// Lets default to RFC2833
			sendDigitRFC2833(digit, length);
		}

		public function accept():void
		{
			if (_state != CallState.STATE_PROGRESS) return;
			
			// Build a jingle ringing packet and send it
			var j:IQPacket = new IQPacket();
			var je:Jingle = new Jingle();
			j.addExtension(je);
			j.jidTo = new JID(_remoteJid);
			j.type = IQPacket.TYPE_SET;
			je.initiator = _initiator;
			je.action = Jingle.ACTION_SESSION_INFO;
			je.sid = _id;
			je.ringing = true;
			j.id = UIDUtil.createUID();
			_stream.sendPacket(j, ringingResult);
			
			_state = CallState.STATE_RINGING;
		}
		
		public function start():void
		{
			if (_state != CallState.STATE_INITIAL) return;
			
			var destination:String = _to; 
			
			// re-write the URI scheme
			if (!_to.indexOf("sip:")) destination = Utils.escapeNode(_to.substr(4))+"@sip";//."+_stream.userJID.domain;
			if (!_to.indexOf("app:")) destination = Utils.escapeNode(_to.substr(4))+"@app";//."+_stream.userJID.domain;
			if (!_to.indexOf("tel:")) destination = Utils.escapeNode(_to.substr(4))+"@tel";//."+_stream.userJID.domain;
			
			var j:IQPacket = new IQPacket();
			var je:Jingle = new Jingle();
			j.addExtension(je);
			_toJid = destination;
			_fromJid = _stream.userJID.bareId.toString();
			_remoteJid = _toJid;
			j.jidTo = new JID(_toJid);
			j.type = IQPacket.TYPE_SET;
			_initiator = _stream.userJID.toString();
			je.initiator = _stream.userJID.bareJid.toString();
			je.action = Jingle.ACTION_SESSION_INITIATE;
			je.sid = _id;
			je.content_creator = "initiator";
			je.content_name = "voice";
			je.content_senders = "both";

			var d:RtmpDescription = new RtmpDescription();
			d.media = "audio";
			var p:Payload = new Payload(SPEEX_PT,"speex","16000");
			d.payloads.push(p);
			
			// Set the custom headers
			je.custom_headers = _custom_headers;
						
			je.description = d;
			var t:RtmpTransport = new RtmpTransport();
			je.transport = t;
			_stream.sendPacket(j);
			
			this._state = CallState.STATE_PROGRESS;
		}

		public function addHeader(name:String, data:String):void
		{
			var ch:CustomHeader = new CustomHeader(name, data);
			_custom_headers.push(ch);
		}
		
		public function header(name:String):String
		{
			for (var id:String in _custom_headers) 
			{
				var header:CustomHeader = _custom_headers[id];
				if (header.name == name) return header.data;
			}
			return "";
		}

		internal function sendDigitRFC2833(digit:String, length:Number):void
		{
			// Send as RFC2833 through the relay
			_media.sendDigit(digit);
		}

		internal function sendDigitInfo(digit:String, length:Number):void
		{
			// Build a jingle dtmf packet and send it
			var j:IQPacket = new IQPacket();
			var je:Jingle = new Jingle();
			j.addExtension(je);
			j.jidTo = new JID(_remoteJid);
			j.type = IQPacket.TYPE_SET;
			je.initiator = _initiator;
			je.action = Jingle.ACTION_SESSION_INFO;
			je.sid = _id;
			var d:DTMF = new DTMF(digit, length);
			je.dtmf = d;
			_stream.sendPacket(j);
		}
				
		private function mediaListener( e:Event ):void
		{
			dispatchEvent(new MediaEvent(MediaEvent(e).type,null,MediaEvent(e).reason));
		}
			
		private function answerResult(packet:Packet):void
		{
			// Generate an event to the User API");
			dispatchEvent(new CallEvent(CallEvent.ANSWERED, null, this));
		}
		
		private function ringingResult(packet:Packet):void
		{
			// Generate an event to the User API
			// We should only dispatch RINGING for an outbound call
			//dispatchEvent(new CallEvent(CallEvent.RINGING, null, this));
		}	
		
		private function hangupResult(packet:Packet):void
		{
			// Generate an event to the User API
			dispatchEvent(new CallEvent(CallEvent.HANGUP, null, this));
		}
					
		internal function processJingle(j:Jingle, from:JID):IQPacket
		{
			var cand:Candidate;
			switch (j.action) {
   			case Jingle.ACTION_SESSION_INITIATE:
   				trace("session initiate");
   				// Inbound call, tell the client api
   				_cand = j.transport.candidates.pop();
   				var desc:Description = j.description;
   				var foundSpeex:Boolean = false;
   				for each (var payload:Payload in desc.payloads) {
   					if (desc.media.toLowerCase() == "audio" && payload.name.toLowerCase() == "speex") {
   						foundSpeex = true;
   						break;
   					}
   				}
   				trace("foundspeex="+foundSpeex);
   				if (foundSpeex) {
   					if (_cand.rtmpUri && _cand.playName && _cand.publishName) {
   						//XXX
   					} else {
   						// No viable candidate
   						var errorIq:IQPacket = new IQPacket();
	       				errorIq.type = IQPacket.TYPE_SET;
	       				var je:Jingle = new Jingle();
						errorIq.addExtension(je);
						je.initiator = _initiator;
						je.action = Jingle.ACTION_SESSION_TERMINATE;
						je.sid = _id;
						je.reason = Jingle.REASON_INCOMPATIBLE_PARAMETERS;
						return errorIq;
   					}
   				} else {
   					// No compatible codecs
   					errorIq = new IQPacket();
	       			errorIq.type = IQPacket.TYPE_SET;
	       			je = new Jingle();
					errorIq.addExtension(je);
					je.initiator = _initiator;
					je.action = Jingle.ACTION_SESSION_TERMINATE;
					je.sid = _id;
					je.reason = Jingle.REASON_INCOMPATIBLE_PARAMETERS;
					return errorIq;
   				}
   				trace("dispatchEvent");
   				_remoteJid = from.toString();
				_initiator = j.initiator;	
				_custom_headers = j.custom_headers;
				
   				this._state = CallState.STATE_PROGRESS;
   
   				dispatchEvent(new CallEvent(CallEvent.CREATED, null, this));  	
				
				// Auto ring the call
				this.accept();
   				break;
   			case Jingle.ACTION_SESSION_TERMINATE:
   				_media.stopAudio();
   				this._state = CallState.STATE_DISCONNECTED;
   				dispatchEvent(new CallEvent(CallEvent.HANGUP, null, this));
   				break;
   			case Jingle.ACTION_SESSION_ACCEPT:
   				cand = j.transport.candidates.pop();
   				desc = j.description;
   				foundSpeex = false;
   				for each (payload in desc.payloads) {
   					if (desc.media.toLowerCase() == "audio" && payload.name.toLowerCase() == "speex") {
   						foundSpeex = true;
   						break;
   					}
   				}
   				if (foundSpeex) {
   					if (cand.rtmpUri && cand.playName && cand.publishName) {
   						_media.startAudio(cand.rtmpUri,cand.playName,cand.publishName);
   						this._state = CallState.STATE_CONNECTED;
   						dispatchEvent(new CallEvent(CallEvent.ANSWERED, null, this));
   					} else {
   						// No viable candidate
   						errorIq = new IQPacket();
	       				errorIq.type = IQPacket.TYPE_SET;
	       				je = new Jingle();
						errorIq.addExtension(je);
						je.initiator = _initiator;
						je.action = Jingle.ACTION_SESSION_TERMINATE;
						je.sid = _id;
						je.reason = Jingle.REASON_INCOMPATIBLE_PARAMETERS;
						return errorIq;
   					}
   				} else {
   					// No compatible codecs
   					errorIq = new IQPacket();
	       			errorIq.type = IQPacket.TYPE_SET;
	       			je = new Jingle();
					errorIq.addExtension(je);
					je.initiator = _initiator;
					je.action = Jingle.ACTION_SESSION_TERMINATE;
					je.sid = _id;
					je.reason = Jingle.REASON_INCOMPATIBLE_PARAMETERS;
					return errorIq;
   				}
   				break;
   			case Jingle.ACTION_SESSION_INFO:
   				// What payload do we have?
   				if (j.ringing) {
   					// We are ringing
   					trace("RINGING");
   					this._state = CallState.STATE_RINGING;
   					dispatchEvent(new CallEvent(CallEvent.RINGING, null, this));
   				} else if (j.dtmf){
   					// We have a dtmf digit
   					var ce:CallEvent = new CallEvent(CallEvent.DIGIT, null, this);
   					ce.digit = j.dtmf.code;
   					ce.digitDuration = j.dtmf.duration;
   					dispatchEvent(ce);
   				} else {
   				 	// unknown session-info - send error
   				 	errorIq = new IQPacket();
	       			errorIq.type = IQPacket.TYPE_ERROR;
	       			errorIq.error = new PacketError();
	       			errorIq.error.condition = PacketError.CONDITION_FEATURE_NOT_IMPLEMENTED;
	       			errorIq.error.type = PacketError.TYPE_CANCEL;
					return errorIq;
   				}  				
   				break;
   			case Jingle.ACTION_TRANSPORT_ACCEPT:
   				break;
   			case Jingle.ACTION_TRANSPORT_REJECT:
   				break;
   			case Jingle.ACTION_TRANSPORT_REPLACE:
   				break;
   			case Jingle.ACTION_TRANSPORT_INFO:
   				break;
   			case Jingle.ACTION_CONTENT_ACCEPT:
   				break;
   			case Jingle.ACTION_CONTENT_ADD:
 				// This can be used for early media - see XEP-0269
   				break;
   			case Jingle.ACTION_CONTENT_MODIFY:
   				break;
   			case Jingle.ACTION_CONTENT_REJECT:
   				break;
   			case Jingle.ACTION_DESCRIPTION_INFO:
   				break;
   			case Jingle.ACTION_SECURITY_INFO:
   				break;
   			}
   			var reply:IQPacket = new IQPacket();
   			reply.type = IQPacket.TYPE_RESULT;
   			return reply;
		}
	}
}