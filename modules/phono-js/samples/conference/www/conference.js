/* Phono mobile conference demo */
var phono, call, conferenceId;
var defaultRoomName = "Room name without spaces";
var defaultDisplayName = "Display name";
var dm = {}
$(function() {
	if (!window.localStorage.displayName) { 
    	window.localStorage.displayName = ""; 
    }
    	
	connectPhono();
	
	/* Home Page */
	$("#home").live("pageshow",function(event, ui){
		if(!phono.connected()) $.mobile.showPageLoadingMsg();
		
		if($("#conference-id-input").val() == "")
			$("#conference-id-input").val(defaultRoomName);
			
		$("#conference-id-input").focus(function(){
			if($(this).val() == defaultRoomName) $(this).val("");	
		});
		$("#conference-id-input").blur(function(){
			if($(this).val() == "") $(this).val(defaultRoomName);	
		});
		$(".participant").live("tap", function(event, ui){
			dm.jid = $(this).attr("jid");
			dm.name = $(this).find("a").text();
		})
		$("#conference-action-button").tap(function(){
			if($("#conference-id-input").val() == "" || $("#conference-id-input").val() == defaultRoomName){
				alert("Enter a room name");
				return;
			}
			
			if(call == null){
				if(window.localStorage.displayName == ""){
					$.mobile.changePage( "#name-dialog", {
						transition: "pop"
					});
				}else{
					dialConference();
				}
			}else{
				call.hangup();
			}	
		});
	});
	
	/* Settings Page */
	$("#settings").live("pageshow",function(event, ui){
		(window.localStorage.displayName.length) ? 
		$("#display-name-input").val(window.localStorage.displayName) : $("#display-name-input").val(defaultDisplayName)
	});
	
	$("#display-name-button").tap(function(event, ui){
		$.mobile.showPageLoadingMsg();
		if($("#display-name-input").val() != "" && $("#display-name-input").val() != defaultDisplayName){
			window.localStorage.displayName = $("#display-name-input").val();
			$.mobile.hidePageLoadingMsg();
			$.mobile.changePage( "#home" );	
		}
	});
	
	$("#display-name-input").focus(function(){
		if($(this).val() == defaultDisplayName) $(this).val("");	
	});
	
	$("#display-name-input").blur(function(){
		if($(this).val() == "") $(this).val(defaultDisplayName);	
	});
	
	/* Display name dialog page */
	$("#dialog-display-name-button").tap(function(event, ui){
		if($("#dialog-display-name-input").val() != ""){
			window.localStorage.displayName = $("#dialog-display-name-input").val();
			$(".ui-dialog").dialog("close");
		}
	});
	
	/* Send message dialog */
	$("#send-dm-button").live("tap",function(event, ui){
		sendDm(dm.jid, $("#send-dm-input").val());
	});
	
	$("#send-message-dialog").live("pageshow",function(event, ui){
		$("#send-dm-to").text(dm.name);
	});
	
	/* Recieve message dialog */
	$("#send-dm-button").live("pageshide",function(event, ui){
		$("#dm-from").text("");
		$("#dm-body").text("");
	});
	
	$("#dm-reply-button").live("tap", function(event, ui){
		$(".ui-dialog").dialog("close");
		$.mobile.changePage( "#send-message-dialog", {
			transition: "pop"
		});
	});

});

function connectPhono(){
	phono = $.phono({
	    apiKey: "C17D167F-09C6-4E4C-A3DD-2025D48BA243",
	    gateway: "staging.phono.com",
	    onReady: function(event) {
	        $("#home-content").show();
	        $.mobile.hidePageLoadingMsg();
	    },
	    onUnready: function(event){
	    	disconnectPhono();
	    },
	    audio: {
            swf: "../../../plugins/audio/phono.audio.swf",
	    },
	    messaging: {
	    	onMessage: function(event) {
	    		var message = event.message;
	    	    try {
	    	    	msgBody = $.parseJSON(message.body);
	    	    	processParticipantEvent(msgBody);
	    	    } catch (e) {
	    	        processDirectMessage(message);
	    	    }
	    	}
	    }
	});
}

function disconnectPhono(){
	phono = null;
	$.mobile.showPageLoadingMsg();
	connectPhono();
}

function dialConference(){
	$.mobile.showPageLoadingMsg();
	console.log(phono.sessionId);
	call = phono.phone.dial("app:9996137020", {
        gain: 25,
        volume: 50,
	    tones: false,
	    headers: [{
        	name: "x-conferenceID",
        	value: $("#conference-id-input").val()
        },
        {
        	name: "x-jid",
        	value: phono.sessionId
        },
        {
        	name: "x-name",
        	value: window.localStorage.displayName
        }],
        onAnswer: function(event) {
            $.mobile.hidePageLoadingMsg();
            $("#conference-action-button").attr("data-theme", "a").removeClass("ui-btn-up-b").addClass("ui-btn-up-a");
			$("#conference-action-button").find(".ui-btn-text").text("End Call");
        },
        onHangup: function() {
	        hangUpConference();
        }
    });
}

function hangUpConference(){
	call = null;
	$("#conference-id-input").val(defaultRoomName);
	$("#participant-holder").hide();
	$("#participant-list").empty();
	$("#conference-action-button").attr("data-theme", "b").removeClass("ui-btn-up-a").addClass("ui-btn-up-b");
	$("#conference-action-button").find(".ui-btn-text").text("Join Conference");
}

function processParticipantEvent(message){
	console.log("participant event");
	console.log(message);
	
	//Initial participant list
	if(message.length && message.length > 0){
		$.each(message, function(index, value){
			addParticipant(value.id, value.name, value.jid);
		});
	}else{
		if(message.status == "added"){
			addParticipant(message.id, message.name, message.jid);
		}else if(message.status == "deleted"){
			deleteParticipant(message.id);
		}
	}
	
}

function addParticipant(id, name, jid){
	console.log("adding participant: "+name)
	var participant = $("<li>")
		.attr("id",id)
		.attr("class","participant")
		.attr("data-transition","pop")
		.attr("jid",jid)
		.attr("data-theme","c")
		.attr("data-icon","chat");
		
	var participantLink = $("<a>")
		.attr("href","#send-message-dialog")
		.attr("data-rel","dialog")
		.text(name)
		.appendTo(participant);
		
	if(! $("#participant-holder").is(":visible")) $("#participant-holder").show();
		
	participant.appendTo($("#participant-list"));
	$("#participant-list").listview('refresh');	
}

function deleteParticipant(id){
	console.log("deleting participant: "+id)
	$("#"+id).remove();
	$("#participant-list").listview('refresh');
	if($("#participant-list li").length == 0) $("#participant-holder").hide();
}

function processDirectMessage(message){
	var jid = message.from.split("/")[0];
	var name = $("li[jid='"+jid+"']").find("a").text();
	dm.name = name;
	dm.jid = jid;
	displayDm(name, message.body)	
}

function sendDm(jid, msg){
	phono.messaging.send(jid, msg);
	$("#send-dm-input").val("");
	$(".ui-dialog").dialog("close");
}

function displayDm(jid, msg){
	$.mobile.changePage( "#direct-message-dialog", {
		transition: "pop"
	});
	$("#dm-from").text(jid);
	$("#dm-body").text(msg);
}

