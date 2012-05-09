;(function() {
    //@Include=phono.as-audio.js
    //@Include=phono.java-audio.js
    //@Include=phono.phonegap-ios-audio.js
    //@Include=phono.phonegap-android-audio.js
    //@Include=phono.webrtc-audio.js

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
                
            } else if (config.type === "none") {
                window.setTimeout(callback,10);
                return null;
                
            } else if (config.type === "auto") {
                
                console.log("Detecting Audio Plugin");
                
                if (PhonegapIOSAudio.exists())  { 
                    console.log("Detected iOS"); 
                    return Phono.util.loggify("PhonegapIOSAudio", new PhonegapIOSAudio(phono, config, callback));
                    
                } else if (PhonegapAndroidAudio.exists()) { 
                    console.log("Detected Android"); 
                    return Phono.util.loggify("PhonegapAndroidAudio", new PhonegapAndroidAudio(phono, config, callback));
                    
                } else if (WebRTCAudio.exists()) { 
                    console.log("Detected WebRTC");
                    return Phono.util.loggify("WebRTCAudio", new WebRTCAudio(phono, config, callback));
                } else { 
                    console.log("Detected Flash"); 
                    return Phono.util.loggify("FlashAudio", new FlashAudio(phono, config, callback));
                    
                }
            }
        }
    });
      
})();
