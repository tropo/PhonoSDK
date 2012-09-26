package org.minijingle.xmpp.smack.parser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class NParser {

    final private XmlPullParser parser;

    public NParser(XmlPullParser parser) {
        this.parser = parser;
    }

    public String getAsString() {

        StringBuilder str = new StringBuilder();
        int ll = -1, lc = -1;

        final String name = parser.getName();

        try {
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG || parser.getEventType() == XmlPullParser.END_TAG) {
                    int cl = parser.getLineNumber();
                    int cc = parser.getColumnNumber();
                    if (ll != cl || lc != cc) {
                        str.append(parser.getText());
                        if (parser.getEventType() == XmlPullParser.END_TAG && name.equals(parser.getName())) {
                            break;
                        } else if (parser.getEventType() != XmlPullParser.END_TAG && parser.isEmptyElementTag() && name.equals(parser.getName())) {
                            break;
                        }
                    }
                }
                ll = parser.getLineNumber();
                lc = parser.getColumnNumber();
                parser.next();
            }

            System.out.println("Received: " + str.toString());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str.toString();
    }
}