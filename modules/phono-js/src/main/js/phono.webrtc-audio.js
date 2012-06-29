function WebRTCAudio(phono, config, callback) {

    console.log("Initialize WebRTC");

    if (typeof webkitDeprecatedPeerConnection == "function") {
        WebRTCAudio.peerConnection = webkitDeprecatedPeerConnection;
    } else {
        WebRTCAudio.peerConnection = webkitPeerConnection;
    }

    this.config = Phono.util.extend({
        media: {audio:true,video:true}
    }, config);
    
    var plugin = this;
    
    var localContainerId = this.config.localContainerId;

    // Create audio continer if user did not specify one
    if(!localContainerId) {
	this.config.localContainerId = this.createContainer();
    }

    WebRTCAudio.localVideo = document.getElementById(this.config.localContainerId);

    try { 
        console.log("Request access to local media, use new syntax");
        navigator.webkitGetUserMedia(this.config.media, 
                                     function(stream) {
                                         WebRTCAudio.localStream = stream;
                                         console.log("We have a stream");
                                         var url = webkitURL.createObjectURL(stream);
                                         WebRTCAudio.localVideo.style.opacity = 1;
                                         WebRTCAudio.localVideo.src = url;
                                         callback(plugin);
                                     },
                                     function(error) {
                                         console.log("Failed to get access to local media. Error code was " + error.code);
                                         alert("Failed to get access to local media. Error code was " + error.code + ".");   
                                     });
    } catch (e) {
        console.log("getUserMedia error, try old syntax");
        navigator.webkitGetUserMedia("video,audio", 
                                     function(stream) {
                                         WebRTCAudio.localStream = stream;
                                         console.log("We have a stream");
                                         var url = webkitURL.createObjectURL(stream);
                                         WebRTCAudio.localVideo.style.opacity = 1;
                                         WebRTCAudio.localVideo.src = url;
                                         callback(plugin);
                                     },
                                     function(error) {
                                         console.log("Failed to get access to local media. Error code was " + error.code);
                                         alert("Failed to get access to local media. Error code was " + error.code + ".");   
                                     });    
    }
}

WebRTCAudio.exists = function() {
    return (typeof webkitDeprecatedPeerConnection == "function")|| (typeof webkitPeerConnection == "function");
}

WebRTCAudio.stun = "STUN stun.l.google.com:19302";
WebRTCAudio.count = 0;

// WebRTCAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
WebRTCAudio.prototype.play = function(transport, autoPlay) {
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
      	        .attr("id","_phono-audioplayer-webrtc" + (WebRTCAudio.count++))
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
WebRTCAudio.prototype.share = function(transport, autoPlay, codec) {
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
//            WebRTCAudio.remoteVideo.style.opacity = 0;
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

// Do we have WebRTC permission? 
WebRTCAudio.prototype.permission = function() {
    return true;
};

// Returns an object containg JINGLE transport information
WebRTCAudio.prototype.transport = function(config) {
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
                pc = new WebRTCAudio.peerConnection(WebRTCAudio.stun,
                                                          function(message) {
                                                              console.log("C->S SDP: " + message);
                                                              var roap = jQuery.parseJSON(message.substring(4,message.length));
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
                                                                      roapAnswer = jQuery.parseJSON(offer.substring(4,message.length));
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
                    console.log("Local stream is: " + WebRTCAudio.localStream);
                    var url = webkitURL.createObjectURL(event.stream);
                    remoteVideo.style.opacity = 1;
                    remoteVideo.src = url;
                };
                pc.onremovestream = function(event) {
                    conole.log("Remote stream removed.");
                };
                console.log("Created new PeerConnection, passing it :" + offer);
                pc.addStream(WebRTCAudio.localStream); 
                pc.processSignalingMessage(offer);
            } else {
                // We are creating an outbound call
                if (pc != null) {
                    pc.close();
                    pc = null;
                }
                pc = new WebRTCAudio.peerConnection(WebRTCAudio.stun,
                                                          function(message) {
                                                              console.log("C->S SDP: " + message);
                                                              // Canary is giving a null s= line, so 
                                                              // we replace it with something useful
                                                              message = message.replace("s=", "s=Canary");
                                                              //message = message.replace("a=group:BUNDLE audio video", "a=group:BUNDLE 2 1");
                                                              //message = message.replace("a=mid:audio", "a=mid:2");
                                                              //message = message.replace("a=mid:video", "a=mid:1");
                                                              var roap = jQuery.parseJSON(message.substring(4,message.length));
                                                              if (roap['messageType'] == "OFFER") {
                                                                  j.c('transport',{xmlns:"http://phono.com/webrtc/transport"})
                                                                      .c('roap',Base64.encode(message));  
                                                                  offer = message;
                                                                  callback();
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
                    console.log("Local stream is: " + WebRTCAudio.localStream);
                    var url = webkitURL.createObjectURL(event.stream);
                    remoteVideo.style.opacity = 1;
                    remoteVideo.src = url;
                };
                pc.addStream(WebRTCAudio.localStream);
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
                roap = jQuery.parseJSON(message.substring(4,message.length));
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
WebRTCAudio.prototype.codecs = function() {
    var result = new Array();
    result.push({
        id: 1,
        name: "webrtc",
        rate: 16000,
        p: 20
    });
    return result;
};

WebRTCAudio.prototype.audioInDevices = function(){
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
WebRTCAudio.prototype.createContainer = function() {
    var webRTC = $("<video>")
      	.attr("id","_phono-audio-webrtc" + (WebRTCAudio.count++))
        .attr("autoplay","autoplay")
      	.appendTo("body");

    var containerId = $(webRTC).attr("id");       
    return containerId;
};      
