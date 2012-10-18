function C24Audio(phono, config, callback) {

    console.log("Initialize C24");

    if (typeof webkitRTCPeerConnection== "function") {
        C24Audio.GUM = function(p,s,f) {navigator.webkitGetUserMedia(p,s,f)};
        C24Audio.mkPeerConnection = function (a,b) { return new webkitRTCPeerConnection(a,b);};
	C24Audio.hasCallbacks = true;
    } else if (typeof mozRTCPeerConnection == "function") {
	C24Audio.GUM = function(p,s,f) {navigator.mozGetUserMedia(p,s,f);};
        C24Audio.mkPeerConnection = function (a,b) {return new mozRTCPeerConnection(a,b);};
	C24Audio.hasCallbacks = false;
    } else {
	C24Audio.GUM = navigator.getUserMedia;
	C24Audio.mkPeerConnection = function (a,b) {return new RTCPeerConnection(a,b);};
	C24Audio.hasCallbacks = true;
    }

    this.config = Phono.util.extend({
        media: {audio:true}
    }, config);
    
    var plugin = this;
    
    var localContainerId = this.config.localContainerId;

    // Create audio continer if user did not specify one
    if(!localContainerId) {
	this.config.localContainerId = this.createContainer();
    }

    console.log("Request access to local media, use new syntax");
    C24Audio.GUM({'audio':true, 'video':true}, 
                 function(stream) {
                     C24Audio.localStream = stream;
                     console.log("Have access to realtime audio - Happy :-) ");
                     callback(plugin);
                 },
                 function(error) {
                     console.log("Failed to get access to local media. Error code was " + error.code);
                     alert("Failed to get access to local media. Error code was " + error.code + ".");   
                 });
}

C24Audio.exists = function() {
    return (typeof webkitPeerConnection == "function") 
	|| (typeof mozRTCPeerConnection == "function")
	|| (typeof RTCPeerConnection == "function");
}

C24Audio.stun = "STUN stun.l.google.com:19302";
C24Audio.count = 0;

// C24Audio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
C24Audio.prototype.play = function(transport, autoPlay) {
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
      	        .attr("id","_phono-audioplayer-webrtc" + (C24Audio.count++))
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
C24Audio.prototype.share = function(transport, autoPlay, codec) {
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
C24Audio.prototype.permission = function() {
    return true;
};

function fakeRoap(sdes){
    var fake = "SDP\n{\n\"answererSessionId\":\"" +
	"1" + "\",\n" +
	"\"messageType\":\"" + sdes.type.toUpperCase() + "\",\n" +
	"\"offererSessionId\":\"" +
	"1" + "\",\n" +
	"\"seq\":2,\n" +
	"\"sdp\":\"" + sdes.sdp
	+ "\"}";
    console.log("FAKE ROAP======================\r\n" + fake);
    return fake;
}

// Returns an object containg JINGLE transport information
C24Audio.prototype.transport = function(config) {
    var pc;
    var configuration = {iceServers:[ { url:"stun:stun.l.google.com:19302" }  ]};
    var constraints =   {has_audio:true, has_video:false};
    var candidates = 0;

    return {
        name: "http://phono.com/webrtc/transport",
        description: "http://phono.com/webrtc/description",
        buildTransport: function(direction, j, callback, u, updateCallback) {
            ;
            if (direction == "answer") {
                console.log("inbound");
                // We are the result of an inbound call, so provide answer
                if (C24Audio.hasCallbacks) {
                    console.log("adding callbacks");
	       	    pc.onicecandidate = function(evt) {
                        console.log("onicecandidate: " + JSON.stringify(evt.candidate));
                        if (evt.candidate != null) {
        		    console.log("An Ice candidate "+JSON.stringify(evt.candidate));
                            if (candidates >=0) candidates = candidates + 1;
                        } if (candidates > 4) { //XXX HACK
			    console.log("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                            var answer = fakeRoap(pc.localDescription);
			    console.log("fake roap answer:"+answer);
			    j.c('transport',{xmlns:"http://phono.com/webrtc/transport"}).c('roap',Base64.encode(answer));
		            callback();
                            candidates = -1;
                        }
                    }
                    pc.onconnecting = function(message) {console.log("onSessionConnecting");};
	            pc.onopen = function(message) {console.log("onSessionOpened");};
                    pc.onaddstream = function (event) {
                        // XXX We will need to add the video stream here
                        console.log("Remote stream added."); 
                    };
                    pc.onremovestream = function (event) {console.log("Remote stream removed."); };
		    pc.onicechange= function (event) {console.log("ice state change now: "+pc.iceState); };
		    pc.onnegotiationneeded = function (event) {console.log("Call a diplomat - "); };
                    pc.onstatechange = function (event) {console.log("state change: "+pc.readyState); };
                } else {
		    console.log("Moz - so not adding Cbs");
                }
                console.log("add local");
                pc.addStream(C24Audio.localStream);
		var cb = function(answer) {
                    console.log("Created answer");
   		    pc.setLocalDescription(answer);
		    var msgString = JSON.stringify(answer,null," ");
                    console.log('set local desc ' + msgString);
		    if (!C24Audio.hasCallbacks){
			/* duplicate of onicecandy */
                        var answer = fakeRoap(pc.localDescription);
                        console.log("fake roap offer:"+answer);
                        j.c('transport',{xmlns:"http://phono.com/webrtc/transport"}).c('roap',Base64.encode(offer));
                        callback();
		    }
		};
		pc.createAnswer(cb , null, constraints);
            } else {
                console.log("outbound");
                pc = C24Audio.mkPeerConnection(configuration,constraints);
                console.log("create PC");
                if (C24Audio.hasCallbacks) {
                    console.log("adding callbacks");
	       	    pc.onicecandidate = function(evt) {
                        if (evt.candidate != null) {
        		    console.log("An Ice candidate "+JSON.stringify(evt.candidate));
                        } else {
			    console.log("All Ice candidates in description is now: "+JSON.stringify(pc.localDescription));
                            var offer = fakeRoap(pc.localDescription);
			    console.log("fake roap offer:"+offer);
			    j.c('transport',{xmlns:"http://phono.com/webrtc/transport"}).c('roap',Base64.encode(offer));
		            callback();
                        }
                    }
                    pc.onconnecting = function(message) {console.log("onSessionConnecting");};
	            pc.onopen = function(message) {console.log("onSessionOpened");};
                    pc.onaddstream = function (event) {
                        // XXX We will need to add the video stream here
                        console.log("Remote stream added."); 
                    };
                    pc.onremovestream = function (event) {console.log("Remote stream removed."); };
		    pc.onicechange= function (event) {console.log("ice state change now: "+pc.iceState); };
		    pc.onnegotiationneeded = function (event) {console.log("Call a diplomat - "); };
                    pc.onstatechange = function (event) {console.log("state change: "+pc.readyState); };
                } else {
		    console.log("Moz - so not adding Cbs");
                }
		console.log("add local");
                pc.addStream(C24Audio.localStream);
		var cb = function(offer) {
                    console.log("Created offer");
   		    pc.setLocalDescription(offer);
		    var msgString = JSON.stringify(offer,null," ");
                    console.log('set local desc ' + msgString);
		    if (!C24Audio.hasCallbacks){
			/* duplicate of onicecandy */
                        var offer = fakeRoap(pc.localDescription);
                        console.log("fake roap offer:"+offer);
                        j.c('transport',{xmlns:"http://phono.com/webrtc/transport"}).c('roap',Base64.encode(offer));
                        callback();
		    }
		};
		pc.createOffer(cb , null, constraints);

                // We are creating an outbound call
            }
        },
        processTransport: function(t, update) {
            console.log("process message");
            var roap;
            var message;
            t.find('roap').each(function () {
                var encoded = this.textContent;
                message = Base64.decode(encoded);
                console.log("S->C SDP: " + message);
                roap = $.parseJSON(message.substring(4,message.length));
                console.log("roap: "+JSON.stringify(roap));
            });
            if (roap['messageType'] == "OFFER") {
                // We are receiving an inbound call
                pc = C24Audio.mkPeerConnection(configuration,constraints);

                var sdp = roap.sdp;
                sdp=sdp.replace(/\bUDP\b/gi,'udp');
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"offer"} );
		console.log("about to set the remote description: "+JSON.stringify(sd,null," "));
                pc.setRemoteDescription(sd,
                                        function(){console.log("remotedescription happy");
				                   console.log("Pc now: "+JSON.stringify(pc,null," "));
			                          },
			                function(){console.log("remotedescription sad")});
            } else if (roap['messageType'] == "ANSWER") {
                // We are having an outbound call answered (must already have a PeerConnection)
                var sdp = roap.sdp;
                sdp=sdp.replace(/\bUDP\b/gi,'udp');
                var sd = new RTCSessionDescription({'sdp':sdp, 'type':"answer"} );
		console.log("about to set the remote description: "+JSON.stringify(sd,null," "));
		pc.setRemoteDescription(sd,
			                function(){console.log("remotedescription happy");
				                   console.log("Pc now: "+JSON.stringify(pc,null," "));
			                          },
			                function(){console.log("remotedescription sad")});

            }
            return {input:{uri:"webrtc"}, output:{uri:"webrtc"}};
        },
        destroyTransport: function() {
            // Destroy any transport state we have created
            pc.close();
        }
    }
};

// Returns an array of codecs supported by this plugin
// Hack until we get capabilities support
C24Audio.prototype.codecs = function() {
    var result = new Array();
    result.push({
        id: 1,
        name: "webrtc",
        rate: 16000,
        p: 20
    });
    return result;
};

C24Audio.prototype.audioInDevices = function(){
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
C24Audio.prototype.createContainer = function() {
    var webRTC = $("<video>")
      	.attr("id","_phono-audio-webrtc" + (C24Audio.count++))
        .attr("autoplay","autoplay")
      	.appendTo("body");

    var containerId = $(webRTC).attr("id");       
    return containerId;
};      
