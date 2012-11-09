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
      this.transport = this.audioLayer.transport(config);
      this.connection = this.phono.connection;
      
      this.config = Phono.util.extend({
         pushToTalk: false,
         mute: false,
         talking: false,
         hold: false,
         volume: 50,
         gain: 50,
         tones: false,
         codecs: phone.config.codecs,
         security: phone._security
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
      this.codec = null;

      this.srtpPropsr = undefined;
      this.srtpPropsl = undefined;

      if (this._security != "disabled" && this.transport.supportsSRTP == true) {
          // Set up some local SRTP crypto parameters
          this.tag = "1";
          this.crypto = "AES_CM_128_HMAC_SHA1_80";
          this.keyparams = "inline:" + Phono.util.genKey(30);
          this.srtpPropsl = Phono.util.srtpProps(this.tag, this.crypto, this.keyparams);
      }
       
      this.headers = [];
      
      if(this.config.headers) {
         this.headers = this.config.headers;
      }
      
      // Bind Event Listeners
      Phono.events.bind(this, config);
      
      this.ringer = this.audioLayer.play({uri:phone.ringTone()}); 
      this.ringback = this.audioLayer.play({uri:phone.ringbackTone()});
      if (this.audioLayer.audioIn){
         this.audioLayer.audioIn(phone.audioInput());
      }
      
   };

   Call.prototype.bind = function(config) {
       Phono.events.bind(this, config);
   }

   Call.prototype.startAudio = function(iq) {
      if(this.input) {
         this.input.start();
      }
      if(this.output) {
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
       
      var initiateIq = $iq({type:"set", to:call.remoteJid});
      
      var initiate = initiateIq.c('jingle', {
         xmlns: Strophe.NS.JINGLE,
         action: "session-initiate",
         initiator: call.initiator,
         sid: call.id
      });
                     
      $(call.headers).each(function() {
         initiate.c("custom-header", {name:this.name, data:this.value}).up();
      });
             
       var updateIq = $iq({type:"set", to:call.remoteJid});
       
       var update = updateIq.c('jingle', {
           xmlns: Strophe.NS.JINGLE,
           action: "transport-accept",
           initiator: call.initiator,
           sid: call.id
       });
       
       var partialUpdate = update
           .c('content', {creator:"initiator"})
           .c('description', {xmlns:this.transport.description})
       
       this.transport.buildTransport("offer", initiate, 
                                     function() {
                                         console.log("callback is running");
                                         
                                         if (call.transport.description) {
                                             initiate = initiate
                                                 .c('content', {creator:"initiator"})
                                                 .c('description', {xmlns:call.transport.description})
                                             
                                             // Add any codecs that were not in the transport layer
                                             Phono.util.each(call.config.codecs(Phono.util.filterWideband(call.audioLayer.codecs(),call.phone.wideband())), function() {
                                                 initiate = initiate.c('payload-type', {
                                                     id: this.id,
                                                     name: this.name,
                                                     clockrate: this.rate
                                                 }).up();           
                                             });
                                             
                                             // Add any crypto that wasn't in the transport layer
                                             var required = "0";
                                             if (call._security == "mandatory") required = "1";
                                             if (call._security != "disabled" && call.transport.supportsSRTP == true) {
                                                 initiate = initiate.c('encryption', {required: required}).c('crypto', {
                                                     tag: call.tag,
                                                     'crypto-suite': call.crypto,
                                                     'key-params': call.keyparams
                                                 }).up();    
                                             }
                                         }

                                         call.connection.sendIQ(initiateIq, function (iq) {
                                             call.state = CallState.PROGRESS;
                                         });
                                     },
                                     partialUpdate.up(),
                                     function() {
                                         call.connection.sendIQ(updateIq, function (iq) {
                                         });   
                                     }
                                    );

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

       var acceptIq = $iq({type:"set", to:call.remoteJid});
      
       var accept = acceptIq.c('jingle', {
           xmlns: Strophe.NS.JINGLE,
           action: "session-accept",
           initiator: call.initiator,
           sid: call.id
       });
       
       
       
       var updateIq = $iq({type:"set", to:call.remoteJid});
      
       var update = updateIq.c('jingle', {
           xmlns: Strophe.NS.JINGLE,
           action: "transport-replace",
           initiator: call.initiator,
           sid: call.id
       });
       
       var partialUpdate = update
           .c('content', {creator:"initiator"})
           .c('description', {xmlns:this.transport.description})

       this.transport.buildTransport("answer", accept, 
                                     function(){
                                         if (call.transport.description) {
                                             var partialAccept = accept
                                                 .c('content', {creator:"initiator"})
                                                 .c('description', {xmlns:call.transport.description});
                                             
                                             partialAccept = partialAccept.c('payload-type', {
                                                 id: call.codec.id,
                                                 name: call.codec.name,
                                                 clockrate: call.codec.rate
                                             }).up();           
                                             
                                             $.each((call.audioLayer.codecs()), function() {
                                                 if (this.name == "telephone-event") {
                                                     partialAccept = partialAccept.c('payload-type', {
                                                         id: this.id,
                                                         name: this.name,
                                                         clockrate: this.rate
                                                     }).up();     
                                                 } 
                                             });
                                             
                                             // Add our crypto
                                             if (call.srtpPropsl != undefined && call.srtpPropsr != undefined) {
                                                 partialAccept = partialAccept.c('encryption').c('crypto', {
                                                     tag: call.tag,
                                                     'crypto-suite': call.crypto,
                                                     'key-params': call.keyparams
                                                 }).up();    
                                             }
                                         }

                                         call.connection.sendIQ(acceptIq, function (iq) {
                                             call.state = CallState.CONNECTED;
                                             if (call.ringer != null) call.ringer.stop();
                                             call.setupBinding();
                                             // Check security
                                             if (call._security == "mandatory" && call.output.secure() == false) {
                                                 // We must fail the call, remote end did not agree on crypto
                                                 Phono.log.error("Security error, call not secure when mandatory specified");
                                                 call.hangup();
                                             } else {
                                                 Phono.events.trigger(call, "answer");
                                                 call.startAudio();
                                             }
                                         });
                                     },
                                     partialUpdate.up(),
                                     function() {
                                         call.connection.sendIQ(updateIq, function (iq) {
                                         });   
                                     });
   };

   Call.prototype.bindAudio = function(binding) {
      this.input = binding.input;
      this.output = binding.output;
      this.volume(this.volume());
      this.gain(this.gain());
      this.mute(this.mute());
      this.hold(this.hold());
      this.headset(this.headset());
      this.pushToTalkStateChanged();

      Phono.events.bind(this.output, {
      	  onMediaReady: function() {
              Phono.events.trigger(call, "mediaReady");
          }});
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

      call.stopAudio();
      if (call.transport.destroyTransport) call.transport.destroyTransport();
             
      this.connection.sendIQ(jingleIq, function (iq) {
          call.state = CallState.DISCONNECTED;
          Phono.events.trigger(call, "hangup");
          if (call.ringer != null) call.ringer.stop();
          if (call.ringback != null) call.ringback.stop();          
      });
      
   };
   
   Call.prototype.digit = function(value, duration) {
      if(!duration) {
         duration = 349;
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

   Call.prototype.energy = function() {
   	if(this.output) {
   	   ret = this.output.energy();
   	}
	return ret;
   };

   Call.prototype.secure = function() {
       var ret = false;
       if (this.output) {
           ret = this.output.secure();
       }
       return ret;
   };

   Call.prototype.security = function(value) {
   	if(arguments.length === 0) {
   	    return this._security;
   	}
   	this._security = value;
   };
   
   Call.prototype.headset = function(value) {
   	if(arguments.length === 0) {
   	    return this._headset;
   	}
   	this._headset = value;
   	if(this.output) {
   	   this.output.suppress(!value);
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

      // Find a matching audio codec
      var description = $(iq).find('description');
      var codec = null;
      description.find('payload-type').each(function () {
         var codecName = $(this).attr('name');
         var codecRate = $(this).attr('clockrate');
          var codecId = $(this).attr('id');
          $.each(call.config.codecs(Phono.util.filterWideband(call.audioLayer.codecs(),call.phone.wideband())), function() {
             if ((this.name == codecName && this.rate == codecRate && this.name != "telephone-event") || (parseInt(this.id) < 90 && this.id == codecId)) {
                 if (codec == null) codec = {id: codecId , name:this.name,  rate: this.rate, p: this.p};
                 return false;
            } 
         });
      });
      
      // No matching codec
      if (!codec) {
          Phono.log.error("No matching jingle codec (not a problem if using WebRTC)");
          // Voodoo up a temporary codec as a placeholder
          codec = {
              id: 1,
              name: "webrtc-ulaw",
              rate: 8000,
              p: 20
          };
      }

      // Check to see if we have crypto, we only support AES_CM_128_HMAC_SHA1_80
      if (call._security != "disabled" && this.transport.supportsSRTP == true) {
          description.find('crypto').each(function () {
              if ($(this).attr('crypto-suite') == call.crypto) {
                  call.srtpPropsr = Phono.util.srtpProps($(this).attr('tag'), 
                                                         $(this).attr('crypto-suite'), 
                                                         $(this).attr('key-params'), 
                                                         $(this).attr('session-params'));
                  call.tag = $(this).attr('tag'); // So we can answer with the correct tag
              }
          });
          
          if (call._security == "mandatory" && call.srtpPropsr == undefined) {
              // We must fail the call, remote end did not agree on crypto
              Phono.log.error("No security when mandatory specified");
              return null;
          }
      }

      // Find a matching media transport
      var foundTransport = false;
      $(iq).find('transport').each(function () {
          if (call.transport.name == $(this).attr('xmlns') && foundTransport == false) {
              var transport = call.transport.processTransport($(iq), false);      
              if (transport != undefined) {
                  call.setupBinding = function () {
                      return call.bindAudio ({
                          input: call.audioLayer.play(transport.input, false),
                          output: call.audioLayer.share(transport.output, false, codec, call.srtpPropsl, call.srtpPropsr)
                      });
                  };
                  foundTransport = true;
              } else {
                  Phono.log.error("No valid candidate in transport");
              }
          }
      });

      if (foundTransport == false) {
          Phono.log.error("No matching valid transport");
          return null;
      }
      return codec;
       
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

      // Initial state
      this._wideband = true;

      // Define defualt config and merge from constructor
      this.config = Phono.util.extend({
         audioInput: "System Default",
         ringTone: "//s.phono.com/ringtones/Diggztone_Marimba.mp3",
         ringbackTone: "//s.phono.com/ringtones/ringback-us.mp3",
         wideband: true,
         headset: false,
         codecs: function(offer) {return offer;},
         security: "optional" // mandatory, disabled
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
         
            call = Phono.util.loggify("Call", new Call(phone, id, Direction.INBOUND));
            call.phone = phone;
            call.remoteJid = $(iq).attr('from');
            call.initiator = jingle.attr('initiator');
            
            // Register Call
            phone.calls[call.id] = call;

            call.state = CallState.PROGRESS;
          
            // Negotiate SDP
            call.codec = call.negotiate(iq);
            if(call.codec == null) {
                Phono.log.warn("Failed to negotiate incoming call", iq);
                call.hangup();
                break;
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
            if (call.ringer != null) call.ringer.start();
            
            // Auto accept the call (i.e. send ringing)
            call.accept();

            // Fire imcoming call event
            Phono.events.trigger(this, "incomingCall", {
               call: call
            });
          
            // Get microphone permission if we are going to need it
            if(!audioLayer.permission()) {
                Phono.events.trigger(audioLayer, "permissionBoxShow");
            }
                        
            break;
            
         // Accepted Outbound Call
         case "session-accept":
         
            // Negotiate SDP
            call.codec = call.negotiate(iq);
            if(call.codec == null) {
                Phono.log.warn("Failed to negotiate outbound call", iq);
                call.hangup();
                break;
            }
          
            // Stop ringback
            if (call.ringback != null) call.ringback.stop();
          
            // Connect audio streams
            call.setupBinding();
          
            // Belt and braces
            if (call._security == "mandatory" && call.output.secure() == false) {
                // We must fail the call, remote end did not agree on crypto
                Phono.log.error("Security error, call not secure when mandatory specified");
                call.hangup();
                break;
            }

            call.startAudio();

            call.state = CallState.CONNECTED;
                
            // Fire answer event
            Phono.events.trigger(call, "answer")
            break;

         // Transport information update
         case "transport-replace":
         case "transport-accept":
            call.transport.processTransport($(iq), true);
            break;

         // Hangup
         case "session-terminate":
            
            call.state = CallState.DISCONNECTED;
            
            call.stopAudio();
            if (call.ringer != null) call.ringer.stop();
            if (call.ringback != null) call.ringback.stop();
            if (call.transport.destroyTransport) call.transport.destroyTransport();

            // Fire hangup event
            Phono.events.trigger(call, "hangup")
            
            break;
            
         // Ringing
         case "session-info":
         
            if ($(iq).find('ringing')) {
               call.state = CallState.RINGING;
               if (call.ringback != null) call.ringback.start();
               Phono.events.trigger(call, "ring")
            }
            
            break;
      }

      // Send Reply
      this.connection.send(
         $iq({
            type: "result", 
             id: $(iq).attr('id'),
             to:call.remoteJid
         })
      );
      
      return true;      
   };
   
   Phone.prototype.dial = function(to, config) {
      
      //Generate unique ID
      var id = Phono.util.guid();

      // Configure Call properties inherited from Phone
      config = Phono.util.extend({
         headset: this.headset(),
         callerId: this.connection.jid
      }, (config || {}));

      // Create and configure Call
      var call = new Phono.util.loggify("Call", new Call(this, id, Direction.OUTBOUND, config));
      call.phone = this;
      call.remoteJid = to;
      call.initiator = config.callerId;

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
      else if(to.match("^xmpp:")) {
         call.remoteJid = to.substr(5); 
      }
      else if(to.match("^app:")) {
         call.remoteJid = Phono.util.escapeXmppNode(to.substr(4)) + "@app";
      }
      else if(to.match("^tel:")) {
         call.remoteJid = "9996182316@app";
         call.headers.push({
            name: "x-numbertodial",
            value: to.substr(4)
         });
      }
      else {
         var number = to.replace(/[\(\)\-\.\ ]/g, '');
         if(number.match(/^\+?\d+$/)) {
            call.remoteJid = "9996182316@app";
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

   Phone.prototype.audioInput = function(value) {
      if(arguments.length == 0) {
         return this._audioInput;
      }
      this._audioInput = value;
   };
   
   Phone.prototype.audioInDevices = function(){
       var audiolayer = this.phono.audio;
       var ret = new Object();
       if (audiolayer.audioInDevices){
           ret = audiolayer.audioInDevices();
       }
       return ret;
   }

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

   Phone.prototype.headset = function(value) {
      if(arguments.length == 0) {
         return this._headset;
      }
      this._headset = value;
      Phono.util.each(this.calls, function() {
        this.headset(value);
      });
   };

   Phone.prototype.wideband = function(value) {
      if(arguments.length == 0) {
         return this._wideband;
      }
      this._wideband = value;
   }

   Phone.prototype.security = function(value) {
       if(arguments.length == 0) {
           return this._security;
       }
       this._security = value;
   }

   Phono.registerPlugin("phone", {
      create: function(phono, config, callback) {
         return Phono.util.loggify("Phone", new Phone(phono, config, callback));
      }
   });
      
})();
