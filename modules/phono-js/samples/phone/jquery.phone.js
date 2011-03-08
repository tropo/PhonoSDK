(function( $ ){
	
	$.fn.phone = function( options ) {
	
		var thisPhone = this, dispatcher = $({}), phonoOptions = {}, phono, call; 
		var settings = {
			apikey : "",
			dialpad: true,
			togglemic: true,
			slideopen: true,
			buttontext: "loading...",
			buttontextready: "Call me",
			numbertodial: "app:9991457008"
    	};
    	
    	if(options) {
	    	//lowercase all options passed in
	    	$.each(options, function(k,v){
	    		phonoOptions[k.toLowerCase()] = v;
	    	});
    	}
    	    	
    	return this.each(function(){
    		
    		if ( options ) { 
        		$.extend( settings, phonoOptions );
      		}
      		
      		var phone = buildPhone( settings );
      		
      		$(this).append(phone);
      		
      		// event handlers
      		phone.find(".phono-digit").bind({
      			mouseenter: function(){
      				$(this).addClass("ui-state-hover");
      			},
      			mouseleave: function(){
      				$(this).removeClass("ui-state-hover");
      			},
      			mousedown: function(){
      				$(this).addClass("ui-state-active");
      			},
      			mouseup: function(){
      				$(this).removeClass("ui-state-active");
      			},
      			click: function(e){
      				if( call )
      					call.digit( $(this).attr("title") );
      				e.preventDefault();
      			}
      		});
      		
      		phone.find(".phono-mic-toggle").bind({
      			click: function(){
      				(this.checked)? phono.phone.headset(true): phono.phone.headset(false);
      			}
      		});
      		
      		dispatcher.bind({
      			// phono is ready, bind events to the call button
      			phonoReady: function(){
      				var btn = phone.find("a.phono-phone-button");
      				btn.bind({
      					click: function(e){
      						(call) ? hangUpCall(settings, phone) : makeCall(settings, phone);
      						e.preventDefault();
      					}
      				});
      				
      				btn.text(settings.buttontextready);
      			}
      		});

    		// initialize phono 
    		phono = $.phono({
        		apiKey: settings.apikey,
        		onReady: function(){
        			dispatcher.trigger("phonoReady");
        		},
        		phone: {
					onDisconnect: function(event) {
				    	hangUpCall(settings, phone);
					}
				}
      		});
    		
    	});
    	
    	function buildPhone( settings ){
    		var phoneHldr = $( "<div/>")
    			.addClass("phono-hldr ui-widget ui-corner-all");
    			
    		var phoneContent = $("<div/>")
    			.addClass("ui-widget-content ui-corner-all")
    			.css("padding","2px")
    			.appendTo(phoneHldr);
    			
    		var statusLink = $("<a>")
    			.attr("href","#")
    			.addClass("phono-phone-button ui-widget-header ui-corner-all")
    			.css({
    				"text-align":"center",
    				"display":"block",
    				"padding":"8px 10px",
    				"text-decoration":"none"
    			})
    			.html(settings.buttontext)
    			.appendTo(phoneContent);
    			
    		if ( settings.togglemic ){
    			var micToggle = $("<div/>")
    				.addClass("phono-mic-toggle-hldr")
    				.css({
    					"margin":"5px 0",
    					"font-size":"75%",
    					"text-align":"center"
    				})
    				.html("<input class='phono-mic-toggle' type='checkbox'/> Wearing a headset?")
    				.appendTo(phoneContent);
    				
    			if(settings.slideopen)
    				micToggle.css("display","none");
    		}
    		
    		if( settings.dialpad ){
    			var digitTblHldr = $("<div/>")
    				.addClass("phono-digit-hldr")
    				.appendTo(phoneContent);
    				
    			if(settings.slideopen)
    				digitTblHldr.css("display","none");
    			
    			var digitTbl = $( "<table/>" )
    				.addClass( "phono-digits-tbl" )
    				.css({
    					"width": "100%"
    				})
    				.appendTo( digitTblHldr );
    				
    			var digits = [
    				{key:"1", value:"&nbsp;"},
    				{key:"2", value:"ABC"},
    				{key:"3", value:"DEF"},
    				{key:"4", value:"GHI"},
    				{key:"5", value:"JKL"},
    				{key:"6", value:"MNO"},
    				{key:"7", value:"PQRS"},
    				{key:"8", value:"TUV"},
    				{key:"9", value:"WXYZ"},
    				{key:"*", value:"&nbsp;"},
    				{key:"0", value:"+"},
    				{key:"#", value:"&nbsp;"}
    			];
    			
    			var tblRows = $("<tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr><tr><td></td><td></td><td></td></tr>")
    				.appendTo(digitTbl);
    			
    			$.each(tblRows.find("td"), function( i, el ) { 
    				var digitObj = digits[i];
    				$(el).css({
    						"padding":"1px",
    						"vertical-align":"top",
    						"width":"33%"
    					});
    				
    				var digit = $( "<a/> ")
    				    .addClass( "phono-digit ui-state-default ui-corner-all" )
    				    .attr("title", digitObj.key)
    				    .attr("href","#")
    				    .css({
    				    	"padding":"3px",
    				    	"display":"block",
    				    	"text-align":"center",
    				    	"text-decoration":"none",
    				    	"font-size":"90%"
    				    })
    				    .html( digitObj.key )
    				    .appendTo( el );
    				    
    				var digitText = $("<span>")
    					.css({
    						"display":"block",
    						"font-size":"60%"
    					})
    					.html(digitObj.value)
    					.appendTo(digit);
    			});	
    		}
    		return phoneHldr;
    	}
    	
    	function makeCall(settings, phone){
    		phoneBtn = phone.find(".phono-phone-button");
    		phoneBtn.text("Calling...");
    		
    		if( settings.togglemic && settings.slideopen )
    			phone.find(".phono-mic-toggle-hldr").slideDown();
    		if( settings.dialpad && settings.slideopen )
    			phone.find(".phono-digit-hldr").slideDown();
    			
    		call = phono.phone.dial(settings.numbertodial, {
            	tones: true,
            	onAnswer: function(event) {	
				    phoneBtn.text("Hangup");
            	},
            	onHangup: function() {
				    hangUpCall(settings, phone);
            	},
            	onDisconnect: function() {
				    hangUpCall(settings, phone);						
            	}
        	});
    	}
    	
    	function hangUpCall(settings, phone){
    		if(call){
    			call.hangup();
    			call = null;
    		}
    		phone.find(".phono-phone-button").text(settings.buttontextready);
    		
    		if( settings.togglemic && settings.slideopen )
    			phone.find(".phono-mic-toggle-hldr").slideUp();
    		if( settings.dialpad && settings.slideopen )
    			phone.find(".phono-digit-hldr").slideUp();
    	}
	
	};
	
})( jQuery );