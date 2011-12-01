package mx.collaboration.xmpp.protocol.tests
{
    
    import flexunit.framework.*;
    import mx.collaboration.xmpp.protocol.*;
    import mx.collaboration.xmpp.protocol.tests.*;
        
    public class TestSuiteXMPP
    {
    	public static function suite() : TestSuite
    	{
    		var suite:TestSuite = new TestSuite();
    		
    		//suite.addTest( TestXMPPStream.suite() );
    		//suite.addTest( TestNonSASLAuthenticator.suite() );     
    		//suite.addTest( TestPacketCollector.suite() );        
            //suite.addTest( TestJID.suite() );                		
    		
    		return suite;
    	}
    }
        
}
