package com.phono.impl
{
	import flash.events.SampleDataEvent;
	import flash.media.Sound;
	import flash.media.SoundChannel;
	import flash.utils.Dictionary;
	
	public class Tones
	{
		private var _digit:String;
		private var _duration:Number;
		
		private var _toneMap:Dictionary;
		private var _channel:SoundChannel;
		
		/**
		 * Create a new Tones instance.
		 */		
		public function Tones() {
			_toneMap = new Dictionary();
			_toneMap['1'] = [1209, 697];	
			_toneMap['2'] = [1336, 697];
			_toneMap['3'] = [1477, 697];
			_toneMap['4'] = [1209, 770];
			_toneMap['5'] = [1336, 770];
			_toneMap['6'] = [1477, 770];
			_toneMap['7'] = [1209, 852];
			_toneMap['8'] = [1336, 852];
			_toneMap['9'] = [1477, 852];
			_toneMap['*'] = [1209, 941];
			_toneMap['0'] = [1336, 941];
			_toneMap['#'] = [1477, 941];			
		}
		
		/**
		 * Play the given tone for the given duration. If invoked a second time while playing an existing tone the new tone will 
		 * replace the existing one. Only one tone may be played at a time.
		 * 
		 * @param digit The digit to play.
		 * @param dutation The length of the tone to play in milliseconds.
		 * @return A boolean indicating if the digit was successfully played.
		 */
		public function play(digit:String, duration:Number=250):Boolean {
			if (!_toneMap[digit]) {
				trace("invalid digit");
				return false;
			}
			_digit = digit;
			_duration = duration;

			if (_channel != null) _channel.stop();
			
			var toneSound:Sound = new Sound();			
			toneSound.addEventListener(SampleDataEvent.SAMPLE_DATA,waveGenerator);
			_channel = toneSound.play();
			return true;
		}
				
		private function waveGenerator(event:SampleDataEvent):void {
			if (event.position < 44100 * _duration/1000) {
				var val1:Number = (2*Math.PI) * _toneMap[_digit][0] / 44100;
				var val2:Number = (2*Math.PI) * _toneMap[_digit][1] / 44100;
				for ( var c:int=0; c<8192; c++ ) {					
					event.data.writeFloat(Math.sin(Number(c+event.position)*val1)/2
						+ Math.sin(Number(c+event.position)*val2)/2); // Left
					event.data.writeFloat(Math.sin(Number(c+event.position)*val1)/2
						+ Math.sin(Number(c+event.position)*val2)/2); // Right
				}
			}
		}		
	}
}
