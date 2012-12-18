function FlashAudio(phono, config, callback) {
    this.type = "flash";

    // Define defualt config and merge from constructor
    this.config = Phono.util.extend({
        protocol: "rtmfp",
        swf: "//" + MD5.hexdigest(window.location.host+phono.config.apiKey) + ".u.phono.com/releases/" + Phono.version + "/plugins/audio/phono.audio.swf",
        cirrus: "rtmfp://phono-fms1-ext.voxeolabs.net/phono",
        bridged: false,
        media: {audio:true,video:true}
    }, config);

    // Bind Event Listeners
    Phono.events.bind(this, config);
    
    var containerId = this.config.containerId;
    
    // Create flash continer is user did not specify one
    if(!containerId) {
	this.config.containerId = containerId = this.createContainer();
    }
    
    // OMG! Fix position of flash movie to be integer pixel
    Phono.events.bind(this, {
        onPermissionBoxShow: function() {
            var p = $("#"+containerId).position();
            $("#"+containerId).css("left",parseInt(p.left));
            $("#"+containerId).css("top",parseInt(p.top));
        } 
    });		
    
    var plugin = this;
    
    // Flash movie is embedded asynchronously so we need a listener 
    // to fire when the SWF is loaded and ready for action
    FABridge.addInitializationCallback(containerId, function(){
        Phono.log.info("FlashAudio Ready");
        plugin.$flash = this.create("Wrapper").getAudio();
        plugin.$flash.addEventListener(null, function(event) {
            var eventName = (event.getType()+"");
            Phono.events.trigger(plugin, eventName, {
                reason: event.getReason()
            });
            if (eventName == "mediaError") {
                Phono.events.trigger(phono, "error", {
                    reason: event.getReason()
                });
            }
        });
        plugin.$flash.setVersion(Phono.version);
        callback(plugin);
    });

    wmodeSetting = "opaque";
    
    if ((navigator.appVersion.indexOf("X11")!=-1) || (navigator.appVersion.indexOf("Linux")!=-1) || ($.browser.opera)) {
        wmodeSetting = "window";
    }
    
    // Embed flash plugin
    flashembed(containerId, 
               {
                   id:containerId + "id",
                   src:this.config.swf + "?rnd=" + new Date().getTime(),
                   wmode:wmodeSetting
               }, 
               {
                   bridgeName:containerId
               }
              );
};

FlashAudio.count = 0;

// FlashAudio Functions
//
// Most of these will simply pass through to the underlying Flash layer.
// In the old API this was done by 'wrapping' the Flash object. I've chosen a more verbos 
// approach to aid in debugging now that the Flash side has been reduced to a few simple calls.
// =============================================================================================

FlashAudio.prototype.getCaps = function(c) {
    return c.c(this.type,{protocol:this.config.protocol, bridged:this.config.bridged}).up();
};

// Show the Flash Audio permission box
FlashAudio.prototype.showPermissionBox = function() {
    this.$flash.showPermissionBox();
};

// Returns true if the FLash movie has microphone access
FlashAudio.prototype.permission = function() {
    return this.$flash.getHasPermission();
};

// Creates a new Player and will optionally begin playing
FlashAudio.prototype.play = function(transport, autoPlay) {
    url = transport.uri.replace("protocol",this.config.protocol);
    var luri = url;
    var uri = Phono.util.parseUri(url);
    var location = Phono.util.parseUri(document.location);
    
    if (uri.protocol == "rtp") return null;
    if (url.indexOf("//") == 0) {
        luri = location.protocol+":"+url;
    } else if (uri.protocol.length < 2) {
        // We are relative, so use the document.location
        luri = location.protocol+"://"+location.authority+location.directoryPath+url;
    }
    
    var player;
    if (this.config.bridged == false && transport.peerID != undefined && this.config.cirrus != undefined) {
        Phono.log.info("Direct media play with peer " + transport.peerID);
        player = this.$flash.play(luri, autoPlay, transport.peerID, this.config.video);
    }
    else player = this.$flash.play(luri, autoPlay);
    return {
        url: function() {
            return player.getUrl();
        },
        start: function() {
            player.start();
        },
        stop: function() {
            player.stop();
            // This is final, you can't restart if it was rtmp or rtmfp
            player.release();
        },
        volume: function(value) {
   	    if(arguments.length === 0) {
   		return player.getVolume();
   	    }
   	    else {
   		player.setVolume(value);
   	    }
        }
    }
};

// Creates a new audio Share and will optionally begin playing
FlashAudio.prototype.share = function(transport, autoPlay, codec) {
    var url = transport.uri.replace("protocol",this.config.protocol);
    var peerID = "";
    if (this.config.bridged == false && transport.peerID != undefined && this.config.cirrus != undefined) { 
        peerID = transport.peerID;
        Phono.log.info("Direct media share with peer " + transport.peerID);
    }
    var isSecure = false;
    var share = this.$flash.share(url, autoPlay, codec.id, codec.name, codec.rate, true, peerID, this.config.video);
    if (url.indexOf("rtmfp://") == 0) isSecure = true;

    var s = {
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
            // This is final, you can't restart
            share.release();
        },
        digit: function(value, duration, audible) {
            share.digit(value, duration, audible);
        },
        // Properties
        gain: function(value) {
   	    if(arguments.length === 0) {
   		return share.getGain();
   	    }
   	    else {
   		share.setGain(value);
   	    }
        },
        mute: function(value) {
   	    if(arguments.length === 0) {
   		return share.getMute();
   	    }
   	    else {
   		share.setMute(value);
   	    }
        },
        suppress: function(value) {
   	    if(arguments.length === 0) {
   		return share.getSuppress();
   	    }
   	    else {
   		share.setSuppress(value);
   	    }
        },
        energy: function() {
            return {
               mic: 0.0 ,
               spk: 0.0
            }
        },
        secure: function() {
            return isSecure;
        }
    };

    share.addEventListener(null, function(event) {
        var eventName = (event.getType()+"");
        Phono.events.trigger(s, eventName, {
            reason: event.getReason()
        });
    });

    return s;
};   

// Returns an object containg JINGLE transport information
FlashAudio.prototype.transport = function() {
    var $flash = this.$flash;
    var config = this.config;
    var cirrus = this.config.cirrus;
    var plugin = this;
    var name = this.$flash.getTransport();
    var description = this.$flash.getDescription();

    return {
        name: name,
        description: description,
        buildTransport: function(direction, j, callback) {
            var nearID = "";
            if (!config.bridged) {
                // XXX HOW LONG WILL WAIT BEFORE ABORTING?
                var onConnected = function() {
                    if (nearID == "") {
                        // First call
                        nearID = $flash.nearID(cirrus);
                        Phono.log.info("Got nearID = " + nearID);
                        if (nearID != "") {
                            j.c('transport',{xmlns:name, peerID:nearID});
                        } else {
                            j.c('transport',{xmlns:name});
                        }
                        callback();
                    }
                }
                
                // Connect to cirrus, and once we get the good event, grab the nearID and continue
                var connected = $flash.doCirrusConnect(cirrus);
                Phono.log.info("doCirrusConnect");
                if (connected) {
                    // Will not get an additional callabck
                    Phono.log.info("doCirrusConnect - already connected");
                    onConnected();
                } else {
                    Phono.events.add(plugin, "flashConnected", onConnected);
                }
            } else {
                j.c('transport',{xmlns:this.name});
                callback();
            }
        },
        processTransport: function(t) {
            var pID = t.attr('peerid');
            var transport;
            // If we have a Peer ID, and no other transport, fake one
            if (pID != undefined)
                transport = {input: {uri: "rtmfp://invalid/invalid",
                                     peerID: pID},   
                             output: {uri: "rtmfp://invalid/invalid",
                                      peerID: pID}
                            };
            t.find('candidate').each(function () {
                transport = { input: {uri: $(this).attr('rtmpUri') + "/" + $(this).attr('playName'),
                                      peerID: pID},   
                              output: {uri: $(this).attr('rtmpUri') + "/" + $(this).attr('publishName'),
                                       peerID: pID}
                            };
            });
            return transport;
        },
        destroyTransport: function() {
            // Disconnect from cirrus server, reference counting is done in phono-as-audio
            if (!config.bridged) {
                Phono.log.info("Disconnecting from cirrus server");
                $flash.doCirrusDisconnect(cirrus);
            }
        }
    }
};

// Returns an array of codecs supported by this plugin
FlashAudio.prototype.codecs = function() {
    var result = new Array();
    var codecs = this.$flash.getCodecs();
    Phono.util.each(codecs, function() {
        result.push({
            id: this.getId(),
            name: this.getName(),
            rate: this.getRate()
        });
    });
    return result;
};

// Creates a DIV to hold the Flash movie if one was not specified by the user
FlashAudio.prototype.createContainer = function(phono) {
    
    var flashDiv = $("<div>")
      	.attr("id","_phono-audio-flash" + (FlashAudio.count++))
      	.addClass("phono_FlashHolder")
      	.appendTo("body");
    
    flashDiv.css({
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
    
    var containerId = $(flashDiv).attr("id");
    
    Phono.events.bind(this, {
      	onPermissionBoxShow: function() {
	    $("#"+containerId).css({
		"width":"240px",
   		"height":"160px"
	    });
      	},
      	onPermissionBoxHide: function() {
	    $("#"+containerId).css({
		"width":"1px",
   		"height":"1px"
	    });
      	}
    });
    
    return containerId;
    
};      

