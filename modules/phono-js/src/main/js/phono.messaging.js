;(function() {

   function Message(connection) {
      this.from = null;
      this.body = null;
      this.connection = connection;
   };
   
   Message.prototype.reply = function(body) {
      this.connection.send($msg({to:this.from, type:"chat"}).c("body").t(body));
   };

   function StropheMessaging(phono, config, callback) {
      
      this.connection = phono.connection;
      
      this.connection.addHandler(
         this.handleMessage.bind(this), 
         null, "message", "chat"
      );
      
      Phono.events.bind(this, config);
      
      callback(this);
   };
   
   StropheMessaging.prototype.send = function(to, body) {
      this.connection.send($msg({to:to, type:"chat"}).c("body").t(body));
   };

   StropheMessaging.prototype.handleMessage = function(msg) {
      var message = new Message(this.connection);
      message.from = Strophe.getBareJidFromJid($(msg).attr("from"));
      message.body = $(msg).find("body").text();
      Phono.events.trigger(this, "message", {
         message: message
      }, [message]);
      return true;
   };
   
   Phono.registerPlugin("messaging", {
      create: function(phono, config, callback) {
         return new StropheMessaging(phono, config, callback);
      }
   });
      
})();
