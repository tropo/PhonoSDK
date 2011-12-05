function PhonegapAndroidAudio(phono, config, callback) {
    
    // Bind Event Listeners
    Phono.events.bind(this, config);

    var plugin = this;

    // Register our Java plugin with Phonegap so that we can call it later
    PhoneGap.exec(null, null, "App", "addService", ['PhonogapAudio', 'com.phono.android.phonegap.Phono']);
    
    // FIXME: Should not have to do this twice!
    this.allocateEndpoint();
    this.initState(callback, plugin);
};

PhonegapAndroidAudio.exists = function() {
    return ((typeof PhoneGap != "undefined") && Phono.util.isAndroid());
}

PhonegapAndroidAudio.codecs = new Array();
PhonegapAndroidAudio.endpoint = "rtp://0.0.0.0";

PhonegapAndroidAudio.prototype.allocateEndpoint = function () {
    
    PhonegapAndroidAudio.endpoint = "rtp://0.0.0.0";

    PhoneGap.exec(function(result) {console.log("endpoint: success");
                                    PhonegapAndroidAudio.endpoint = result.uri;
                                   },
                  function(result) {console.log("endpoint: fail");},
                  "PhonogapAudio",  
                  "allocateEndpoint",              
                  [{}]);      
}

PhonegapAndroidAudio.prototype.initState = function(callback, plugin) {

    this.allocateEndpoint();

    var codecSuccess = function(result) {
        console.log("codec: success");
        var codecs = result.codecs;
        for (l=0; l<codecs.length; l++) {
            var name;
            if (codecs[l].name.startsWith("SPEEX")) {name = "SPEEX";}
            else name = codecs[l].name;
            PhonegapAndroidAudio.codecs.push({
                id: codecs[l].ptype,
                name: name,
                rate: codecs[l].rate,
                p: codecs[l]
            });
        }
        // We are done with initialisation
        callback(plugin);
    }
    
    var codecFail = function(result) {
        console.log("codec:fail");
    }
    
    // Get the codec list
    PhoneGap.exec(codecSuccess,
                  codecFail,
                  "PhonogapAudio",
                  "codecs",
                  [{}]);
};

// PhonegapAndroidAudio Functions
//
// Most of these will simply pass through to the underlying Phonegap layer.
// =============================================================================================

// Creates a new Player and will optionally begin playing
PhonegapAndroidAudio.prototype.play = function(url, autoPlay) {
    
    var luri = url;
    var uri = Phono.util.parseUri(url);
    var location = Phono.util.parseUri(document.location);

    if (uri.protocol == "rtp") return null;
    if (url.indexOf("//") == 0) {
        luri = location.protocol+":"+url;
    } else if (uri.protocol.length < 2){
        // We are relative, so use the document.location
        luri = location.protocol+"://"+location.directoryPath.substring(0,location.directoryPath.length)+url;
        luri = encodeURI(luri);
    }

    // Get PhoneGap to create the play
    PhoneGap.exec(function(result) {console.log("play: success");},
                  function(result) {console.log("play: fail");},
                  "PhonogapAudio",  
                  "play",              
                  [{
                      'uri':luri,
                      'autoplay': autoPlay == true ? "YES":"NO"
                  }]);      

    return {
        url: function() {
            return luri;
        },
        start: function() {
            console.log("play.start " + luri);
            PhoneGap.exec(function(result) {console.log("start: success");},
                  function(result) {console.log("start: fail");},
                  "PhonogapAudio",  
                  "start",              
                  [{
                      'uri':luri
                  }]);   
        },
        stop: function() {
            console.log("play.stop " + luri);
            PhoneGap.exec(function(result) {console.log("stop: success");},
                  function(result) {console.log("stop: fail");},
                  "PhonogapAudio",  
                  "stop",              
                  [{
                      'uri':luri
                  }]);   

        },
        volume: function() { 
            if(arguments.length === 0) {
                
   	    }
   	    else {
   	    }
        }
    }
};

// Creates a new audio Share and will optionally begin playing
PhonegapAndroidAudio.prototype.share = function(url, autoPlay, codec) {

    // Get PhoneGap to create the share
    PhoneGap.exec(function(result) {console.log("share: success");},
                  function(result) {console.log("share: fail");},
                  "PhonogapAudio",  
                  "share",              
                  [{
                      'uri':url,
                      'autoplay': autoPlay == true ? "YES":"NO",
                      'codec':codec.name
                  }]);   

    var luri = Phono.util.localUri(url);
    var muteStatus = false;
    var gainValue = 50;

    // Return a shell of an object
    return {
        // Readonly
        url: function() {
            return url;
        },
        codec: function() {
            var codec;
            return {
                id: codec.getId(),
                name: codec.getName(),
                rate: codec.getRate()
            }
        },
        // Control
        start: function() {
            console.log("share.start " + luri);
            PhoneGap.exec(function(result) {console.log("start: success");},
                          function(result) {console.log("start: fail");},
                          "PhonogapAudio",  
                          "start",              
                          [{
                              'uri':luri
                          }]);   
        },
        stop: function() {
            console.log("share.stop " + luri);
            PhoneGap.exec(function(result) {console.log("stop: success");},
                          function(result) {console.log("stop: fail");},
                          "PhonogapAudio",  
                          "stop",              
                          [{
                              'uri':luri
                          }]);   
        },
        digit: function(value, duration, audible) {
            console.log("share.digit " + luri);
            PhoneGap.exec(function(result) {console.log("digit: success");},
                          function(result) {console.log("digit: fail");},
                          "PhonogapAudio",  
                          "digit",              
                          [{
                              'uri':luri,
                              'digit':value,
                              'duration':duration,
                              'audible':audible == true ? "YES":"NO"
                          }]);   
        },
        // Properties
        gain: function(value) {
   	    if(arguments.length === 0) {
                return gainValue;
   	    }
   	    else {
                console.log("share.gain " + luri);
                PhoneGap.exec(function(result) {console.log("gain: success");},
                              function(result) {console.log("gain: fail");},
                              "PhonogapAudio",  
                              "gain",              
                              [{
                                  'uri':luri,
                                  'value':value
                              }]);   
   	    }
        },
        mute: function(value) {
   	    if(arguments.length === 0) {
                return muteStatus;
   	    }
   	    else {
                console.log("share.mute " + luri);
                PhoneGap.exec(function(result) {console.log("mute: success");},
                              function(result) {console.log("mute: fail");},
                              "PhonogapAudio",  
                              "mute",              
                              [{
                                  'uri':luri,
                                  'value':value == true ? "YES":"NO"
                              }]);   
   	    }
        },
        suppress: function(value) {
   	    if(arguments.length === 0) {
   	    }
   	    else {
   	    }
        },
        energy: function(){
            return {
               mic: 0.0,
               spk: 0.0
            }
        }
    }
};   

// We always have phonegap audio permission
PhonegapAndroidAudio.prototype.permission = function() {
    return true;
};

// Returns an object containg JINGLE transport information
PhonegapAndroidAudio.prototype.transport = function() {
    
    var endpoint = PhonegapAndroidAudio.endpoint;
    // We've used this one, get another ready
    this.allocateEndpoint();

    return {
        name: "urn:xmpp:jingle:transports:raw-udp:1",
        description: "urn:xmpp:jingle:apps:rtp:1",
        buildTransport: function(j) {
            console.log("buildTransport: " + endpoint);
            var uri = Phono.util.parseUri(endpoint);
            j.c('transport',{xmlns:"urn:xmpp:jingle:transports:raw-udp:1"})
                .c('candidate',{ip:uri.domain, port:uri.port, generation:"1"});
        },
        processTransport: function(t) {
            var fullUri;
            t.find('candidate').each(function () {
                fullUri = endpoint + ":" + $(this).attr('ip') + ":" + $(this).attr('port');
            });
            return fullUri;
        }
    }
};

String.prototype.startsWith = function(str) {
    return (this.match("^"+str)==str)
};

// Returns an array of codecs supported by this plugin
PhonegapAndroidAudio.prototype.codecs = function() {
    return PhonegapAndroidAudio.codecs;
};

