var Strophe = null;

;
function Phono(config) {
    Strophe = PhonoStrophe;
    // Define defualt config and merge from constructor
    this.config = Phono.util.extend({
        gateway: "gw-v6.d.phono.com",
        connectionUrl: window.location.protocol+"//app.v1.phono.com/http-bind"
    }, config);
    if (this.config.connectionUrl.indexOf("file:")==0){
        this.config.connectionUrl = "https://app.v1.phono.com/http-bind";
    }

    // Bind 'on' handlers
    Phono.events.bind(this, config);

    // Wrap ourselves with logging
    Phono.util.loggify("Phono", this);
   
    if(!config.apiKey) {
        this.config.apiKey = prompt("Please enter your Phono API Key.\n\nTo get a new one sign up for a free account at: http://www.phono.com");
        if(!this.config.apiKey) {
            var message = "A Phono API Key is required. Please get one at http://www.phono.com";
            Phono.events.trigger(this, "error", {
                reason: message
            });
            throw message;
        }
    }
   
    // Initialize Fields
    this.sessionId = null;
    this.connTimers = [];
    Phono.log.debug("[CONFIG] ConnectionUrl: " + this.config.connectionUrl);

    if(navigator.appName.indexOf('Internet Explorer')>0){
        xmlSerializer = {};
        xmlSerializer.serializeToString = function(body) {
            return body.xml;
        };
    } else {
        xmlSerializer = new XMLSerializer();
    }

    // Existing connection? do some voodoo to make sure we use their Strophe not PhonoStrophe
    if (this.config.connection != null) {
        Strophe = window.Strophe;
        Strophe.build = $build;
        Strophe.msg = $msg;
        Strophe.iq = $iq;
        Strophe.pres = $pres;
        this.connection = this.config.connection;
    } else {
        // We need to make a connection
        var phono = this;
        var cfunc = function(curl) {
            if (!phono.connected()) { 
                Phono.log.debug("trying connection URL "+curl);
                if (phono.connection != null){
                    phono.connection.disconnect();
                }
                phono.connection = new Strophe.Connection(curl);
                
                phono.connection.xmlInput = function (body) {
                    Phono.log.debug("[WIRE] (i) " + xmlSerializer.serializeToString(body));
                };
                
                phono.connection.xmlOutput = function (body) {
                Phono.log.debug("[WIRE] (o) " + xmlSerializer.serializeToString(body));
                };
                
                phono.connect();
            } else {
            Phono.log.debug("[LB] already connected... not trying URL "+curl);
            }
        }  
        
        Phono.log.debug("[LB] Invoke loadbalancer");
        // Create a dummy object so that the Strophe plugins get loaded
        var dummy = connection = new PhonoStrophe.Connection(this.config.connectionUrl);
        var a="",b= function (){} ,c="";
        var sr = new PhonoStrophe.Request(a,b,c,0);
        var srvreq = sr.xhr;
        var curls = [];
        var uri = document.createElement('a');
        var srv = "_phono";
        uri.href = this.config.connectionUrl
        Phono.log.debug("[LB] OrigT ="+uri.hostname+" path ="+uri.pathname);
        if(uri.protocol == "https:"){
           srv = srv+"s";
        }
        var dnsUrl = uri.protocol+"//"+uri.host+"/Phono/srvlookup/"+srv+"._tcp."+uri.hostname;
        srvreq.open("GET", dnsUrl, false);     // this blocks because there is really nothing else we can do untill we have a server to talk to.

        if (srvreq.overrideMimeType) {
            srvreq.overrideMimeType("application/json");
        }
        try {
            // Set the ready state handler
            srvreq.onreadystatechange = function() {
                if (srvreq.readyState == 4) {
                    Phono.log.debug("[LB] Got reply :" + srvreq.status)
                    if (srvreq.status == 200) {
                        Phono.log.debug("[LB] Reply was "+srvreq.responseText);
                        var srv = eval('(' +srvreq.responseText+ ')');
                        for (var s in srv.servers) {
                            var nexts = srv.servers[s];
                            var curl = "";
                            // if the target matches the original connectionURL then use the path from that
                            // otherwise just append /http-bind
                            var path = uri.pathname;
                            if (uri.hostname == nexts.target) {
                                if (path.indexOf('/') != 0) path = "/" + path; 
                            } else {
                                path = "/http-bind";
                            }
                            curl = uri.protocol+"//"+nexts.target +":"+nexts.port+path;
                            if (typeof nexts.target != 'undefined') {
                                Phono.log.debug("[LB] Adding connection URL "+curl);
                                curls.push(curl);
                            }
                        }
                        Phono.log.debug("[LB] Adding default connection URL "+phono.config.connectionUrl);
                        curls.push(phono.config.connectionUrl);
                        Phono.log.debug("[LB] Initial connection URL "+curls[0]);        
                        
                        // add timers for all possible srv entries (and default)
                        // if any work, we will skip the rest
                        var t = 0;
                        for (var c in curls) {
                            if (curls[c].substring) {
                                setTimeout(function(){if (curls[t].substring) cfunc(curls[t]);t = t+1;},20+(c*10000));
                            }
                        }
                    } else {
                        Phono.log.debug("[LB] loadbalancer status was "+srvreq.status);
                        Phono.log.debug("[LB] Using default connection URL "+phono.config.connectionUrl);
                        cfunc(phono.config.connectionUrl);
                    }
                }
            }
            // Send the request
            srvreq.send(null);
        } catch (e) {
            Phono.log.debug("[LB] error - ignoring a loadbalance error "+e);
            Phono.log.debug("[LB] Using default connection URL "+phono.config.connectionUrl);
            cfunc(phono.config.connectionUrl);
        }
    } 
};

(function() {
   
    // ======================================================================
   
    //@Include=phono.util.js
    //@Include=phono.logging.js
   
    // ======================================================================

   
    // Global
    Phono.version = "1.0";
   
    Phono.log = new PhonoLogger();
   
    Phono.registerPlugin = function(name, config) {
        if(!Phono.plugins) {
            Phono.plugins = {};
        }
        Phono.plugins[name] = config;
    };

    // ======================================================================


    Phono.prototype.connect = function() {
        var phono = this;
        // If this is our own internal connection
        if(!this.config.connection) {
            if(!this.connection.connected) {
                Phono.log.debug("Connecting....");
                phono.connection.connect(
                    phono.config.gateway, 
                    null, 
                    phono.handleStropheStatusChange,
                    50
                    );
            }
        }
        else {
            new PluginManager(this, this.config, function(plugins) {
                this.handleConnect();
            }).init();
        }
    };

    Phono.prototype.disconnect = function() {
        this.connection.disconnect();
    };

    Phono.prototype.connected = function() {
        return (typeof(this.connection) != 'undefined' && this.connection.connected);
    };

    Phono.prototype.handleStropheStatusChange = function(status) {
        if (status === Strophe.Status.CONNECTED) {
            if (this.connTimer != null){ 
                Phono.log.debug("Clear timeout");
                clearTimeout(this.connTimer);
            }
            new PluginManager(this, this.config, function(plugins) {
                this.handleConnect();
            }).init();
        } else if (status === Strophe.Status.DISCONNECTED) {
            this.handleDisconnect();
        } else if (status === Strophe.Status.ERROR
            || status === Strophe.Status.CONNFAIL
            || status === Strophe.Status.CONNFAIL
            || status === Strophe.Status.AUTHFAIL) {
            this.handleError();
        }
    };

    // Fires when the underlying Strophe Connection is estabilshed
    Phono.prototype.handleConnect = function() {
        var phono = this;
        phono.sessionId = Strophe.getBareJidFromJid(this.connection.jid);

        if (!this.config.connection) {
            var apiKeyIQ = Strophe.iq(
            {
                type:"set"
            })
            .c("apikey", {
                xmlns:"http://phono.com/apikey"
            })
            .t(phono.config.apiKey).up()
            .c("caps", {
                xmlns:"http://phono.com/caps", 
                ver:Phono.version
                });
           
            // Loop over all plugins adding any caps that we have
            for(pluginName in Phono.plugins) {
                if (phono[pluginName] && phono[pluginName].getCaps) {
                    apiKeyIQ = phono[pluginName].getCaps(apiKeyIQ.c(pluginName));
                    apiKeyIQ.up();
                }
            }
            apiKeyIQ = apiKeyIQ.c('browser',{
                version:navigator.appVersion, 
                agent:navigator.userAgent
                }).up();
           
            phono.connection.sendIQ(apiKeyIQ, 
                phono.handleKeySuccess,
                function() {
                    Phono.events.trigger(phono, "error", {
                        reason: "API key rejected"
                    });
                });
            if(phono.config.provisioningUrl) {
                phono.connection.send(
                    Strophe.iq({
                        type:"set"
                    })
                    .c("provisioning", {
                        xmlns:"http://phono.com/provisioning"
                    })
                    .t(phono.config.provisioningUrl)
                    );
            }
        } else {
            Phono.events.trigger(this, "ready");
        }
    };

    Phono.prototype.handleKeySuccess = function() {
        Phono.events.trigger(this, "ready");
    }
    // Fires when the underlying Strophe Connection errors out
    Phono.prototype.handleError = function() {
        // add load balance retry code here ?
        Phono.log.debug("connection failed - logging in handleError");

        Phono.events.trigger(this, "error", {
            reason: "Error connecting to XMPP server"
        });
    };

    // Fires when the underlying Strophe Connection disconnects
    Phono.prototype.handleDisconnect = function() {
        Phono.events.trigger(this, "unready");
    };

    // ======================================================================

    //@Include=flXHR.js
    //@Include=strophe.js   
    //@Include=strophe.cors.js
    //@Include=phono.events.js
    //@Include=flashembed.min.js
    //@Include=$phono-audio
    //@Include=phono.messaging.js
    //@Include=phono.sdp.js
    //@Include=phono.phone.js

    // ======================================================================

    PhonoStrophe.log = function(level, msg) {
        Phono.log.debug("[PSTROPHE] " + msg);
    };

    // Register Loggign Callback
    Phono.events.add(Phono.log, "log", function(event) {
        var date = event.timeStamp;
        var formattedDate = 
        Phono.util.padWithZeroes(date.getHours(), 2) + ":" + 
        Phono.util.padWithZeroes(date.getMinutes(), 2) + ":" + 
        Phono.util.padWithZeroes(date.getSeconds(), 2) + "." +
        Phono.util.padWithZeroes(date.getMilliseconds(), 3);
        var formattedMessage = formattedDate + " " + Phono.util.padWithSpaces(event.level.name, 5) + " - " + event.getCombinedMessages();
        var throwableStringRep = event.getThrowableStrRep();
        if (throwableStringRep) {
            formattedMessage += newLine + throwableStringRep;
        }
        console.log(formattedMessage);
    });

    // PluginManager is responsible for initializing plugins an 
    // notifying when all plugins are initialized
    function PluginManager(phono, config, readyHandler) {
        this.index = 0;
        this.readyHandler = readyHandler;
        this.config = config;
        this.phono = phono;
        this.pluginNames = new Array();
        for(pluginName in Phono.plugins) {
            this.pluginNames.push(pluginName);
        }
    };

    PluginManager.prototype.init = function(phono, config, readyHandler) {
        this.chain();
    };

    PluginManager.prototype.chain = function() {
        var manager = this;
        var pluginName = manager.pluginNames[this.index];
        Phono.plugins[pluginName].create(manager.phono, manager.config[pluginName], function(plugin) {
            manager.phono[pluginName] = plugin;
            manager.index++;
            if(manager.index === manager.pluginNames.length) {
                manager.readyHandler.apply(manager.phono);
            }
            else {
                manager.chain();
            }
        });
    };
   
})();

