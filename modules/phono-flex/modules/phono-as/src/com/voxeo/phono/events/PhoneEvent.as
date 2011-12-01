package com.voxeo.phono.events
{
	import com.voxeo.phono.Phone;
	
	import flash.events.*;
	
	/**
	 * Events related to Phones.
	 */
	public class PhoneEvent extends Event
	{
		/**
		 * Dispatched when the phone connection to the server has disconencted
		 */
        public static const DISCONNECTED:String = "disconnect";
        /**
		 * Dispatched when the phone makes a successful connection to the server.
		 */
        public static const CONNECTED:String = "connect";
        /**
		 * Dispatched when an error is encountered.
		 */
        public static const ERROR:String = "phoneError";
        /**
		 * The reason for the error/disconnect is socket related.
		 */
		public static const REASON_SOCKET:String = "socket";

		/**
		 * The Phone object associated with this event.
		 */
		public var phone:Phone;	
		/**
		 * Additional information about this event.
		 */
        public var reason:String;
        		
		public function PhoneEvent(type:String,
									phone:Phone,
									reason:String,
		                            bubbles:Boolean=true,
    								cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
			this.phone = phone;
			this.reason = reason;
		}
	}
}