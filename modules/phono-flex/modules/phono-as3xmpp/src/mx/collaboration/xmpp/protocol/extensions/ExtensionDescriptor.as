package mx.collaboration.xmpp.protocol.extensions
{
	public class ExtensionDescriptor
	{
		public function ExtensionDescriptor( elementName:String,
											 namespaceUri:String,
											 extensionType:Class,
											 order:Number = 0 )
		{
			this.elementName = elementName;
			this.namespaceUri = namespaceUri;
			this.extensionType = extensionType;
			this.order = order;
		}
		
		public var elementName:String;
		public var namespaceUri:String;
		public var extensionType:Class;
		public var order:Number;
		
	}
}