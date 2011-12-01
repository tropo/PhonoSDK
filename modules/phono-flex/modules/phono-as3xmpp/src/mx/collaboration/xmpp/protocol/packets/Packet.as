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
    import flash.xml.*;
    
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.extensions.ExtensionDescriptor;
    import mx.collections.ArrayCollection;
    import mx.collections.IViewCursor;
    import mx.collections.ListCollectionView;
    import mx.collections.Sort;
    import mx.collections.SortField;
    import mx.utils.UIDUtil;
    
	use namespace xmpp_internal;    
    
    /**
     * The XMPP protocol specifies three types of packets (also called
     * stanzas) that can be sent in the context of an XMPP session. Specific
     * packets types are based off this class and have these attributes in
     * common.
     */
    public class Packet
    {	
    
        xmpp_internal static function createPacket( stanza:XML, stream:XMPPStream ):Packet
        {            
            var p:Packet;
            
            // Again, I am having to coerce this into a string, should I?
            var type:String = stanza.name();
            type = ( type.indexOf("::") > -1 ) ? type.substring( type.indexOf("::") + 2, type.length ) : type;
            switch( type )
            {
                case "iq"       : p = new IQPacket();
                                  p.processStanza( stanza, stream );
                                  break;
                case "message"  : p = new MessagePacket();
                                  p.processStanza( stanza, stream );
                                  break;
                case "presence" : p = new PresencePacket();
                                  p.processStanza( stanza, stream );
                                  break;
                default :         p = null;
            }
            
            return p;
        }
        
        /*
         *	Packet
         */
        
        public var jidTo:JID;
        public var jidFrom:JID;
        public var id:String;
        public var type:String;
        public var error:PacketError;
        
        private var _extensions:ArrayCollection;
        private var _error:XML;
        
        protected var packetName:String;
        
        public function Packet( packetName:String ):void
        {
        	this.packetName = packetName;
        	
        	_extensions = new ArrayCollection();
        }
        
        public function processStanza( stanza:XML, stream:XMPPStream ):void
        {
        	jidTo = 	new JID( stanza.@to );
        	jidFrom = 	new JID( stanza.@from );
        	id = 		stanza.@id;
        	type = 		stanza.@type;
        	
        	if( stanza.error.length() > 0 )
        	{
        		error = new PacketError();
        		error.type = stanza.error.@type;
        		if (stanza.error.children().lenght > 0)
        			error.condition = stanza.error.children()[0].name();
        		error.text = stanza.error.text;
        		error.code = stanza.error.@code;
        	}
        	
        	processChildren( stanza, stream );
        }
        
        private function processChildren( stanza:XML, stream:XMPPStream ):void
        {
        	for each( var x:XML in stanza.children() )
        	{
        		if( !processChild( x, stanza, stream ) )
        			processExtension( x, stanza, stream );
        	}
        }
        
        protected function processChild( value:XML, stanza:XML, stream:XMPPStream ):Boolean
        {
        	if( value.name() == "error" )
        	{
        		_error = value;
        		return true;
        	}
        	return false;
        }
        
        private function processExtension( value:XML, stanza:XML, stream:XMPPStream ):void
        {
        	if( value.@xmlns )
        	{
	    		var sort:Sort = new Sort();
	    			sort.fields = [ new SortField( "namespaceUri" ),
	    							new SortField( "order", false, true, true ) ];
	    			
	    		var descriptors:ListCollectionView = new ListCollectionView( stream.extensionManager.getExtensionDescriptors() );
	    		descriptors.sort = sort;
	    		descriptors.refresh();

	    		var cursor:IViewCursor = descriptors.createCursor();
	    		var extension:PacketExtension;
								
				if( cursor.findFirst( { elementName:value.name().localName,
										namespaceUri:value.name().uri } ) )
				{
					var extensionDescriptor:ExtensionDescriptor = ExtensionDescriptor( cursor.current );
					extension = new extensionDescriptor.extensionType();
				}
				else
				{
					extension = new PacketExtension();
				}

				//we first add the extension to the list of extensions so
				//that the extension will have it's parent packet value set.
				//this is just incase the processing depends on the packet type					
				addExtension( extension );
				extension.processExtension( value );				
        	}
        }
        
        public function attachUUID():String
        {
            var uid:String = UIDUtil.createUID();
            id = uid;
            return uid;
        }
    	
        public function addExtension( extension:PacketExtension ):void
        {
        	extension.packet = this;
        	_extensions.addItem( extension );
        }
        
        public function removeExtension( extension:PacketExtension ):void
        {
        	_extensions.removeItemAt( _extensions.getItemIndex( extension ) );
        }
        
        public function hasExtensionType( extensionType:Class ):Boolean
        {
        	return	_extensions.source.some(
        			function( element:PacketExtension, index:int, array:Array ):Boolean
        			{
        				return element is extensionType;
        			}, this );
        }
        
        public function getExtensionByType( extensionType:Class ):PacketExtension
        {
			var a:Array = getExtensionsByType( extensionType );
			return ( a.length > 0 ) ? a[0] : null;
        }
        
        public function getExtensionsByType( extensionType:Class ):Array
        {
			return _extensions.source.filter(
					function( item:*, index:Number, array:Array ):Boolean
					{
						return item is extensionType;
					}, this );
        }
        
        private function filter_byType( item:*, index:Number, array:Array ):Boolean
        {
        	var b:Boolean = item is PacketExtension;
			return b;
        } 
        
        public function getExtensions( elementName:String, namespaceUri:String ):Array
        {
			return _extensions.source.filter( 
							function( item:PacketExtension, index:Number, array:Array ):Boolean
							{
								return item.elementName == elementName && item.namespaceUri == namespaceUri;
							}, this );   
        }
        
        public function getExtension( elementName:String, namespaceUri:String ):PacketExtension
        {
			var a:Array = getExtensions( elementName, namespaceUri );
			return ( a.length > 0 ) ? a[0] : null;
        }
        
        public function get extensions():ArrayCollection
        {
        	return _extensions;
        }
        
        public function toXML():XML
    	{
    		var x:XML = new XML(<packet></packet>);
    		x.setName( packetName );
    		generatePacketAttributes( x );
    		generatePacketExtensions( x );
    		generatePacketErrors( x );
    		return x;
    	}
    	
    	protected function generatePacketAttributes( xml:XML ):void
    	{
    		if( !id ) attachUUID();
    		
    		if( jidTo )		xml.@to = jidTo.toString();
    		if( jidFrom ) 	xml.@from = jidFrom.toString();
    		if( id ) 		xml.@id = id;
    		if( type ) 		xml.@type = type;
    	}
    	
    	protected function generatePacketExtensions( xml:XML ):void
    	{
    		var cursor:IViewCursor = _extensions.createCursor();
    		while( !cursor.afterLast )
    		{
    			xml.appendChild( PacketExtension( cursor.current ).toXML() );
    			cursor.moveNext();
    		}
    	}
        
        protected function generatePacketErrors( xml:XML ):void
        {
        	if (error) xml.appendChild( error.toXML() );
        }
        
    }
    
}

    	/**
    	 * The intended recepient of this packet.
    	 */
    	
    	/**
    	 * The sender of the packet.
    	 */
    	
    	/**
    	 * Used for internal tracking of stanzas that are sent and received.
    	 */
    	/**
    	 * Detailed information about the purpose or context of the message,
    	 * presence, or IQ packet. The particular allowable values for the type
    	 * attribute vary depending on whether the packet is a message, presence, or IQ
    	 */
    	
    	/**
    	 * Return the error type for this packet error.
    	 */
