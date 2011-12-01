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

/**
 *  @private
 *  The HTTPBindingResponse class unpacks the stanza payload from within the &lt;body/&gt; element
 *  of a response and exposes the &lt;body/&gt; element and any wrapped stanzas directly.
 */
internal class HTTPBindingResponse
{
	public var body:XML;
	public var isBindingError:Boolean;
	public var isRecoverableError:Boolean;
	public var stanzas:Array;		
	public var hasStanzas:Boolean;						
	
	/**
		*  <p>Constructs a new HTTPBindingResponse instance based on a response payload.
		*  If the &lt;body/&gt; wrapper element is not empty, then it MUST contain one of the following:
		*  <ul>
		*    <li>One or more complete XMPP stanzas (&lt;message/&gt;, &lt;presence/&gt;, and/or &lt;iq/&gt;).</li>
		*    <li>A complete &lt;stream:features/&gt; element qualified by the 'http://etherx.jabber.org/streams' namespace.</li>
		*    <li>A complete element used for SASL negotiation and qualified by the 'urn:ietf:params:xml:ns:xmpp-sasl' namespace.</li>
		*    <li>XML elements associated with a binding error condition.
		*  </ul></p>
		*
		*  <p>Binding errors are handled internally by the HTTPBindingChannel. All other stanzas are passed up as data by the channel.</p>
		*/
	public function HTTPBindingResponse(payload:String)
	{
		// Extract the opening <body> element and save it.
		var leftIndex:int = payload.indexOf(">");			
		if (payload.charAt(leftIndex - 1) == '/')
		{
			body = XML(payload.substring(0, leftIndex + 1));
		}
		else
		{
			body = XML(payload.substring(0, leftIndex + 1) + "</body>");
		}
		isBindingError = (body.@type == "terminate") ? true : false;
		isRecoverableError = (body.@type == "error") ? true : false;
					
		// Extract nested data - start by finding the closing </body> element.
		var rightIndex:int = payload.lastIndexOf("</body>");			
		// If a closing </body> element exists, parse nested stanzas.
		if ((rightIndex != -1) && ((rightIndex - leftIndex) > 1))
		{
			var stanzaData:String = payload.substring(leftIndex + 1, rightIndex);
			if (!isBindingError)
			{
				processCommonStanzas(stanzaData);				
			}
			else
			{
				processBindingErrorStanzas(stanzaData);
			}
			hasStanzas = true;
		}
		else
		{	// No nested stanzas exist.
			hasStanzas = false;
		}			
	}
			
	private function processCommonStanzas(value:String):void
	{
		stanzas = [];
		var parser:StanzaParser = new StanzaParser(value);			
		while (parser.moveNext())
		{	
			switch (parser.elementName)
			{					
				case "message":
				case "presence":
				case "iq":
					// Push any general XMPP stanzas and continue.
					stanzas.push(value.substring(parser.elementStartIndex, parser.elementEndIndex + 1));		
				break;
				case "stream:features":
					// A complete <stream:features/> element is always a single stanza. Push it and return.
					stanzas.push(value.substring(parser.elementStartIndex, parser.elementEndIndex + 1));
					return;
				break;
				default:					
					// Check for a complete element used for SASL negotiation and qualified 
					// by the 'urn:ietf:params:xml:ns:xmpp-sasl' namespace. If found, push it and return.
					var element:String = value.substring(parser.elementStartIndex, parser.elementEndIndex + 1);
					if (element.indexOf("urn:ietf:params:xml:ns:xmpp-sasl") != -1)
					{
						stanzas.push(element);
						return;
					}
				break;
			}
		}
	}
	
	private function processBindingErrorStanzas(value:String):void
	{
		// TBD
	}	
}

}