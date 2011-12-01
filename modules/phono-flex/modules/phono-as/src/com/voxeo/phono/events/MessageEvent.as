package com.voxeo.phono.events
{
	import com.voxeo.phono.Phone;
	import com.voxeo.phono.Message;
	
	import flash.events.*;
	
	/**
	 * Events related to text Messages.
	 */
	public class MessageEvent extends PhoneEvent
	{
		/**
		 * Dispatched when a new inbound text message has arrived.
		 */
        public static const MESSAGE:String = "incomingText";
            
        /**
		 * The Message object associated with this event.
		 */
        public var message:Message;
		
		public function MessageEvent(type:String,
									 phone:Phone,
									 message:Message,
    								 bubbles:Boolean=true,
    								 cancelable:Boolean=false)
		{
			super(type, phone, null, bubbles, cancelable);
			this.message = message;
		}
	}
}