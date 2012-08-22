//@Include=FABridge.js

// FIXME: Needed by flXHR
var flensed={base_path:"//s.phono.com/deps/flensed/1.0/"};

(function($) {

//@Include=$phono-core
   
   $.phono = function(config) {
      return new Phono(config);
   }
   
})(jQuery);
