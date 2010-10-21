package com.phono.events
{
	import com.phono.Audio;
	
	import flash.events.*;
	
	/**
	 * Events related to Media.
	 */
	public class MediaEvent extends Event
	{
        /**
		 * Dispatched when flash needs to open a permission box for access to the local microphone.
		 */
        public static const OPEN:String = "permissionBoxShow";
        /**
		 * Dispatched when flash has finished with the permission box.
		 */
		public static const CLOSE:String = "permissionBoxHide";
		/**
		 * Dispatched when an error is encountered with media.
		 */
		public static const ERROR:String = "mediaError";
        /**
		 * The Audio object associated with this media event.
		 */
        public var audio:Audio;
        		
		public function MediaEvent(type:String,
								   	audio:Audio=null,
								    bubbles:Boolean=true,
    								cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
			this.audio = audio;
		}
	}
}