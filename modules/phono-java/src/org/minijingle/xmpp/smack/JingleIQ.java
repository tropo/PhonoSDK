package org.minijingle.xmpp.smack;

import org.jivesoftware.smack.packet.IQ;
import org.minijingle.jingle.Jingle;
import org.minijingle.xmpp.smack.parser.XStreamIQ;

public class JingleIQ extends XStreamIQ<Jingle> {

    public JingleIQ(final Jingle element) {
        super(element);
        this.setType(IQ.Type.SET);
    }

    public Jingle getElement() {
        return super.getElement();
    }

    public static JingleIQ fromXml(final String xml) {
        Jingle j = (Jingle) JingleIQ.getStream().fromXML(xml);
        return new JingleIQ(j);
    }

    public static IQ createResult(final IQ request) {
        IQ iq = new IQ() {
            public String getChildElementXML() {
                return "";
            }
        };
        iq.setType(IQ.Type.RESULT);
        iq.setTo(request.getFrom());
        iq.setFrom(request.getTo());
        return iq;
    }

}
