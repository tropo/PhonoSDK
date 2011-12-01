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
     * The basic message element which will contain any information sent between
     * entities that is not structured data (IQ) or presence information (Presence).
     * Most messages sent through this mechanism include text messages between
     * client entities on the network. There are five categories that a message
     * element can be a member of as specified in the type property. Clients can
     * choose to handle the different message types in a way that helps distinguish
     * them to the user. For example, headline messages may be handled by being kept
     * in a list in which the user can browse and delete specific messages. On the
     * other hand, chat messages can be handled in the traditional manner by being
     * added to an expanding text field linearly.     */
    public class MessagePacket extends Packet
    {
    	/**
    	 * Simple messages that are often one time in nature and do not expect a
    	 * response. A similar mechanism would be considered email    	 */
    	public static var TYPE_NORMAL:String = "normal";

    	/**
    	 * Generic messages sent between entities. Intended to be responded to
    	 * in real time.    	 */
        public static var TYPE_CHAT:String = "chat";

    	/**
    	 * Messages sent to a multiuser chat room, intended to be answered
    	 * in real time.    	 */
        public static var TYPE_GROUPCHAT:String = "groupchat";

    	/**
    	 * Carrys news type information, much like what is contained in an RSS news feed.    	 */
        public static var TYPE_HEADLINE:String = "headline";

    	/**
    	 * Conveys error information to the client. This is intended to be readable by the
    	 * user. Many different entities can generate error messages.    	 */
        public static var TYPE_ERROR:String = "error";

		/*
		 *	MessagePacket
		 */
		
    	/**
    	 * The body of the message beind sent or received.    	 */
		public var body:String;
    	/**
    	 * The subject of a TYPE_HEADLINE or TYPE_NORMAL message. For multi user
    	 * chat, this property contains the topic of the room.    	 */
		public var subject:String;
    	/**
    	 * The thread property is used to specify snippets of a conversation that
    	 * are relavant to one another. If a message is received by a user containing
    	 * a thread, reply messages that are relavant to that conversation should contain
    	 * that thread id when sent to the originator.    	 */
		public var thread:String;
		
        public function MessagePacket()
        {
            super("message");
        }
		
		override public function processStanza( stanza:XML, stream:XMPPStream ):void
		{
			super.processStanza( stanza, stream );

            // Per section 2.1.1 of RFC 3921 (XMPP IM):
            // An IM application SHOULD support all of the foregoing message types; if an application
            // receives a message with no 'type' attribute or the application does not understand the
            // value of the 'type' attribute provided, it MUST consider the message to be of type "normal"
            // (i.e., "normal" is the default).			
			if(stanza.@type == undefined)
                type = TYPE_NORMAL;
		}
		
        override protected function processChild( value:XML, stanza:XML, stream:XMPPStream ):Boolean
        {
        	switch( value.name().localName )
        	{
        		case "body" : 	  body = value.valueOf();
        					  	  return true;
        		case "subject" :  subject = value.valueOf();
        						  return true;
        		case "thread" :   thread = value.valueOf();
        						  return true;
        	}
        	return super.processChild( value, stanza, stream );
        }
                
		override protected function generatePacketExtensions( xml:XML ):void
    	{
    		super.generatePacketExtensions( xml );
    		
    		if( body ) 	  xml.body = body;
    		if( subject ) xml.subject = subject;
    		if( thread )  xml.thread = thread;
    	}

    }
}