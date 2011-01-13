package com.phono
{
	public interface Share
	{
		function start():void;
		function stop():void;
		function digit(value:String, duration:Number=250, audible:Boolean=true):void;
		
		function get URL():String;
		function get codec():Codec;
		
		function get gain():Number;
		function set gain(value:Number):void;
		
		function set mute(value:Boolean):void;
		function get mute():Boolean;
	}
}