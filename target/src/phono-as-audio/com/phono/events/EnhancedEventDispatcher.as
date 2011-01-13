package com.phono.events
{
	import flash.events.*;

	public class EnhancedEventDispatcher extends EventDispatcher
	{
		
		private var wildcardListeners:Array = new Array()
		
		public override function dispatchEvent(event:Event):Boolean {
			for (var i:int = 0; i < wildcardListeners.length; i++) {
				wildcardListeners[i](event)
			}			
			return super.dispatchEvent(event);
		}
		
		public override function addEventListener(type:String, listener:Function, useCapture:Boolean = false, priority:int = 0, useWeakReference:Boolean = false):void {
			if(type == null) {
	 			wildcardListeners.push(listener);
			}
			else {
				super.addEventListener(type, listener, useCapture, priority, useWeakReference);
			}
		}
		
		public override function removeEventListener(type:String, listener:Function, useCapture:Boolean = false):void {
			if(type == null) {
				for (var i:int = 0; i < wildcardListeners.length; i++) {
					wildcardListeners = wildcardListeners.map(function(item:*):Boolean {
						return item != listener;
					});
				}			
				wildcardListeners.push(listener);
			}
			else {
				super.removeEventListener(type, listener, useCapture);
			}
		}

		
	}
}