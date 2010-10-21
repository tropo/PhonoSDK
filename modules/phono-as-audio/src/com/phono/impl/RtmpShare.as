package com.phono.impl
{
	import com.phono.Audio;
	import com.phono.Codec;
	import com.phono.Share;
	
	import flash.media.Microphone;
	import flash.media.SoundCodec;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.net.ObjectEncoding;
	import flash.net.Responder;
	
	import mx.utils.*;
	
	public class RtmpShare implements Share
	{
		private var _audio:Audio;
		private var _nc:NetConnection;
		private var _tx:NetStream;
		private var _streamName:String;
		private var _url:String;
		private var _tones:Tones;
		private var _codec:Codec;
		private var _gain:Number;
		private var _mute:Boolean = false;
		private var _queue:Array;
		private var _mic:Microphone;
		
		public function RtmpShare(queue:Array, nc:NetConnection, streamName:String, codec:Codec, url:String, mic:Microphone)
		{
			_nc = nc;
			_streamName = streamName;
			_url = url;
			_tones = new Tones();
			_codec = codec;
			_mic = mic;
			_mic.setSilenceLevel(0,2000);
			_mic.gain = 50;
			_gain = _mic.gain;
			_mic.setUseEchoSuppression(true);			
			_mic.framesPerPacket = 1;
			_queue = queue;
			
			if (codec.name.toUpperCase() == "SPEEX") {
				_mic.codec = SoundCodec.SPEEX;
			} else _mic.codec = SoundCodec.SPEEX; // Anyway for now...
		}

		public function start():void
		{
			if (!_nc.connected) _queue.push(this.start);
			else {
				// Start the stream				
				if (_mic) {
					_tx = new NetStream(_nc);
					_tx.client = this;
					_tx.attachAudio(_mic);
					_tx.publish(_streamName, "live");
				}
			}
		}
		
		public function stop():void
		{
			if (!_nc.connected) _queue.push(this.stop);
			if (_tx != null) _tx.close();
		}
		
		public function digit(digit:String, duration:Number=250, audible:Boolean=true):void
		{
			var onResult:Function = function(result:Object):void 
			{
				trace("sendDigit result:"+ObjectUtil.toString(result));
			};
			var onFault:Function = function(fault:Object):void 
			{
				trace("sendDigit: fault:"+ObjectUtil.toString(fault));
			};            
			
			if (audible) _tones.play(digit, duration);
			_nc.call("sendDigit", new Responder(onResult, onFault), _streamName, digit);
		}
		
		public function get URL():String 
		{
			return _url;
		}
		
		public function get codec():Codec
		{
			return _codec;
		}
		
		public function get gain():Number
		{
			return _gain;
		}
		
		public function set gain(value:Number):void
		{
			_gain = value;	
			if (_mic != null) _mic.gain = _gain;
		}
		
		public function set mute(value:Boolean):void
		{
			_mute = value;
			if (_mute) {
				_mic.gain = 0;
			} else {
				_mic.gain = _gain;
			}
		}
		
		public function get mute():Boolean
		{
			return _mute;
		}		
	}
}