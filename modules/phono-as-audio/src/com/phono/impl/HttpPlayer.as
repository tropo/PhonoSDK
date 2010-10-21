package com.phono.impl
{
	import com.phono.Player;
	
	import flash.media.Sound;
	import flash.media.SoundChannel;
	import mx.core.SoundAsset;
	import flash.net.URLRequest;
	import flash.errors.IOError;
	
	public class HttpPlayer implements Player
	{
		private var _channel:SoundChannel;
		private var _sound:Sound;
		private var _uri:String = null;
		private var _volume:Number;
	
		public function HttpPlayer(soundUri:String)
		{
			_uri = soundUri;
			_sound = new Sound(new URLRequest(soundUri));
		}

		public function start():void
		{
			if (_channel == null) {
				_channel = _sound.play(0, 1000);	
				_channel.soundTransform.volume = _volume / 100; 
			}
		}
		
		public function stop():void
		{
			if (_channel != null) {
			   _channel.stop();
			   _channel = null;
		   }
		}
		
		public function set volume(vol:Number):void
		{
			_volume = vol;
			if (_channel != null) _channel.soundTransform.volume = _volume / 100; 
		}
		
		public function get volume():Number
		{
			return _volume;
		}
		
		public function get URL():String
		{
			return _uri;
		}
	}
}