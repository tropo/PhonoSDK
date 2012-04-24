function JavaAudio(phono, config, callback) {

    if (JavaAudio.exists()){
      // Define defualt config and merge from constructor
      this.config = Phono.util.extend({
          jar: "//s.phono.com/releases/" + Phono.version + "/plugins/audio/phono.audio.jar"
      }  , config);
    
      // Bind Event Listeners
      Phono.events.bind(this, config);
    
      var containerId = this.config.containerId;
    
      // Create applet continer is user did not specify one
      if(!containerId) {
          this.config.containerId = containerId = _createContainer();
      }
    
      var plugin = this;
    
      // Install the applet
      plugin.$applet = _loadApplet(containerId, this.config.jar, callback, plugin);
      window.setInterval(function(){
        var str = "Loading...";
        try { 
         var json = plugin.$applet[0].getJSONStatus();
         if (json){
	   var statusO = eval('(' +json+ ')');
           if (!statusO.userTrust){
             Phono.events.trigger(phono, "error", {
                reason: "Java Applet not trusted by user - cannot continue"
             });
           } else {
             eps = statusO.endpoints;
             if (eps.length >0){
                if ((eps[0].sent > 50) && (eps[0].rcvd == 0)){
                  Phono.events.trigger(phono, "error", {
                    reason: "Java Applet detected firewall."
                  });
                }
                str = "share: "+eps[0].uri ;
                str +=" sent " +eps[0].sent ;
                str +=" rcvd " +eps[0].rcvd ;
                str +=" error " +eps[0].error ;
                Phono.log.debug("[JAVA RTP] "+str);
             }
           } 
         } else {
          Phono.events.trigger(phono, "error", {
            reason: "Java applet did not load."
          });
          Phono.log.debug("[JAVA Load errror] no status returned.");
         }
        } catch (e) {
          Phono.events.trigger(phono, "error", {
            reason: "Can not communicate with Java Applet - perhaps it did not load."
          });
          Phono.log.debug("[JAVA Load error] "+e);
        }
      },25000); 
    } else {
         Phono.events.trigger(phono, "error", {
            reason: "Java not available in this browser."
         });
    }
};

JavaAudio.exists = function() {
    return (navigator.javaEnabled());
}

JavaAudio.count = 0;

// JavahAudio Functions
//
// Most of these will simply pass through to the underlying Java layer.
// =============================================================================================

// Creates a new Player and will optionally begin playing
JavaAudio.prototype.play = function(url, autoPlay) {
    var applet = this.$applet[0];
    var player;
    var luri = url;
    var uri = Phono.util.parseUri(url);

    if (uri.protocol == "rtp") return null;
    if (uri.protocol.length < 2) {
        // We are relative, so use the document.location
        var location = Phono.util.parseUri(document.location);
        luri = location.protocol+"://"+location.authority+location.directoryPath+url;
    }

    if (autoPlay === undefined) autoPlay = false;
    player = applet.play(luri, autoPlay);
    return {
        url: function() {
            return player.getUrl();
        },
        start: function() {
            player.start();
        },
        stop: function() {
            player.stop();
        },
        volume: function() { 
            if(arguments.length === 0) {
   		return player.volume();
   	    }
   	    else {
   		player.volume(value);
   	    }
        }
    }
};

// Creates a new audio Share and will optionally begin playing
JavaAudio.prototype.share = function(url, autoPlay, codec) {
    var applet = this.$applet[0];

    Phono.log.debug("[JAVA share codec ] "+codec.p.pt +" id = "+codec.id);
    var acodec = applet.mkCodec(codec.p, codec.id);
    var share = applet.share(url, acodec, autoPlay);
    return {
        // Readonly
        url: function() {
            return share.getUrl();
        },
        codec: function() {
            var codec = share.getCodec();
            return {
                id: codec.getId(),
                name: codec.getName(),
                rate: codec.getRate()
            }
        },
        // Control
        start: function() {
            share.start();
        },
        stop: function() {
            share.stop();
        },
        digit: function(value, duration, audible) {
            share.digit(value, duration, audible);
        },
        // Properties
        gain: function(value) {
   	    if(arguments.length === 0) {
   		return share.gain();
   	    }
   	    else {
   		share.gain(value);
   	    }
        },
        mute: function(value) {
   	    if(arguments.length === 0) {
   		return share.mute();
   	    }
   	    else {
   		share.mute(value);
   	    }
        },
        suppress: function(value) {
   	    if(arguments.length === 0) {
   		return share.doES();
   	    }
   	    else {
   		share.doES(value);
   	    }
        },
        energy: function(){
            var en = share.energy();
            return {
               mic: Math.floor(Math.max((Math.LOG2E * Math.log(en[0])-4.0),0.0)),
               spk: Math.floor(Math.max((Math.LOG2E * Math.log(en[1])-4.0),0.0))
            }
        }
    }
};   

// We always have java audio permission
JavaAudio.prototype.permission = function() {
    return true;
};

// Returns an object containg JINGLE transport information
JavaAudio.prototype.transport = function() {
    var applet = this.$applet[0];
    var endpoint = applet.allocateEndpoint();
    
    return {
        name: "urn:xmpp:jingle:transports:raw-udp:1",
        description: "urn:xmpp:jingle:apps:rtp:1",
        buildTransport: function(direction, j, callback) {
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
            return {input:fullUri, output:fullUri};
        }
    }
};

String.prototype.startsWith = function(str) {
    return (this.match("^"+str)==str)
};

// Returns an array of codecs supported by this plugin
JavaAudio.prototype.codecs = function() {
    var result = new Array();
    var applet = this.$applet[0];
    var codecs = applet.codecs();
    
    for (l=0; l<codecs.length; l++) {
        var name;
        if (codecs[l].name.startsWith("SPEEX")) {name = "SPEEX";}
        else name = codecs[l].name;
        result.push({
            id: codecs[l].pt,
            name: name,
            rate: codecs[l].rate,
            p: codecs[l]
        });
    }
    
    return result;
};

JavaAudio.prototype.audioIn = function(str) {
     var applet = this.$applet[0];
    applet.setAudioIn(str);
}

JavaAudio.prototype.audioInDevices = function(){
    var result = new Array();

    //var applet = this.$applet;
    //var jsonstr = applet.getAudioDeviceList();
    
    //console.log("seeing this.audioDeviceList as "+this.audioDeviceList);
    var devs = eval ('(' +this.audioDeviceList+ ')');
    var mixers = devs.mixers;
    result.push("Let my system choose");
    for (l=0; l<mixers.length; l++) {
        if (mixers[l].targets.length > 0){
            result.push(mixers[l].name );
        }
    }

    return result;
}


// Creates a DIV to hold the capture applet if one was not specified by the user
_createContainer = function() {
    
    var appletDiv = $("<div>")
        .attr("id","_phono-appletHolder" + (JavaAudio.count++))
        .addClass("phono_AppletHolder")
        .appendTo("body");
    
    appletDiv.css({
        "width":"1px",
        "height":"1px",
        "position":"absolute",
        "top":"50%",
        "left":"50%",
   	"margin-top":"-69px",
   	"margin-left":"-107px",
        "z-index":"10001",
        "visibility":"visible"
    });
    
    var containerId = $(appletDiv).attr("id");
    return containerId;
}

_loadApplet = function(containerId, jar, callback, plugin) {
    var id = "_phonoAudio" + (JavaAudio.count++);
    
    var callbackName = id+"Callback";
    
    window[callbackName] = function(devJson) {
            //console.log("Java audio device list json is "+devJson);
            plugin.audioDeviceList = devJson;
            t = window.setTimeout( function () {callback(plugin);},10);
            };
    var applet = $("<applet>")
        .attr("id", id)
        .attr("name",id)
        .attr("code","com.phono.applet.rtp.RTPApplet")
        .attr("archive",jar)
        .attr("width","1px")
        .attr("height","1px")
        .attr("mayscript","true")
        .append($("<param>")
                .attr("name","doEC")
                .attr("value","true")
               )
        .append($("<param>")
                .attr("name","callback")
                .attr("value",callbackName)
               )
        .appendTo("#" + containerId)
    // Firefox 7.0.1 seems to treat the applet object as a function
    // which causes mayhem later on - so we return an array containing it
    // which seems to sheild us from issue.
    return applet; 
};
