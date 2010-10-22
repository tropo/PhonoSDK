package com.phono
{
	import com.phono.*;
	import com.phono.events.*;
	import com.phono.impl.*;
	
	import flash.events.*;
	import flash.media.Microphone;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.net.ObjectEncoding;
	import flash.net.Responder;
	import flash.system.Security;
	import flash.system.SecurityPanel;
	import flash.utils.Dictionary;
	import flash.utils.Timer;
	import flash.utils.setTimeout;
	
	import mx.controls.Alert;
		
	public class Audio extends EnhancedEventDispatcher
	{	
		private var _mic:Microphone; // Internal mic handle for permission checking
		private var _boxOpen:Boolean = false;
		
		private var _ncDict:Dictionary = new Dictionary(); // rtmpUri -> NC		
		private var _waitQs:Dictionary = new Dictionary(); // rtmpUri -> Array of functions to call
		
		public function Audio()
		{	
			_mic = Microphone.getMicrophone();
			if (_mic) {
				_mic.addEventListener(StatusEvent.STATUS, onMicStatus);			
			}
		}		
		
		// --- Public API
		
		public function get transport():String
		{
			return "http://voxeo.com/gordon/transports/rtmp";	
		}
		
		public function get description():String
		{
			return "http://voxeo.com/gordon/apps/rtmp";
		}
		
		public function get codecs():Array
		{
			var cds:Array = new Array();
			cds.push(new Codec("97", "SPEEX", "8000"));
			cds.push(new Codec("97", "SPEEX", "16000"));
			return cds;
		}
		
		public function play(url:String, autoStart:Boolean):Player
		{
			var player:Player = null
			var protocolName:String = getProtocolName(url);
			if (protocolName.toLowerCase() == "rtmp") {
				var streamName:String = getStreamName(url);	
				var rtmpUri:String = getRtmpUri(url);
				var nc:NetConnection = getNetConnection(rtmpUri);
				
				var queue:Array = _waitQs[rtmpUri]
				player = new RtmpPlayer(queue, nc, streamName, url);
			} else if (protocolName.toLowerCase() == "http") {
				player = new HttpPlayer(url);
			}
			if (autoStart) player.start();
			return player;
		}
		
		public function share(url:String, autoStart:Boolean, codecId:String="97", codecName:String="SPEEX", codecRate:String="16000"):Share
		{
		   
		   var codec:Codec = new Codec(codecId, codecName, codecRate);
		   
			// Force an open request for microphone permisson if we don't already have it - 
			// Flash will automatically open the box, so we need to be ready
         setTimeout(function():void {
   			if (_mic.muted) {
   				_boxOpen = true;
   				dispatchEvent( new MediaEvent(MediaEvent.OPEN) );			
   			}
         },1000);			
			
			var protocolName:String = getProtocolName(url);
			if (protocolName.toLowerCase() == "rtmp") {
				var streamName:String = getStreamName(url);	
				var rtmpUri:String = getRtmpUri(url);
				var nc:NetConnection = getNetConnection(rtmpUri);
			
				var queue:Array = _waitQs[rtmpUri]
				var share:Share = new RtmpShare(queue, nc, streamName, codec, url, _mic);
				if (autoStart) share.start();
				return share;
			} else return null;
		}
		
		public function get hasPermission():Boolean
		{
			if (_mic) {
				return !_mic.muted;
			}
			return false;
		}
				
		public function showPermissionBox():void
		{
			var audio:Audio = this;
			setTimeout(function():void {
				
				var count:Number = 0;
				
				dispatchEvent(new MediaEvent(MediaEvent.OPEN, audio));
				var alert:Alert = Alert.show('Click "OK" to continue.', "Success", Alert.OK, null, function(e:Event):void {
					dispatchEvent(new MediaEvent(MediaEvent.CLOSE, audio));
				});
				
				var listener:Function = function(event:flash.events.Event):void {
					if(count == 0) {
						Security.showSettings(SecurityPanel.PRIVACY);
						count = count +1;
					}
					else if(count == 1) {
						count = count +1;
					}
					else {
						alert.removeEventListener(flash.events.Event.RENDER, listener);
						alert.visible = false;
						dispatchEvent(new MediaEvent(MediaEvent.CLOSE, audio));
					}
				};
				
				alert.addEventListener(flash.events.Event.RENDER, listener);
				
			}, 1000);
			
		}	
		
		// --- Internal functions
	
		private function onMicStatus(event:StatusEvent):void 
		{
			if ((event.code == "Microphone.Unmuted" || event.code == "Microphone.Muted") && _boxOpen == true) {
			   _boxOpen = false;
				dispatchEvent( new MediaEvent(MediaEvent.CLOSE) );
			}
		}
		
		// Parse the uri, lookup or create a network connection and return it
		private function getNetConnection(uri:String):NetConnection
		{
			var nc:NetConnection;
			var rtmpUri:String = uri;
			if (_ncDict[rtmpUri] != null) nc = _ncDict[rtmpUri];
			else {
				NetConnection.defaultObjectEncoding = flash.net.ObjectEncoding.AMF0;	
				nc = new NetConnection();
				nc.objectEncoding = ObjectEncoding.AMF0;
				nc.client = this;
				nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onNCSecurityError);    
				nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onNCAsyncError);
				nc.addEventListener(NetStatusEvent.NET_STATUS, 
					function(event:NetStatusEvent):void
					{
						switch (event.info.code)
						{
							case "NetConnection.Connect.Success":
								// Now we can play and publish                	
								for each (var op:Object in _waitQs[rtmpUri]) {
									op();
								}	
								delete _waitQs[rtmpUri];
							break;
						}
					});
				nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onNCSecurityError);    
				nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onNCAsyncError);
				nc.connect(rtmpUri);
				// Allocate storage
				_ncDict[rtmpUri] = nc;
				_waitQs[rtmpUri] = new Array();
			}
			return nc;
		}
		
		// Parse the uri and return the rtmpUri
		private function getRtmpUri(uri:String):String
		{
			var rtmpUri:String = "";
			return uri.substr(0,uri.lastIndexOf("/"));
		}
		
		// Parse the uri and return the stream name
		private function getStreamName(uri:String):String
		{
			var streamName:String = "";
			return uri.substr(uri.lastIndexOf("/")+1);
		}
		
		// Parse the uri and return the protocol name
		private function getProtocolName(uri:String):String
		{
			var protocolName:String = "";
			var split:Array = uri.split(":");	
			protocolName = split[0];
			return protocolName;
		}
		
		internal function onNCSecurityError(event:SecurityErrorEvent):void
		{
			dispatchEvent( new MediaEvent(MediaEvent.ERROR) );
		}
		
		internal function onNCAsyncError(event:AsyncErrorEvent):void 
		{
			dispatchEvent( new MediaEvent(MediaEvent.ERROR) );
		}
		
		public function onBWDone():void
		{
			// Blank template for callback from rtmp relay
		}			
	}
}

