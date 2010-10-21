;Phono.util = {
   guid: function() {
     return MD5.hexdigest((new Date()).getTime()) 
   },
   escapeXmppNode: function(input) {
      var node = input;
		node = node.replace(/\\/g, "\\5c");
		node = node.replace(/ /g, "\\20");
		node = node.replace(/\"/, "\\22");
		node = node.replace(/&/g, "\\26");
		node = node.replace(/\'/, "\\27");
		node = node.replace(/\//g, "\\2f");
		node = node.replace(/:/g, "\\3a");
		node = node.replace(/</g, "\\3c");
		node = node.replace(/>/g, "\\3e");
		node = node.replace(/@/g, "\\40");         
      return node;
   },
   // From jQuery 1.4.2
	each: function( object, callback, args ) {
		var name, i = 0,
			length = object.length,
			isObj = length === undefined || jQuery.isFunction(object);

		if ( args ) {
			if ( isObj ) {
				for ( name in object ) {
					if ( callback.apply( object[ name ], args ) === false ) {
						break;
					}
				}
			} else {
				for ( ; i < length; ) {
					if ( callback.apply( object[ i++ ], args ) === false ) {
						break;
					}
				}
			}

		// A special, fast, case for the most common use of each
		} else {
			if ( isObj ) {
				for ( name in object ) {
					if ( callback.call( object[ name ], name, object[ name ] ) === false ) {
						break;
					}
				}
			} else {
				for ( var value = object[0];
					i < length && callback.call( value, i, value ) !== false; value = object[++i] ) {}
			}
		}

		return object;
	},   
	isFunction: function( obj ) {
		return toString.call(obj) === "[object Function]";
	},

	isArray: function( obj ) {
		return toString.call(obj) === "[object Array]";
	},   
	isPlainObject: function( obj ) {
		if ( !obj || toString.call(obj) !== "[object Object]" || obj.nodeType || obj.setInterval ) {
			return false;
		}
		if ( obj.constructor
			&& !hasOwnProperty.call(obj, "constructor")
			&& !hasOwnProperty.call(obj.constructor.prototype, "isPrototypeOf") ) {
			return false;
		}
		var key;
		for ( key in obj ) {}
		
		return key === undefined || hasOwnProperty.call( obj, key );
	},	
   extend: function() {
   	var target = arguments[0] || {}, i = 1, length = arguments.length, deep = false, options, name, src, copy;
   	if ( typeof target === "boolean" ) {
   		deep = target;
   		target = arguments[1] || {};
   		i = 2;
   	}
   	if ( typeof target !== "object" && !jQuery.isFunction(target) ) {
   		target = {};
   	}
   	if ( length === i ) {
   		target = this;
   		--i;
   	}
   	for ( ; i < length; i++ ) {
   		if ( (options = arguments[ i ]) != null ) {
   			for ( name in options ) {
   				src = target[ name ];
   				copy = options[ name ];
   				if ( target === copy ) {
   					continue;
   				}
   				if ( deep && copy && ( jQuery.isPlainObject(copy) || jQuery.isArray(copy) ) ) {
   					var clone = src && ( jQuery.isPlainObject(src) || jQuery.isArray(src) ) ? src
   						: jQuery.isArray(copy) ? [] : {};
   					target[ name ] = jQuery.extend( deep, clone, copy );
   				} else if ( copy !== undefined ) {
   					target[ name ] = copy;
   				}
   			}
   		}
   	}
   	return target;
   },
   
   
   // Inspired by...
   // written by Dean Edwards, 2005
   // with input from Tino Zijdel, Matthias Miller, Diego Perini   
   eventCounter: 1,
   addEvent: function(target, type, handler) {
		// assign each event handler a unique ID
		if (!handler.$$guid) handler.$$guid = this.eventCounter++;
		// create a hash table of event types for the target
		if (!target.events) target.events = {};
		// create a hash table of event handlers for each target/event pair
		var handlers = target.events[type];
		if (!handlers) {
			handlers = target.events[type] = {};
			// store the existing event handler (if there is one)
			if (target["on" + type]) {
				handlers[0] = target["on" + type];
			}
		}
		// store the event handler in the hash table
		handlers[handler.$$guid] = handler;
		// assign a global event handler to do all the work
		target["on" + type] = handleEvent;
   },
   removeEvent: function(target, type, handler) {
		// delete the event handler from the hash table
		if (target.events && target.events[type]) {
			delete target.events[type][handler.$$guid];
		}
   },
   handleEvent: function(event) {
   	var returnValue = true;
   	// get a reference to the hash table of event handlers
   	var handlers = this.events[event.type];
   	// execute each event handler
   	for (var i in handlers) {
   		this.$$handleEvent = handlers[i];
   		if (this.$$handleEvent(event) === false) {
   			returnValue = false;
   		}
   	}
   	return returnValue;
   }

   
}