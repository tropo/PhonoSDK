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
	[Bindable]
	public class PacketError
	{
		public var type:String;
		public var condition:String;
		public var code:String;
		public var text:String;
		public var customXML:XML;
		
    	/**
    	 * do not retry (the error is unrecoverable)
    	 */
    	public static var TYPE_CANCEL:String = "cancel";
    	
    	/**
    	 * proceed (the condition was only a warning)
    	 */
    	public static var TYPE_CONTINUE:String = "continue";
    	
    	/**
    	 * retry after changing the data sent
    	 */
    	public static var TYPE_MODIFY:String = "modify";
    	
    	/**
    	 * retry after providing credentials
    	 */
    	public static var TYPE_AUTH:String = "auth";
    	
    	/**
    	 * retry after waiting (the error is temporary)
    	 */
    	public static var TYPE_WAIT:String = "wait";
    	
    	/**
    	 * the sender has sent XML that is malformed or that cannot be processed (e.g., an IQ stanza that includes an unrecognized value of the 'type' attribute); the associated error type SHOULD be "modify".
    	 */
    	public static var CONDITION_BAD_REQUEST:String = "bad-request"; 
    		
    	/**
    	 * access cannot be granted because an existing resource or session exists with the same name or address; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_CONFLICT:String = "conflict"; 
    		
    	/**
    	 * the feature requested is not implemented by the recipient or server and therefore cannot be processed; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_FEATURE_NOT_IMPLEMENTED:String = "feature-not-implemented"; 
    	
    	/**
    	 * the requesting entity does not possess the required permissions to perform the action; the associated error type SHOULD be "auth".
    	 */
    	public static var CONDITION_FORBIDDEN:String = "forbidden"; 
    		
    	/**
    	 * the recipient or server can no longer be contacted at this address (the error stanza MAY contain a new address in the XML character data of the <gone/> element); the associated error type SHOULD be "modify".
    	 */
    	public static var CONDITION_GONE:String = "gone"; 
    	
    	/**
    	 * the server could not process the stanza because of a misconfiguration or an otherwise-undefined internal server error; the associated error type SHOULD be "wait".
    	 */
    	public static var CONDITION_INTERNAL_SERVER_ERROR:String = "internal-server-error"; 
    	
    	/**
    	 * the addressed JID or item requested cannot be found; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_ITEM_NOT_FOUND:String = "item-not-found"; 
    	
    	/**
    	 * the sending entity has provided or communicated an XMPP address (e.g., a value of the 'to' attribute) or aspect thereof (e.g., a resource identifier) that does not adhere to the syntax defined in Addressing SchemeAddressing Scheme; the associated error type SHOULD be "modify".
    	 */
    	public static var CONDITION_JID_MALFORMED:String = "jid-malformed"; 
    		
    	/**
    	 * the recipient or server understands the request but is refusing to process it because it does not meet criteria defined by the recipient or server (e.g., a local policy regarding acceptable words in messages); the associated error type SHOULD be "modify".
    	 */
    	public static var CONDITION_NOT_ACCEPTABLE:String = "not-acceptable"; 
    	
    	/**
    	 * the recipient or server does not allow any entity to perform the action; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_NOT_ALLOWED:String = "not-allowed"; 
    	
    	/**
    	 * the sender must provide proper credentials before being allowed to perform the action, or has provided improper credentials; the associated error type SHOULD be "auth".
    	 */
    	public static var CONDITION_NOT_AUTHORIZED:String = "not-authorized"; 
    	
    	/**
    	 * the requesting entity is not authorized to access the requested service because payment is required; the associated error type SHOULD be "auth".
    	 */
    	public static var CONDITION_PAYMENT_REQUIRED:String = "payment-required"; 
    	
    	/**
    	 * the intended recipient is temporarily unavailable; the associated error type SHOULD be "wait" (note: an application MUST NOT return this error if doing so would provide information about the intended recipient's network availability to an entity that is not authorized to know such information).
    	 */
    	public static var CONDITION_RECIPIENT_UNAVAILABLE:String = "recipient-unavailable"; 
    	
    	/**
    	 * the recipient or server is redirecting requests for this information to another entity, usually temporarily (the error stanza SHOULD contain the alternate address, which MUST be a valid JID, in the XML character data of the <redirect/> element); the associated error type SHOULD be "modify".
    	 */
    	public static var CONDITION_REDIRECT:String = "redirect"; 
    	
    	/**
    	 * the requesting entity is not authorized to access the requested service because registration is required; the associated error type SHOULD be "auth".
    	 */
    	public static var CONDITION_REGISTRATION_REQUIRED:String = "registration-required"; 
    	
    	/**
    	 * a remote server or service specified as part or all of the JID of the intended recipient does not exist; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_REMOTE_SERVER_NOT_FOUND:String = "remote-server-not-found"; 
    	
    	/**
    	 * a remote server or service specified as part or all of the JID of the intended recipient (or required to fulfill a request) could not be contacted within a reasonable amount of time; the associated error type SHOULD be "wait".
    	 */
    	public static var CONDITION_REMOTE_SERVER_TIMEOUT:String = "remote-server-timeout"; 
    	
    	/**
    	 * the server or recipient lacks the system resources necessary to service the request; the associated error type SHOULD be "wait".
    	 */
    	public static var CONDITION_RESOURCE_varRAINT:String = "resource-varraint"; 
    	
    	/**
    	 * the server or recipient does not currently provide the requested service; the associated error type SHOULD be "cancel".
    	 */
    	public static var CONDITION_SERVICE_UNAVAILABLE:String = "service-unavailable"; 
    	
    	/**
    	 * the requesting entity is not authorized to access the requested service because a subscription is required; the associated error type SHOULD be "auth".
    	 */
    	public static var CONDITION_SUBSCRIPTION_REQUIRED:String = "subscription-required"; 
    	
    	/**
    	 * the error condition is not one of those defined by the other conditions in this list; any error type may be associated with this condition, and it SHOULD be used only in conjunction with an application-specific condition.
    	 */
    	public static var CONDITION_UNDEFINED_CONDITION:String = "undefined-condition"; 
    	
    	/**
    	 * the recipient or server understood the request but was not expecting it at this time (e.g., the request was out of order); the associated error type SHOULD be "wait".
    	 */
    	public static var CONDITION_UNEXPECTED_REQUEST:String = "unexpected-request";		
    	
    	public function toXML():XML
		{
			var x:XML = <error></error>
			if (type) x.@type = type;
			if (condition) {
				x.appendChild(new XML("<"+condition+"/>").@xmlns="urn:ietf:params:xml:ns:xmpp-stanzas");				
			}
			if (customXML) {
				x.appendChild(customXML);
			}
			if (text) x.text = text;
			if (code) x.@code = code;
			return x;
		}
		
	}
}