function JSEPAudio(phono, config, callback) {
    this.type = "jsep";

    Phono.log.info("Initialize JSEP");
    if (webkitAudioContext){
        JSEPAudio.webAudioContext = new webkitAudioContext();
    } 

    if (typeof webkitRTCPeerConnection== "function") {
        JSEPAudio.GUM = function(p,s,f) {navigator.webkitGetUserMedia(p,s,f)};
        JSEPAudio.mkPeerConnection = function (a,b) { return new webkitRTCPeerConnection(a,b);};
    }

    this.config = Phono.util.extend({
        media: {audio:true, video:false}
    }, config);
    
    var plugin = this;
    
    var localContainerId = this.config.localContainerId;

    // Create audio continer if user did not specify one
    if(!localContainerId) {
	this.config.localContainerId = this.createContainer();
    }

    JSEPAudio.localVideo = document.getElementById(this.config.localContainerId);

    callback(plugin);
}

JSEPAudio.exists = function() {
    return (typeof webkitRTCPeerConnection == "function");
}

JSEPAudio.stun = "STUN stun.l.google.com:19302";
JSEPAudio.count = 0;
JSEPAudio.toneMap = {
                '0':[1336,941],
                '1':[1209,697],
                '2':[1336,697],
                '3':[1477,696],
                '4':[1209,770],
                '5':[1336,770],
                '6':[1477,770],
                '7':[1209,852],
                '8':[1336,852],
                '9':[1447,852],
                '*':[1209,941],
                '#':[1477,941]
            };

// JSEPAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
JSEPAudio.prototype.play = function(transport, autoPlay) {
    var url = null;
    var audioPlayer = null;
    if (transport.uri) {
        url = transport.uri;
    }
    
    return {
        url: function() {
            return url;
        },
        start: function() {
            if (url) {
                audioPlayer = new Audio(url); 
                console.log("starting");
                var loop = function() {
                    audioPlayer = new Audio(url); 
                    audioPlayer.play();
                    audioPlayer.addEventListener('ended', loop);
                }
                loop();
            }
        },
        stop: function() {
            if (audioPlayer) audioPlayer.pause();
            audioPlayer = null;
        },
        volume: function(value) {
   	    if(arguments.length === 0) {
   		return transport.volume * 100;
   	    }
   	    else {
   		transport.volume = (value / 100);
   	    }
        }
    }
};

// Creates a new audio Share and will optionally begin playing
JSEPAudio.prototype.share = function(transport, autoPlay, codec) {
    var share;

    return {
        // Readonly
        url: function() {
            // No Share URL
            return null;
        },
        codec: function() {
            return codec;
        },
        // Control
        start: function() {
            // Audio started automatically
            return null;
        },
        stop: function() {
            if (JSEPAudio.localStream) {
                JSEPAudio.localStream.stop();
            }
        },
        // Properties
        gain: function(value) {
            // We have no control over this
            return null;
        },
        mute: function(value) {
            if(arguments.length === 0) {
                var muted = true;
                Phono.util.each(JSEPAudio.localStream.audioTracks, function() {
                    if (this.enabled == true) muted = false;
                });
   		return muted;
   	    }
            if (value == true) {
                Phono.util.each(JSEPAudio.localStream.audioTracks, function() {
                    this.enabled = false;
                });
            } else {
                Phono.util.each(JSEPAudio.localStream.audioTracks, function() {
                    this.enabled = true;
                });
            }
        },
        suppress: function(value) {
            // Echo canceller is on always
            return null;
        },
        energy: function(){        
            return {
                mic: 0.0,
                spk: 0.0
            }
        },
        secure: function() {
            return true;
        },
        freep: function(value, duration, audible) {
            if (audible){ 
                var context = JSEPAudio.webAudioContext; 
                if (context){
                    var note1;
                    var note2;
                    if (duration < 100) duration = 100;// sensible sound
                    note1 = context.createOscillator();
                    note2 = context.createOscillator();
                    note1.connect(context.destination);
                    note2.connect(context.destination);
    
                    var twoTone = JSEPAudio.toneMap[value];
                    note1.frequency.value = twoTone[0];
                    note2.frequency.value = twoTone[1];
                    note1.noteOn(0.0);
                    note2.noteOn(0.0);
                    window.setTimeout(
                      function(){
                        note1.noteOff(0.0);
                        note2.noteOff(0.0);
                        }, duration);
               } 
          }
      }
   };
};   

JSEPAudio.prototype.showPermissionBox = function(callback) {
    Phono.log.info("Requesting access to local media");

    JSEPAudio.GUM({'audio':this.config.media['audio'], 'video':this.config.media['video']}, 
                  function(stream) {
                      JSEPAudio.localStream = stream;
                      var url = webkitURL.createObjectURL(stream);
                      JSEPAudio.localVideo.style.opacity = 1;
                      JSEPAudio.localVideo.src = url;
                      if (typeof callback == 'function') callback(true);
                  },
                  function(error) {
                      Phono.log.info("Failed to get access to local media. Error code was " + error.code);
                      alert("Failed to get access to local media. Error code was " + error.code + ".");   
                      if (typeof callback == 'function') callback(false);
                  });

};

JSEPAudio.prototype.permission = function() {
    return (JSEPAudio.localStream != undefined);
};


// Returns an object containg JINGLE transport information
JSEPAudio.prototype.transport = function(config) {
    var pc;
    var inboundOffer;
    var configuration = {iceServers:[ { url:"stun:stun.l.google.com:19302" } ]};
    var constraints;
    var remoteContainerId;
    var complete = false;
    var audio = this;
    var candidateCount = 0;

    constraints =  {'mandatory': {'OfferToReceiveAudio':this.config.media['audio'], 'OfferToReceiveVideo':this.config.media['video']}};

    if(!config || !config.remoteContainerId) {
        if (this.config.remoteContainerId) {
            remoteContainerId = this.config.remoteContainerId;
        } else {
            remoteContainerId = this.createContainer();
        }
    } else {
        remoteContainerId = config.remoteContainerId;
    }

    var remoteVideo = document.getElementById(remoteContainerId);   

    return {
        name: "urn:xmpp:jingle:transports:ice-udp:1",
        buildTransport: function(direction, j, callback, u, updateCallback) {
            
            pc = JSEPAudio.mkPeerConnection(configuration,constraints);
            
	    pc.onicecandidate = function(evt) {
                if (!complete) {
                    if ((evt.candidate == null) || 
                        (candidateCount >= 1 && !audio.config.media['video'] && direction == "answer")) {
		        Phono.log.info("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                        complete = true;
                        var sdpObj = Phono.sdp.parseSDP(pc.localDescription.sdp);
                        Phono.log.info("sdpObj = " + JSON.stringify(sdpObj));
                        Phono.sdp.buildJingle(j, sdpObj);
                        var codecId = 0;
                        if (sdpObj.contents[0].codecs[0].name == "telephone-event") codecId = 1;
                        var codec = 
                            {
                                id: sdpObj.contents[0].codecs[codecId].id,
                                name: sdpObj.contents[0].codecs[codecId].name,
                                rate: sdpObj.contents[0].codecs[codecId].clockrate
                            };
		        callback(codec);
                    } else {
        	        Phono.log.info("An Ice candidate "+JSON.stringify(evt.candidate));
                        candidateCount += 1;
                    }
                }
            }
            pc.onconnecting = function(message) {Phono.log.info("onSessionConnecting.");};
	    pc.onopen = function(message) {Phono.log.info("onSessionOpened.");};
            pc.onaddstream = function (event) {
                Phono.log.info("onAddStream."); 
                var url = webkitURL.createObjectURL(event.stream);
                remoteVideo.style.opacity = 1;
                remoteVideo.src = url;
            };
            pc.onremovestream = function (event) {Phono.log.info("onRemoveStream."); };
	    pc.onicechange= function (event) {Phono.log.info("onIceChange: "+pc.iceState); };
	    pc.onnegotiationneeded = function (event) {Phono.log.info("onNegotiationNeeded."); };
            pc.onstatechange = function (event) {Phono.log.info("onStateChange: "+pc.readyState); };

            Phono.log.info("Adding localStream");

            var cb2 = function() {
                pc.addStream(JSEPAudio.localStream);
                
	        var cb = function(localDesc) {
                    Phono.log.info("Created localDesc");
                    var sdpObj = Phono.sdp.parseSDP(localDesc.sdp);
                    // Nail it to google-ice
                    sdpObj.contents[0].ice.options = "google-ice";
                    var sdp = Phono.sdp.buildSDP(sdpObj);
                    var sd = new RTCSessionDescription({'sdp':sdp, 'type':localDesc.type} );
   		    pc.setLocalDescription(sd);
		    var msgString = JSON.stringify(sd,null," ");
                    Phono.log.info('Set localDesc ' + msgString);
                    Phono.log.info("Pc now: "+JSON.stringify(pc,null," "));
	        };
                
                if (direction == "answer") {
                    pc.setRemoteDescription(inboundOffer,
                                            function(){Phono.log.info("remotedescription happy");
				                       Phono.log.info("Pc now: "+JSON.stringify(pc,null," "));
                                                       pc.createAnswer(cb , null, constraints);
			                              },
			                    function(){Phono.log.info("remotedescription sad")});
                } else {
		    pc.createOffer(cb , null, constraints);
                }
            }
            
            if (audio.permission()) {
                cb2();
            } else {
                audio.showPermissionBox(cb2);
            }
        },
        processTransport: function(t, update, iq) {
            Phono.log.info("process message");

            var sdpObj = Phono.sdp.parseJingle(iq);
            var sdp = Phono.sdp.buildSDP(sdpObj);
            var codecId = 0;
            if (sdpObj.contents[0].codecs[0].name == "telephone-event") codecId = 1;
            var codec = 
                {
                    id: sdpObj.contents[0].codecs[codecId].id,
                    name: sdpObj.contents[0].codecs[codecId].name,
                    rate: sdpObj.contents[0].codecs[codecId].clockrate
                };

            if (pc) {
                // We are an answer to an outbound call
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"answer"} );
		Phono.log.info("about to set the remote description: "+JSON.stringify(sd,null," "));
		pc.setRemoteDescription(sd,
			                function(){Phono.log.info("remotedescription happy");
				                   Phono.log.info("Pc now: "+JSON.stringify(pc,null," "));
			                          },
			                function(){Phono.log.info("remotedescription sad")});
                
            } else {
                // We are an offer for an inbound call
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"offer"} );
                inboundOffer = sd;
            }
            return {codec:codec, input:remoteVideo};
        },
        destroyTransport: function() {
            // Destroy any transport state we have created
            if (pc) {
                pc.close();
            }

            if (JSEPAudio.localStream) {
                JSEPAudio.localStream.stop();
                JSEPAudio.localStream = null;
            }
        }
    }
};

// Returns an array of codecs supported by this plugin
// Hack until we get capabilities support
JSEPAudio.prototype.codecs = function() {
    return {};
};

JSEPAudio.prototype.audioInDevices = function(){
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
JSEPAudio.prototype.createContainer = function() {
    var webRTC = $("<video>")
      	.attr("id","_phono-audio-webrtc" + (JSEPAudio.count++))
        .attr("autoplay","autoplay")
      	.appendTo("body");

    var containerId = $(webRTC).attr("id");       
    return containerId;
};      
