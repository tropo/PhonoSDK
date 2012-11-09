function C24JingleAudio(phono, config, callback) {
    this.type = "webrtc";

    console.log("Initialize C24Jingle");

    if (typeof webkitRTCPeerConnection== "function") {
        C24JingleAudio.GUM = function(p,s,f) {navigator.webkitGetUserMedia(p,s,f)};
        C24JingleAudio.mkPeerConnection = function (a,b) { return new webkitRTCPeerConnection(a,b);};
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

    C24JingleAudio.localVideo = document.getElementById(this.config.localContainerId);

    console.log("Request access to local media, use new syntax");
    C24JingleAudio.GUM({'audio':this.config.media['audio'], 'video':this.config.media['video']}, 
                 function(stream) {
                     C24JingleAudio.localStream = stream;
                     console.log("Have access to realtime audio - Happy :-) ");
                     var url = webkitURL.createObjectURL(stream);
                     C24JingleAudio.localVideo.style.opacity = 1;
                     C24JingleAudio.localVideo.src = url;
                     callback(plugin);
                 },
                 function(error) {
                     console.log("Failed to get access to local media. Error code was " + error.code);
                     alert("Failed to get access to local media. Error code was " + error.code + ".");   
                 });
}

C24JingleAudio.exists = function() {
    return (typeof webkitPeerConnection == "function") 
	|| (typeof mozRTCPeerConnection == "function")
	|| (typeof RTCPeerConnection == "function");
}

C24JingleAudio.stun = "STUN stun.l.google.com:19302";
C24JingleAudio.count = 0;

// C24JingleAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
C24JingleAudio.prototype.play = function(transport, autoPlay) {
    var url = transport.uri;
    var luri = url;
    var audioPlayer = null;
    
    return {
        url: function() {
            return luri;
        },
        start: function() {
            if (audioPlayer != null) {
                $(audioPlayer).remove();
            }
            audioPlayer = $("<audio>")
      	        .attr("id","_phono-audioplayer-webrtc" + (C24JingleAudio.count++))
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
C24JingleAudio.prototype.share = function(transport, autoPlay, codec) {
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
	    console.log("share() start");
            // XXX This is where we should start the peerConnection audio
        },
        stop: function() {
	    console.log("share() stop");
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

// Do we have C24 permission? 
C24JingleAudio.prototype.permission = function() {
    return true;
};


// Returns an object containg JINGLE transport information
C24JingleAudio.prototype.transport = function(config) {
    var pc;
    var inboundOffer;
    var configuration = {iceServers:[ { url:"stun:stun.l.google.com:19302" }  ]};
    var constraints;
    var candidates = 0;
    var remoteContainerId;

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
            if (direction == "answer") {
                console.log("inbound");
                // We are the result of an inbound call, so provide answer
                console.log("adding callbacks");
                pc = C24JingleAudio.mkPeerConnection(configuration,constraints);

	       	pc.onicecandidate = function(evt) {
                    console.log("onicecandidate: " + JSON.stringify(evt.candidate));
                    if (evt.candidate != null) {
        		console.log("An Ice candidate "+JSON.stringify(evt.candidate));
                        if (candidates >=0) candidates = candidates + 1;
                    } else if (candidates > 1) {
			console.log("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                        var blob = Phono.sdp.parseSDP(pc.localDescription.sdp);
                        console.log("blob = " + JSON.stringify(blob));
                        Phono.sdp.buildJingle(j, blob);
                        var codec = 
                            {
                                id: blob.contents[0].codecs[0].id,
                                name: blob.contents[0].codecs[0].name,
                                rate: blob.contents[0].codecs[0].clockrate
                            };
		        callback(codec);

                        candidates = -1;
                    }
                }
                pc.onconnecting = function(message) {console.log("onSessionConnecting");};
	        pc.onopen = function(message) {console.log("onSessionOpened");};
                pc.onaddstream = function (event) {
                    console.log("Remote stream added."); 
                    var url = webkitURL.createObjectURL(event.stream);
                    remoteVideo.style.opacity = 1;
                    remoteVideo.src = url;
                };
                pc.onremovestream = function (event) {console.log("Remote stream removed."); };
		pc.onicechange= function (event) {console.log("ice state change now: "+pc.iceState); };
		pc.onnegotiationneeded = function (event) {console.log("Call a diplomat - "); };
                pc.onstatechange = function (event) {console.log("state change: "+pc.readyState); };

		console.log("Have a remote description: "+JSON.stringify(inboundOffer,null," "));
                pc.setRemoteDescription(inboundOffer,
                                        function(){console.log("remotedescription happy");
				                   console.log("Pc now: "+JSON.stringify(pc,null," "));
			                          },
			                function(){console.log("remotedescription sad")});

                console.log("add local");
                pc.addStream(C24JingleAudio.localStream);
		var cb = function(answer) {
                    console.log("Created answer");
   		    pc.setLocalDescription(answer);
		    var msgString = JSON.stringify(answer,null," ");
                    console.log('set local desc ' + msgString);
		};
		pc.createAnswer(cb , null, constraints);
            } else {
                console.log("outbound");
                pc = C24JingleAudio.mkPeerConnection(configuration,constraints);
                console.log("create PC");
                console.log("adding callbacks");
	       	pc.onicecandidate = function(evt) {
                    if (evt.candidate != null) {
        		console.log("An Ice candidate "+JSON.stringify(evt.candidate));
                        if (candidates >=0) candidates = candidates + 1;
                    } else if (candidates > 1) {
			console.log("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                        var blob = Phono.sdp.parseSDP(pc.localDescription.sdp);
                        console.log("blob = " + JSON.stringify(blob));
                        Phono.sdp.buildJingle(j, blob);

		        callback();
                        candidates = -1;
                    }
                }
                pc.onconnecting = function(message) {console.log("onSessionConnecting");};
	        pc.onopen = function(message) {console.log("onSessionOpened");};
                pc.onaddstream = function (event) {
                    console.log("Remote stream added.");
                    var url = webkitURL.createObjectURL(event.stream);
                    remoteVideo.style.opacity = 1;
                    remoteVideo.src = url;
                };
                pc.onremovestream = function (event) {console.log("Remote stream removed."); };
		pc.onicechange= function (event) {console.log("ice state change now: "+pc.iceState); };
		pc.onnegotiationneeded = function (event) {console.log("Call a diplomat - "); };
                pc.onstatechange = function (event) {console.log("state change: "+pc.readyState); };
                
		console.log("add local");
                pc.addStream(C24JingleAudio.localStream);
		var cb = function(offer) {
                    console.log("Created offer");
   		    pc.setLocalDescription(offer);
		    var msgString = JSON.stringify(offer,null," ");
                    console.log('set local desc ' + msgString);
		};
		pc.createOffer(cb , null, constraints);

                // We are creating an outbound call
            }
        },
        processTransport: function(t, update) {
            console.log("process message");

            var blobObj = Phono.sdp.parseJingle(t);
            console.log("blobObj: "+JSON.stringify(blobObj,null," "));
            var sdp = Phono.sdp.buildSDP(blobObj);
            console.log("contents: "+JSON.stringify(blobObj.contents,null," "));
            var selectedCodec = 
                {
                    id: blobObj.contents[0].codecs[0].id,
                    name: blobObj.contents[0].codecs[0].name,
                    rate: blobObj.contents[0].codecs[0].clockrate
                };

            if (pc) {
                // We are an answer to an outbound call
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"answer"} );
		console.log("about to set the remote description: "+JSON.stringify(sd,null," "));
		pc.setRemoteDescription(sd,
			                function(){console.log("remotedescription happy");
				                   console.log("Pc now: "+JSON.stringify(pc,null," "));
			                          },
			                function(){console.log("remotedescription sad")});
                
            } else {
                // We are an offer for an inbound call
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"offer"} );
                inboundOffer = sd; // Temp stash
            }
            return {input:{uri:"webrtc"}, output:{uri:"webrtc"}, codec:selectedCodec};
        },
        destroyTransport: function() {
            // Destroy any transport state we have created
            pc.close();
        }
    }
};

// Returns an array of codecs supported by this plugin
// Hack until we get capabilities support
C24JingleAudio.prototype.codecs = function() {
    return {};
};

C24JingleAudio.prototype.audioInDevices = function(){
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
C24JingleAudio.prototype.createContainer = function() {
    var webRTC = $("<video>")
      	.attr("id","_phono-audio-webrtc" + (C24JingleAudio.count++))
        .attr("autoplay","autoplay")
      	.appendTo("body");

    var containerId = $(webRTC).attr("id");       
    return containerId;
};      
