package com.phono.events
{
    import com.phono.Audio;
    import flash.events.*;
    
    /**
     * Events related to Media.
     */
    public class MediaEvent extends Event
    {
        /**
	 * Dispatched when flash needs to open a permission box for access to the local microphone.
	 */
        public static const OPEN:String = "permissionBoxShow";
        /**
	 * Dispatched when flash has finished with the permission box.
	 */
	public static const CLOSE:String = "permissionBoxHide";
        /**
	 * Dispatched when flash is ready with media
	 */
	public static const READY:String = "mediaReady";
	/**
	 * Dispatched when an error is encountered with media.
	 */
	public static const ERROR:String = "mediaError";
        /**
	 * The Audio object associated with this media event.
	 */
        public var audio:Audio;
        /**
          * A descriptive reason sting
          */
        public var reason:String;
        
	public function MediaEvent(type:String,
				   audio:Audio=null,
                                   reason:String="",
				   bubbles:Boolean=true,
    				   cancelable:Boolean=false)
	{
	    super(type, bubbles, cancelable);
	    this.audio = audio;
            this.reason = reason;
	}
    }
}