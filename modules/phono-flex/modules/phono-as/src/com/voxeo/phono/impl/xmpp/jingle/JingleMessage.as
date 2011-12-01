package com.voxeo.phono.impl.xmpp.jingle
{
	import com.voxeo.phono.events.*;
	import com.voxeo.phono.*;
	import com.voxeo.phono.impl.xmpp.*;
	
	import flash.events.*;
	
	import mx.collaboration.xmpp.protocol.*;
	import mx.collaboration.xmpp.protocol.authenticators.*;
	import mx.collaboration.xmpp.protocol.channels.*;
	import mx.collaboration.xmpp.protocol.events.*;
	import mx.collaboration.xmpp.protocol.packets.*;
	import mx.utils.*;
	
	public class JingleMessage extends EnhancedEventDispatcher implements Message
	{
		private var _stream:XMPPStream;
		private var _message:MessagePacket;
				
		public function JingleMessage(stream:XMPPStream, message:MessagePacket=null)
		{
			_stream = stream;
			if (message != null)
				_message = message;	
			else
				_message = new MessagePacket();
		}
		
		public function send(dest:String):void
		{
			_message.jidTo = new JID(dest);
			_stream.sendPacket(_message);
		}
						
		public function reply(body:String):void
		{
			var reply:MessagePacket = new MessagePacket();
			reply.jidTo = _message.jidFrom;
			reply.subject = _message.subject;
			reply.thread = _message.thread;
			reply.type = _message.type;
			reply.body = body;
			_stream.sendPacket(reply);
		}
		
		public function get body():String
		{
			return _message.body;
		}
		
		public function set body(body:String):void
		{
			_message.body = body;
		}
		
		public function get from():String
		{
			return _message.jidFrom.toString();
		}
		
		public function get type():String
		{
			return _message.type;
		}
		
		public function set type(type:String):void
		{
			_message.type = type;
		}
		
		public function get subject():String
		{
			return _message.subject;
		}
		
		public function set subject(subject:String):void
		{
			_message.subject = subject;
		}
		
		public function get thread():String
		{
			return _message.thread;	
		}
		
		public function set thread(thread:String):void
		{
			_message.thread = thread;
		}
	}
}