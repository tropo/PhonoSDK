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

package mx.collaboration.xmpp.protocol
{
    
    import flash.events.*;
    import mx.collaboration.xmpp.protocol.events.*;
    
    /**
     * Event thrown when a new packet has been recieved that meets the criteria set
     * by the PacketFilter.
     */
    [Event("data")]
     
    /**
     * The PacketCollector class allows the catching of packets sent from the XMPP server
     * to the client that meet the criteria specified by the PacketFilter.
     */
    public class PacketCollector extends EventDispatcher
    {    
        private var _filter:PacketFilter;
        private var _filtering:Boolean;
        private var _stream:XMPPStream;
    
    	/**
    	 * Create a new PacketCollector.
    	 */
    	public function PacketCollector(stream:XMPPStream, filter:PacketFilter)
        {
            super();
            _stream = stream;                    
            _filter = filter;
        }        
    	
    	public function start():void
    	{
    		if( _stream != null )
	    		_stream.addEventListener( XMPPStreamEvent.DATA, onPacketReceived );
    	}
    	
    	/**
    	 * Stop this PacketCollector from receiving any more messages from the XMPPStream
    	 * that it is listening for messages on.
    	 */
    	public function stop():void
        {
        	if( _stream != null)
	            _stream.removeEventListener( XMPPStreamEvent.DATA, onPacketReceived );
        }

    	
    	/**
    	 * The filter that this collector is using to filter packets received
    	 * by the stream.
    	 */
    	public function get stream():XMPPStream
        {
            return _stream;
        }
    	public function set stream(streamArg:XMPPStream):void
        {
        	if( _filtering )
	        	stop();
            
            _stream = streamArg;
            
            if( _filtering )
            	start();
        }
    	
    	/**
    	 * The filter that this collector is using to filter packets received
    	 * by the stream.    	 */
    	public function get filter():PacketFilter
        {
            return _filter;
        }
    	public function set filter(filterArg:PacketFilter):void
        {
            _filter = filterArg;
        }
        
        /**
         * @private
         */
        public function onPacketReceived( event:XMPPStreamEvent ):void
        {
            if( _filter.passesFilterTest( event.packet ) )
            {
                dispatchEvent( new PacketCollectorEvent( PacketCollectorEvent.PACKET, true, false, event.packet ) );
            }      		
        }    	
    }

}