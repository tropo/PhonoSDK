/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var setTimeout,
        clearTimeout,
        setInterval,
        csnd,
        player,
        spoll,
        clearInterval,
        inbound = false,
        done,
        cnt,
        speakTesting = false;

function onIncommingCall() {
    inbound = true;
    testPassed("callback");
}
function onReady() {
    testPassed("load");
    call = phono.phone.dial("9996179470@app", null);
}
function onAnswer() {
    testPassed("call");
    checkEnergy();
}
function onMessage(message) {
    eval(message);
}

function onHangup() {
    if (inbound) {
        testPassed("hangup");
        done = true;
    }
}

addTest("load");
addTest("call");
addTest("speak");
addTest("message");
addTest("dtmf");
addTest("mic");
addTest("callback");
addTest("hangup");
function saysnd() {
    say(scnd);
}
function sayafter(snd, delay) {
    csnd = "http://s.phono.com/releases/1.1/samples/autotests/phono/" + snd;
    say(csnd);
}
function say(snd) {
    player = phono.newPlayer(snd);
    player.start();
}
function stopSay() {
    player.stop();
}
function dtmf(d) {
    call.digit(d);
}

function pollEnergy() {
    if ((audio != null) && (speakTesting)) {
        var ergy = audio.getEnergy();
        var spk = ergy[1];
        if (spk > 300.0) {
            speakTesting = false;
            testPassed("speak");
        }
        cnt++;
        if (cnt > 10) {
            speakTesting = false;
            testFailed("speak");
        }
    }
}

function checkEnergy() {
    cnt = 0;
    speakTesting = true;
}

function addTest(name) {
    //println("test " + name + " added ");
}
function testPassed(name) {
    println("test " + name + " passed ");
}
function testFailed(name) {
    println("test " + name + " failed ");
}
