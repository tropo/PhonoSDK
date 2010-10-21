//@Include=FABridge.js

// FIXME: Needed by flXHR
var flensed={base_path:"http://s.phono.com/deps/flensed/1.0/"};

;function Phono(config) {

   // Define defualt config and merge from constructor
   this.config = Phono.util.extend({
      gateway: "gw.phono.com",
      connectionUrl: "http://app.phono.com/http-bind"
   }, config);

   // Bind 'on' handlers
   Phono.events.bind(this, config);
   
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
   this.connection = new Strophe.Connection(this.config.connectionUrl);
   this.connect();
   
};

(function() {
   
   //@Include=log4javascript_lite_stub.js

   // Global
   Phono.version = "0.1";
   Phono.log = log4javascript.getDefaultLogger();

   Phono.registerPlugin = function(name, config) {
      if(!Phono.plugins) {
         Phono.plugins = {};
      }
      Phono.plugins[name] = config;
   };

   // ======================================================================
   
   Phono.prototype.connect = function() {

      // Noop if already connected
      if(this.connection.connected) return;

      var phono = this;

      this.connection.connect(phono.config.gateway, null, function (status) {
         if (status === Strophe.Status.CONNECTED) {
            phono.connection.send(
               $iq({type:"set"})
                  .c("apikey", {xmlns:"http://phono.com/apikey"})
                  .t(phono.config.apiKey)
            );
            phono.handleConnect();
         } else if (status === Strophe.Status.DISCONNECTED) {
            phono.handleDisconnect();
         } else if (status === Strophe.Status.ERROR 
                 || status === Strophe.Status.CONNFAIL 
                 || status === Strophe.Status.CONNFAIL 
                 || status === Strophe.Status.AUTHFAIL) {
            phono.handleError();
          }
      });
   };

   Phono.prototype.disconnect = function() {
      this.connection.disconnect();
   };

   Phono.prototype.connected = function() {
      return this.connection.connected;
   };

   // Fires when the underlying Strophe Connection is estabilshed
   Phono.prototype.handleConnect = function() {
      this.sessionId = Strophe.getBareJidFromJid(this.connection.jid);
      new PluginManager(this, this.config, function(plugins) {
         Phono.events.trigger(this, "ready");
      }).init();
   };

   // Fires when the underlying Strophe Connection errors out
   Phono.prototype.handleError = function() {
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
   //@Include=phono.util.js
   //@Include=phono.events.js
   //@Include=flashembed.min.js
   //@Include=phono.audio.js
   //@Include=phono.messaging.js
   //@Include=phono.phone.js

   // ======================================================================

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

