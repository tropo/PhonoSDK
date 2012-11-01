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

/*
    a=candidate:257138899 1 udp 2113937151 192.168.0.151 53973 typ host generation 0
    {
        priority:'257138899'
        component:'1',
        protocol:'udp'
        id:'el0747fg11',
        ip:'10.0.1.1'
        port:'8998'
        type:'host'
        generation:'0',
    }
*/    
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

/*
    //a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:zvrxmXFpomTqz7CJYhN5G7JM3dVVxG/fZ0Il6DDo
    crypto: {
        crypto-suite: 'AES_CM_128_HMAC_SHA1_80', 
        key-params: 'inline:WVNfX19zZW1jdGwgKCkgewkyMjA7fQp9CnVubGVz|2^20|1:32',
        session-params: 'KDR=1 UNENCRYPTED_SRTCP',
        tag: '1'
    }
*/
    _parseCrypto = function(params) {
        var crypto = {
            'tag':params[0],
            'crypto-suite':params[1],
            'key-params':params[2],
        }
        return crypto;
    }

/*
    //a=rtpmap:101 telephone-event/8000"
    codec: {
        id: 101, // payload-type
        name: "telephone-event",
        rate: 8000, 
        p: 20 // ptime XXX
    },
*/
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

    _buildCandidate = function(candidateObj) {
        var c = candidateObj;
        var sdp = "a=candidate:" + c.priority + " " +
            c.component + " " + 
            c.protocol + " " +
            c.id + " " +
            c.ip + " " +
            c.port;
        if (c.type) sdp = sdp + " type " + c.type;
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
        while (mi + 1 < sdpObj.media.pts.length) {
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
        while (ci + 1 < sdpObj.candidates.length) {
            sdp = sdp + _buildCandidate(sdpObj.candidates[ci]);
            ci = ci + 1;
        }

        if (sdpObj.ice) {
            var ice = sdpObj.ice;
            sdp = sdp + "a=ice-ufrag:" + ice.ufrag + "\r\n";
            sdp = sdp + "a=ice-pwd:" + ice.pwd + "\r\n";
            if (ice.options) {
                sdp = sdp + "a=ice-options:" + ice.options + "\r\n";
            }
        }

        if (sdpObj.direction == "recvonly") {
            sdp = sdp + "a=recvonly\r\n";
        } else if (sdpObj.direction == "sendonly") {
            sdp = sdp + "a=sendonly\r\n";
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
        while (cdi + 1 < sdpObj.codecs.length) {
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

    if (typeof Phono == 'undefined') {
        Phono = {};
    }

    Phono.sdp = {
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
        },
        
        /*
        //a=ice-ufrag:2d8439a71c719f2f5c51c35f1daaf2a4
        //a=ice-pwd:663aefd879858ef8686d32826121d16e
        ice: {
          pwd:'asd88fgpdd777uzjYhagZg'
          ufrag:'8hhy'
        },
          
        "v=0
        o=- 611498911 611498911 IN IP4 127.0.0.1
        ns=-
        t=0 0
        m=audio 20298 RTP/SAVPF 0 101
        c=IN IP4 192.67.4.20
        
        a=sendrecv
        */
        
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
                        sdpObj.codecs.push(codec);
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
                "v=0\r\n" +
                "s=-\r\n" + 
                "t=0 0\r\n" +
                "o=" + session.username + " " + session.id + " " + session.ver + " " + 
                session.nettype + " " + session.addrtype + " " + session.address + "\r\n"; 
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