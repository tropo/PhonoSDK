var piDig = "31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";
function testDTMF(){
	var next = ask("", {
	          choices:"[1 DIGITS]",
	          terminator:"#",
	          timeout:10.0,
	          mode:"dtmf"});
	return next.value;
}

function checkPi(){
   var i=0;
   for (i=0;i<piDig.length;i++){
     var dig = testDTMF();
     if (dig == piDig[i]){
         say(""+dig);
     } else {
	     say("The "+i+" digit of pi is "+piDig[i]+" I heard "+dig);
	     say("goodbye");
	     break;
     }
   }
   if (i >= piDig.length){
	   say("congratulations we got to the "+i+" digit");
   }
   hangup();
}

log("--------> Jid is "+phonoid);
call("sip:"+phonoid,{onAnswer: function(){
    log("--------> Answered");
    checkPi();
}});

wait(150000);



