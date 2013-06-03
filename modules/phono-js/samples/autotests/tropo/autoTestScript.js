var vo = "Elizabeth";
var v = {voice: vo};
var _conn; 
var _cm;
var _chat;
function getFunny(channel,show){
       var ret = "http://downloads.bbc.co.uk/podcasts/radio4/fricomedy/fricomedy_20130111-1900a.mp3";

      importPackage(java.io, java.net, javax.xml.xpath, org.xml.sax);
            var rssUrl = "http://downloads.bbc.co.uk/podcasts/" + channel + "/" + show + "/rss.xml";
            log("RSS URL ="+rssUrl);
            var url = new URL(rssUrl);
            var urlStream = url.openStream();

            var paths = "/rss/channel/item/link";
            var ips = new InputSource(urlStream);
            var xPath = XPathFactory.newInstance().newXPath();
           ret = xPath.evaluate(paths, ips); 
        
      return ret;
}

function jmessage(mes){
    importPackage(org.jivesoftware.smack, org.jivesoftware.smack.packet);
          if (_conn == null){
            _conn = new XMPPConnection("jit.si");
            _conn.connect();
            _conn.login("9996160714@jit.si", "9996160714");
            _cm = _conn.getChatManager();
            _chat = _cm.createChat(jid, null);
          }
          _chat.sendMessage(mes);
          log("-----> message is sent");
}


var jid = currentCall.getHeader("x-phono-sessionid");
log("-----> jid is "+jid);
wait(1000);
function jqm(s){
    if (jid) {
        jmessage(s);
    }
}

function testDTMF(){
	say("Testing D T M F",v);
	jqm("dtmf('1')");
	var next = ask("", {
		      voice:vo,
	          choices:"0,1,2,3,4,5,6,7",
	          terminator:"#",
	          timeout:10.0,
	          mode:"dtmf"});
	var val = "" +next.value;
	log("val= "+val);
	if (val == '1') {
		say("D T M F test Passed",v);
		jqm("testPassed('dtmf')");
	} else {
	        say("D T M F test Failed",v);
		jqm("testFailed('dtmf')");
	}
}
function testMic(){
	jqm("sayafter('7.mp3',500)");
	next = ask("", {
		      voice:vo,
		       choices:"[3 DIGITS]",
		       terminator:"#",
		       timeout:10.0,
		       mode:"speech"});
	val = "" +next.value;
	jqm("stopSay()");
	if (val == '777') {
		say("Mike test Passed",v);
		jqm("testPassed('mic')");
	} else {
	        say("Mike test Failed. I heard "+val,v);
		jqm("testFailed('mic')");
	}
}

say("Welcome to the Phono automatic test call.",v);
say("If you hear this, please tick off the speak test.",v);
say("Now sending a text message.",v);
jqm("testPassed('message')");
testDTMF(); 
testMic();
say("Calling you back.",v);
hangup();
wait(5000);
call("sip:"+jid,{onAnswer: function(){
	wait(5000);
	say("Called you back.",v);
	jqm("call.hangup()");
}});
wait(15000);
if (_conn != null){
    _conn.disconnect();
}



