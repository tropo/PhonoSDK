function BowserAudio(phono, config, callback) {

    console.log("Initialize Bowser");

    if (typeof webkitDeprecatedPeerConnection == "function") {
        BowserAudio.peerConnection = webkitDeprecatedPeerConnection;
    } else {
        BowserAudio.peerConnection = webkitPeerConnection;
    }

    this.config = Phono.util.extend({
        media: "audio,video"
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
    return (typeof webkitDeprecatedPeerConnection == "function")|| (typeof webkitPeerConnection == "function");
}

BowserAudio.stun = "STUN 173.194.70.126:19302";
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
        name: "http://phono.com/webrtc/transport",
        description: "http://phono.com/webrtc/description",
        buildTransport: function(direction, j, callback, u, updateCallback) {
            if (direction == "answer") {
                // We are the result of an inbound call, so provide answer
                if (pc != null) {
                    pc.close();
                    pc = null;
                }
                pc = new BowserAudio.peerConnection(BowserAudio.stun,
                                                          function(message) {
                                                              console.log("Have a signalling message to send.");
                                                              console.log("C->S SDP: " + message);
                                                              var roap = $.parseJSON(message.substring(4,message.length));
                                                              if (roap['messageType'] == "ANSWER") {
                                                                  console.log("Received ANSWER from PeerConnection: " + message);
                                                                  // Canary is giving a null s= line, so 
                                                                  // we replace it with something useful
                                                                  message = message.replace("s=", "s=Canary");
                                                                  answer = message;
                                                                  j.c('transport',{xmlns:"http://phono.com/webrtc/transport"})
                                                                      .c('roap',Base64.encode(answer));
                                                                  ok = "SDP\n{\n\"answererSessionId\":\"" +
                                                                      roap['offererSessionId'] + "\",\n" +
                                                                      "\"messageType\":\"OK\",\n" +
                                                                      "\"offererSessionId\":\"" +
                                                                      roap['answererSessionId'] + "\",\n" +
                                                                      "\"seq\":1\n}"
                                                                  
                                                                  setTimeout(function() {
                                                                      // Auto OK it
                                                                      console.log("H->C SDP: " + ok);
                                                                      pc.processSignalingMessage(ok);
                                                                  }, 1);
                                                                  // Invoke the callback to finish 
                                                                  callback();
                                                              } else if (roap['messageType'] == "OFFER") {
                                                                  // Oh no, here we go
                                                                  if (offer.indexOf("video") != -1) {
                                                                      offer = message;
                                                                      u.c('transport',{xmlns:"http://phono.com/webrtc/transport"})
                                                                          .c('roap',Base64.encode(offer));
                                                                      updateCallback();
                                                                  } else {
                                                                      // This is an audio only call, lets lie
                                                                      roapAnswer = $.parseJSON(BowserAudio.offer.substring(4,message.length));
                                                                      fakeAnswer = "SDP\n{\n\"answererSessionId\":\"" +
                                                                      roap['answererSessionId'] + "\",\n" +
                                                                      "\"messageType\":\"ANSWER\",\n" +
                                                                      "\"offererSessionId\":\"" +
                                                                      roap['offererSessionId'] + "\",\n" +
                                                                          "\"seq\":2,\n" +
                                                                          "\"sdp\":\"" + roapAnswer['sdp']
                                                                          + "\"}";
                                                                      console.log("H->C SDP: " + fakeAnswer);
                                                                      pc.processSignalingMessage(fakeAnswer);
                                                                  }
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
                pc.onremovestream = function(event) {
                    conole.log("Remote stream removed.");
                };
                console.log("Created new PeerConnection, passing it :" + offer);
                pc.addStream(BowserAudio.localStream); 
                pc.processSignalingMessage(offer);
            } else {
                // We are creating an outbound call
                if (pc != null) {
                    pc.close();
                    pc = null;
                }
                pc = new BowserAudio.peerConnection(BowserAudio.stun,
                                                          function(message) {
                                                              console.log("Have a signalling message to send");
                                                              //console.log("C->S SDP: " + message);
                                                              // Canary is giving a null s= line, so 
                                                              // we replace it with something useful
                                                              message = message.replace("s=", "s=Canary");
                                                              //message = message.replace("a=group:BUNDLE audio video", "a=group:BUNDLE 2 1");
                                                              //message = message.replace("a=mid:audio", "a=mid:2");
                                                              //message = message.replace("a=mid:video", "a=mid:1");
                                                              var roap = $.parseJSON(message.substring(4,message.length));
                                                              console.log("roap messageType == " + roap['messageType']);
                                                              if (roap['messageType'] == "OFFER") {
                                                                  j.c('transport',{xmlns:"http://phono.com/webrtc/transport"})
                                                                      .c('roap',Base64.encode(message));  
                                                                  offer = message;
                                                                  console.log("about to callback to send");
                                                                  window.setTimeout(callback, 10);
                                                                  console.log("done callback");
                                                              } else if (roap['messageType'] == "OK") {
                                                                  // Ignore, we autogenerate on remote side
                                                              }
                                                              else if (roap['messageType'] == "ANSWER") {
                                                                  // Oh no, here we go
                                                                  answer = message;
                                                                  u.c('transport',{xmlns:"http://phono.com/webrtc/transport"})
                                                                      .c('roap',Base64.encode(answer));
                                                                  updateCallback();
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
        processTransport: function(t, update) {
            var roap;
            var message;
            t.find('roap').each(function () {
                var encoded = this.textContent;
                message = Base64.decode(encoded);
                console.log("S->C SDP: " + message);
                roap = $.parseJSON(message.substring(4,message.length));
            });
            if (roap['messageType'] == "OFFER") {
                // We are receiving an inbound call
                // Store the offer so we can use it to create an answer
                //  when the user decides to do so
                offer = message;
                // Or we are getting an update...
                if (pc != null && update == true) pc.processSignalingMessage(message);
            } else if (roap['messageType'] == "ANSWER") {

                // We are having an outbound call answered (must already have a PeerConnection)
                pc.processSignalingMessage(message);
            }
            return {input:{uri:"webrtc"}, output:{getPC: function() {return pc;}}};
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
