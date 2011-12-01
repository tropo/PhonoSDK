package com.voxeo.phono
{
	/**
	 * The definitions of Phone states which may be retrieved from the Phone object using getState().
	 */
	public class PhoneState
	{
		/**
		 * The phone is connected to the server and should be able to make calls and send text messages.
		 */
		public static const STATE_CONNECTED:String = "connected";
		/**
		 * THe phone id disconnected from the server and will not be able to make calls or send text messages.
		 */
		public static const STATE_DISCONNECTED:String = "disconnected";
		/**
		 * The call is in progress, but not yet ringing or answered.
		 */
		
		public function PhoneState()
		{
		}
	}
}