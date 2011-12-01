package com.voxeo.phono
{
	import com.voxeo.phono.events.CallEvent;
	import com.voxeo.phono.events.PhoneEvent;
	
	import flash.events.*;

	/**
	 * The Phone interface provides global phone level methods such as the ability to make a call, send a text message and register event handlers.
	 * Before making a call the Phone must be connected to a server by calling connect().
	 */	
	public interface Phone
	{	
		/**
     	 * Event generated when the Phone is successfully connected.
     	 */
    	[Event(PhoneEvent.CONNECTED)]
    	
    	/**
     	 * Event generated when the Phone is disconnected.
     	 */
    	[Event(PhoneEvent.DISCONNECTED)]
		
		/**
     	 * Event generated when the Phone encounters an error.
     	 */
    	[Event(PhoneEvent.ERROR)]
		
		/**
     	 * Event generated when a new call is created.
     	 */
    	[Event(CallEvent.CREATED)]
    	
    	/**
     	 * Event generated when a new message is received.
     	 */
    	[Event(MessageEvent.MESSAGE)]
		
	 	/**
	 	 * Connect to a given server using the credentials supplied. For standard use no server or credentials should be supplied and the phone will connect to a well known server. 
	 	 * 
	 	 * @param server The server to connect to.
	 	 * @param username The username used to authenticate.
	 	 * @param password The password used to authenticate.
	 	 */
		function connect(server:String="gw.phono.com", username:String="anon", password:String=""):void;
		
		/**
		 * Disconnects the phone from the signaling server  
		 */
		function disconnect():void;
		
		/**
		 * Show the permission box requesting access to the local microphone. This enables permission to be granted by the user before making a call.
		 */
		function showFlashPermissionBox():void;
		
		/**
		 * Create a new call object and dials the supplied 'to' destination application. The 'to' should be specified in the form "app:APPLICATION_ID" where APPLICATION_ID is the application id for the destination Voxeo application.
		 * 
		 * @param to The destination for the call. eg. sip:user@example.com.
		 * @return Returns a Call object representing the call.
		 */
		function dial(to:String):Call;	
					
		/**
		 * Factory method for creating a new Call object
		 * 
		 * @return Returns a Call object with a state of CallState.STATE_INITIAL
		 */
		function createCall():Call;
				
		/**
		 * Send an instant message to a remote user using XMPP.
		 * 
		 * @param to The desintation user of the message. eg. user@example.com
		 * @param body The body of the message to send.
		 * @param type The XMPP message type. Defaults to a 'chat' message.
		 * @param subject The XMPP message subject. Defaults to null.
		 * @param thread The XMPP thread identifier for the message. Defaults to null.
		 * @return Returns the phone object to enable method chaining.
		 */
		function text(to:String, body:String, type:String="chat", subject:String=null, thread:String=null):void;

		/**
		 * Add an event listener callback that will receive phone level events.
		 * 
		 * @param type The event type to listen for.
		 * @param listener The callback function that should be called on an event of 'type'.
		 */
		function addEventListener(type:String, listener:Function, useCapture:Boolean = false, priority:int = 0, useWeakReference:Boolean = false):void;

		/**
		 * Remove an event listener for phone level events.
		 * 
		 * @param type The type of event to listen for.
		 * @param listener The callback function that should not now be called on an event of 'type'.
		 */
		function removeEventListener(type:String, listener:Function, useCapture:Boolean = false):void;

		/**
		 * Get the current phone state.
		 * 
		 * @return Returns a string representing the phone state as defined in PhoneState.
		 */
		function get state():String;	
		
		/**
		 * Return the session Id that corresponds to this phone. This session Id may be used to place inbound calls to this phone using a destination of "sip:PHONE_SESSION_ID" from your Voxeo application.
		 * 
		 * @return Returns a string representing this session.
		 */
		function get sessionId():String;
		
		/**
		 * @private
		 */
		function get id():String;
		
		/**
		 * @private
		 */
		function set id(id:String):void;
		
		/**
		 * Used to determine if the Phone is connected to the signaling server 
		 * 
		 * @return Returns true if the Phone is currently connected to the signaling server
		 */
		function get connected():Boolean
					
		/**
		 * Set the ring tone URL to use (must be an MP3 file).
		 * 
		 * @param tone A URL to an MP3 file to play for ring sounds.
		 */
		function set ringTone(newTone:String):void;
		
		/**
		 * Get the current ring tone URL.
		 * 
		 * @return A string representing the current ring tone URL.
		 */
		function get ringTone():String;
		
		/**
		 * Set the ringback tone URL to use (must be an MP3 file). 
		 * 
		 * @param tone A URL to an MP3 file to play for ring sounds.
		 */
		function set ringbackTone(newTone:String):void;
		
		/**
		 * Get the current ringback tone URL.
		 * 
		 * @return A string representing the current ring tone URL.
		 */
		function get ringbackTone():String;
		
		/** 
		 * Set the tones status of the call.
		 * 
		 * @param enable A boolean value of true for toned on and false for tones off.
		 */ 
		function set tones(tones:Boolean):void;
		
		/** 
		 * Get the tones status of the call.
		 * 
		 * @return A boolean value of true for tones on and false for tones off.
		 */ 
		function get tones():Boolean;	
		
		/**
		 * Get the permission status of the micrphone
		 * 
		 * @return A boolean value of true if permission has already been granted, false if not
		 */
		function get flashPermissionState():Boolean;
		
		/** 
		 * Report an issue with this phone.
		 * 
		 * @param body Freeform text describing the problem encountered.
		 */
		function reportIssue(body:String):void;
	}
}