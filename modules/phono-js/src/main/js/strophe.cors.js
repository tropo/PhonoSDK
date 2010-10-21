/* CORS plugin
**
** flXHR.js should be loaded before this plugin if flXHR support is required.
*/

Strophe.addConnectionPlugin('cors', {
    init: function () {
        // replace Strophe.Request._newXHR with new CORS version
        if (window.XDomainRequest) {
            // We are in IE with CORS support
            Strophe.debug("CORS with IE");
            Strophe.Request.prototype._newXHR = function () {
                var stateChange = function(xhr, state) {
                    // Fudge the calling of onreadystatechange()
                    xhr.status = state;
                    xhr.readyState = 4;
                    try {
                        xhr.onreadystatechange();
                    }catch(err){}
                    xhr.readyState = 0;
                    try{
                        xhr.onreadystatechange();
                    }catch(err){}
                }
                var xhr = new XDomainRequest();
                xhr.readyState = 0;
                xhr.onreadystatechange = this.func.prependArg(this);
                xhr.onload = function () {
                    // Parse the responseText to XML
                    xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
                    xmlDoc.async = "false";
                    xmlDoc.loadXML(xhr.responseText);
                    xhr.responseXML = xmlDoc;
                    stateChange(xhr, 200);
                }
                xhr.onerror = function () {
                    stateChange(xhr, 500);
                }
                xhr.ontimeout = function () {
                    stateChange(xhr, 500);
                }
                return xhr;
            };
        } else if (new XMLHttpRequest().withCredentials !== undefined) {
            // We are in a sane browser with CROS support - no need to do anything
            Strophe.debug("CORS with Firefox/Safari/Chome");
        } else if (flensed && flensed.flXHR) {
            // We don't have CORS support, so include flXHR
            Strophe.debug("CORS not supported, using flXHR");
            Strophe.Request.prototype._newXHR = function () {
                var xhr = new flensed.flXHR({
                    autoUpdatePlayer: true,
                    instancePooling: true,
                    noCacheHeader: false});
                xhr.onreadystatechange = this.func.prependArg(this);
                return xhr;
            };
        } else {
            Strophe.error("No CORS and no flXHR. You may experience cross domain turbulence.");
        }
    }
});
