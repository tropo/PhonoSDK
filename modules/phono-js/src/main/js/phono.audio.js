;(function() {
    //@Include=phono.as-audio.js
    //@Include=phono.java-audio.js
    //@Include=phono.phonegap-audio.js

    Phono.registerPlugin("audio", {
        
        create: function(phono, config, callback) {
            config = Phono.util.extend({
                type: "auto"
            }, config);
            
            // What are we going to create? Look at the config...
            if (config.type === "java") {
                return new JavaAudio(phono, config, callback);
            } else if (config.type === "phonegap") {
                return new PhonegapAudio(phono, config, callback);
            } else if (config.type === "flash") {
                return new FlashAudio(phono, config, callback);
            } else if (config.type === "none") {
                window.setTimeout(callback,10);
                return null;
            } else if (config.type === "auto") {
                if (JavaAudio.exists()) return new JavaAudio(phono, config, callback);
                else if (PhonegapAudio.exists()) return new PhonegapAudio(phono, config, callback);
                else return new FlashAudio(phono, config, callback);
            }
        }
    });
      
})();
