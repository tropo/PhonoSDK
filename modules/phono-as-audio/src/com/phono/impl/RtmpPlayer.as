package com.phono.impl
{
	import com.phono.Player;
	
	import flash.events.*;
	import flash.media.SoundTransform;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	
	public class RtmpPlayer implements Player
	{
		private var _volume:Number = 100;
		private var _url:String = null;
		private var _nc:NetConnection;
		private var _rx:NetStream;
		private var _streamName:String;
		private var _queue:Array;
		
		public function RtmpPlayer(queue:Array, nc:NetConnection, streamName:String, url:String)
		{
			_streamName = streamName;
			_url = url;
			_queue = queue;
			_nc = nc;
		}

		public function start():void
		{
			if (!_nc.connected) _queue.push(this.start);
			else {
				_rx = new NetStream(_nc);
				_rx.client = this;
				_rx.play(_streamName);
				_volume = _rx.soundTransform.volume;
			}
		}
		
		public function stop():void
		{
			if (!_nc.connected) _queue.push(this.stop);
			if (_rx != null) _rx.close(); 
		}
		
		public function set volume(vol:Number):void
		{
			_volume = (vol/100);
			if (_rx != null) {
				var transform:SoundTransform = _rx.soundTransform;
				transform.volume = _volume;
				_rx.soundTransform = transform;
			}
		}
		
		public function get volume():Number
		{
			return _volume;
		}
		
		public function get URL():String
		{
			return _url;
		}
	}
}