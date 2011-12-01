package com.voxeo.phono
{
	import com.voxeo.phono.events.CallEvent;
	
	/**
	 * The Call interface represents a specific call.
	 */	
	public interface Call
	{	
		/**
     	 * Event generated when a new call is received.
     	 */
    	[Event(CallEvent.CREATED)]
    	
    	/**
     	 * Event generated when a call is ringing.
     	 */
    	[Event(CallEvent.RINGING)]
    	   	
    	/**
     	 * Event generated when a call is answered.
     	 */
    	[Event(CallEvent.ANSWERED)]
    	
    	/**
     	 * Event generated when a call is hungup.
     	 */
    	[Event(CallEvent.HANGUP)]
    	
    	/**
     	 * Event generated when a digit is received on a call.
     	 */
    	[Event(CallEvent.DIGIT)]
    	
    	/**
     	 * Event generated when a call encounters an error.
     	 */
    	[Event(CallEvent.ERROR)]
    		
		/**
		 * Answer the call.
		 */
		function answer():void;
		
		/**
		 * Hangup the call.
		 */		
		function hangup():void;
		
		/**
		 * Set the call state to ringing. The remote party will then hear ringing until the
		 * call is answered.
		 * 
		 * @private
		 */
		function accept():void;

		/**
		 * Places an outgoing call to the target destination as specified by the "to" property.
		 * 
		 * @see to
		 */
		function start():void;
		
		/**
		 * Send a digit on the call.
		 * 
		 * @param digit A single character string representing the digit to send.
		 * @param length A number representing the length of time to play the digit for in milliseconds.
		 */		
		function digit(digit:String, length:Number=400):void;
		
		/**
		 * Add a custom SIP header to this call.
		 * 
		 * @param name The header name.
		 * @param data The data to provide in the header.
		 * @return the Call object.
		 */
		function addHeader(name:String, data:String):void;

		/**
		 * Get the unique identifier for the Call
		 * 
		 * @return Returns the Call's unique identifier
		 */
		function get id():String;	

		/**
		 * Get the current call state.
		 * 
		 * @return Returns a string representing the call state as defined in CallState
		 */
		function get state():String;	
		
		/**
		 * Get the target destination of this Call. 
		 * For outgoing calls this property contains the URI dialed by the user (e.g. app:1234).
		 * For incoming calls this property contains the Phone's sessionId as it was final recipient of the Call.
		 * 
		 * @return Returns a string representing the taget address.
		 */	
		function get to():String;

		/**
		 * Sets the target destination for the Call. The 'to' should be specified 
		 * in the form "app:APPLICATION_ID" where APPLICATION_ID is the application 
		 * id for the destination Voxeo application.
		 * 
		 * This property is read only for incoming calls.
		 * 
		 * @param to The target destination for the Call
		 */	
		function set to(to:String):void;
		
		/**
		/**
		 * Set push-to-talk mode.
		 * 
		 * @param ptt A boolean value of true for PTT enabled and false for PTT disabled.
		 * @return The Call object. 
		 */
		function set pushToTalk(ptt:Boolean):void;
		
		/**
		 * Get push-to-talk mode.
		 * 
		 * @return A boolean value of true for PTT enabled and false for PTT disabled. 
		 */
		function get pushToTalk():Boolean;
		
		/** 
		 * Set the push-to-talk state
		 * 
		 * @param talking A boolean value of true for talking and false or not talking.
		 * @return The Call object.
		 */
		function set talking(talking:Boolean):void;
		
		/**
		 * Set the output volume of the call. 
		 * 
		 * @param volume A value between 0 and 100 specifying the output volume.
		 * @return The Call object.
		 */
		function set volume(value:Number):void;
		
		/**
		 * Get the output volume of the call. 
		 * 
		 * @return volume A value between 0 and 100 specifying the output volume.
		 */
		function get volume():Number;
		
		/**
		 * Set the microphone mute status.
		 * 
		 * @private
		 * @param mute A boolean value of true for muted and false for un-muted.
		 * @return The Call object.
		 */
		function set muted(mute:Boolean):void;
		
		/**
		 * Get the microphone mute status.
		 * 
		 * @return A boolean value of true for muted and false for un-muted.
		 */
		function get muted():Boolean;
		
		/** 
		 * Set the hold status of the call.
		 * 
		 * @private
		 * @param hold A boolean value of true for hold and false for un-hold.
		 */ 
		function set hold(hold:Boolean):void;
		
		/** 
		 * Get the hold status of the call.
		 * 
		 * @return A boolean value of true for hold and false for un-hold.
		 */ 
		function get hold():Boolean;	
		
		/**
		 * Get the data for the given custom header. 
		 * 
		 * @param name The name of the header (must start with "X-")
		 * @return The data assocaited with that header, or "" if the header doesn't exist.
		 */
		function header(name:String):String;
		
		/** 
		 * Report an issue with this call.
		 * 
		 * @param body Freeform text describing the problem encountered.
		 */
		function reportIssue(body:String):void;
		
		/**
		 * Add an event listener callback that will receive call level events.
		 * 
		 * @param type The event type to listen for.
		 * @param listener The callback function that should be called on an event of 'type'.
		 */
		function addEventListener(type:String, listener:Function, useCapture:Boolean = false, priority:int = 0, useWeakReference:Boolean = false):void;
		
		/**
		 * Remove an event listener for call level events.
		 * 
		 * @param type The type of event to listen for.
		 * @param listener The callback function that should not now be called on an event of 'type'.
		 */
		function removeEventListener(type:String, listener:Function, useCapture:Boolean = false):void;
		
		
	}
}