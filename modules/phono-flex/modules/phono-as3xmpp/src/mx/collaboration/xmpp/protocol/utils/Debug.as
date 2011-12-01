
package mx.collaboration.xmpp.protocol.utils
{
    import flash.net.*;
    
    public class Debug
    {
    	// --------------------------------------------
    	// Class:	de.richinternet.utils.Dumper
    	//
    	// Description:	adds realtime trace/debug messaging to Flex
    	//
    	// Usage:		this class can be used in two ways, either static
    	//				or by using a 'hook'. 
    	//
    	// This code is fully in the public domain. An individual or company 
    	// may do whatever they wish with it. It is provided by the author 
    	// "as is" and without warranty, expressed or implied - enjoy!
    	// Dirk Eismann, deismann@herrlich-ramuschkat.de
        
    	private static var sender:LocalConnection = null;
    	
    	// loglevel bitmasks
    	public static var INFO:Number = 2;
    	public static var WARN:Number = 4;
    	public static var ERROR:Number = 8;
    	
    	// --- private constructor ---
    	public function Debug()
    	{	
    		// don't call this directly but use the
    		// static functions instead
    		return;
    	}
    	
    	// --- private setup function --- 
    	private static function initSender():void
    	{
    		sender = new LocalConnection();
    	}
    
    	// main function, use this from your application
    	// or one of the convenience methods below
    	public static function dump(val:Object, level:Number):void
    	{
    		if (sender == null) initSender();
    		if (isNaN(level)) level = 2;
    		sender.send("_tracer", "onMessage", val, level);
    	}
    
    	// --- public convenience methods ---
    	public static function trace(val:Object):void
    	{
    		dump(val, INFO);
    	}
    	
    	public static function info(val:Object):void
    	{
    		dump(val, INFO);
    	}
    	
    	public static function warn(val:Object):void
    	{
    		dump(val, WARN);
    	}
    	
    	public static function error(val:Object):void
    	{
    		dump(val, ERROR);
    	}
        
        public static function xmppOut(message:String):void
        {
            if (sender == null) initSender();
            sender.send("_tracer", "onXMPPOut", message);
        }

        public static function xmppIn(message:String):void
        {   
            if (sender == null) initSender();
            sender.send("_tracer", "onXMPPIn", message);
        }
        
    }

}