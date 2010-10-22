;(function() {

   Strophe.addNamespace('JINGLE', "urn:xmpp:jingle:1");
   Strophe.addNamespace('JINGLE_SESSION_INFO',"urn:xmpp:jingle:apps:rtp:1:info");

   var CallState = {
       CONNECTED: 0,
       RINGING: 1,
       DISCONNECTED: 2,
       PROGRESS: 3,
       INITIAL: 4
   };

   var Direction = {
       OUTBOUND: 0,
       INBOUND: 1
   };
   
   // Call
   //
   // A Call is the central object in the Phone API. Calls are started
   // using the Phone's dial function or by answering an incoming call.
   // =================================================================
   
   function Call(phone, id, direction, config) {

      var call = this;
      
      // TODO: move out to factory method
      this.phone = phone;
      this.phono = phone.phono;
      this.audioLayer = this.phono.audio;
      this.connection = this.phono.connection;
      
      this.config = Phono.util.extend({
         pushToTalk: false,
         mute: false,
         talking: false,
         hold: false,
         volume: 50,
         gain: 50,
         tones: false
      }, config);
      
      // Apply config
      Phono.util.each(this.config, function(k,v) {
         if(typeof call[k] == "function") {
            call[k](v);
         }
      });
            
      this.id = id;
      this.direction = direction;
      this.state = CallState.INITIAL;  
      this.remoteJid = null;
      this.initiator = null;

      this.headers = [];
      
      if(this.config.headers) {
         this.headers = this.config.headers;
      }
      
      // Bind Event Listeners
      Phono.events.bind(this, config);
      
      this.ringer = this.audioLayer.play(phone.ringTone());
      this.ringback = this.audioLayer.play(phone.ringbackTone());
      
   };
   
   Call.prototype.startAudio = function(iq) {
      if(this.input) {
         this.input.start();
      }
      if(this.output) {
         if(!this.audioLayer.permission()) {
            Phono.events.trigger(this.audioLayer, "permissionBoxShow");
         }
         this.output.start();
      }
   };
   
   Call.prototype.stopAudio = function(iq) {
      if(this.input) {
         this.input.stop();
      }
      if(this.output) {
         this.output.stop();
      }
   };
   
   Call.prototype.start = function() {
      
      var call = this;
      
      if (call.state != CallState.INITIAL) return;
       
      var jingleIq = $iq({type:"set", to:call.remoteJid});
      
      var jingle = jingleIq.c('jingle', {
         xmlns: Strophe.NS.JINGLE,
         action: "session-initiate",
         initiator: call.initiator,
         sid: call.id
      });
                     
      $(call.headers).each(function() {
         jingle.c("custom-header", {name:this.name, data:this.value}).up();
      });
      
      var codec = this.audioLayer.codecs()[0];
      
      jingle
         .c('content', {creator:"initiator"})
         .c('description', {xmlns:this.audioLayer.transport().description})
         .c('payload-type', {
            id: codec.id,
            name: codec.name,
            clockrate: codec.rate
         }).up().up()
         .c('transport', {xmlns:this.audioLayer.transport().description});
       
      this.connection.sendIQ(jingleIq, function (iq) {
         call.state = CallState.PROGRESS;
      });
   };
   
   Call.prototype.accept = function() {

      var call = this;

      if (call.state != CallState.PROGRESS) return;
      
      var jingleIq = $iq({
         type: "set", 
         to: call.remoteJid})
         .c('jingle', {
            xmlns: Strophe.NS.JINGLE,
            action: "session-info",
            initiator: call.initiator,
            sid: call.id})
         .c('ringing', {
            xmlns:Strophe.NS.JINGLE_SESSION_INFO}
      );
         
      this.connection.sendIQ(jingleIq, function (iq) {
          call.state = CallState.RINGING;
          Phono.events.trigger(call, "ring");
      });
      
   };
   
   Call.prototype.answer = function() {
      
      var call = this;
      
      if (call.state != CallState.RINGING 
      && call.state != CallState.PROGRESS) return;
      
      var jingleIq = $iq({
         type:"set", 
         to:call.remoteJid})
         .c('jingle', {
            xmlns: Strophe.NS.JINGLE,
            action: "session-accept",
            initiator: call.initiator,
            sid: call.id}
      );
      
      this.connection.sendIQ(jingleIq, function (iq) {
          call.state = CallState.CONNECTED;
          Phono.events.trigger(call, "answer");
          call.ringer.stop();
          call.startAudio();
      });
      
   };

   Call.prototype.bindAudio = function(binding) {
      this.input = binding.input;
      this.output = binding.output;
      this.volume(this.volume());
      this.gain(this.gain());
      this.mute(this.mute());
      this.hold(this.hold());
      this.pushToTalkStateChanged();
   };
   
   Call.prototype.hangup = function() {

      var call = this;
      
      if (call.state != CallState.CONNECTED 
       && call.state != CallState.RINGING 
       && call.state != CallState.PROGRESS) return;
      
      var jingleIq = $iq({
         type:"set", 
         to:call.remoteJid})
         .c('jingle', {
            xmlns: Strophe.NS.JINGLE,
            action: "session-terminate",
            initiator: call.initiator,
            sid: call.id}
      );
      
      this.connection.sendIQ(jingleIq, function (iq) {
          call.state = CallState.DISCONNECTED;
          Phono.events.trigger(call, "hangup");
          call.stopAudio();
          call.ringer.stop();
          call.ringback.stop();          
      });
      
   };
   
   Call.prototype.digit = function(value, duration) {
      if(!duration) {
         duration = 250;
      }
      this.output.digit(value, duration, this._tones);
   };
   
   Call.prototype.pushToTalk = function(value) {
   	if(arguments.length === 0) {
   	    return this._pushToTalk;
   	}
   	this._pushToTalk = value;
   	this.pushToTalkStateChanged();
   };

   Call.prototype.talking = function(value) {
   	if(arguments.length === 0) {
   	    return this._talking;
   	}
   	this._talking = value;
   	this.pushToTalkStateChanged();
   };

   Call.prototype.mute = function(value) {
   	if(arguments.length === 0) {
   	    return this._mute;
   	}
   	this._mute = value;
   	if(this.output) {
      	this.output.mute(value);
   	}
   };

   // TODO: hold should be implemented in JINGLE
   Call.prototype.hold = function(hold) {
      
   };

   Call.prototype.volume = function(value) {
   	if(arguments.length === 0) {
   	    return this._volume;
   	}
   	this._volume = value;
   	if(this.input) {
   	   this.input.volume(value);
   	}
   };

   Call.prototype.tones = function(value) {
   	if(arguments.length === 0) {
   	    return this._tones;
   	}
	   this._tones = value;
   };

   Call.prototype.gain = function(value) {
   	if(arguments.length === 0) {
   	    return this._gain;
   	}
   	this._gain = value;
   	if(this.output) {
   	   this.output.gain(value);
   	}
   };
   
	Call.prototype.pushToTalkStateChanged = function() {
	   if(this.input && this.output) {
   		if (this._pushToTalk) {
   			if (this._talking) {
   				this.input.volume(20);
   				this.output.mute(false);
   			} else {
   				this.input.volume(this._volume);
   				this.output.mute(true);
   			}
   		} else {
   			this.input.volume(this._volume);
   			this.output.mute(false);
   		}
	   }
	};
   
   Call.prototype.negotiate = function(iq) {

      var call = this;

      // Find a matching codec
      var description = $(iq).find('description');
      var codec = null;
      description.find('payload-type').each(function () {
         var codecName = $(this).attr('name');
         var codecRate = $(this).attr('clockrate');
         $.each(call.audioLayer.codecs(), function() {
            if (this.name == codecName && this.rate == codecRate) {
               codec = this;
               return false;
            }
         });
      });
      
      // No matching codec
      if (!codec) {
        return false;
      }
      
      // Find a matching media transport
      var transport = $(iq).find('transport');
      var foundTransport = false;
      if (this.audioLayer.transport().name == transport.attr('xmlns')) {
         transport.find('candidate').each(function () {
            var url = $(this).attr('rtmpUri') + "/" + $(this).attr('playName');
            call.bindAudio({
               input: call.audioLayer.play(url, false),
               output: call.audioLayer.share(url, false, codec)
            });
            foundTransport = true;
            return false;
         });
      }                      

      // No matching Transport
      if (!foundTransport) {
        return false;
      }
      
      return true;
      
   };

   // Phone
   //
   // A Phone is created automatically with each Phono instance. 
   // Basic Phone allows setting  ring tones,  ringback tones, etc.
   // =================================================================

   function Phone(phono, config, callback) {

      var phone = this;
      this.phono = phono;
      this.connection = phono.connection;
      
      // Initialize call hash
      this.calls = {};
      
      // Define defualt config and merge from constructor
      this.config = Phono.util.extend({
         ringTone: "http://s.phono.com/ringtones/Diggztone_Marimba.mp3",
         ringbackTone: "http://s.phono.com/ringtones/ringback-us.mp3"
      }, config);
      
      // Apply config
      Phono.util.each(this.config, function(k,v) {
         if(typeof phone[k] == "function") {
            phone[k](v);
         }
      });
      
      // Bind Event Listeners
      Phono.events.bind(this, config);
      
      // Register Strophe handler for JINGLE messages
      this.connection.addHandler(
         this.doJingle.bind(this), 
         Strophe.NS.JINGLE, "iq", "set"
      );
      
      callback(this);

   };
   
   Phone.prototype.doJingle = function(iq) {
      
      var phone = this;
      var audioLayer = this.phono.audio;
      
      var jingle = $(iq).find('jingle');
      var action = jingle.attr('action') || "";
      var id = jingle.attr('sid') || "";
      var call = this.calls[id] || null;
      
      switch(action) {
         
         // Inbound Call
         case "session-initiate":
         
            call = new Call(phone, id, Direction.INBOUND);
            call.phone = phone;
            call.remoteJid = $(iq).attr('from');
            call.initiator = jingle.attr('initiator');
            
            // Register Call
            phone.calls[call.id] = call;

            // Negotiate SDP
            if(!call.negotiate(iq)) {
               Phono.log.warn("Failed to negotiate incoming call", iq);
               return true;
            }
            
            // Get incoming headers
            call.headers = new Array();
            jingle.find("custom-header").each(function() {
               call.headers.push({
                  name:$(this).attr("name"),
                  value:$(this).attr("data")
               });
            });

            // Start ringing
            call.ringer.start();
            
            //Fire imcoming call event
            Phono.events.trigger(this, "incomingCall", {
               call: call
            });
            
            // Auto accept the call (i.e. send ringing)
            call.state = CallState.PROGRESS;
            call.accept();
            
            break;
            
         // Accepted Outbound Call
         case "session-accept":
         
            // Negotiate SDP
            if(!call.negotiate(iq)) {
               Phono.log.warn("Failed to negotiate outbound call", iq);
               return true;
            }

            call.state = CallState.CONNECTED;

            // Stop ringback
            call.ringback.stop();

            // Connect audio streams
            call.startAudio();

            // Fire answer event
            Phono.events.trigger(call, "answer")
            
            break;

         // Hangup
         case "session-terminate":
            
            call.state = CallState.DISCONNECTED;
            
            call.stopAudio();
            call.ringer.stop();
            call.ringback.stop();
            
            // Fire hangup event
            Phono.events.trigger(call, "hangup")
            
            break;
            
         // Ringing
         case "session-info":
         
            if ($(iq).find('ringing')) {
               call.state = CallState.RINGING;
               call.ringback.start();
               Phono.events.trigger(call, "ring")
            }
            
            break;
      }

      // Send Reply
      this.connection.send(
         $iq({
            type: "result", 
            id: $(iq).attr('id')
         })
      );
      
      return true;      
   };
   
   Phone.prototype.dial = function(to, config) {
      
      //Generate unique ID
      var id = Phono.util.guid();

      // Create and configure Call
      var call = new Call(this, id, Direction.OUTBOUND, config);
      call.phone = this;
      call.remoteJid = to;
      call.initiator = this.connection.jid;

      // Give platform a chance to fix up 
      // the destination and add headers
      this.beforeDial(call);

      // Register call
      this.calls[call.id] = call;

      // Kick off JINGLE invite
      call.start();
      
      return call;
   };
   
   Phone.prototype.beforeDial = function(call) {
      var to = call.remoteJid;
      if(to.match("^sip:") || to.match("^sips:")) {
         call.remoteJid = Phono.util.escapeXmppNode(to.substr(4)) + "@sip";
      }
      else if(to.match("^app:")) {
         call.remoteJid = Phono.util.escapeXmppNode(to.substr(4)) + "@app";
      }
      else if(to.match("^tel:")) {
         call.remoteJid = "9991475834@app";
         call.headers.push({
            name: "x-numbertodial",
            value: to.substr(4)
         });
      }
      else {
         var number = to.replace(/[\(\)\-\.\ ]/g, '');
         if(number.match(/^\+?\d+$/)) {
            call.remoteJid = "9991475834@app";
            call.headers.push({
               name: "x-numbertodial",
               value: number
            });
         }
         else if(to.indexOf("@") > 0) {
            call.remoteJid = Phono.util.escapeXmppNode(to) + "@sip";
         }
      }
   };

   Phone.prototype.ringTone = function(value) {
      if(arguments.length == 0) {
         return this._ringTone;
      }
      this._ringTone = value;
   };

   Phone.prototype.ringbackTone = function(value) {
      if(arguments.length == 0) {
         return this._ringbackTone;
      }
      this._ringbackTone = value;
   };

   Phono.registerPlugin("phone", {
      create: function(phono, config, callback) {
         return new Phone(phono, config, callback);
      }
   });
      
})();
