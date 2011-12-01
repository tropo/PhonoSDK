package com.voxeo.phono
{
	/**
	 * The definitions of Call states which may be retrieved from the Call object using getState().
	 */
	public class CallState
	{
		/**
		 * The call is connected. Both parties should be able to speak.
		 */
		public static const STATE_CONNECTED:String = "connected";
		/**
		 * The call is in the ringing state.
		 */
		public static const STATE_RINGING:String = "ringing";
		/**
		 * The call is disconnected following either a local or remote hangup.
		 */
		public static const STATE_DISCONNECTED:String = "disconnected";
		/**
		 * The call is in progress, but not yet ringing or answered.
		 */
		public static const STATE_PROGRESS:String = "progress";
		/**
		 * The call has just been created and has not yet be dialed.
		 */
		public static const STATE_INITIAL:String = "initial";
		
		public function CallState()
		{
		}
	}
}