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
	import flash.events.Event;
	import flash.events.EventDispatcher;
	
    
    /**
     * A JID represents a distinct entity on an XMPP network. It is more than just a
     * user. Users could have multiple JIDs, distinguished by different resources.
     * A JID is made up of a domain, node, and resource in the form of:
     * 
     * [ node "@" ] domain [ "/" resource ]
     * 
     * An example JID would be ddura@macromedia.com/Trillian. For more information JIDs
     * see http://www.xmpp.org/specs/rfc3920.html#addressing     */
    public class JID extends EventDispatcher
    {	
    	private var _node:String;
    	private var _domain:String;
    	private var _resource:String;
    	
    	/**
    	 * @param jid A string representation of the JID in the form node@domain/resource    	 */
    	public function JID(jid:String)
    	{
    		var a:Array  = jid.split("@");
    	
    		if(a.length > 1)
    		{
    			node = a[0];
    			
    			a = String(a[1]).split("/");
    		}
    		else
    		{
    			a = jid.split("/");
    		}
    		
    		domain = a[0];
    		
    		if(a.length > 1)
    		{
    			resource = a[1];
    		}
    	}
    	
    	[Bindable("domainChange")]
    	public function set domain(domainarg:String):void
    	{
    		_domain = domainarg;
    		dispatchEvent( new Event("domainChange") );
    		dispatchEvent( new Event("bareIdChange") );
   		}
        public function get domain():String { return _domain; }
    	
    	[Bindable("resourceChange")]
        public function set resource(resourcearg:String):void
        {
        	_resource = resourcearg;
        	dispatchEvent( new Event("resourceChange") );
    		dispatchEvent( new Event("bareIdChange") );
        }
        public function get resource():String { return _resource; }
    	
    	[Bindable("nodeChange")]
        public function set node(nodearg:String):void
        {
        	_node = nodearg;
        	dispatchEvent( new Event("nodeChange") );
    		dispatchEvent( new Event("bareIdChange") );
    		dispatchEvent( new Event("bareJidChange") );
        }
        public function get node():String { return _node; }
    	
    	[Bindable("bareIdChange")]
    	public function get bareId():String
    	{
    		var n:String = (_node) ? _node : "";
    		var d:String = (_domain) ? _domain : "";
    		var at:String = (_node && _domain) ? "@" : "";
    		return n + at + d;
    	}
    	
    	[Bindable("bareJidChange")]
    	public function get bareJid():JID
    	{
    		return new JID( bareId );
    	}
    	
    	/**
    	 * Returns a string representation of the JID in the form of:
    	 * 
    	 * [ node "@" ] domain [ "/" resource ]    	 */
        public override function toString():String
    	{
    		var jid:String = new String();
    		if(node != "" && node != null)
    		{
                jid = node + "@";
    		}
            jid += domain;
    		if(resource != "" && resource != null)
    		{
                jid += "/" + resource;
    		}
            return jid;
    	}
        
        
        public function valueOf():String
        {
            return toString();
        }
        
    }
    
}