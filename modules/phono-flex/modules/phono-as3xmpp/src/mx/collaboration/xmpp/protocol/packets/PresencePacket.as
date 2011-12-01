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

package mx.collaboration.xmpp.protocol.packets
{
    
    import mx.collaboration.xmpp.protocol.*;
    
    /**
     * This object is used to convey an entities availability on the network. For
     * the most part, it is clients who will convey presence to other entities, not
     * the server. But, the server can convey the unavailability of an entity. This
     * information is exchanged through a subscription mechanism. Entities must
     * subscribe to receive presence information from other entities. Subscriptions
     * are managed by the server and are closly related to the roster (think Buddy List).
     * When a client adds a user to its roster, cia the IQ element, the user has the
     * option of accepting or declining the clients request. Once a user has been added
     * to the clients roster and subscription has been approved, the client will receive
     * presence notifications from the user.     */
    public class PresencePacket extends Packet
    {
    	/**
    	 * The entity is connected to the network.    	 */
    	public static const TYPE_AVAILABLE:String = "available";
    	/**
    	 * Signals that the entity is no longer available for communication.
    	 */
    	public static const TYPE_UNAVAILABLE:String = "unavailable";
    	/**
    	 * A request for an entity's current presence; SHOULD be generated only by a server on behalf of a user.
    	 */
    	public static const TYPE_PROBE:String = "probe";
    	/**
    	 * The sender wishes to subscribe to the recipient's presence.
    	 */
    	public static const TYPE_SUBSCRIBE:String = "subscribe";
    	/**
    	 * The sender is unsubscribing from another entity's presence.
    	 */
    	public static const TYPE_UNSUBSCRIBE:String = "unsubscribe";
    	/**
    	 * The sender has allowed the recipient to receive their presence.
    	 */
    	public static const TYPE_SUBSCRIBED:String = "subscribed";
    	/**
    	 * The subscription request has been denied or a previously-granted subscription has been cancelled.
    	 */
    	public static const TYPE_UNSUBSCRIBED:String = "unsubscribed";
    	/**
    	 * An error has occurred regarding processing or delivery of a previously-sent presence stanza.
    	 */
    	public static const TYPE_ERROR:String = "error";
    	
    	/**
    	 * The entity or resource is temporarily away.    	 */
    	public static const SHOW_AWAY:String = "away";
    	/**
    	 * The entity or resource is actively interested in chatting.    	 */
    	public static const SHOW_CHAT:String = "chat";
    	/**
    	 * The entity or resource is busy (dnd = "Do Not Disturb").    	 */
    	public static const SHOW_DND:String = "dnd";
    	/**
    	 * The entity or resource is away for an extended period (xa = "eXtended Away"). For example, 
    	 * status may be "will be back from vacation in a week."    	 */
    	public static const SHOW_XA:String = "xa";
    	
    	/*
    	 *	PresencePacket
    	 */
    	
    	/**
    	 * The current presence of the user specified in this packet.    	 */
    	public var show:String;
    	/**
    	 * The status is a string with human readable text that further
    	 * elaborates on the 'show' status that the users presence
    	 * currently indicated. For example, if the current status is SHOW_AWAY,
    	 * the status may be 'Back in an hour after a meeting...'.    	 */
    	public var status:String;
    	/**
    	 * The priority of this resources presence information. For example,
    	 * you may be logged in as two resources (same user) and want to make
    	 * this client's presence priority higher than other clients so that it
    	 * appears as this in clients who have subscribed to your presence.    	 */
    	public var priority:Number;
    	
        public  function PresencePacket()
        {
        	super("presence");
        }
        
        override public function processStanza( stanza:XML, stream:XMPPStream ):void
		{
			super.processStanza( stanza, stream );

            // Per section 2.1.1 of RFC 3921 (XMPP IM):
            // An IM application SHOULD support all of the foregoing message types; if an application
            // receives a message with no 'type' attribute or the application does not understand the
            // value of the 'type' attribute provided, it MUST consider the message to be of type "normal"
            // (i.e., "normal" is the default).			
			if( !stanza.@type ) type = TYPE_AVAILABLE;
		}
		
        override protected function processChild( value:XML, stanza:XML, stream:XMPPStream ):Boolean
        {
        	switch( value.name().localName )
        	{
        		case "show" : 	  show = value.valueOf();
        					  	  return true;
        		case "status" :   status = value.valueOf();
        						  return true;
        		case "priority" : priority = value.valueOf();
        						  return true;
        	}
        	return super.processChild( value, stanza, stream );
        }
        
		override protected function generatePacketExtensions( xml:XML ):void
    	{
    		super.generatePacketExtensions( xml );
    		
    		if( show )     xml.show = show;
    		if( status )   xml.status = status;
    		if( priority ) xml.priority = priority;
    	}
    }

}    