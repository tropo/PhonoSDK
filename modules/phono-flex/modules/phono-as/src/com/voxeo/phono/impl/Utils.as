package com.voxeo.phono.impl
{
	public class Utils
	{
		private static var _doubleQuote:RegExp = new RegExp('"', "g");
       	private static var _singleQuote:RegExp = new RegExp("'", "g");
		
		public static function escapeNode(node:String):String
		{
			node = node.replace(/\\/g, "\\5c");
			node = node.replace(/ /g, "\\20");
			node = node.replace(_doubleQuote, "\\22");
			node = node.replace(/&/g, "\\26");
			node = node.replace(_singleQuote, "\\27");
			node = node.replace(/\//g, "\\2f");
			node = node.replace(/:/g, "\\3a");
			node = node.replace(/</g, "\\3c");
			node = node.replace(/>/g, "\\3e");
			node = node.replace(/@/g, "\\40");
			
            return node;             
		}
		
		public static function unescapeNode(node:String):String
		{
			node = node.replace("\\20", " ");
			node = node.replace(_doubleQuote, '"');
			node = node.replace("\\26","&");
			node = node.replace(_singleQuote, "'");
			node = node.replace("\\2f", "/");
			node = node.replace("\\3a", ":");
			node = node.replace("\\3c", "<");
			node = node.replace("\\3e", ">");
			node = node.replace("\\40", "@");
			node = node.replace("\\5c", "\\");
            return node;             
		}
	}
}