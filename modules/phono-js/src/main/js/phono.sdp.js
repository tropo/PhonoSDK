;(function() {

    // Helper library to translate to and from SDP and an intermediate javascript object
    // representation of candidates, offers and answers

    _parseLine = function(line) {
        var s1 = line.split("=");
        return {
            type: s1[0],
            contents: s1[1]
        }
    }

    _parseA = function(attribute) {
        var s1 = attribute.split(":");
        return {
            key: s1[0],
            params: attribute.substring(attribute.indexOf(":")+1).split(" ")
        }
    }

    _parseM = function(media) {
        var s1 = media.split(" ");
        return {
            type:s1[0],
            port:s1[1],
            proto:s1[2],
            pts:media.substring((s1[0]+s1[1]+s1[2]).length+3).split(" ")
        }
    }

    _parseO = function(media) {
        var s1 = media.split(" ");
        return {
            username:s1[0],
            id:s1[1],
            ver:s1[2],
            nettype:s1[3],
            addrtype:s1[4],
            address:s1[5]
        }
    }

     _parseC = function(media) {
        var s1 = media.split(" ");
        return {
            nettype:s1[0],
            addrtype:s1[1],
            address:s1[2]
        }
    }

    //a=candidate:257138899 1 udp 2113937151 192.168.0.151 53973 typ host generation 0
    //a=candidate:1 1 udp 1.0 192.168.157.40 40877 typ host name rtp network_name en0 username root password mysecret generation 0
        /*
    candidate-attribute   = "candidate" ":" foundation SP component-id SP
    transport SP
    priority SP
    connection-address SP     ;from RFC 4566
    port         ;port from RFC 4566
    SP cand-type
    [SP rel-addr]
    [SP rel-port]
     *(SP extension-att-name SP
    extension-att-value)

    foundation            = 1*32ice-char
    component-id          = 1*5DIGIT
    transport             = "UDP" / transport-extension
    transport-extension   = token              ; from RFC 3261
    priority              = 1*10DIGIT
    cand-type             = "typ" SP candidate-types
    candidate-types       = "host" / "srflx" / "prflx" / "relay" / token
    rel-addr              = "raddr" SP connection-address
    rel-port              = "rport" SP port
    extension-att-name    = byte-string    ;from RFC 4566
    extension-att-value   = byte-string
    ice-char              = ALPHA / DIGIT / "+" / "/"
     */
    _parseCandidate = function (params) {
        var candidate = {
            foundation:params[0],
            component:params[1],
            protocol:params[2],
            priority:params[3],
            ip:params[4],
            port:params[5]
        };
        var index = 6;
        while (index + 1 <= params.length) {
            if (params[index] == "typ") candidate["type"] = params[index+1];
            if (params[index] == "generation") candidate["generation"] = params[index+1];
            if (params[index] == "username") candidate["username"] = params[index+1];
            if (params[index] == "password") candidate["password"] = params[index+1];

            index += 2;
        }

        return candidate;
    }

    //a=rtcp:1 IN IP4 0.0.0.0
    _parseRtcp = function (params) {
        var rtcp = {
            port:params[0]
        };
        if (params.length > 1) {
            rtcp['nettype'] = params[1];
            rtcp['addrtype'] = params[2];
            rtcp['address'] = params[3];
        }
        return rtcp;
    }

    /*
      a=rtcp-fb:100 ccm fir
      a=rtcp-fb:100 nack
      a=rtcp-fb:100 goog-remb
    */
    _parseRtcpFb = function (params) {
        // We should already have a codec with the right payload type
        var rtcpfb = {
            id:params[0],
            type:params[1]
        };
        if (params.length > 2) {
            rtcpfb['subtype'] = params[2];
        }
        return rtcpfb;
    }

    //a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:zvrxmXFpomTqz7CJYhN5G7JM3dVVxG/fZ0Il6DDo
    _parseCrypto = function(params) {
        var crypto = {
            'tag':params[0],
            'crypto-suite':params[1],
            'key-params':params[2]
        }
        return crypto;
    }
    _parseFingerprint = function(params) {
        var finger = {
            'hash':params[0],
            'print':params[1],
            'required':'1'
        }
        return finger;
    }

    //a=rtpmap:101 telephone-event/8000"
    _parseRtpmap = function(params) {
        var bits = params[1].split("/");
        var codec = {
            id: params[0],
            name: bits[0],
            clockrate: bits[1]
        }
        if (bits.length >2){
            codec.channels = bits[2];
        }
        return codec;
    }

    _parseSsrc = function(params, ssrc) {
        var ssrcObj = {};
        if (ssrc != undefined) ssrcObj = ssrc;
        ssrcObj.ssrc = params[0];
        var value = params[1];
        ssrcObj[value.split(":")[0]] = value.split(":")[1];
        return ssrcObj;
    }

    _parseGroup = function(params) {
        var group = {
            type: params[0]
        }
        group.contents = [];
        var index = 1;
        while (index + 1 <= params.length) {
            group.contents.push(params[index]);
            index = index + 1;
        }
        return group;
    }

    _parseMid = function(params) {
        var mid = params[0];
        return mid;
    }

    // Object -> SDP

    _buildCandidate = function(candidateObj, iceObj) {
        var c = candidateObj;
        var sdp = "a=candidate:" + c.foundation + " " +
            c.component + " " + 
            c.protocol.toUpperCase() + " " +
            c.priority + " " +
            c.ip + " " +
            c.port;
	if (c.type == "srflx") {
           sdp = sdp + " typ host"; //+ c.type;
	} else {
           if (c.type) sdp = sdp +" typ "+  c.type;
	}
        if (c.component == 1) sdp = sdp + " name rtp";
        if (c.component == 2) sdp = sdp + " name rtcp";
        sdp = sdp + " network_name en0";
        if (c.username && c.password ){
            sdp = sdp + " username "+c.username;
            sdp = sdp + " password "+c.password;
            if (!iceObj.ufrag)  iceObj.ufrag = c.username;
            if (!iceObj.pwd) iceObj.pwd=c.username;;
        } else if (iceObj) {
            if (iceObj.ufrag) sdp = sdp + " username " + iceObj.ufrag;
            if (iceObj.pwd) sdp = sdp + " password " + iceObj.pwd;
        } else {
            sdp = sdp+ " username root password mysecret";// I know a secret
        }
        if (c.generation) sdp = sdp + " generation " + c.generation;
        sdp = sdp + "\r\n";
        return sdp;
    }

    _buildCodec = function(codecObj) {
        var sdp = "a=rtpmap:" + codecObj.id + " " + codecObj.name + "/" + codecObj.clockrate 
        if (codecObj.channels){
            sdp+="/"+codecObj.channels;
        }
        sdp += "\r\n";
	if (codecObj.ptime) {
	    sdp+="a=ptime:"+codecObj.ptime;
	    sdp += "\r\n";
        } else if (codecObj.name.toLowerCase().indexOf("opus")==0) {
	    sdp+="a=ptime:20\r\n";
	}
	if (codecObj.name.toLowerCase().indexOf("telephone-event")==0) {
	    sdp+="a=fmtp:"+codecObj.id+" 0-15\r\n";
	}
        return sdp;
    }

    _buildRtcpFb = function(rtcpFbObj) {
        var sdp ="a=rtcp-fb:"+rtcpFbObj.id+" "+rtcpFbObj.type;
        if (rtcpFbObj.suybtype) sdp += " "+rtcpFbObj.subtype;
        sdp += "\r\n";
        return sdp;
    }

    _buildCrypto = function(cryptoObj) {
        var sdp = "a=crypto:" + cryptoObj.tag + " " + cryptoObj['crypto-suite'] + " " + 
            cryptoObj["key-params"] + "\r\n";
        return sdp;
    }

    _buildFingerprint = function(fingerObj) {
        var sdp = "a=fingerprint:" + fingerObj.hash + " " + fingerObj.print + "\r\n";
        return sdp;
    }

    _buildIce= function(ice) {
	var sdp="";
        if (ice.ufrag) {
            if (!ice.filterLines) {
                sdp = sdp + "a=ice-ufrag:" + ice.ufrag + "\r\n";
                sdp = sdp + "a=ice-pwd:" + ice.pwd + "\r\n";
            }
            if (ice.options) {
                sdp = sdp + "a=ice-options:" + ice.options + "\r\n";
	    }
	}
	return sdp;
    }

    _buildSessProps = function(sdpObj) {
        var sdp ="";
        if (sdpObj.fingerprint) {
            sdp = sdp + _buildFingerprint(sdpObj.fingerprint);
        }
        if (sdpObj.ice) {
	    sdp= sdp + _buildIce(sdpObj.ice);
        }
        return sdp;
    }

    _buildMedia =function(sdpObj) {
        var sdp ="";
        sdp += "m=" + sdpObj.media.type + " " + sdpObj.media.port + " " + sdpObj.media.proto;
        var mi = 0;
        while (mi + 1 <= sdpObj.media.pts.length) {
            sdp = sdp + " " + sdpObj.media.pts[mi];
            mi = mi + 1;
        }
        sdp = sdp + "\r\n";
        
        if (sdpObj.connection) {
            sdp = sdp + "c=" + sdpObj.connection.nettype + " " + sdpObj.connection.addrtype + " " +
                sdpObj.connection.address + "\r\n";
        }
        
        if (sdpObj.mid) {
            sdp = sdp + "a=mid:" + sdpObj.mid + "\r\n";
        }

        if (sdpObj.rtcp) {
            sdp = sdp + "a=rtcp:" + sdpObj.rtcp.port + " " + sdpObj.rtcp.nettype + " " + 
                sdpObj.rtcp.addrtype + " " +
                sdpObj.rtcp.address + "\r\n";
        }
        if (sdpObj.ice) {
	    sdp= sdp + _buildIce(sdpObj.ice);
        }

        var ci = 0;
        while (ci + 1 <= sdpObj.candidates.length) {
            sdp = sdp + _buildCandidate(sdpObj.candidates[ci], sdpObj.ice);
            ci = ci + 1;
        }


        if (sdpObj.direction) {
            if (sdpObj.direction == "recvonly") {
                sdp = sdp + "a=recvonly\r\n";
            } else if (sdpObj.direction == "sendonly") {
                sdp = sdp + "a=sendonly\r\n";
            } else if (sdpObj.direction == "none") {
                sdp = sdp;
            } else {
               sdp = sdp + "a=sendrecv\r\n";
            }
	} else {
            sdp = sdp + "a=sendrecv\r\n";
	}

        if (sdpObj['rtcp-mux']) {
            sdp = sdp + "a=rtcp-mux" + "\r\n";
        } 
 
        if (sdpObj.crypto) {
            sdp = sdp + _buildCrypto(sdpObj.crypto);
        }
        if (sdpObj.fingerprint) {
            sdp = sdp + _buildFingerprint(sdpObj.fingerprint);
        }
 
        var cdi = 0;
        while (cdi + 1 <= sdpObj.codecs.length) {
            sdp = sdp + _buildCodec(sdpObj.codecs[cdi]);
            cdi = cdi + 1;
        }

        Phono.util.each(sdpObj.rtcpFbs, function () { 
            sdp = sdp + _buildRtcpFb(this);
        });

        if (sdpObj.ssrc) {
            var ssrc = sdpObj.ssrc;
            if (ssrc.cname) sdp = sdp + "a=ssrc:" + ssrc.ssrc + " " + "cname:" + ssrc.cname + "\r\n";
            if (ssrc.mslabel) sdp = sdp + "a=ssrc:" + ssrc.ssrc + " " + "mslabel:" + ssrc.mslabel + "\r\n";
            if (ssrc.label) sdp = sdp + "a=ssrc:" + ssrc.ssrc + " " + "label:" + ssrc.label + "\r\n";
        }

        return sdp;
    }

// Entry points

    // Fake Phono for node.js
    if (typeof Phono == 'undefined') {
        var util = require("util");
        Phono = {
            log:{debug:function(mess){util.print(mess);}}
        };
        $ = {isFunction:function(){}};
	require("./phono.util.js");
    }

    Phono.sdp = {

        // jingle: A container to place the output jingle in
        // blob: A js object representing the input SDP
        buildJingle: function(jingle, blob) {
            var description = "urn:xmpp:jingle:apps:rtp:1";
            var c = jingle;
            if (blob.group) {
                var bundle = "";
                c.c('group', {type:blob.group.type,
                              contents:blob.group.contents.join(",")}).up();
            }

            Phono.util.each(blob.contents, function () {
                var sdpObj = this;
                
                var desc = {xmlns:description,
                            media:sdpObj.media.type};

                if (sdpObj.ssrc) {
                    desc.ssrc = sdpObj.ssrc.ssrc,
                    desc.cname = sdpObj.ssrc.cname,
                    desc.mslabel = sdpObj.ssrc.mslabel,
                    desc.label = sdpObj.ssrc.label
                }

                if (sdpObj.mid) {
                    desc.mid = sdpObj.mid
                }

                if (sdpObj['rtcp-mux']) {
                    desc['rtcp-mux'] = sdpObj['rtcp-mux'];
                }

                c = c.c('content', {creator:"initiator"})
                .c('description', desc);
                
                Phono.util.each(sdpObj.codecs, function() {
                    c = c.c('payload-type', this);
                    Phono.util.each(sdpObj.rtcpFbs, function() {
                        this.xmlns='urn:xmpp:jingle:apps:rtp:rtcp-fb:0';
                        c = c.c('rtcp-fb', this).up(); 
                    });
                    c = c.up();
                });
                
                if (sdpObj.crypto) {
                    c = c.c('encryption', {required: '1'}).c('crypto', sdpObj.crypto).up();    
                    c = c.up();
                }

                // Raw candidates
	        c = c.up().c('transport',{xmlns:"urn:xmpp:jingle:transports:raw-udp:1"});
                c = c.c('candidate', {component:'1',
                                      ip: sdpObj.connection.address,
                                      port: sdpObj.media.port}).up();
                if(sdpObj.rtcp) {
                    c = c.c('candidate', {component:'2',
                                      ip: sdpObj.rtcp.address,
                                      port: sdpObj.rtcp.port}).up();
                }
                c = c.up();

		// 3 places we might find ice creds - in order of priority:
		// candidate username
		// media level icefrag
		// session level icefrag
		var iceObj = {};
		if (sdpObj.candidates[0].username ){
			iceObj = {ufrag:sdpObj.candidates[0].username,pwd:sdpObj.candidates[0].password};
		} else if ((sdpObj.ice) && (sdpObj.ice.ufrag)){
			iceObj = sdpObj.ice;
		} else if ((blob.session.ice) && (blob.session.ice.ufrag)){
			iceObj = blob.session.ice;
		}
                // Ice candidates
                var transp = {xmlns:"urn:xmpp:jingle:transports:ice-udp:1",
                             pwd: iceObj.pwd,
                             ufrag: iceObj.ufrag};
                if (iceObj.options) {
                    transp.options = iceObj.options;
                }
	        c = c.c('transport',transp);
                Phono.util.each(sdpObj.candidates, function() {
                    c = c.c('candidate', this).up();           
                });
		// two places to find the fingerprint
		// media 
		// session
		var fp = null;
		if (sdpObj.fingerprint) {
		    fp= sdpObj.fingerprint;
		}else if(blob.session.fingerprint){
		    fp = blob.session.fingerprint;
		}
                if (fp){
                    c = c.c('fingerprint',{xmlns:"urn:xmpp:tmp:jingle:apps:dtls:0",
				hash:fp.hash,
                                required:fp.required});
                    c.t(fp.print);
                    c.up();
		}
                c = c.up().up();
            });
            return c;
        },
        
        // jingle: Some Jingle to parse
        // Returns a js object representing the SDP
        parseJingle: function(jingle) {
            var blobObj = {};

            jingle.find('group').each(function () {
                blobObj.group = {};
                blobObj.group.type =  $(this).attr('type');
                blobObj.group.contents = $(this).attr('contents').split(",");
            });

            blobObj.contents = [];
            jingle.find('content').each(function () {
                var sdpObj = {};
                var mediaObj = {};
                mediaObj.pts = [];
                
                blobObj.contents.push(sdpObj);
                sdpObj.candidates = [];
                sdpObj.codecs = [];
                sdpObj.rtcpFbs = [];

                $(this).find('description').each(function () {
                  if($(this).attr('xmlns') == "urn:xmpp:jingle:apps:rtp:1"){
		    var mediaType = $(this).attr('media');
                    mediaObj.type = mediaType;
                    mediaObj.proto = "RTP/SAVPF"; // HACK
                    mediaObj.port = 1000;
                    var ssrcObj = {};
                    if ($(this).attr('ssrc')) {
                        ssrcObj.ssrc = $(this).attr('ssrc');
                        if ($(this).attr('cname')) ssrcObj.cname = $(this).attr('cname');
                        if ($(this).attr('mslabel')) ssrcObj.mslabel = $(this).attr('mslabel');
                        if ($(this).attr('label')) ssrcObj.label = $(this).attr('label');
                        sdpObj.ssrc = ssrcObj;
                    }
                    if ($(this).attr('rtcp-mux')) {
                        sdpObj['rtcp-mux'] = $(this).attr('rtcp-mux');
                    }
                    if ($(this).attr('mid')) {
                        sdpObj['mid'] = $(this).attr('mid');
                    }
                    sdpObj.media = mediaObj;
		    $(this).find('payload-type').each(function () {
                        var codec = Phono.util.getAttributes(this);
                        Phono.log.debug("codec: "+JSON.stringify(codec,null," "));
                        sdpObj.codecs.push(codec);
                        mediaObj.pts.push(codec.id);
                        var pt = codec.id;
                        // Find all the rtcp-fb and create it
                        $(this).find('rtcp-fb').each(function () {
                            var rtcpFb = Phono.util.getAttributes(this);
                            Phono.log.debug("rtcp-fb: "+JSON.stringify(rtcpFb,null," "));
                            rtcpFb.id = pt;
                            sdpObj.rtcpFbs.push(rtcpFb);
                        });
                    });
		  } else {
	            Phono.log.debug("skip description with wrong xmlns: "+$(this).attr('xmlns'));
		  }
                });

                $(this).find('crypto').each(function () {
                    var crypto = Phono.util.getAttributes(this);
                    //Phono.log.debug("crypto: "+JSON.stringify(crypto,null," "));
                    sdpObj.crypto = crypto;
                });
                $(this).find('fingerprint').each(function () {
                    var fingerprint = Phono.util.getAttributes(this);
                    fingerprint.print = Strophe.getText(this);
                    Phono.log.debug("fingerprint: "+JSON.stringify(fingerprint,null," "));
                    sdpObj.fingerprint = fingerprint;
                });
                sdpObj.ice = {};
                $(this).find('transport').each(function () {
                    if ($(this).attr('xmlns') == "urn:xmpp:jingle:transports:raw-udp:1") {
                        $(this).find('candidate').each(function () {
                            var candidate = Phono.util.getAttributes(this);
                            //Phono.log.debug("candidate: "+JSON.stringify(candidate,null," "));
                            if (candidate.component == "1") {
                                sdpObj.media.port = candidate.port;
                                sdpObj.connection = {};
                                sdpObj.connection.address = candidate.ip;
                                sdpObj.connection.addrtype = "IP4";
                                sdpObj.connection.nettype = "IN";
                            }
                            if (candidate.component == "2") {
                                sdpObj.rtcp = {};
                                sdpObj.rtcp.port = candidate.port;
                                sdpObj.rtcp.address = candidate.ip;
                                sdpObj.rtcp.addrtype = "IP4";
                                sdpObj.rtcp.nettype = "IN";
                            }
                        });
                    } 
                    if ($(this).attr('xmlns') == "urn:xmpp:jingle:transports:ice-udp:1") {
                        sdpObj.ice.pwd = $(this).attr('pwd');
                        sdpObj.ice.ufrag = $(this).attr('ufrag');
                        if ($(this).attr('options')) {
                            sdpObj.ice.options = $(this).attr('options');
                        }
                        $(this).find('candidate').each(function () {
                            var candidate = Phono.util.getAttributes(this);
                            //Phono.log.debug("candidate: "+JSON.stringify(candidate,null," "));
                            sdpObj.candidates.push(candidate);
                        });
                    }
                });
            });
            return blobObj;
        },
        
        dumpSDP: function(sdpString) {
            var sdpLines = sdpString.split("\r\n");
            for (var sdpLine in sdpLines) {
                //Phono.log.debug(sdpLines[sdpLine]);
            }
        },

        // sdp: an SDP text string representing an offer or answer, missing candidates
        // Return an object representing the SDP in Jingle like constructs
        parseSDP: function(sdpString) {
            var contentsObj = {};
            contentsObj.contents = [];
            var sdpObj = null;

            // Iterate the lines
            var sdpLines = sdpString.split("\r\n");
            for (var sdpLine in sdpLines) {
                Phono.log.debug(sdpLines[sdpLine]);
                var line = _parseLine(sdpLines[sdpLine]);

                if (line.type == "o") {
                    contentsObj.session = _parseO(line.contents);
		    contentsObj.session.ice = {};
		    sdpObj = contentsObj.session;
                }
                if (line.type == "m") {
                    // New m-line, 
                    // create a new content
                    var media = _parseM(line.contents);
                    sdpObj = {};
                    sdpObj.candidates = [];
                    sdpObj.rtcpFbs = [];
                    sdpObj.codecs = [];
                    sdpObj.ice = {};
                    if (contentsObj.session.fingerprint != null){
                        sdpObj.fingerprint = contentsObj.session.fingerprint;
                    }
                    sdpObj.media = media;
                    contentsObj.contents.push(sdpObj);
                }
                if (line.type == "c") {
                    if (sdpObj != null) {
                        sdpObj.connection = _parseC(line.contents);
                    } else {
                        contentsObj.connection = _parseC(line.contents);
                    }
                }
                if (line.type == "a") {
                    var a = _parseA(line.contents);
                    switch (a.key) {
                    case "candidate":
                        var candidate = _parseCandidate(a.params);
                        sdpObj.candidates.push(candidate);
                        break;
                    case "group":
                        var group = _parseGroup(a.params);
                        contentsObj.group = group;
                        break;
                    case "mid":
                        var mid = _parseMid(a.params);
                        sdpObj.mid = mid;
                        break;
                    case "rtcp":
                        var rtcp = _parseRtcp(a.params);
                        sdpObj.rtcp = rtcp;
                        break;
                    case "rtcp-fb":
                        var rtcpFb = _parseRtcpFb(a.params, sdpObj.codecs);
                        sdpObj.rtcpFbs.push(rtcpFb);
                        break;
                    case "rtcp-mux":
                        sdpObj['rtcp-mux'] = true;
                        break;
                    case "rtpmap":
                        var codec = _parseRtpmap(a.params);
                        if (codec) sdpObj.codecs.push(codec);
                        break;
                    case "sendrecv":
                        sdpObj.direction = "sendrecv";
                        break;
                    case "sendonly":
                        sdpObj.direction = "sendonly";
                        break;
                    case "recvonly":
                        sdpObj.recvonly = "recvonly";
                        break;
                    case "ssrc":
                        sdpObj.ssrc = _parseSsrc(a.params, sdpObj.ssrc);
                        break;
                    case "fingerprint":
                        var print = _parseFingerprint(a.params);
                        sdpObj.fingerprint = print;
                        break;
                    case "crypto":
                        var crypto = _parseCrypto(a.params);
                        sdpObj.crypto = crypto;
                        break;
                    case "ice-ufrag":
                        sdpObj.ice.ufrag = a.params[0];
                        break;
                    case "ice-pwd":
                        sdpObj.ice.pwd = a.params[0];
                        break;
                    case "ice-options":
                        sdpObj.ice.options = a.params[0];
                        break;
                    }
                }

            }
            return contentsObj;
        },
        
        // sdp: an object representing the body
        // Return a text string in SDP format  
        buildSDP: function(contentsObj) {
            // Write some constant stuff
            var session = contentsObj.session;
            var sdp = 
                "v=0\r\n";
            if (contentsObj.session) {
                var session = contentsObj.session;
                sdp = sdp + "o=" + session.username + " " + session.id + " " + session.ver + " " + 
                session.nettype + " " + session.addrtype + " " + session.address + "\r\n"; 
            } else {
                var id = new Date().getTime();
                var ver = 2;
                sdp = sdp + "o=-" + " 3" + id + " " + ver + " IN IP4 192.67.4.14" + "\r\n"; // does the IP here matter ?!?
            }

            sdp = sdp + "s=-\r\n" + 
                "t=0 0\r\n";

            if (contentsObj.connection) {
                var connection = contentsObj.connection;
                sdp = sdp + "c=" + connection.nettype + " " + connection.addrtype + 
                    " " + connection.address + "\r\n";
            }
            if (contentsObj.group) {
                var group = contentsObj.group;
                sdp = sdp + "a=group:" + group.type;
                var ig = 0;
                while (ig + 1 <= group.contents.length) {
                    sdp = sdp + " " + group.contents[ig];
                    ig = ig + 1;
                }
                sdp = sdp + "\r\n";
            }

	    if (contentsObj.session){
	        sdp = sdp + _buildSessProps(contentsObj.session);
	    }
            var contents = contentsObj.contents;
            var ic = 0;
            while (ic + 1 <= contents.length) {
                var sdpObj = contents[ic];
                sdp = sdp + _buildMedia(sdpObj);
                ic = ic + 1;
            }
            return sdp;
        },

        // candidate: an SDP text string representing a cadidate
        // Return: an object representing the candidate in Jingle like constructs
        parseCandidate: function(candidateSDP) {
            var line = _parseLine(candidateSDP);
            return _parseCandidate(line.contents);
        },
        
        // candidate: an object representing the body
        // Return a text string in SDP format
        buildCandidate: function(candidateObj) {
            return _buildCandidate(candidateObj);
        }
    };

    if (typeof window === 'undefined') {
        // Unit tests under node.js

        var SDP ={
		chromeVideo:"v=0\r\no=- 466604799 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=msid-semantic: WMS 0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7k\r\nm=audio 51233 RTP/SAVPF 109 0 8 101\r\nc=IN IP4 192.67.4.10\r\na=rtcp:62387 IN IP4 192.67.4.10\r\na=candidate:2812693356 1 udp 2113937151 192.67.4.10 51233 typ host generation 0\r\na=candidate:2812693356 2 udp 2113937150 192.67.4.10 62387 typ host generation 0\r\na=ice-ufrag:A7xRf5m5sDv8Qnda\r\na=ice-pwd:sIxXUQ1R5euE6QY/ntMS9xpu\r\na=fingerprint:sha-256 A4:06:4B:AC:92:8B:FA:A0:CE:56:78:A4:B9:A4:2A:41:16:DD:D7:6C:E9:D2:71:81:20:99:F1:3A:4E:C7:71:8D\r\na=sendrecv\r\na=mid:audio\r\na=rtpmap:109 opus/48000/2\r\na=fmtp:109 minptime=10\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:101 telephone-event/8000\r\na=maxptime:60\r\na=ssrc:3307173785 cname:3iNNp5tCCbH8QdE8\r\na=ssrc:3307173785 msid:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7k 0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7ka0\r\na=ssrc:3307173785 mslabel:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7k\r\na=ssrc:3307173785 label:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7ka0\r\nm=video 51841 RTP/SAVPF 120\r\nc=IN IP4 192.67.4.10\r\na=rtcp:58612 IN IP4 192.67.4.10\r\na=candidate:2812693356 1 udp 2113937151 192.67.4.10 51841 typ host generation 0\r\na=candidate:2812693356 2 udp 2113937150 192.67.4.10 58612 typ host generation 0\r\na=ice-ufrag:vYDcPP0KgdP9VHjY\r\na=ice-pwd:JEkYOiuKuiny1sJNZlBHZyZ5\r\na=fingerprint:sha-256 A4:06:4B:AC:92:8B:FA:A0:CE:56:78:A4:B9:A4:2A:41:16:DD:D7:6C:E9:D2:71:81:20:99:F1:3A:4E:C7:71:8D\r\na=sendrecv\r\na=mid:video\r\na=rtpmap:120 VP8/90000\r\na=ssrc:1230164494 cname:3iNNp5tCCbH8QdE8\r\na=ssrc:1230164494 msid:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7k 0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7kv0\r\na=ssrc:1230164494 mslabel:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7k\r\na=ssrc:1230164494 label:0c2n3jRwGhjfwzKzLxMER8lpRIHMQaZiIw7kv0\r\n",

	chromeAudio:"v=0\r\no=- 2751679977 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio\r\na=msid-semantic: WMS YyMaveYaWtkfdWeZtSHs3AHFuH4TEYh4MZDh\r\nm=audio 63231 RTP/SAVPF 111 103 104 0 8 107 106 105 13 126\r\nc=IN IP4 192.67.4.11\r\na=rtcp:63231 IN IP4 192.67.4.11\r\na=candidate:521808905 1 udp 2113937151 192.67.4.11 63231 typ host generation 0\r\na=candidate:521808905 2 udp 2113937151 192.67.4.11 63231 typ host generation 0\r\na=ice-ufrag:1VZUXywcfSTmvPBK\r\na=ice-pwd:NHrjWPuvIlyBQD7UVw4zi/4F\r\na=ice-options:google-ice\r\na=fingerprint:sha-256 49:1E:A3:EB:78:C2:89:55:5D:0D:6E:F2:B7:41:50:DB:10:C4:B2:54:8F:D8:24:A5:E8:56:0A:56:F4:BA:3A:ED\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=sendrecv\r\na=mid:audio\r\na=rtcp-mux\r\na=crypto:0 AES_CM_128_HMAC_SHA1_32 inline:MpqMpDpEDjNDfpquFL8jIkO9oLp2Dp4NOYiSmrea\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:/yAvMdC0p1e/4c/Jc6ljepmHpIuHV9jO3FyrrTX4\r\na=rtpmap:111 opus/48000/2\r\na=fmtp:111 minptime=10\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:107 CN/48000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:126 telephone-event/8000\r\na=maxptime:60\r\na=ssrc:3334051080 cname:ECpt57S24HzaX1WY\r\na=ssrc:3334051080 msid:YyMaveYaWtkfdWeZtSHs3AHFuH4TEYh4MZDh YyMaveYaWtkfdWeZtSHs3AHFuH4TEYh4MZDha0\r\na=ssrc:3334051080 mslabel:YyMaveYaWtkfdWeZtSHs3AHFuH4TEYh4MZDh\r\na=ssrc:3334051080 label:YyMaveYaWtkfdWeZtSHs3AHFuH4TEYh4MZDha0\r\n",

	firefoxVideo:"v=0\r\no=Mozilla-SIPUA-24.0a1 12643 0 IN IP4 0.0.0.0\r\ns=SIP Call\r\nt=0 0\r\na=ice-ufrag:1a870bf3\r\na=ice-pwd:948d30c7fe15b95a7bd63743ae84ac2e\r\na=fingerprint:sha-256 1C:D2:EC:A0:51:89:35:BE:84:4B:BC:11:F3:D4:D6:C7:F7:39:52:C5:2D:55:88:1D:61:24:7A:54:20:8A:AE:C2\r\nm=audio 50859 RTP/SAVPF 109 0 8 101\r\nc=IN IP4 192.67.4.11\r\na=rtpmap:109 opus/48000/2\r\na=ptime:20\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:101 telephone-event/8000\r\na=fmtp:101 0-15\r\na=sendrecv\r\na=candidate:0 1 UDP 2113601791 192.67.4.11 50859 typ host\r\na=candidate:0 2 UDP 2113601790 192.67.4.11 53847 typ host\r\nm=video 62311 RTP/SAVPF 120\r\nc=IN IP4 192.67.4.11\r\na=rtpmap:120 VP8/90000\r\na=sendrecv\r\na=candidate:0 1 UDP 2113601791 192.67.4.11 62311 typ host\r\na=candidate:0 2 UDP 2113601790 192.67.4.11 54437 typ host\r\n",
        

	firefoxAudio:"v=0\r\no=Mozilla-SIPUA-24.0a1 20557 0 IN IP4 0.0.0.0\r\ns=SIP Call\r\nt=0 0\r\na=ice-ufrag:66600851\r\na=ice-pwd:aab7c3c8d881f6406eff1f1ff2e3bc5e\r\na=fingerprint:sha-256 C3:C4:98:95:D0:58:B1:D2:F9:72:A0:44:EB:C7:C4:49:95:8F:EE:00:05:10:82:A8:6E:F6:4A:DF:43:A3:2A:16\r\nm=audio 56026 RTP/SAVPF 109 0 8 101\r\nc=IN IP4 192.67.4.11\r\na=rtpmap:109 opus/48000/2\r\na=ptime:20\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:101 telephone-event/8000\r\na=fmtp:101 0-15\r\na=sendrecv\r\na=candidate:0 1 UDP 2113601791 192.67.4.11 56026 typ host\r\na=candidate:0 2 UDP 2113601790 192.67.4.11 56833 typ host\r\n",

            chromeRtcpFb:"v=0\r\no=- 6227611786937931562 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS 0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRv\r\nm=audio 50388 RTP/SAVPF 111 103 104 0 8 106 105 13 126\r\nc=IN IP4 78.150.48.131\r\na=rtcp:50388 IN IP4 78.150.48.131\r\na=candidate:3100903175 1 udp 2113937151 192.168.0.22 50388 typ host generation 0\r\na=candidate:3100903175 2 udp 2113937151 192.168.0.22 50388 typ host generation 0\r\na=candidate:1321500371 1 udp 1845501695 78.150.48.131 50388 typ srflx raddr 192.168.0.22 rport 50388 generation 0\r\na=candidate:1321500371 2 udp 1845501695 78.150.48.131 50388 typ srflx raddr 192.168.0.22 rport 50388 generation 0\r\na=candidate:4132961271 1 tcp 1509957375 192.168.0.22 0 typ host generation 0\r\na=candidate:4132961271 2 tcp 1509957375 192.168.0.22 0 typ host generation 0\r\na=ice-ufrag:+cegK9S9iT7r2aHD\r\na=ice-pwd:DGWCo4/RklpitTmYz4AsXpV4\r\na=ice-options:google-ice\r\na=fingerprint:sha-256 D2:C8:80:23:F3:86:45:B3:C0:43:71:4E:DB:F4:BE:86:9B:B5:F3:D9:60:38:0F:B3:25:3F:68:A1:4F:A2:29:EA\r\na=setup:actpass\r\na=mid:audio\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=sendrecv\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:VH63QXLG3CblsU2mP538gpep+yRmfhfCEvTK815O\r\na=rtpmap:111 opus/48000/2\r\na=fmtp:111 minptime=10\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:126 telephone-event/8000\r\na=maxptime:60\r\na=ssrc:3547047282 cname:SS1NySdXoBxXcPcN\r\na=ssrc:3547047282 msid:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRv 0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRva0\r\na=ssrc:3547047282 mslabel:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRv\r\na=ssrc:3547047282 label:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRva0\r\nm=video 50388 RTP/SAVPF 100 116 117\r\nc=IN IP4 78.150.48.131\r\na=rtcp:50388 IN IP4 78.150.48.131\r\na=candidate:3100903175 1 udp 2113937151 192.168.0.22 50388 typ host generation 0\r\na=candidate:3100903175 2 udp 2113937151 192.168.0.22 50388 typ host generation 0\r\na=candidate:1321500371 1 udp 1845501695 78.150.48.131 50388 typ srflx raddr 192.168.0.22 rport 50388 generation 0\r\na=candidate:1321500371 2 udp 1845501695 78.150.48.131 50388 typ srflx raddr 192.168.0.22 rport 50388 generation 0\r\na=candidate:4132961271 1 tcp 1509957375 192.168.0.22 0 typ host generation 0\r\na=candidate:4132961271 2 tcp 1509957375 192.168.0.22 0 typ host generation 0\r\na=ice-ufrag:+cegK9S9iT7r2aHD\r\na=ice-pwd:DGWCo4/RklpitTmYz4AsXpV4\r\na=ice-options:google-ice\r\na=fingerprint:sha-256 D2:C8:80:23:F3:86:45:B3:C0:43:71:4E:DB:F4:BE:86:9B:B5:F3:D9:60:38:0F:B3:25:3F:68:A1:4F:A2:29:EA\r\na=setup:actpass\r\na=mid:video\r\na=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\na=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na=sendrecv\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:VH63QXLG3CblsU2mP538gpep+yRmfhfCEvTK815O\r\na=rtpmap:100 VP8/90000\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack\r\na=rtcp-fb:100 goog-remb\r\na=rtpmap:116 red/90000\r\na=rtpmap:117 ulpfec/90000\r\na=ssrc:1617029726 cname:SS1NySdXoBxXcPcN\r\na=ssrc:1617029726 msid:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRv 0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRvv0\r\na=ssrc:1617029726 mslabel:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRv\r\na=ssrc:1617029726 label:0ZT6IhacZdslMZvZ93R5lwxHP9kMSzGbrdRvv0\r\n" 

};

        for (s in SDP){
		var bro = s;
                var bs = SDP[s];	
		Phono.log.debug("testing "+ s);
		var sdpObj = Phono.sdp.parseSDP(bs);
		Phono.log.debug(JSON.stringify(sdpObj,null," "));

		var resultSDP = Phono.sdp.buildSDP(sdpObj);
		Phono.log.debug(s+ " Resulting SDP:");
		Phono.log.debug(resultSDP);
        }

    }
    
}()); 
