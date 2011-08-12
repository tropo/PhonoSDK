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
    PhoneGap.exec("Phono.allocateEndpoint", 
                  GetFunctionName(function(result) {console.log("endpoint success: " + result);
                                                    PhonegapIOSAudio.endpoint = result;}), 
                  GetFunctionName(function(result) {console.log("endpoint fail:" + result);}));
}

PhonegapIOSAudio.prototype.initState = function(callback, plugin) {

    this.allocateEndpoint();
    PhoneGap.exec("Phono.codecs", 
                  GetFunctionName(function(result) {
                      console.log("codec success: " + result);
                      var codecs = jQuery.parseJSON(result);
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
                  }), 
                  GetFunctionName(function(result) {console.log("codec fail:" + result);})
                 );
};

// PhonegapIOSAudio Functions
//
// Most of these will simply pass through to the underlying Phonegap layer.
// =============================================================================================

// Creates a new Player and will optionally begin playing
PhonegapIOSAudio.prototype.play = function(url, autoPlay) {
    
    var luri = url;
    var uri = Phono.util.parseUri(url);

    if (uri.protocol == "rtp") return null;
    if (uri.protocol.length < 2) {
        // We are relative, so use the document.location
        var location = Phono.util.parseUri(document.location);
        luri = location.protocol+"://"+location.directoryPath.substring(0,location.directoryPath.length-1)+url;
        luri = encodeURI(luri);
    }

    // Get PhoneGap to create the play
    PhoneGap.exec("Phono.play", 
                  GetFunctionName(function(result) {console.log("play success: " + result);}),
                  GetFunctionName(function(result) {console.log("play fail:" + result);}),
                  {
                      'uri':luri,
                      'autoplay': autoPlay == true ? "YES":"NO"
                  });


    return {
        url: function() {
            return luri;
        },
        start: function() {
            console.log("play.start " + luri);
            PhoneGap.exec("Phono.start", 
                          GetFunctionName(function(result) {console.log("start success: " + result);}),
                          GetFunctionName(function(result) {console.log("start fail:" + result);}),
                          {
                              'uri':luri
                          });
        },
        stop: function() {
            PhoneGap.exec("Phono.stop", 
                          GetFunctionName(function(result) {console.log("stop success: " + result);}),
                          GetFunctionName(function(result) {console.log("stop fail:" + result);}),
                          {
                              'uri':luri
                          });
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
PhonegapIOSAudio.prototype.share = function(url, autoPlay, codec) {

    // Get PhoneGap to create the share
    PhoneGap.exec("Phono.share", 
                  GetFunctionName(function(result) {console.log("share success: " + result);}),
                  GetFunctionName(function(result) {console.log("share fail:" + result);}),
                  {
                      'uri':url,
                      'autoplay': autoPlay == true ? "YES":"NO",
                      'codec':codec.id,
                  });

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
            PhoneGap.exec("Phono.start", 
                          GetFunctionName(function(result) {console.log("start success: " + result);}),
                          GetFunctionName(function(result) {console.log("start fail:" + result);}),
                          {
                              'uri':luri
                          });
        },
        stop: function() {
            PhoneGap.exec("Phono.stop", 
                          GetFunctionName(function(result) {console.log("stop success: " + result);}),
                          GetFunctionName(function(result) {console.log("stop fail:" + result);}),
                          {
                              'uri':luri
                          });
        },
        digit: function(value, duration, audible) {
            PhoneGap.exec("Phono.digit", 
                          GetFunctionName(function(result) {console.log("digit success: " + result);}),
                          GetFunctionName(function(result) {console.log("digit fail:" + result);}),
                          {
                              'uri':luri,
                              'digit':value,
                              'duration':duration,
                              'audible':audible == true ? "YES":"NO"
                          }
                         );
        },
        // Properties
        gain: function(value) {
   	    if(arguments.length === 0) {
                return gainValue;
   	    }
   	    else {
                PhoneGap.exec("Phono.gain", 
                              GetFunctionName(function(result) {
                                  console.log("gain success: " + result + " " + value);
                                  gainValue = value;
                              }),
                              GetFunctionName(function(result) {console.log("gain fail:" + result);}),
                              {
                                  'uri':luri,
                                  'value':value
                              }
                             );
   	    }
        },
        mute: function(value) {
   	    if(arguments.length === 0) {
                return muteStatus;
   	    }
   	    else {
                PhoneGap.exec("Phono.mute", 
                              GetFunctionName(function(result) {
                                  console.log("mute success: " + result + " " + value);
                                  muteStatus = value;
                              }),
                              GetFunctionName(function(result) {console.log("mute fail:" + result);}),
                              {
                                  'uri':luri,
                                  'value':value == true ? "YES":"NO"
                              }
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
            PhoneGap.exec("Phono.energy",
                        GetFunctionName(function(result) {
                            console.log("energy success: " + result);
                            var en = jQuery.parseJSON(result);
                            micEnergy = Math.floor(Math.max((Math.LOG2E * Math.log(en[0])-4.0),0.0));
                            spkEnergy = Math.floor(Math.max((Math.LOG2E * Math.log(en[1])-4.0),0.0));
                            }),
                        GetFunctionName(function(result) {console.log("energy fail:" + result);}),
                        {'uri':luri}
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
PhonegapIOSAudio.prototype.codecs = function() {
    return PhonegapIOSAudio.codecs;
};

