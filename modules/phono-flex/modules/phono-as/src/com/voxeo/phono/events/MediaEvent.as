package com.voxeo.phono.events
{
	import com.voxeo.phono.Phone;
	
	import flash.events.*;
	
	/**
	 * Events related to Media.
	 */
	public class MediaEvent extends Event
	{
		/**
		 * Dispatched when media connection is disconnected.
		 */
        public static const DISCONNECTED:String = "mediaDisconnect";
        /**
		 * Dispatched when media connection is connected..
		 */
        public static const CONNECTED:String = "mediaConnect";
        /**
		 * Dispatched when an error is encountered with media.
		 */
        public static const ERROR:String = "mediaError";
        /**
		 * Dispatched when flash needs to open a permission box for access to the local microphone.
		 */
        public static const OPEN:String = "flashPermissionShow";
        /**
		 * Dispatched when flash has finished with the permission box.
		 */
		public static const CLOSE:String = "flashPermissionHide";
		/**
		 * Additional information about the event.
		 */
        public var reason:String;
        /**
		 * The Phone object associated with this media event.
		 */
        public var phone:Phone;
        		
		public function MediaEvent(type:String,
									phone:Phone=null,
									reason:String="",
		                            bubbles:Boolean=true,
    								cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
			this.reason = reason;
			this.phone = phone;
		}
	}
}