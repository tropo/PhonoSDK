package com.voxeo.phono
{
	import com.voxeo.phono.events.PhoneEvent;
	import com.voxeo.phono.impl.xmpp.jingle.JinglePhone;
	
	/**
	 * The factory class for obtaining Phone objects.
	 */
	public class Factory
	{
		/**
		 * Allocate a new Phone object
		 * 
		 * @return A new Phone object that is disconnected.
		 */
		public static function createPhone():JinglePhone
		{
			var phone:JinglePhone = new JinglePhone();
			trace("Phone()");
			return phone; 
		}
	}
}