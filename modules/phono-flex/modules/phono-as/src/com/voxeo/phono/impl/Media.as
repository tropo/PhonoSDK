package com.voxeo.phono.impl
{
	import com.voxeo.phono.events.MediaEvent;
	
	import flash.events.*;
	import flash.media.Microphone;
	import flash.media.SoundCodec;
	import flash.media.SoundTransform;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.net.ObjectEncoding;
	import flash.net.Responder;
	
	import mx.utils.*;
	
	public class Media extends EventDispatcher
	{
		private var _nc:NetConnection;
		private var _rx:NetStream;
		private var _tx:NetStream;
		private var _playName:String;
		private var _publishName:String;
		private var _relayServer:String;
		public var mic:Microphone;
		
		public function Media()
		{
			mic = Microphone.getMicrophone();
            if (mic) {
 	           	mic.gain = 50;
    	       	mic.rate = 8;
            	mic.setSilenceLevel(0,2000);
            	mic.setUseEchoSuppression(true);
            	mic.codec = SoundCodec.SPEEX;
            	mic.framesPerPacket = 1;
            	//mic.gain = 50;
            	//trace("Microphone initialised");
          		mic.addEventListener(StatusEvent.STATUS, onMicStatus);			
            }
			
			NetConnection.defaultObjectEncoding = flash.net.ObjectEncoding.AMF0;	
            _nc = new NetConnection();
            _nc.objectEncoding = ObjectEncoding.AMF0;
            _nc.client = this;
            _nc.addEventListener(NetStatusEvent.NET_STATUS, onNCNetStatus);
            _nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onNCSecurityError);    
            _nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onNCAsyncError);
		}
		
		public function onBWDone():void
		{
			// Blank template for callback from rtmp relay
		}
				
		public function startAudio(uri:String="", playName:String="", publishName:String=""):void
		{
			if (uri!="") _relayServer = uri;
			if (playName!="") _playName = playName;
			if (publishName!="") _publishName = publishName;
			if (_nc != null) _nc.connect(_relayServer);
		}

 		public function stopAudio():void
  		{
  			var onResult:Function = function(result:Object):void 
            {
                trace("stopRelay: result:"+ObjectUtil.toString(result));
            };
            var onFault:Function = function(fault:Object):void 
            {
                trace("stopRelay: fault:"+ObjectUtil.toString(fault));
            };  

 			if (_rx != null) _rx.close(); 
            if (_tx != null) _tx.close();
            if (_nc != null) _nc.close();
  		}
										
		private function publish(publishName:String):void
  		{
  			trace("publish: " + publishName);
  			if (mic && publishName != "") {
	            _tx = new NetStream(_nc);
	            _tx.client = this;
	            _tx.addEventListener(NetStatusEvent.NET_STATUS, onNCNetStatus);
	            _tx.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onNCSecurityError);
	            _tx.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onNCAsyncError);
	            _tx.attachAudio(mic);
	            _tx.publish(publishName, "live");      
	  			if (mic.muted) {
					dispatchEvent( new MediaEvent(MediaEvent.OPEN) );			
				}  
  			}
  		}
  		 
  		private function play(playName:String):void
  		{
  			trace("play: " + playName);
  			if (playName != "") {
  				_rx = new NetStream(_nc);
            	_rx.client = this;
            	_rx.addEventListener(NetStatusEvent.NET_STATUS, onNCNetStatus);
            	_rx.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onNCSecurityError);
            	_rx.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onNCAsyncError);
            	_rx.play(playName);
     		}            
  		}
  		  	
  		private function onMicStatus(event:StatusEvent):void {
			if (event.code == "Microphone.Unmuted" || event.code == "Microphone.Muted") {
				dispatchEvent( new MediaEvent(MediaEvent.CLOSE) );
			}
		}
  		  			 				
		// callback invoked when stream is played out
		public function onPlayStatus(info:Object):void
        { 
            switch (info.code)
                {
                case "NetStream.Play.Complete":
                    trace("NetStream.Play.Complete");
                    break;

                default:
                    trace("onPlayStatus: info:"+ObjectUtil.toString(info));
                }
        }
		
		internal function onNCNetStatus(event:NetStatusEvent):void
		{
			switch (event.info.code)
            {
                case "NetConnection.Connect.Success":
                	dispatchEvent( new MediaEvent(MediaEvent.CONNECTED) );
                	// Now we can play and publish                	
                	if (_playName != "") play(_playName);
                	if (_publishName != "") publish(_publishName);
                	break; 
                case "NetConnection.Connect.Closed":
                	dispatchEvent( new MediaEvent(MediaEvent.DISCONNECTED) );
                	break;
                case "NetStream.Play.Start": // start is preceded by reset
               	case "NetStream.Publish.Start":
                case "NetConnection.Connect.Failed":
                case "NetConnection.Connect.Rejected":
                case "NetConnection.Connect.AppShutDown":
                case "NetConnection.Connect.InvalidApp":
                case "NetConnection.Call.BadVersion":
                case "NetConnection.Call.Prohibited":
                case "NetConnection.Call.Failed":
                case "NetStream.Record.Start":               
                case "NetStream.Play.Failed":
                case "NetStream.Play.Reset":                	
                case "NetStream.Play.Stop":
                case "NetStream.Play.UnpublishNotify":
                default:
                   //trace("onNCNetStatus: event:"+ObjectUtil.toString(event));
             }
             trace(event.info.code);
		}
		  		
  		public function sendDigit(digit:String):void
  		{
  			var onResult:Function = function(result:Object):void 
            {
                trace("sendDigit result:"+ObjectUtil.toString(result));
            };
            var onFault:Function = function(fault:Object):void 
            {
                trace("sendDigit: fault:"+ObjectUtil.toString(fault));
            };            

            _nc.call("sendDigit", new Responder(onResult, onFault), _playName, digit);
  		}
  		
  		public function setVolume(vol:Number):void
  		{
  			trace("setVolume("+vol+")");
  			if (_rx != null) {
  				var transform:SoundTransform = _rx.soundTransform;
            	transform.volume = vol;
            	_rx.soundTransform = transform;
  			}
  		}
  		
  		public function setMute(state:Boolean):void
  		{
  			if (state == true) {
  				mic.gain = 0;
  			} else {
  				mic.gain = 50;
  			}
  		}
  		
  		public function setHold(hold:Boolean):void
  		{
  			if (hold) {
  				stopAudio();
  			} else {
  				startAudio();
  			}
  		}
  		
  		internal function onNCSecurityError(event:SecurityErrorEvent):void
        {
            trace("onNCSecurityError: event:"+ObjectUtil.toString(event));
            dispatchEvent( new MediaEvent(MediaEvent.ERROR) );
        }

        internal function onNCAsyncError(event:AsyncErrorEvent):void 
        {
            trace("onNCAsyncError: event:"+ObjectUtil.toString(event));
            dispatchEvent( new MediaEvent(MediaEvent.ERROR) );
        }
	}
}