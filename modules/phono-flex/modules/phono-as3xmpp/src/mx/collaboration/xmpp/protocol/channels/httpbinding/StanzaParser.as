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
 *  The StanzaParser class is a very simple parser for stepping through a response payload in
 *  search of stanzas of interest.
 */
internal class StanzaParser
{
    private var _payload:String;
    private var _end:uint;
    private var _index:uint;
    private var _currentElementName:String;
    private var _currentElementStartIndex:int;
    private var _currentElementEndIndex:int; 
    
    public function StanzaParser(payload:String)
    {
	    _payload = payload;
	    _end = _payload.length;
	    _currentElementName = "";
	    _index = 0;
	    _currentElementStartIndex = -1;
	    _currentElementEndIndex = -1;
    }
    
    public function get elementName():String
    {
    	return _currentElementName;
    }
    
    public function get elementStartIndex():int
    {
    	return _currentElementStartIndex;
    }
    
    public function get elementEndIndex():int
    {
    	return _currentElementEndIndex;
    }
    
    public function moveNext():Boolean
    {    			
    	var leftIndex:int;    			
    	while (true)
    	{   			
			leftIndex = _payload.indexOf("<", _index);
			if (leftIndex == -1 || leftIndex >= _end)
			{
				return false; // No start element was found.
			}
			else
			{
				if (_payload.charAt(leftIndex + 1) == '/')
				{
					_index++;
				}
				else
				{
					break;
				}
			}
    	}    		
		var spaceIndex:int = _payload.indexOf(" ", leftIndex);
		var gtIndex:int = _payload.indexOf(">", leftIndex);
		var rightIndex:int;
		if (spaceIndex > 0 && gtIndex > 0)
		{
			rightIndex = Math.min(spaceIndex, gtIndex);
		}
		else
		{
			rightIndex = Math.max(spaceIndex, gtIndex);
		}
		if (_payload.charAt(rightIndex - 1) == '/')
		{
			--rightIndex;
		}
		_currentElementName = _payload.substring(leftIndex + 1, rightIndex);		
		_currentElementStartIndex = leftIndex;
		// Find the end of this element.
		var endIndex:int = _payload.indexOf("</" + _currentElementName, _currentElementStartIndex);
		if (endIndex == -1)
		{
			endIndex = _currentElementStartIndex;
		}
		endIndex = _payload.indexOf(">", endIndex);
		if (endIndex != -1)
		{
			_currentElementEndIndex = endIndex;
		}
		else
		{
			_currentElementEndIndex = -1
		}
		// Advance the internal index into the full payload.
		_index = (_currentElementEndIndex != -1) ? _currentElementEndIndex : gtIndex;
		return true;
    }
}

}