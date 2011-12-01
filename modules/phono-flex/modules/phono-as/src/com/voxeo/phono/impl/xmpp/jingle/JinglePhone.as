package com.voxeo.phono.impl.xmpp.jingle
{
	import com.voxeo.phono.*;
	import com.voxeo.phono.events.*;
	import com.voxeo.phono.impl.Ringer;
	import com.voxeo.phono.impl.Tones;
	import com.voxeo.phono.impl.xmpp.*;
	
	import flash.events.*;
	import flash.media.Microphone;
	import flash.system.Security;
	import flash.system.SecurityPanel;
	import flash.utils.Timer;
	import flash.utils.setTimeout;
	
	import mx.collaboration.xmpp.protocol.*;
	import mx.collaboration.xmpp.protocol.authenticators.*;
	import mx.collaboration.xmpp.protocol.channels.*;
	import mx.collaboration.xmpp.protocol.events.*;
	import mx.collaboration.xmpp.protocol.packets.*;
	import mx.controls.Alert;
		
	public class JinglePhone extends EnhancedEventDispatcher implements Phone
	{	
		private var _id:String;
		private var _activeCallRemoteJid:String;
		private var _activeCallSid:String;
		private var _calls:Array = new Array();
		private var _stream:XMPPStream;
		private var _xmppConnected:Boolean = false;
		private var _relayConnected:Boolean = false;
		private var _ringer:Ringer = null;
		private var _ringbackRinger:Ringer = null;
		private var _dtmfTones:Tones = null;
   		private var _caps:Capabilities = new Capabilities("client", "web", "gordon", "http://voxeo.com/gordon",
    											  		 "voice-v1",
    											  		 "http://jabber.org/protocol/caps",
    											  		 "http://jabber.org/protocol/disco#info",		
    											  		 "urn:xmpp:jingle:1",
    											  		 "http://voxeo.com/gordon/apps/rtmp");										 

		public function get flashPermissionState():Boolean
		{
			var mic:Microphone = Microphone.getMicrophone();
			if (mic) {
				if (!mic.muted) return true;
			}
			return false;
		}
		
		public function set ringTone(newTone:String):void
		{
			if (_ringer != null) 
				_ringer.ringTone = newTone;
			else
				_ringer = new Ringer(newTone);
		}
		
		public function get ringTone():String
		{
			if (_ringer) return _ringer.ringTone;
			else return null;
		}
		
		public function set ringbackTone(newTone:String):void
		{
			if (_ringbackRinger != null) 
				_ringbackRinger.ringTone = newTone;
			else
				_ringbackRinger = new Ringer(newTone);
		}
		
		public function get ringbackTone():String
		{
			if (_ringbackRinger) return _ringbackRinger.ringTone;
			else return null;
		}
		
		public function set tones(enable:Boolean):void
		{
			if (enable) {
				_ringer = new Ringer();
				_ringbackRinger = new Ringer();
				_dtmfTones = new Tones();
			} else {
				if (_ringer != null) _ringer.stop();
				if (_ringbackRinger != null) _ringbackRinger.stop();
				_ringer = null;
				_ringbackRinger = null;
				_dtmfTones = null;
			}
		}
		
		public function get tones():Boolean
		{
			if (_ringer != null) return true;
			else return false;
		}
			
		public function get sessionId():String
		{
			if (!connected) 
			{
				dispatchEvent( new PhoneEvent(PhoneEvent.ERROR, this, "Not connected.") );
				return null;
			}			
			return _stream.userJID.bareJid.toString();
		}
		
		public function set id(id:String):void
		{
			_id = id;
		}
		
		public function get state():String
		{
			if (connected) return PhoneState.STATE_CONNECTED;
			else return PhoneState.STATE_DISCONNECTED;
		}
		
		public function get id():String
		{
			return _id;
		}
		
		public function connect(server:String="gw.phono.com", username:String="anon", password:String=""):void
		{
			if (!_xmppConnected) 
			{	
				// Create the stream
				_stream = new XMPPStream();  
				
				// Register for Jingle
				_stream.extensionManager.addExtension(Jingle.ELEMENT_NAME,Jingle.NAMESPACE_URI,Jingle);
				
				// Register for Disco
				_stream.extensionManager.addExtension(Discovery.ELEMENT_NAME,Discovery.NAMESPACE_URI,Discovery);
				
				// Set up the user  
		        _stream.userJID = new JID( username );
		        try {		        	
	          		_stream.channel = new SocketChannel( server, server, 5222);
	            } catch (ex:Error) {
	            	var ge:PhoneEvent = new PhoneEvent(PhoneEvent.ERROR, this, "");
	            	ge.reason = PhoneEvent.REASON_SOCKET;
	            	dispatchEvent( ge );
	            }
	            _stream.channel.tracePackets = true;
	            if (password != "")
	            	_stream.authenticator = new SASLAuthenticator(password, SASLAuthenticator.TYPE_PLAIN);
	            else
	            	_stream.authenticator = new SASLAuthenticator(password, SASLAuthenticator.TYPE_ANONYMOUS);
	            _stream.addEventListener( XMPPStreamEvent.ERROR, onError);
	            _stream.addEventListener( XMPPStreamEvent.DATA, onData);
	            _stream.addEventListener( XMPPStreamEvent.CONNECT, onConnect);
	            _stream.addEventListener( XMPPStreamEvent.DISCONNECT, onDisconnect);     
	            // Try to connect XMPP session
	            _stream.connect();
	   		}
		}
		
		public function disconnect():void
		{
			// Close the XMPP session
			if (_xmppConnected) _stream.disconnect();
		}
		
		public function get connected():Boolean
		{
			return (_xmppConnected);
		}
		
		public function showFlashPermissionBox():void
		{
			var phone:Phone = this;
			setTimeout(function():void {
				dispatchEvent(new MediaEvent(MediaEvent.OPEN, phone));
				Security.showSettings(SecurityPanel.PRIVACY);
				Alert.show('Click "OK" to continue.', "Success", Alert.OK, null, function(e:Event):void { 
					dispatchEvent(new MediaEvent(MediaEvent.CLOSE, phone));
				});
			}, 1000);
			
		}		
		
		public function dial(to:String):Call {
			if (!connected) {
				dispatchEvent( new PhoneEvent(PhoneEvent.ERROR, this, "Not connected.") );
				return null;
			}
			var call:Call = createCall();
			call.talking = false;
			call.to = to;
			call.start();
			return call;
		}
		
		
		public function createCall():Call
		{
			var call:JingleCall = new JingleCall(_stream);
			if (_dtmfTones != null) call.tones = true;
			
			// Add a listener for the call
			call.addEventListener(CallEvent.ANSWERED,callListener);			
			call.addEventListener(CallEvent.DIGIT,callListener);
			call.addEventListener(CallEvent.HANGUP,callListener);
			call.addEventListener(CallEvent.RINGING,callListener);
			call.addEventListener(CallEvent.CREATED,callListener);
			call.addEventListener(MediaEvent.OPEN,mediaListener);
			call.addEventListener(MediaEvent.CLOSE,mediaListener);
			
			_calls[call.id] = call;
			
			return call;
		}
		
		public function text(who:String, body:String, type:String="chat", subject:String=null, thread:String=null):void
		{
			var m:MessagePacket = new MessagePacket();
			m.jidTo = new JID(who);
			m.body = body;
			m.type = type;
			if (subject != null) m.subject = subject;
			if (thread != null) m.thread = thread;
			_stream.sendPacket(m);
		}
		
		public function reportIssue(body:String):void
		{
			var m:MessagePacket = new MessagePacket();
			m.jidTo = new JID("issue@log."+_stream.userJID.domain);
			m.body = "phone:" + body;
			m.type="chat";
			_stream.sendPacket(m);
		}
						
		private function callListener( e:Event ):void
		{
			switch (CallEvent(e).type) {
				case CallEvent.RINGING: // Outbound call
					if (_ringbackRinger != null) _ringbackRinger.start();
					break;
				case CallEvent.CREATED: // Inbound call
					if (_ringer != null) _ringer.start();
					break;
				case CallEvent.ANSWERED: 
				case CallEvent.HANGUP:
					if (_ringer != null) _ringer.stop();
					if (_ringbackRinger != null) _ringbackRinger.stop();
					break;
			}
		 	dispatchEvent(new CallEvent(CallEvent(e).type,this,CallEvent(e).call));
		}
		
		private function mediaListener( e:Event ):void
		{
			dispatchEvent(new MediaEvent(MediaEvent(e).type,this,MediaEvent(e).reason));
		}

		// XMPP stream event handlers
		
		internal function onConnect( e:Event ):void
        {
        	if (e is XMPPStreamEvent)
        	{
        		trace("XMPP CONNECTED");
        		_xmppConnected = true;
        	        	
      			// Send an initial presence
    			var p:PresencePacket = new PresencePacket();
				p.addExtension(_caps);			
            	_stream.sendPacket(p);

        	}
                                
            // Tell the API 
            if (connected) {
				dispatchEvent( new PhoneEvent(PhoneEvent.CONNECTED, this, "") );	
			}
        }
        
        internal function onDisconnect( event:Event ):void
        {
        
        	var reason:String;
        	if (event is XMPPStreamEvent)
        	{
        		trace("XMPP DISCONNECTED");
        		_xmppConnected = false;
        		reason="xmpp";
        	}
        	
        	// Tell the API 
        	var ge:PhoneEvent = new PhoneEvent(PhoneEvent.DISCONNECTED, this, "");
        	ge.reason = reason;
            dispatchEvent(ge);
       	}		
       	
       	internal function onData( event:Event ):void
       	{
       		trace("DATA:" + XMPPStreamEvent(event).packet.toXML());
       		var packet:Packet = XMPPStreamEvent(event).packet;
       		if (packet is MessagePacket) {
       			var msg:MessagePacket = packet as MessagePacket;
       			var gmsg:JingleMessage = new JingleMessage(_stream, msg);
       			dispatchEvent(new MessageEvent(MessageEvent.MESSAGE, this, gmsg));
       		} else {	       		
	       		var iq:IQPacket;
	       		if (packet.hasExtensionType(Jingle)) {
	       			//trace("Jingle:" + packet.getExtensionByType(Jingle));
	       			if (packet is IQPacket) {
	       				iq = packet as IQPacket;
	       				var j:Jingle = packet.getExtensionByType(Jingle) as Jingle;
	       				//trace("action: " + j.action);
	       				// Find the call
	       				if (!_calls[j.sid])
	       				{
	       					// If it's an initiate then it's a new call, else it's an error
	       					if (j.action == Jingle.ACTION_SESSION_INITIATE) {
	       						var call:JingleCall = new JingleCall(_stream, j.sid);	
								if (_dtmfTones != null) call.tones = true;
	       						call.addEventListener(CallEvent.ANSWERED,callListener);			
								call.addEventListener(CallEvent.DIGIT,callListener);
								call.addEventListener(CallEvent.HANGUP,callListener);
								call.addEventListener(CallEvent.RINGING,callListener);
								call.addEventListener(CallEvent.CREATED,callListener);
								call.addEventListener(MediaEvent.OPEN,mediaListener);
								call.addEventListener(MediaEvent.CLOSE,mediaListener);
	       						_calls[j.sid] = call;
	       						call.processJingle(j, iq.jidFrom);	       						
	       					} else {
		       					// If no call then send an error that it's unknown
		       					/*
	    	   					<iq from='romeo@montague.lit/orchard'
    								id='ur71vs62'
    								to='juliet@capulet.lit/balcony'
    								type='error'>
  									<error type='cancel'>
    								<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
    								<unknown-session xmlns='urn:xmpp:jingle:errors:1'/>
  								</error>
								</iq>
	       						*/
	       						var errorIq:IQPacket = new IQPacket();
	       						errorIq.id = iq.id;
	       						errorIq.jidFrom = iq.jidTo;
	       						errorIq.jidTo = iq.jidFrom;
	       						errorIq.type = IQPacket.TYPE_ERROR;
	       						errorIq.error = new PacketError();
	       						errorIq.error.condition = PacketError.CONDITION_ITEM_NOT_FOUND;
	       						errorIq.error.type = PacketError.TYPE_CANCEL;
								// Add the jingle specific error
								errorIq.error.customXML = <unknown-session xmlns='urn:xmpp:jingle:errors:1'/>
								_stream.sendPacket(errorIq);
	       					}
	       				} else {
	       					var reply:IQPacket = _calls[j.sid].processJingle(j, iq.jidFrom);
	       					if (reply) {
	       						reply.jidFrom = iq.jidTo;
	       						reply.jidTo = iq.jidFrom;
	       						reply.id = iq.id;
	       						_stream.sendPacket(reply);
	       					}
	       				}       				   				
	       			}
	       		}
	       		if (packet.hasExtensionType(Discovery)) {
	       			//trace("Discovery:" + packet.getExtensionByType(Discovery));
	       			if (packet is IQPacket) {
	       				iq = packet as IQPacket;
	       				if (iq.type == IQPacket.TYPE_GET) {
	       					var discoIq:IQPacket = new IQPacket();
	       					discoIq.id = iq.id;
	       					discoIq.jidFrom = iq.jidTo;
	       					discoIq.jidTo = iq.jidFrom;
	       					discoIq.type = IQPacket.TYPE_RESULT;
	       					var disco:Discovery = new Discovery();
	       					disco.caps = _caps;
	       					discoIq.addExtension(disco);
	       					_stream.sendPacket(discoIq);
	       				}
	       			}
	       		}
	       	}
       	}
       	
       	internal function onError( event:Event ):void
       	{
       		if (event is XMPPStreamEvent)
       		{
       			trace("ERROR:" + XMPPStreamEvent(event).error);
       		}       		
       	}
	}
}

