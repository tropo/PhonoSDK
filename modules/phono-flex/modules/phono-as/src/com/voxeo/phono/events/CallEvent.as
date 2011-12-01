package com.voxeo.phono.events
{
	import com.voxeo.phono.Call;
	import com.voxeo.phono.Phone;
	
	import flash.events.*;
	
	/**
	 * Events related to Calls.
	 */
	public class CallEvent extends Event
	{
		/**
		 * Dispatched when a new inbound call has arrived.
		 */
        public static const CREATED:String = "incomingCall";
        /**
         * Dispatched when an outbound call is ringing.
         */
        public static const RINGING:String = "ringing";
        /**
         * Dispatched when an outbound call is answered.
         */
        public static const ANSWERED:String = "answer";
        /**
         * Dispatched when a call is terminated by the remote party.
         */
        public static const HANGUP:String = "hangup";
        /**
         * Dispatched when a dtmf digit is received from the remote party. 
         */
        public static const DIGIT:String = "digit";
        /**
         * Dispatched when an error is reported relating to this call.
         */
        public static const ERROR:String = "callError";
        
        /**
         * In the case of a DIGIT event this represents the digit received.
         */
        public var digit:String;
        /**
         * In the case of a DIGIT event this represents the duration of the digit received.
         */
        public var digitDuration:Number;
        /**
         * Additional information relating to the event.
         */ 
        public var reason:String;
        /**
         * The Call object associated with this event.
         */
        public var call:Call;
        /**
         * The Phone object associated with this event.
         */
        public var phone:Phone;
		
		public function CallEvent(type:String,
									phone:Phone,
									call:Call,
    								reason:String="",
		                            bubbles:Boolean=true,
    								cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);			
			this.call = call;
			this.reason = reason;
			this.phone = phone;
		}
	}
}