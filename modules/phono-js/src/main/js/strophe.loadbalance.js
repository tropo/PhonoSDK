/* loadbalance plugin
**
** This plugin provides SRV record like functionality
** via an http->dns proxy
*/
console.log("about to add loadbalancer");

PhonoStrophe.addConnectionPlugin('loadbalance', {
    _srv: null,
    _conn: null,
    _offs: 0,
    _path: "http-bind",
    _origT: "",
    _proto: "http:",
    _oldstatus: -1,
    _rotate: function(){
        if (this._srv != null){
            var nexts = this._srv.servers[this._offs];
            this._offs++;
            if (this._offs < this._srv.servers.length){
                console.log("loadbalancer rotating to " + nexts.target);
                // if the target matches the original connectionURL then use the path from that
                // otherwise just append /http-bind
                if (this._origT == nexts.target){
                    this._conn.service = this._proto+"//"+nexts.target +":"+nexts.port+this._path;
                } else {
                    this._conn.service = this._proto+"//"+nexts.target +":"+nexts.port+"/http-bind";
                }
            } else {
                console.log("loadbalancer ran out of options" );
                this._conn.service= null;
            }
        } else {
            console.log("loadbalancer cant rotate _srv null" );
        }

    },
    init: function (conn) {
        console.log("adding loadbalancer");
        this._conn = conn;
        var a="",b= function (){} ,c="";
        var sr = new PhonoStrophe.Request(a,b,c,0);
        var srvreq = sr.xhr;
        var uri = document.createElement('a');
        uri.href = conn.service;
        this._origT = uri.hostname;
        this._path = uri.pathname;
        this._proto = uri.protocol;
        console.log("OrigT ="+this._origT+" path ="+this._path);

        var dnsUrl = uri.protocol+"//"+uri.host+"/PhonoDNS-servlet/LookupSRVServlet/_phono._tcp."+uri.hostname;
        srvreq.open("GET", dnsUrl, false);     // this blocks because there is really nothing else we can do untill we have a server to talk to.

        if (srvreq.overrideMimeType) {
            srvreq.overrideMimeType("application/json");
        }
        try {
            srvreq.send(null);
            if (srvreq.readyState == 4){
                console.log("got reply from loadbalancer")
                if (srvreq.status == 200){
                    if (srvreq.getResponseHeader("Content-Type")== "application/json"){
                        console.log("loadbalancer reply was "+srvreq.responseText);
                        this._srv = eval('(' +srvreq.responseText+ ')');
                        this._rotate();
                    } else {
                        console.log("loadbalancer response content type was " + srvreq.getResponseHeader("Content-Type"));
                    }
                } else {
                    console.log("loadbalancer status was "+srvreq.status);
                }
            }
        } catch (e) {
            console.log("ignoring a loadbalance error "+e);
        }

    },
    statusChanged: function(status){
        console.log("_status changed "+status+ " old status "+this._oldstatus);
        if ((status == Strophe.Status.DISCONNECTED) && (this._oldstatus ==Strophe.Status.CONNECTING )){
            this._rotate();
        }
        this._oldstatus = status;
    }
});
