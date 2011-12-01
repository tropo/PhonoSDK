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

package mx.collaboration.xmpp.protocol
{
    import mx.collaboration.xmpp.protocol.channels.*;
    
    public class ChannelRegistry
    {
        private var _channels:Array;
        
        public function ChannelRegistry()
        {
            _channels = new Array();
        }       
        
        /**
         * Add a channel to the registry. This allows the developer to add custom channel whereby the
         * XMPP Stream can fall back on. By default, when the XMPP Stream class attempts to connect, it
         * will try each channel type before throwing an error event.
         */
        public function registerChannel(channelId:String, channelClass:Class):void
        {
            if(_channels == null)
                _channels = new Array();
            
            _channels[channelId] = channelClass;
        }
        
        public function channelRegistered(channelId:String):Boolean
        {
            return (_channels[channelId] == null);
        }
        
        /**
         * Create a new instance of a registered channel type.
         */
        public function createChannelInstance(channelId:String):Channel
        {
            if(_channels[channelId] != null)
                return new ChannelRegistry[channelId];
            return null;
        }
        
        public function registeredChannelIds():Array
        {
            var ret:Array = new Array();
        
            for(var id:String in _channels)
            {
                ret.push(id);
            }
           
            return ret;
        }
        
        public function flush():void
        {
            _channels = new Array();
        }
    }
}