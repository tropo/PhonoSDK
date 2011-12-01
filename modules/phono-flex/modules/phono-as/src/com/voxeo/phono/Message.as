package com.voxeo.phono
{
	import com.voxeo.phono.events.MessageEvent;
	
	/**
	 * The Message interface represents a specific text message and may be used to query information about that message and create and send a reply.
	 */	
	public interface Message
	{
		/**
     	 * Event generated when a new message is received.
     	 */
    	[Event(MessageEvent.MESSAGE)]
		
		/**
		 * Get the sender of the message.
		 * 
		 * @return The message sender as a string.
		 */
		function get from():String;
		
		/**
		 * Get the body of the message.
		 * 
		 * @return A string representing the message body text.
		 */
		function get body():String;
		
		/**
		 * Set the body of the message.
		 * 
		 * @param body The message body text as a string.
		 */
		function set body(body:String):void
		
		/**
		 * Get the type of the message.
		 * 
		 * @return The message type as a string.
		 */
		function get type():String;
		
		/**
		 * Set the type of the message.
		 * 
		 * @param type The message type as a string.
		 */
		function set type(type:String):void;
		
		/**
		 * Get the subject of the message.
		 * 
		 * @return The message subject as a string.
		 */
		function get subject():String;
		
		/**
		 * Set the subject of the message.
		 * 
		 * @param subject The message subject as a string.
		 */
		function set subject(subject:String):void;
		
		/** 
		 * Get the thread id of a message.
		 * 
		 * @return The thread id as a string.
		 */
		function get thread():String;
		
		/**
		 * Set the thread id.
		 * 
		 * @param thread The thread id as a string.
		 */
		function set thread(thread:String):void;
		
		/**
		 * Reply to this message with the given body text.
		 * 
		 * @body The reply body text as a string.
		 */
		function reply(body:String):void;		
	}
}