package mx.collaboration.xmpp.protocol.extensions
{
	import mx.collections.ArrayCollection;
	import mx.collections.IViewCursor;
	import mx.collections.ListCollectionView;
	import mx.collections.Sort;
	import mx.collections.SortField;
	
	public class ExtensionManager
	{
		private var _extensionDescriptors:ArrayCollection;
		
		public function ExtensionManager()
		{
			// Here we setup the default extensions. In the xmpp protocol library we
			// add the authentication extensions. Roster Management and Blocking extensions
			// described in RFC 3921 would be added by a higher level library.
			_extensionDescriptors =
				new ArrayCollection(
					[
						new ExtensionDescriptor( "query", "jabber:iq:auth", Authentication, 0 ),
						new ExtensionDescriptor( Bind.ELEMENT_NAME, Bind.NAMESPACE_URI, Bind, 0 )			
					]
				);
		}
		
		/**
		 * Adds a new extension type to watch for on packets. Only unique elementName,
		 * namespaceUri, extensionType, combinations are added. If there is already a descriptor
		 * for this extension with the same arguments, it will not be added and 'false' will
		 * be returned.
		 */
		public function addExtension( elementName:String,
									  namespaceUri:String,
									  extensionType:Class ):Boolean
		{
			var cursor:IViewCursor = _extensionDescriptors.createCursor();
			while( !cursor.afterLast )
			{
				var d:ExtensionDescriptor = ExtensionDescriptor( cursor.current );
				if( d.elementName == elementName &&
					d.namespaceUri == namespaceUri &&
					d.extensionType == extensionType )
					return false;
				cursor.moveNext();
			}
			
			_extensionDescriptors.addItem( new ExtensionDescriptor( elementName,
																 	namespaceUri,
																 	extensionType,
																 	_extensionDescriptors.length ) );
			return true;
		}
		
		public function removeExtension( elementName:String,
												namespaceUri:String,
												extensionType:Class ):Boolean
		{
			var cursor:IViewCursor = _extensionDescriptors.createCursor();
			while( !cursor.afterLast )
			{
				var d:ExtensionDescriptor = ExtensionDescriptor( cursor.current );
				if( d.elementName == elementName &&
					d.namespaceUri == namespaceUri &&
					d.extensionType == extensionType )
				{
					cursor.remove();
					return true;
				}
				else
				{
					cursor.moveNext();
				}
			}
			return false;
		}
		
		public function getExtensionDescriptor( elementName:String, namespaceUri:String ):ExtensionDescriptor
		{
			var sort:Sort = new Sort();
				sort.fields = [ new SortField( "namespaceUri" ),
								new SortField( "order", false, false, true ) ];
				
			var view:ListCollectionView = new ListCollectionView( _extensionDescriptors.list );
				view.sort = sort;
				view.refresh();
				
			var cursor:IViewCursor = view.createCursor();
				
			if( cursor.findLast( { elementName:elementName, namespaceUri:namespaceUri } ) )
				return ExtensionDescriptor( cursor.current );
			else
				return null;
		}
		
		public function getExtensionDescriptors():ArrayCollection
		{
			return _extensionDescriptors;
		}
		
	}

}