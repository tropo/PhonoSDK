;(function() {
    //@Include=phono.as-audio.js
    //@Include=phono.java-audio.js
    //@Include=phono.phonegap-ios-audio.js
    //@Include=phono.phonegap-android-audio.js

    Phono.registerPlugin("audio", {
        
        create: function(phono, config, callback) {
            config = Phono.util.extend({
                type: "auto"
            }, config);
            
            // What are we going to create? Look at the config...
            if (config.type === "java") {
                return new JavaAudio(phono, config, callback);
            } else if (config.type === "phonegap-ios") {
                return new PhonegapIOSAudio(phono, config, callback);
            } else if (config.type === "phonegap-android") {
                return new PhonegapAndroidAudio(phono, config, callback);
            } else if (config.type === "flash") {
                return new FlashAudio(phono, config, callback);
            } else if (config.type === "none") {
                window.setTimeout(callback,10);
                return null;
            } else if (config.type === "auto") {
                console.log("Detecting Audio Plugin");
                if (PhonegapIOSAudio.exists())  { 
                    console.log("Detected iOS"); return new PhonegapIOSAudio(phono, config, callback);
                } else if (PhonegapAndroidAudio.exists()) { 
                    console.log("Detected Android"); return new PhonegapAndroidAudio(phono, config, callback);
                } else { 
                    console.log("Detected Flash"); return new FlashAudio(phono, config, callback);
                }
            }
        }
    });
      
})();
