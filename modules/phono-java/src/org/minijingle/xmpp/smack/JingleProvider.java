package org.minijingle.xmpp.smack;


import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;


import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;


import org.minijingle.xmpp.smack.parser.NParser;
import org.minijingle.jingle.Jingle;
import org.minijingle.jingle.transport.RawUdpTransport;
import org.xmlpull.v1.XmlPullParser;




public class JingleProvider implements IQProvider, PacketInterceptor
{
    private static final String JINGLE_NAMESPACE = "urn:xmpp:jingle:1";
    private static final String RAW_UDP_NAMESPACE = "urn:xmpp:jingle:transports:raw-udp:1";
    private static final String RTP_AUDIO = "urn:xmpp:jingle:apps:rtp:audio";

   // private ServiceDiscoveryManager discoManager;

    /**
     * Parse a iq/jingle element.
     */
    public IQ parseIQ(final XmlPullParser parser) throws Exception {

        NParser p = new NParser(parser);
        return JingleIQ.fromXml(p.getAsString());

    }

	public void disableJingle()
	{

	}

    public void enableJingle(final XMPPConnection connection)
    {
        ProviderManager.getInstance().addIQProvider("jingle", Jingle.XMLNS, new JingleProvider());
        JingleIQ.getStream().alias("jingle", Jingle.class);
        JingleIQ.getStream().alias("transport", RawUdpTransport.class);
/* Maybe one day we will need this....
        discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
        discoManager.addFeature(JINGLE_NAMESPACE);
        discoManager.addFeature(RAW_UDP_NAMESPACE);
        discoManager.addFeature(RTP_AUDIO);

		discoManager.setNodeInformationProvider("jitsi-jingle#0.1", new NodeInformationProvider()
		{
			public List<DiscoverItems.Item> getNodeItems()
			{
				return null;
			}

			public List<String> getNodeFeatures()
			{
				List<String> answer = new ArrayList<String>();

				answer.add(JINGLE_NAMESPACE);
				answer.add(RAW_UDP_NAMESPACE);
				answer.add(RTP_AUDIO);
				return answer;
			}

			public List<DiscoverInfo.Identity> getNodeIdentities()
			{
				 return null;
			}
		});
*/
        connection.addPacketInterceptor(this, new PacketTypeFilter(Presence.class));
    }

    public void interceptPacket(Packet packet)
    {
        if ((packet instanceof Presence))
        {
			packet.addExtension(new PacketExtension() {
				public String getElementName() {
					return "c";
				}

				public String getNamespace() {
					return "http://jabber.org/protocol/caps";
				}

				public String toXML() {
					return "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"jitsi-jingle\" ver=\"0.1\" ext=\"\"></c>";
				}
			});
        }
    }
}
