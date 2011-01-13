// Inspired by addEvent - Dean Edwards, 2005
;Phono.events = {
   handlerCount: 1,
   add: function(target, type, handler) {
      // ignore case
      type = type.toLowerCase();
		// assign each event handler a unique ID
		if (!handler.$$guid) handler.$$guid = this.handlerCount++;
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
		target["on" + type] = this.handle;
   },
   bind: function(target, config) {
      var name;
      for(k in config) {
   		if(k.match("^on")) {
   			this.add(target, k.substr(2).toLowerCase(), config[k]);
   		}
      }
   },
   remove: function(target, type, handler) {
      // ignore case
      type = type.toLowerCase();
		// delete the event handler from the hash table
		if (target.events && target.events[type]) {
			delete target.events[type][handler.$$guid];
		}
   },
   trigger: function(target, type, event, data) {
      event = event || {};
      event.type = type;
      var handler = target["on"+type.toLowerCase()]
      if(handler) {
         handler.call(target, event, data); 
      }
   },
   handle: function(event, data) {
   	// get a reference to the hash table of event handlers
   	var handlers = this.events[event.type.toLowerCase()];
   	// set event source
   	event.source = this;
   	// build arguments
   	var args = new Array();
   	args.push(event);
   	if(data) {
   	   var i;
   	   for(i=0; i<data.length; i++) {
   	      args.push(data[i]);
   	   }
   	}
   	var target = this;
   	// execute each event handler
   	Phono.util.each(handlers, function() {
         this.apply(target,args);
   	});
   }
};