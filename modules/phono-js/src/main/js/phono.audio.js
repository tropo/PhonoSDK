;(function() {
    //@Include=phono.as-audio.js
    //@Include=phono.java-audio.js
    //@Include=phono.phonegap-ios-audio.js
    //@Include=phono.phonegap-android-audio.js
    //@Include=phono.webrtc-audio.js
    //@Include=phono.jsep-audio.js

    Phono.registerPlugin("audio", {
        
        create: function(phono, config, callback) {
            config = Phono.util.extend({
                type: "auto"
            }, config);
            
            // What are we going to create? Look at the config...
            if (config.type === "java") {
                return Phono.util.loggify("JavaAudio", new JavaAudio(phono, config, callback));                
                
            } else if (config.type === "phonegap-ios") {
                return Phono.util.loggify("PhonegapIOSAudio", new PhonegapIOSAudio(phono, config, callback));
                
            } else if (config.type === "phonegap-android") {
                return Phono.util.loggify("PhonegapAndroidAudio", new PhonegapAndroidAudio(phono, config, callback));
                
            } else if (config.type === "flash") {
                return Phono.util.loggify("FlashAudio", new FlashAudio(phono, config, callback));

            } else if (config.type === "webrtc") {
                return Phono.util.loggify("WebRTCAudio", new WebRTCAudio(phono, config, callback));

            } else if (config.type === "jsep") {
                return Phono.util.loggify("JSEPAudio", new JSEPAudio(phono, config, callback));

            } else if (config.type === "none") {
                window.setTimeout(callback,10);
                return null;
                
            } else if (config.type === "auto") {
                
                Phono.log.info("Detecting Audio Plugin");

                if (JSEPAudio.exists()) {
                    Phono.log.info("Detected JSEP browser"); 
                    return Phono.util.loggify("JSEPAudio", new JSEPAudio(phono, config, callback));
                } else if (PhonegapIOSAudio.exists())  { 
                    Phono.log.info("Detected iOS"); 
                    return Phono.util.loggify("PhonegapIOSAudio", new PhonegapIOSAudio(phono, config, callback));
                } else if (PhonegapAndroidAudio.exists()) { 
                    Phono.log.info("Detected Android"); 
                    return Phono.util.loggify("PhonegapAndroidAudio", new PhonegapAndroidAudio(phono, config, callback));
                } else { 
                    Phono.log.info("Using Flash default"); 
                    return Phono.util.loggify("FlashAudio", new FlashAudio(phono, config, callback));
                    
                }
            }
        }
    });
      
})();
