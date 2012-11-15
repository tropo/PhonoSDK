function BowserAudio(phono, config, callback) {

    console.log("Initialize Bowser");

    if (typeof webkitDeprecatedPeerConnection == "function") {
        BowserAudio.peerConnection = webkitDeprecatedPeerConnection;
    } else {
        BowserAudio.peerConnection = webkitPeerConnection;
    }

    this.config = Phono.util.extend({
        media:  {audio:true, video:false}
    }, config);
    
    var plugin = this;
    
    var localContainerId = this.config.localContainerId;

    // Create audio continer if user did not specify one
    if(!localContainerId) {
	this.config.localContainerId = this.createContainer();
    }

    BowserAudio.localVideo = document.getElementById(this.config.localContainerId);
    
    console.log("getUserMedia...");
    navigator.webkitGetUserMedia("video,audio", 
                                 function(stream) {
                                     BowserAudio.localStream = stream;
                                     console.log("We have a stream");
                                     var url = webkitURL.createObjectURL(stream);
                                     BowserAudio.localVideo.style.opacity = 1;
                                     BowserAudio.localVideo.src = url;
                                     var pc = new PeerConnection();
                                     callback(plugin);
                                 },
                                 function(error) {
                                     console.log("Failed to get access to local media. Error code was " + error.code);
                                     alert("Failed to get access to local media. Error code was " + error.code + ".");   
                                 });    
}

BowserAudio.exists = function() {
    return (typeof webkitDeprecatedPeerConnection == "function") || (typeof webkitPeerConnection == "function");
}

BowserAudio.stun = "STUN stun.l.google.com:19302";
BowserAudio.count = 0;

// BowserAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
BowserAudio.prototype.play = function(transport, autoPlay) {
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
      	        .attr("id","_phono-audioplayer-webrtc" + (BowserAudio.count++))
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
BowserAudio.prototype.share = function(transport, autoPlay, codec) {
    var url = transport.uri;
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
            // Start - we already have done...
        },
        stop: function() {
            // Stop
            console.log("Closing PeerConnection");
            if (transport.getPC() != null) {
                transport.getPC().close();
                console.log("closed");
            } 
//            BowserAudio.remoteVideo.style.opacity = 0;
        },
        digit: function(value, duration, audible) {
            // No idea how to do this yet
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

// Do we have Bowser permission? 
BowserAudio.prototype.permission = function() {
    return true;
};

function fakeRoap(sdp, type){
    var fakeJson = {
        answererSessionId: "1",
        messageType: type,
        offererSessionId: "1",
        seq:2,
        sdp: sdp
    };
    var fake = "SDP\r\n" + JSON.stringify(fakeJson);
    console.log("FAKE ROAP======================\r\n" + fake);
    return fake;
}

// Returns an object containg JINGLE transport information
BowserAudio.prototype.transport = function(config) {
    var pc, offer, answer, ok, remoteContainerId;

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

            } else {
                // We are creating an outbound call
                if (pc != null) {
                    pc.close();
                    pc = null;
                }
                pc = new BowserAudio.peerConnection(BowserAudio.stun,
                                                          function(message) {
                                                              console.log("Have a signalling message to send");
                                                              var roap = $.parseJSON(message.substring(4,message.length));
                                                              console.log("roap messageType == " + roap['messageType']);
                                                              var sdpObj = Phono.sdp.parseSDP(roap['sdp']);
                                                              console.log("Have an sdpObj");

                                                              if (roap['messageType'] == "OFFER") {
                                                                  console.log("buildJingle");
                                                                  Phono.sdp.buildJingle(j, sdpObj);
                                                                  var codec = 
                                                                      {
                                                                          id: sdpObj.contents[0].codecs[0].id,
                                                                          name: sdpObj.contents[0].codecs[0].name,
                                                                          rate: sdpObj.contents[0].codecs[0].clockrate
                                                                      };
                                                                  console.log("do callback");
		                                                  callback(codec);
                                                              } else if (roap['messageType'] == "OK") {
                                                                  // Ignore, we autogenerate on remote side
                                                              } else {
                                                                  console.log("Recieved unexpected ROAP: " + message);
                                                              }
                                                          }
                                                         );
                pc.onaddstream = function(event) {
                    console.log("Remote stream added.");
                    console.log("Local stream is: " + BowserAudio.localStream);
                    var url = webkitURL.createObjectURL(event.stream);
                    remoteVideo.style.opacity = 1;
                    remoteVideo.src = url;
                };
                pc.addStream(BowserAudio.localStream);
                console.log("Created PeerConnection for new OUTBOUND CALL");
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

            console.log("codec = " + codec.name);

            if (pc) {
                // We are an answer to an outbound call
                // Turn the answer into a ROAP message

                var roap = fakeRoap(sdp, "ANSWER");
                console.log("Calling processSignallingMessage()");
                pc.processSignalingMessage(roap);
                console.log("Called processSignallingMessage()");
            }
            return {codec: codec};
        }
    }
};

// Returns an array of codecs supported by this plugin
// Hack until we get capabilities support
BowserAudio.prototype.codecs = function() {
    var result = new Array();
    result.push({
        id: 1,
        name: "webrtc",
        rate: 16000,
        p: 20
    });
    return result;
};

BowserAudio.prototype.audioInDevices = function(){
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
BowserAudio.prototype.createContainer = function() {
    var webRTC = $("<video>")
      	.attr("id","_phono-audio-webrtc" + (BowserAudio.count++))
        .attr("autoplay","autoplay")
      	.appendTo("body");

    var containerId = $(webRTC).attr("id");       
    return containerId;
};      
