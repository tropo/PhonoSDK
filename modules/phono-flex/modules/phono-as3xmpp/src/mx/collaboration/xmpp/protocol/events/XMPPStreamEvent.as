/*
	Adobe Systems Incorporated(r) Source Code License Agreement
	Copyright(c) 2005 Adobe Systems Incorporated. All rights reserved.
	
	Please read this Source Code License Agreement carefully before using
	the source code.
	
	Adobe Systems Incorporated grants to you a perpetual, worldwide, non-exclusive, 
	no-charge, royalty-free, irrevocable copyright license, to reproduce,
	prepare derivative works of, publicly display, publicly perform, and
	distribute this source code and such derivative works in source or 
	object code form without any attribution requirements.  
	
	The name "Adobe Systems Incorporated" must not be used to endorse or promote products
	derived from the source code without prior written permission.
	
	You agree to indemnify, hold harmless and defend Adobe Systems Incorporated from and
	against any loss, damage, claims or lawsuits, including attorney's 
	fees that arise or result from your use or distribution of the source 
	code.
	
	THIS SOURCE CODE IS PROVIDED "AS IS" AND "WITH ALL FAULTS", WITHOUT 
	ANY TECHNICAL SUPPORT OR ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING,
	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
	FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  ALSO, THERE IS NO WARRANTY OF 
	NON-INFRINGEMENT, TITLE OR QUIET ENJOYMENT.  IN NO EVENT SHALL MACROMEDIA
	OR ITS SUPPLIERS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
	OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
	WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOURCE CODE, EVEN IF
	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package mx.collaboration.xmpp.protocol.events
{
    
    import flash.events.*;
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.packets.Packet;
    
    /**
     * For information on these events, see xmpp.XMPPStream.     */
    public class XMPPStreamEvent extends Event
    {
    	public static var DATA:String = "data";
        public static var DISCONNECT:String = "disconnect";
        public static var CONNECT:String = "connect";
        public static var ERROR:String = "error";
        
        private var _packet:Packet;
        private var _error:XML;
        
    	public function XMPPStreamEvent(type:String,
    								    bubbles:Boolean=true,
    								    cancelable:Boolean=false,
                                        packetArg:Packet=null,
                                        errorArg:XML=null)
    	{
    		super (type, bubbles, cancelable);
            
            error = errorArg;
            packet = packetArg;
    	}
        
        /**
         * This property contains the Packet object that the server has
         * sent to the client. This is only available with the DATA event.         */
        public function set packet(packetArg:Packet):void
        {
            _packet = packetArg;
        }
        
        public function get packet():Packet
        {
            return _packet;
        }
        
        /**
         * If an ERROR event has occured, this property contains the
         * <stream:error> stanza that the server returned.         */
        public function set error(errorArg:XML):void
        {
        	_error = errorArg;	
        }
        
        public function get error():XML
        {
        	return _error;	
        }
    }
    
}