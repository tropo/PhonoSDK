package com.voxeo.phono.impl
{
	import flash.events.SampleDataEvent;
	import flash.media.Sound;
	import flash.media.SoundChannel;
	import mx.core.SoundAsset;
	import flash.net.URLRequest;
	import flash.errors.IOError;
	
	import flash.utils.Dictionary;
	
	public class Ringer
	{
		private var _channel:SoundChannel;
		private var _sound:Sound;
		[Embed('/../assets/ringring.mp3')]
		private var _ringClass:Class;
		private var _uri:String = null;

		
		/**
		 * Create a new Ringer instance. If we are given a soundUri we will use that sound, otherwise use the canned one.
		 */		
		public function Ringer(soundUri:String = null) {
			ringTone = soundUri;			
		}
		
		public function set ringTone(soundUri:String):void
		{
			stop();
			if (soundUri != null) {
				_uri = soundUri;
				_sound = new Sound(new URLRequest(soundUri));
			} else {
				_uri = null;
				_sound = SoundAsset(new _ringClass());
			}
		}
		
		public function get ringTone():String
		{
			return _uri;
		}
		
		/**
		 * Play the ring sound we were initialized with.
		 */
		public function start():void {
			_channel = _sound.play(0, 1000);
		}
		
		/**
		 * Stop playing the ring sound.
		 */
		public function stop():void {
			if (_channel != null) _channel.stop();
			if (_sound != null) {
				try {
					_sound.close();
				} catch (e:IOError) {
					// Expected when not a remote stream
					trace("IOError on sound close (expected on remote stream):" + e);
				}
			}
		}		
	}
}
