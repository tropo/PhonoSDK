;Phono.util = {
   guid: function() {
     return MD5.hexdigest(new String((new Date()).getTime())) 
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
			isObj = length === undefined || $.isFunction(object);

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
   	if ( typeof target !== "object" && !$.isFunction(target) ) {
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
   				if ( deep && copy && ( $.isPlainObject(copy) || $.isArray(copy) ) ) {
   					var clone = src && ( $.isPlainObject(src) || $.isArray(src) ) ? src
   						: $.isArray(copy) ? [] : {};
   					target[ name ] = $.extend( deep, clone, copy );
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
   },
    /* parseUri JS v0.1, by Steven Levithan (http://badassery.blogspot.com)
       Splits any well-formed URI into the following parts (all are optional):
       ----------------------
       • source (since the exec() method returns backreference 0 [i.e., the entire match] as key 0, we might as well use it)
       • protocol (scheme)
       • authority (includes both the domain and port)
       • domain (part of the authority; can be an IP address)
       • port (part of the authority)
       • path (includes both the directory path and filename)
       • directoryPath (part of the path; supports directories with periods, and without a trailing backslash)
       • fileName (part of the path)
       • query (does not include the leading question mark)
       • anchor (fragment)
    */
    parseUri: function(sourceUri) {
        var uriPartNames = ["source","protocol","authority","domain","port","path","directoryPath","fileName","query","anchor"];
        var uriParts = new RegExp("^(?:([^:/?#.]+):)?(?://)?(([^:/?#]*)(?::(\\d*))?)?((/(?:[^?#](?![^?#/]*\\.[^?#/.]+(?:[\\?#]|$)))*/?)?([^?#/]*))?(?:\\?([^#]*))?(?:#(.*))?").exec(sourceUri);
        var uri = {};
        
        for(var i = 0; i < 10; i++){
        uri[uriPartNames[i]] = (uriParts[i] ? uriParts[i] : "");
        }
        
        // Always end directoryPath with a trailing backslash if a path was present in the source URI
        // Note that a trailing backslash is NOT automatically inserted within or appended to the "path" key
        if(uri.directoryPath.length > 0){
            uri.directoryPath = uri.directoryPath.replace(/\/?$/, "/");
        }
    
        return uri;
    },
    filterWideband: function(offer, wideband) {
        var codecs = new Array();
        Phono.util.each(offer, function() {
            if (!wideband) {
                if (this.name.toUpperCase() != "G722" && this.rate != "16000") {
                    codecs.push(this);
                }
            } else {
                codecs.push(this);
            }
        });
        return codecs;
    },
    isIOS: function() {
        var userAgent = window.navigator.userAgent;
        if (userAgent.match(/iPad/i) || userAgent.match(/iPhone/i)) {
            return true;
        }
        return false;
    },
    isAndroid: function() {
        var userAgent = window.navigator.userAgent;
        if (userAgent.match(/Android/i)) {
            return true;
        }
        return false;
    },
    localUri : function(fullUri) {
        var splitUri = fullUri.split(":");
        return splitUri[0] + ":" + splitUri[1] + ":" + splitUri[2];
    },
    loggify: function(objName, obj) {
        for(prop in obj) {
            if(typeof obj[prop] === 'function') {
                Phono.util.loggyFunction(objName, obj, prop);
            }
        }
        return obj;
    },
    loggyFunction: function(objName, obj, funcName) {
        var original = obj[funcName];
        obj[funcName] = function() {

            // Convert arguments to a real array
            var sep = "";
            var args = "";
            for (var i = 0; i < arguments.length; i++) {
                args+= (sep + arguments[i]);
                sep = ",";
            }
            
            Phono.log.debug("[INVOKE] " + objName + "." + funcName + "(" + args  + ")");
            return original.apply(obj, arguments);
        }
    },
    padWithZeroes: function(num, len) {
        var str = "" + num;
        while (str.length < len) {
            str = "0" + str;
        }
        return str;
    },
    padWithSpaces: function(str, len) {
        while (str.length < len) {
            str += " ";
        }
        return str;
    }
};

