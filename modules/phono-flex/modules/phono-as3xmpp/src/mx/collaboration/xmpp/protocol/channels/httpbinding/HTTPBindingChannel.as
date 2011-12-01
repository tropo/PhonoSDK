/*
	Adobe Systems Incorporated(r) Source Code License Agreement
	Copyright(c) 2005 Adobe Systems Incorporated. All rights reserved.
	
	Please read this Source Code License Agreement carefully before using
	the source code.
	
	Adobe Systems Incorporated grants to you a perpetual, worldwide, non-exclusive, 
	no-charge, royalty-free, irrevocable copyright license, to reproduce,
	prepare derivative works of, publicly display, publicly perform, and
	distribute this source code and such derivative works in source or 
	object code form without any attribution requirements.  
	
	The name "Adobe Systems Incorporated" must not be used to endorse or promote products
	derived from the source code without prior written permission.
	
	You agree to indemnify, hold harmless and defend Adobe Systems Incorporated from and
	against any loss, damage, claims or lawsuits, including attorney's 
	fees that arise or result from your use or distribution of the source 
	code.
	
	THIS SOURCE CODE IS PROVIDED "AS IS" AND "WITH ALL FAULTS", WITHOUT 
	ANY TECHNICAL SUPPORT OR ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING,
	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
	FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  ALSO, THERE IS NO WARRANTY OF 
	NON-INFRINGEMENT, TITLE OR QUIET ENJOYMENT.  IN NO EVENT SHALL MACROMEDIA
	OR ITS SUPPLIERS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
	OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
	WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOURCE CODE, EVEN IF
	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package mx.collaboration.xmpp.protocol.channels.httpbinding
{    

import flash.events.*;
import flash.net.*;
import flash.utils.*;
import mx.collaboration.xmpp.protocol.*;
import mx.logging.*;

/**
 *  The HTTPBindingChannel is a sibling to the SocketChannel used to connect a
 *  client to an XMPP server. This channel is used to bind XMPP traffic to the HTTP
 *  transport protocol to avoid firewall port restrictions that may adversely impact
 *  the SocketChannel. For full details regarding HTTP Binding for XMPP traffic, refer to
 *  <a href="http://www.jabber.org/jeps/jep-0124.html">JEP-0124</a>.
 *
 *  <p>Currently no XMPP server implements JEP-0124, so we're working against a servlet that
 *  provides a partial implementation. More info on the servlet here: 
 *  <a href="http://zeank.in-berlin.de/jhb/">JHBServlet</a>.
 *  <p>Outstanding work items:
 *  <ul>
 *    <li>Sort out approach for raising non-<stream> errors.</li>
 *    <li>Implement fatal binding-error handling.</li>
 *    <li>* Implement recoverable binding-error handling (we need a server that supports this).</li>
 *  </ul>
 *  </p>
 */
public class HTTPBindingChannel extends Channel
{	
	// States that this channel can exist in:
	private static const DISCONNECTED:String = "DISCONNECTED";
	private static const REQUESTING_A_SESSION:String = "REQUESTING_A_SESSION";
	private static const CONNECTED:String = "CONNECTED";
	private static const DISCONNECTING:String = "DISCONNECTING";

	// Defaults for server-driven parameters that are not required to be returned.
	private static const POLLING:uint = 5;
	private static const INACTIVITY:uint = 30;	
	
	// Private variables for negotiating the channel connection.
	private var _uri:String;
	private var _lang:String;
	private var _wait:uint;
	private var _hold:uint;
	private var _route:String;
	private var _secure:Boolean;
	private var _content:String;
	private var _channelState:String;
	
	// Private variables defined by the HTTP connection manager upon connection.
	private var _sid:String;
	private var _authid:String;
	private var _requests:uint;
	private var _polling:uint;
	private var _inactivity:uint;
	private var _accept:String;
	private var _charsets:String;
		
	// Queue of stanzas to send.
	private var _pendingStanzas:Array;
	
	// Queue of stanza-queues sent in previous requests for recovery.
	private var _recoveryRequests:Array;
	
	// Timer for managing the polling cycle for this channel.
	private var _pollTimer:Timer;
		
	// Flag indicating whether a request operation is currently in progress.
	private var _requestInProgress:Boolean;
	
	// Timestamp for the last reponse from the server in milliseconds since midnight January 1, 1970.
	private var _responseTimestamp:uint;
	
	// Flag indicating whether a disconnect has been requested.
	private var _disconnectRequested:Boolean;
	
	// Manages an incrementing request ID sequence.
	private var _requestID:RequestID;
	
	// Performs the actual HTTP polling.
	private var _loader:URLLoader;		
	
	// Logger.
	private var _log:ILogger;

	/**
	 *  Creates an HTTPBindingChannel object.
	 * 
	 *  @param host The XMPP server hostname or IP address.
	 *
	 *  @param port The XMPP server port to connect to.
	 *
	 *  @param uri The URI for the HTTP connection manager that will manage the HTTP-bound XMPP traffic.
	 *
	 *  @param lang The preferred language for human-readable XMPP sent or received.
	 *
	 *  @param wait The longest time in seconds that the connection manager is allowed to wait before responding to
	 *  any request during the session.
	 *
	 *  @param hold The maximum number of requests that the connection manager is allowed to keep waiting at any one time
	 *  during the session.
	 *
	 *  @param route For 'proxy' connection managers, this specifies an XMPP IRI to indicate which protocol, host and port
	 *  to communicate with.
	 *
	 *  @param secure Indicates whether communications between the client and sever must be secure or not.
	 *
	 *  @param content Specifies the value of the HTTP Content-Type header that MUST appear in all the connection manager's
	 *  responses during the session.
	 */
	public function HTTPBindingChannel(host:String, 
									   port:uint=5222, 
									   uri:String=null, 
									   lang:String="en",
									   wait:uint=60, 
									   hold:uint=1, 
									   route:String=null, 
									   secure:Boolean=false,
									   content:String="text/xml; charset=utf-8")
    {
		super(host, host, port);
        _uri = uri;
        _lang = lang;
		_wait = wait; 
        _hold = hold;
        _route = route;
        _secure = secure;
        _content = content;
        
        _requestInProgress = false;
        
        _disconnectRequested = false; 
        
        _requestID = new RequestID();
        
        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, handleComplete);
    	_loader.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
		_loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);
		
		_responseTimestamp = getTimer();
		
		_channelState = DISCONNECTED;
		
		_log = Log.getLogger("mx.collaboration.xmpp.protocol.channels.httpbinding.HTTPBindingChannel");
    }
    
    /**
     *  Connects the channel to the HTTP connection manager by sending a session initiation request.
     */
    public override function connect():void
    {
		if (_channelState == DISCONNECTED)
		{
			super.connect(); 
			var request:URLRequest = new URLRequest();
			request.method = "post";
			request.url = _uri;
			var body:String = new String();
			body += "<body rid='";
			body += _requestID.getNext();
			body += "' to='";
			body += host;
			body += "' xml:lang='";
			body += _lang;
			body += "' wait='";
			body += _wait;
			body += "' hold='";
			body += _hold;
			if (_route)
			{
				body += "' route='";
				body += _route;
			}
			body += "' secure='";
			body += _secure;
			body += "' content='";
			body += _content;
			body += "' xmlns='http://jabber.org/protocol/httpbind'/>";
			request.data = body;
			_channelState = REQUESTING_A_SESSION;
			_loader.load(request);
			_requestInProgress = true;
//			_log.debug("HTTPBindingChannel.connect(): {0}", request.postData);
		}
		else
		{
			_log.warn("connect() was called when the channel state was not DISCONNECTED. State is: {0}", _channelState); 
		}
    }

	/**
	 *  Requests that the channel disconnect from the HTTP connection manager as soon as possible.
	 */
    public override function disconnect():void
    {   
		if (_channelState == CONNECTED || _channelState == REQUESTING_A_SESSION)
		{
			if (_pollTimer != null)
			{
				if (!_requestInProgress)
				{
					sendDisconnect();
				}
				else
				{
					_disconnectRequested = true;
				}
			}
			else
			{
				shutdown();
			}
		}
		else
		{
			_log.warn("disconnect() was called when the channel state was not CONNECTED or REQUESTING_A_SESSION. State is: {0}", _channelState); 
		}
    }
    
    /**
     *  Sends data to the XMPP server.
     *
     *  @param data The stanza data to send.
     */
    public override function sendData(data:XML):void
    {
    	super.sendData(data);
		_pendingStanzas.push(data);
		if (!_requestInProgress)
		{
			sendBody();
		}
    }    
	
    /**
     *  @private
     *  Handles an IO error raised by the URLLoader that polls the HTTP connection manager.
     */
    private function handleIOError(e:IOErrorEvent):void
    {
	    shutdown();
		// TBD: Handle IO Errors.
		_log.error(">> Got an IO Error: " + e.text);
    }
    
    /**
     *  @private
     *  Handles a security error raised by the URLLoader that polls the HTTP connection manager.
     */
    private function handleSecurityError(e:SecurityErrorEvent):void
    {
    	shutdown();
		// TBD: Handle security errors.
		_log.error(">> Got a security error: " + e.text);
    }	
    
    /**
     *  @private
     *	Handles a "complete" event raised by the URLLoader that polls the HTTP connection manager.
     */
    private function handleComplete(e:Event):void
    {
    	_log.debug("HTTPBindingChannel.handleComplete(): {0}", e.target.data.toString());
		_requestInProgress = false;
		_responseTimestamp = getTimer();
		var loader:URLLoader = URLLoader(e.target);		
		var response:HTTPBindingResponse = new HTTPBindingResponse(loader.data.toString());
		if (response.isBindingError)
		{
			// TBD: Handle binding errors.
			_log.error(">> Response contains a binding error.");
		}
		else if (response.isRecoverableError)
		{
			// TBD: Handle a recoverable error by repeating the previous two HTTP requests.
			_log.error(">> Response contains a recoverable error.");
		}
		else // It's a good response!
		{
			switch (_channelState)
			{
				case REQUESTING_A_SESSION:
					handleSessionCreation(response);
				break;
				case CONNECTED:
					handleStanzas(response);
				break;
				case DISCONNECTING:
					handleSessionTermination(response);
				break;
			}
		}
    }    
    
    /**
     *  @private
     *  Handles a response from the HTTP connection manager when the channel is in the process of 
     *  requesting a new session. The channel may need to stay in this state and continue to poll the 
     *  HTTP connection manager until it receives a response containing an "authid" parameter. The
     *  response that contains the "authid" parameter may also contain a <stream:features/> stanza.
     *  At the point that this response arrives, the streamInfo and streamFeatures properties 
     *  for this channel and the connection process can be completed.
     */
    private function handleSessionCreation(response:HTTPBindingResponse):void
    {
		var body:XML = response.body;				
		// Set server-defined connection parameters.
		if (body.@sid != null)
		{
			_sid = body.@sid.toString();
		}				
		if (body.@wait != null)
		{
			_wait = new uint(body.@wait.toString());
		}
		if (body.@requests != null)
		{
			_requests = new uint(body.@requests.toString());
		}
		if (body.@polling != null)
		{
			_polling = new uint(body.@polling.toString());
		}
		if (body.@inactivity != null)
		{
			_inactivity = new uint(body.@inactivity.toString());
		}
		if (body.@accept != null)
		{
			_accept = body.@accept.toString();
		}
		if (body.@charsets != null)
		{
			_charsets = body.@charsets.toString();
		}
		if (body.@authid != null)
		{
			_authid = body.@authid.toString();
		}
		// Start up polling if it's not already running.
		if (_pollTimer == null)
		{
			// Reset pending stanza and recovery queues.		
			_pendingStanzas = [];
			_recoveryRequests = [];
			// Start up polling.
			if (_polling != 0)
			{
				_polling = POLLING;
			}
			_pollTimer = new Timer(_polling * 1000);
			_pollTimer.addEventListener("timer", doPoll);
			_pollTimer.start();
		}
		// If we have an authid, we can finish up the session creation/connection process. 
		// Otherwise, we leave the channel in its current state to wait for a response that
		// contains an "authid" attribute.
		if (_authid != null)
		{
			// Set internal channel state to connected.
			_channelState = CONNECTED;
			// Check for a nested <stream:features/> stanza - its presence determines whether
			// we're dealing with an XMPP version 1.0 HTTP connection manager or not, and we
			// need this information in order to build the streamInfo and streamFeatures properties 
			// for this channel.			
			var hasStreamFeatures:Boolean = false;
			if (response.hasStanzas &&
				response.stanzas[0].indexOf("<stream:features") >= 0)
			{
				hasStreamFeatures = true;
				// To build the streamFeatures property, we have to define the "stream" namespace - jumping through hoops.				
				var streamNamespace:Namespace = new Namespace("http://etherx.jabber.org/streams");
				var wrappedFeatures:String = new String();
				wrappedFeatures += "<stream:stream xmlns:stream='http://etherx.jabber.org/streams'>";
				wrappedFeatures += response.stanzas[0];
				wrappedFeatures += "</stream:stream>";
				var namespacedFeatures:XML = XML(wrappedFeatures);
                streamFeatures = new XML(namespacedFeatures.streamNamespace::features[0].toString());                
			}
			// Setup streamInfo - we're assuming XMPP protocol versions here based on whether we receive <stream:features/> or not.
			// There's nothing that comes back from the HTTP connection manager explicitely informing us of the XMPP stream version.
			var info:String = new String();
			info += "<stream:stream xmlns:stream='http://etherx.jabber.org/streams' id='";
			// The XMPP server session ID comes back to us as the authid.
			info += _authid;
			info += "' version='";
			if (hasStreamFeatures)
			{
				info += "1.0' />";
			}
			else
			{
				info += "0.9' />";
			}
			streamInfo = XML(info.toString());
			// Fire stream initiation and feature hooks.
			onStreamInitiation();
			onStreamFeatures();
		}
    }        
    
    /**
     *  @private
     *  Handles any stanzas that are contained in a response from the HTTP connection manager.
     */
    private function handleStanzas(response:HTTPBindingResponse):void
    {
		if (response.hasStanzas)
		{
			var stanzas:Array = response.stanzas;
			var count:uint = stanzas.length;
			for (var i:uint = 0; i < count; ++i)
			{
				processStanza(stanzas[i]);
			}
		}
    }
    
    /**
     *  @private
     *  Handles any responses from the HTTP connection manager when a disconnect is pending.
     */
    private function handleSessionTermination(response:HTTPBindingResponse):void
    {
		// Handle any pending responses that arrive and contain stanzas while we're waiting
		// for the actual disconnect acknowledgement.
		if (response.hasStanzas)
		{
			handleStanzas(response);
		}
		else // This is the actual disconnect acknowledgement - no stanzas, just an empty response.
		{
			shutdown();
		}
    }   
    
    /**
     *  @private
     *  Shuts down the channel.
     */
    private function shutdown():void
    {
    	_disconnectRequested = false;
		_sid = null;
		_wait = 0;
		_requests = 0;
		_polling = 0;
		_inactivity = 0;
		_accept = null;
		_charsets = null;
		_authid = null;
		if (_pollTimer != null)
		{
			_pollTimer.stop();
		}
		_pollTimer = null;
		_pendingStanzas = null;
		_recoveryRequests = null;
		_channelState = DISCONNECTED;
		disconnectSuccess();
    }
    
    /**
     *  @private
     *  Polls the HTTP connection manager by sending a request containing any pending outbound
     *  stanzas. If a disconnect has been requested, it ignores pending stanzas and sends a disconnect
     *  request to the HTTP connection manager instead.
     */
    private function doPoll(e:Event):void
    {
		if (shouldPoll())
		{			
			if (!_disconnectRequested)
			{
				sendBody();
			}
			else
			{
				sendDisconnect();
			}
		}
    }
    
    /** 
     *  @private
     *  Determines whether a polling operation should run. If there is no current request in progress,
     *  pending stanzas should be sent immediately. If there are no pending stanzas, we can't send an empty
     *  request faster than the HTTP connection manager's defined polling interval.
     */
    private function shouldPoll():Boolean
    {
    	if (!_requestInProgress)
    	{
    		if ((_pendingStanzas.length > 0) || ((getTimer() - _responseTimestamp) >= _polling))
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     *  @private
     *  Sends a request to the HTTP connection manager that contains any pending outbound stanzas.
     *  A request that doesn't contain any outbound stanzas keeps the session active and gives the HTTP
     *  connection manager a chance to return any pending response stanzas to the client.
     */
    private function sendBody():void
    {
		var request:URLRequest = new URLRequest();
        request.url = _uri;
        request.method = "post";
        var body:String = new String();
        body += "<body rid='";
        body += _requestID.getNext();
        body += "' sid='";
        body += _sid;
        body += "' xmlns='http://jabber.org/protocol/httpbind'>";
        if (_pendingStanzas != null)
        {
			var count:uint = _pendingStanzas.length;
			if (count > 0)
			{
				// Inject the pending stanzas.			
				for (var i:uint = 0; i < count; ++i)
				{
					body += _pendingStanzas[i];
				}
			}
			// Reset pending stanzas.
			_pendingStanzas = [];
		}
        body += "</body>";
        request.data = body;
        _loader.load(request);
        _requestInProgress = true;        				
		_log.debug("HTTPBindingChannel.sendBody(): {0}", request.data);
    }
    
    /**
     *  @private
     *  Sends a disconnect request to the HTTP connection manager.
     */     
    private function sendDisconnect():void
    {
		super.disconnect();
		var request:URLRequest = new URLRequest();
		request.url = _uri;
		request.method = "post"
		var body:String = new String();
		body += "<body rid='";
		body += _requestID.getNext();
		body += "' sid='";
		body += _sid;
		body += "' type='terminate' xmlns='http://jabber.org/protocol/httpbind'><presence type='unavailable' xmlns='jabber:client'/></body>";
		request.data = body.toString();
		_channelState = DISCONNECTING;
		_loader.load(request);
		_requestInProgress = true;
		_log.debug("HTTPBindingChannel.sendDisconnect(): {0}", request.data);
	}     
    
    /** 
     *  @private
     *  Stores stanzas to a history queue, so that in the case of recoverable errors
     *  we can recreate our previous requests and recover by wrapping and resending these 
     *  previous stanzas.
     */
    private function storeForRecovery(stanzas:Array=null):void
    {
		if (stanzas != null)
		{
			_recoveryRequests.push(stanzas.slice());
		}
		else
		{
			_recoveryRequests.push([]);
		}
		// We only need the last request plus the current request to recover from a recoverable error,
		// so we can hold the recovery queue to a two-entry upper bound.
		for (var i:uint = _recoveryRequests.length; i >= 2; --i)
		{
			_recoveryRequests.shift();
		}
    }    
}

}