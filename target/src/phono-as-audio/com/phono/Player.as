package com.phono
{
	public interface Player
	{		
		function start():void;
		function stop():void;
		function set volume(vol:Number):void;
		function get volume():Number;
		function get URL():String;
	}
}