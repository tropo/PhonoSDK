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
    _parseCandidate = function (params) {
        var candidate = {
            priority:params[0],
            component:params[1],
            protocol:params[2],
            id:params[3],
            ip:params[4],
            port:params[5]
        };
        var index = 6;
        while (index + 1 <= params.length) {
            if (params[index] == "typ") candidate["type"] = params[index+1];
            if (params[index] == "generation") candidate["generation"] = params[index+1];
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

    //a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:zvrxmXFpomTqz7CJYhN5G7JM3dVVxG/fZ0Il6DDo
    _parseCrypto = function(params) {
        var crypto = {
            'tag':params[0],
            'crypto-suite':params[1],
            'key-params':params[2],
        }
        return crypto;
    }

    //a=rtpmap:101 telephone-event/8000"
    _parseRtpmap = function(params) {
        var codec = {
            id: params[0],
            name: params[1].split("/")[0],
            clockrate: params[1].split("/")[1]
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
        var sdp = "a=candidate:" + c.priority + " " +
            c.component + " " + 
            c.protocol + " " +
            c.id + " " +
            c.ip + " " +
            c.port;
        if (c.type) sdp = sdp + " typ " + c.type;
        if (c.component == 1) sdp = sdp + " name rtp";
        if (c.component == 2) sdp = sdp + " name rtcp";
        sdp = sdp + " network_name en0";
        if (iceObj) {
            if (iceObj.ufrag) sdp = sdp + " username " + iceObj.ufrag;
            if (iceObj.pwd) sdp = sdp + " password " + iceObj.pwd;
        }
        if (c.generation) sdp = sdp + " generation " + c.generation;
        sdp = sdp + "\r\n";
        return sdp;
    }

    _buildCodec = function(codecObj) {
        var sdp = "a=rtpmap:" + codecObj.id + " " + codecObj.name + "/" + codecObj.clockrate + "\r\n";
        return sdp;
    }

    _buildCrypto = function(cryptoObj) {
        var sdp = "a=crypto:" + cryptoObj.tag + " " + cryptoObj['crypto-suite'] + " " + 
            cryptoObj["key-params"] + "\r\n";
        return sdp;
    }

    _buildMedia = function(sdpObj) {
        var sdp = "m=" + sdpObj.media.type + " " + sdpObj.media.port + " " + sdpObj.media.proto;
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

        if (sdpObj.rtcp) {
            sdp = sdp + "a=rtcp:" + sdpObj.rtcp.port + " " + sdpObj.rtcp.nettype + " " + 
                sdpObj.rtcp.addrtype + " " +
                sdpObj.rtcp.address + "\r\n";
        }

        var ci = 0;
        while (ci + 1 <= sdpObj.candidates.length) {
            sdp = sdp + _buildCandidate(sdpObj.candidates[ci], sdpObj.ice);
            ci = ci + 1;
        }

        if (sdpObj.ice) {
            var ice = sdpObj.ice;
            if (!ice.filterLines) {
                sdp = sdp + "a=ice-ufrag:" + ice.ufrag + "\r\n";
                sdp = sdp + "a=ice-pwd:" + ice.pwd + "\r\n";
            }
            if (ice.options) {
                sdp = sdp + "a=ice-options:" + ice.options + "\r\n";
            }
        }

        if (sdpObj.direction == "recvonly") {
            sdp = sdp + "a=recvonly\r\n";
        } else if (sdpObj.direction == "sendonly") {
            sdp = sdp + "a=sendonly\r\n";
        } else if (sdpObj.direction == "none") {
            sdp = sdp;
        } else {
           sdp = sdp + "a=sendrecv\r\n";
        }

        if (sdpObj.mid) {
            sdp = sdp + "a=mid:" + sdpObj.mid + "\r\n";
        }

        if (sdpObj['rtcp-mux']) {
            sdp = sdp + "a=rtcp-mux" + "\r\n";
        } 
 
        if (sdpObj.crypto) {
            sdp = sdp + _buildCrypto(sdpObj.crypto);
        }
 
        var cdi = 0;
        while (cdi + 1 <= sdpObj.codecs.length) {
            sdp = sdp + _buildCodec(sdpObj.codecs[cdi]);
            cdi = cdi + 1;
        }

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
        Phono = {};
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
                    c = c.c('payload-type', this).up();           
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
                c = c.c('candidate', {component:'2',
                                      ip: sdpObj.rtcp.address,
                                      port: sdpObj.rtcp.port});
                c.up().up();

                // Ice candidates
                var transp = {xmlns:"urn:xmpp:jingle:transports:ice-udp:1",
                             pwd: sdpObj.ice.pwd,
                             ufrag: sdpObj.ice.ufrag};
                if (sdpObj.ice.options) {
                    transp.options = sdpObj.ice.options;
                }
	        c = c.c('transport',transp);
                Phono.util.each(sdpObj.candidates, function() {
                    c = c.c('candidate', this).up();           
                });
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

                $(this).find('description').each(function () {
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
                });

                sdpObj.candidates = [];
                sdpObj.codecs = [];
                $(this).find('payload-type').each(function () {
                    var codec = Phono.util.getAttributes(this);
                    console.log("codec: "+JSON.stringify(codec,null," "));
                    sdpObj.codecs.push(codec);
                    mediaObj.pts.push(codec.id);
                });
                $(this).find('crypto').each(function () {
                    var crypto = Phono.util.getAttributes(this);
                    console.log("crypto: "+JSON.stringify(crypto,null," "));
                    sdpObj.crypto = crypto;
                });
                sdpObj.ice = {};
                $(this).find('transport').each(function () {
                    if ($(this).attr('xmlns') == "urn:xmpp:jingle:transports:raw-udp:1") {
                        $(this).find('candidate').each(function () {
                            var candidate = Phono.util.getAttributes(this);
                            console.log("candidate: "+JSON.stringify(candidate,null," "));
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
                            console.log("candidate: "+JSON.stringify(candidate,null," "));
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
                console.log(sdpLines[sdpLine]);
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
                console.log(sdpLines[sdpLine]);
                var line = _parseLine(sdpLines[sdpLine]);

                if (line.type == "o") {
                    contentsObj.session = _parseO(line.contents);
                }
                if (line.type == "m") {
                    // New m-line, create a new content
                    var media = _parseM(line.contents);
                    sdpObj = {};
                    sdpObj.media = media;
                    sdpObj.candidates = [];
                    sdpObj.codecs = [];
                    sdpObj.ice = {};
                    
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
                sdp = sdp + "o=-" + " " + id + " " + ver + " IN IP4 192.168.0.151" + "\r\n";
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

        var testSDP = "v=0\r\no=- 1825865780 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\nm=audio 51937 RTP/SAVPF 103 104 0 8 106 105 13 126\r\nc=IN IP4 92.20.224.185\r\na=rtcp:51937 IN IP4 92.20.224.185\r\na=candidate:257138899 1 udp 2113937151 192.168.0.151 54066 typ host generation 0\r\na=candidate:257138899 2 udp 2113937151 192.168.0.151 54066 typ host generation 0\r\na=candidate:2384176743 1 udp 1677729535 92.20.224.185 51937 typ srflx generation 0\r\na=candidate:2384176743 2 udp 1677729535 92.20.224.185 51937 typ srflx generation 0\r\na=candidate:1104174115 1 tcp 1509957375 192.168.0.151 49878 typ host generation 0\r\na=candidate:1104174115 2 tcp 1509957375 192.168.0.151 49878 typ host generation 0\r\na=ice-ufrag:2hm6kQUKfYZcwx0Q\r\na=ice-pwd:BFTSrs0UhQfGi2dS3XiPoJ3b\r\na=ice-options:google-ice\r\na=sendrecv\r\na=mid:audio\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:F8KwGDYU0lGx39pduFGhysbmcPLLNwIvGdYBSgNK\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:126 telephone-event/8000\r\na=ssrc:414494470 cname:mCuCMzMwfauXes6i\r\na=ssrc:414494470 mslabel:FlTQmWsZfu8BKQjMHYBkFSLNWthbpQE0e3HP\r\na=ssrc:414494470 label:FlTQmWsZfu8BKQjMHYBkFSLNWthbpQE0e3HP00\r\nm=video 51937 RTP/SAVPF 100 101 102\r\nc=IN IP4 92.20.224.185\r\na=rtcp:51937 IN IP4 92.20.224.185\r\na=candidate:257138899 1 udp 2113937151 192.168.0.151 54066 typ host generation 0\r\na=candidate:257138899 2 udp 2113937151 192.168.0.151 54066 typ host generation 0\r\na=candidate:2384176743 1 udp 1677729535 92.20.224.185 51937 typ srflx generation 0\r\na=candidate:2384176743 2 udp 1677729535 92.20.224.185 51937 typ srflx generation 0\r\na=candidate:1104174115 1 tcp 1509957375 192.168.0.151 49878 typ host generation 0\r\na=candidate:1104174115 2 tcp 1509957375 192.168.0.151 49878 typ host generation 0\r\na=ice-ufrag:2hm6kQUKfYZcwx0Q\r\na=ice-pwd:BFTSrs0UhQfGi2dS3XiPoJ3b\r\na=ice-options:google-ice\r\na=sendrecv\r\na=mid:video\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:F8KwGDYU0lGx39pduFGhysbmcPLLNwIvGdYBSgNK\r\na=rtpmap:100 VP8/90000\r\na=rtpmap:101 red/90000\r\na=rtpmap:102 ulpfec/90000\r\n";
        
        var sdpObj = Phono.sdp.parseSDP(testSDP);
        console.log("SDP Object:");
        console.log(JSON.stringify(sdpObj));

        var resultSDP = Phono.sdp.buildSDP(sdpObj);
        console.log("Resulting SDP:");
        console.log(resultSDP);
        
    }
    
}()); 