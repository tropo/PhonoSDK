function PhonegapIOSAudio(phono, config, callback) {
    
    // Bind Event Listeners
    Phono.events.bind(this, config);
    
    var plugin = this;

    this.initState(callback, plugin);
};

PhonegapIOSAudio.exists = function() {
    return ((typeof PhoneGap != "undefined") && Phono.util.isIOS());
}

PhonegapIOSAudio.codecs = new Array();
PhonegapIOSAudio.endpoint = "rtp://0.0.0.0";

PhonegapIOSAudio.prototype.allocateEndpoint = function () {
    PhonegapIOSAudio.endpoint = "rtp://0.0.0.0";
    PhoneGap.exec( 
                  function(result) {console.log("endpoint success: " + result);
                                                    PhonegapIOSAudio.endpoint = result;}, 
                  function(result) {console.log("endpoint fail:" + result);},
                  "Phono","allocateEndpoint",[]);
}

PhonegapIOSAudio.prototype.initState = function(callback, plugin) {

    this.allocateEndpoint();
    PhoneGap.exec( 
                  function(result) {
                      console.log("codec success: " + result);
                      var codecs = $.parseJSON(result);
                      for (l=0; l<codecs.length; l++) {
                          var name;
                          if (codecs[l].name.startsWith("SPEEX")) {name = "SPEEX";}
                          else name = codecs[l].name;
                          PhonegapIOSAudio.codecs.push({
                              id: codecs[l].ptype,
                              name: name,
                              rate: codecs[l].rate,
                              p: codecs[l]
                          });
                      };
                      
                      // We are done with initialisation
                      callback(plugin);
                  }, 
                  function(result) {console.log("codec fail:" + result);},
                  "Phono","codecs",[]
                 );
};

// PhonegapIOSAudio Functions
//
// Most of these will simply pass through to the underlying Phonegap layer.
// =============================================================================================

// Creates a new Player and will optionally begin playing
PhonegapIOSAudio.prototype.play = function(transport, autoPlay) {
    var url = transport.uri;
    var luri = url;
    var uri = Phono.util.parseUri(url);
    var location = Phono.util.parseUri(document.location);

    if (uri.protocol == "rtp") return null;
    if (url.indexOf("//") == 0) {
        luri = location.protocol+":"+url;
    } else if (uri.protocol.length < 2) {
        // We are relative, so use the document.location
        luri = location.protocol+"://"+location.directoryPath.substring(0,location.directoryPath.length)+url;
        luri = encodeURI(luri);
    }

    // Get PhoneGap to create the play
    console.log("play("+luri+","+autoPlay+")");
    PhoneGap.exec( 
                  function(result) {console.log("play success: " + result);},
                  function(result) {console.log("play fail:" + result);},
                  "Phono","play",
                  [{
                      'uri':luri,
                      'autoplay': autoPlay == true ? "YES":"NO"
                  }] );


    return {
        url: function() {
            return luri;
        },
        start: function() {
            console.log("play.start " + luri);
            PhoneGap.exec( 
                          function(result) {console.log("start success: " + result);},
                          function(result) {console.log("start fail:" + result);},
                          "Phono","start",
                          [{
                              'uri':luri
                          }]);
        },
        stop: function() {
            PhoneGap.exec(
                          function(result) {console.log("stop success: " + result);},
                          function(result) {console.log("stop fail:" + result);},
                          "Phono","stop", 
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
PhonegapIOSAudio.prototype.share = function(transport, autoPlay, codec) {
    var url = transport.uri;
    var codecD = ""+codec.name+":"+codec.rate+":"+codec.id;
    // Get PhoneGap to create the share
    PhoneGap.exec( 
                  function(result) {console.log("share success: " + result);},
                  function(result) {console.log("share fail:" + result);},
                  "Phono","share",
                  [{
                      'uri':url,
                      'autoplay': autoPlay == true ? "YES":"NO",
                      'codec':codecD
                  }]);

    var luri = Phono.util.localUri(url);
    var muteStatus = false;
    var gainValue = 50;
    var micEnergy = 0.0;
    var spkEnergy = 0.0;

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
            PhoneGap.exec( 
                          function(result) {console.log("start success: " + result);},
                          function(result) {console.log("start fail:" + result);},
                          "Phono","start",
                          [{
                              'uri':luri
                          }]);
        },
        stop: function() {
            PhoneGap.exec(
                          function(result) {console.log("stop success: " + result);},
                          function(result) {console.log("stop fail:" + result);},
                          "Phono","stop", 
                          [{
                              'uri':luri
                          }]);
        },
        digit: function(value, duration, audible) {
            PhoneGap.exec(
                          function(result) {console.log("digit success: " + result);},
                          function(result) {console.log("digit fail:" + result);},
                          "Phono","digit", 
                          [{
                              'uri':luri,
                              'digit':value,
                              'duration':duration,
                              'audible':audible == true ? "YES":"NO"
                          }]
                         );
        },
        // Properties
        gain: function(value) {
   	    if(arguments.length === 0) {
                return gainValue;
   	    }
   	    else {
                PhoneGap.exec( 
                              function(result) {
                                  console.log("gain success: " + result + " " + value);
                                  gainValue = value;
                              },
                              function(result) {console.log("gain fail:" + result);},
                              "Phono","gain",
                              [{
                                  'uri':luri,
                                  'value':value
                              }]
                             );
   	    }
        },
        mute: function(value) {
   	    if(arguments.length === 0) {
                return muteStatus;
   	    }
   	    else {
                PhoneGap.exec( 
                              function(result) {
                                  console.log("mute success: " + result + " " + value);
                                  muteStatus = value;
                              },
                              function(result) {console.log("mute fail:" + result);},
                              "Phono","mute",
                              [{
                                  'uri':luri,
                                  'value':value == true ? "YES":"NO"
                              }]
                             );
   	    }
        },
        suppress: function(value) {
   	    if(arguments.length === 0) {
   	    }
   	    else {
   	    }
        },
        energy: function(){
            PhoneGap.exec(
                        function(result) {
                            console.log("energy success: " + result);
                            var en = $.parseJSON(result);
                            micEnergy = Math.floor(Math.max((Math.LOG2E * Math.log(en[0])-4.0),0.0));
                            spkEnergy = Math.floor(Math.max((Math.LOG2E * Math.log(en[1])-4.0),0.0));
                            },
                        function(result) {console.log("energy fail:" + result);},
                        "Phono","energy",
                        [{'uri':luri}]
            );
            return {
               mic: micEnergy,
               spk: spkEnergy
            }
        }
    }
};   

// We always have phonegap audio permission
PhonegapIOSAudio.prototype.permission = function() {
    return true;
};

// Returns an object containg JINGLE transport information
PhonegapIOSAudio.prototype.transport = function() {
    
    var endpoint = PhonegapIOSAudio.endpoint;
    // We've used this one, get another ready
    this.allocateEndpoint();

    return {
        name: "urn:xmpp:jingle:transports:raw-udp:1",
        description: "urn:xmpp:jingle:apps:rtp:1",
        buildTransport: function(direction, j, callback) {
            console.log("buildTransport: " + endpoint);
            var uri = Phono.util.parseUri(endpoint);
            j.c('transport',{xmlns:"urn:xmpp:jingle:transports:raw-udp:1"})
                .c('candidate',{ip:uri.domain, port:uri.port, generation:"1"});
            callback();
        },
        processTransport: function(t) {
            var fullUri;
            t.find('candidate').each(function () {
                fullUri = endpoint + ":" + $(this).attr('ip') + ":" + $(this).attr('port');
            });
            return {input:{uri:fullUri}, output:{uri:fullUri}};
        }
    }
};

String.prototype.startsWith = function(str) {
    return (this.match("^"+str)==str)
};

// Returns an array of codecs supported by this plugin
PhonegapIOSAudio.prototype.codecs = function() {
    return PhonegapIOSAudio.codecs;
};



