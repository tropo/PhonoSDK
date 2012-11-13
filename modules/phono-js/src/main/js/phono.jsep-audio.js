function JSEPAudio(phono, config, callback) {
    this.type = "jsep";

    Phono.log.info("Initialize JSEP");

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

// JSEPAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
JSEPAudio.prototype.play = function(transport, autoPlay) {
    var url = null;;
    var audioPlayer = null;
    if (transport) {
        url = transport.uri;
    }
    
    return {
        url: function() {
            return url;
        },
        start: function() {
            if (audioPlayer != null) {
                $(audioPlayer).remove();
            }
            audioPlayer = $("<audio>")
      	        .attr("id","_phono-audioplayer-webrtc" + (JSEPAudio.count++))
                .attr("autoplay","autoplay")
                .attr("src",url)
                .attr("loop","loop")
      	        .appendTo("body");
        },
        stop: function() {
            $(audioPlayer).remove();
            audioPlayer = null;
        },
        volume: function() { 
        }
    }
};

// Creates a new audio Share and will optionally begin playing
JSEPAudio.prototype.share = function(transport, autoPlay, codec) {
    var share;
    var localStream;  

    return {
        // Readonly
        url: function() {
            return null;
        },
        codec: function() {
            return null;
        },
        // Control
        start: function() {
	    Phono.log.info("share() start");
            // XXX This is where we should start the peerConnection audio
        },
        stop: function() {
	    Phono.log.info("share() stop");
            // XXX This is where we should stop the peerConneciton audio
        },
        digit: function(value, duration, audible) {
            // XXX No idea how to do this yet
        },
        // Properties
        gain: function(value) {
            return null;
        },
        mute: function(value) {
            return null;
        },
        suppress: function(value) {
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
        }
    }
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
    var configuration = {iceServers:[ { url:"stun:stun.l.google.com:19302" }  ]};
    var constraints;
    var remoteContainerId;
    var complete = false;
    var audio = this;
    var candidateCount = 0;

    constraints =  {has_audio:this.config.media['audio'], has_video:this.config.media['video']};

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
                if ((evt.candidate != null || complete == true) && (candidateCount < 1 || direction == "offer")) {
        	    Phono.log.info("An Ice candidate "+JSON.stringify(evt.candidate));
                    candidateCount += 1;
                } else {
		    Phono.log.info("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                    complete = true;
                    var sdpObj = Phono.sdp.parseSDP(pc.localDescription.sdp);
                    Phono.log.info("sdpObj = " + JSON.stringify(sdpObj));
                    Phono.sdp.buildJingle(j, sdpObj);
                    var codec = 
                        {
                            id: sdpObj.contents[0].codecs[0].id,
                            name: sdpObj.contents[0].codecs[0].name,
                            rate: sdpObj.contents[0].codecs[0].clockrate
                        };
		    callback(codec);
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
                    // XXX Nail it to google-ice
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
            var codec = 
                {
                    id: sdpObj.contents[0].codecs[0].id,
                    name: sdpObj.contents[0].codecs[0].name,
                    rate: sdpObj.contents[0].codecs[0].clockrate
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
                inboundOffer = sd; // Temp stash
            }
            return {codec:codec};
        },
        destroyTransport: function() {
            // Destroy any transport state we have created
            pc.close();
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
