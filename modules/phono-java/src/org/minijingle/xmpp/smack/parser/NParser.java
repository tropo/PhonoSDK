package org.minijingle.xmpp.smack.parser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.phono.srtplight.Log;

import java.io.IOException;

public class NParser {

	final private XmlPullParser parser;

	public NParser(XmlPullParser parser) {
		this.parser = parser;

	}

	public String getAsString() {
		XmlPullParser xpp = parser;
		final String name = parser.getName();

		String ret = "";
		int eventType;
		try {
			eventType = xpp.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_DOCUMENT) {
					Log.verb("Start document");
				} else if (eventType == XmlPullParser.START_TAG) {
					Log.verb("Start tag " + xpp.getName());
					ret += "<" + xpp.getName();
					String ns = xpp.getNamespace();
					Log.verb("ns " + ns);
					if ((ns != null) && (ns.length() > 0)){
						ret += " xmlns="+'"'+ns+'"';
					}
					int ac = xpp.getAttributeCount();
					for (int i=0; i<ac; i++){
						String aname = xpp.getAttributeName(i);
						String avalue = xpp.getAttributeValue(i);
						String att = " "+aname+"="+'"'+avalue+'"';
						Log.verb("attribute "+att);
						ret += att;
					}
					ret += ">";

				} else if (eventType == XmlPullParser.END_TAG) {
					String et =  parser.getName();
					Log.verb("End tag " + et);
					ret += "</" + et + ">";
					if (name.equals(et)){ break;}
				} else if (eventType == XmlPullParser.TEXT) {
					Log.verb("Text " + xpp.getText());
					ret += xpp.getText();
				}
				eventType = xpp.next();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.error(e.getMessage());
		}
		Log.verb("End document returning: "+ret);
		return ret;
	}

	public String oldgetAsString() {

		StringBuilder str = new StringBuilder();
		int ll = -1, lc = -1;

		final String name = parser.getName();

		try {
			while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() == XmlPullParser.START_TAG
						|| parser.getEventType() == XmlPullParser.END_TAG) {
					int cl = parser.getLineNumber();
					int cc = parser.getColumnNumber();
					if (ll != cl || lc != cc) {
						String t = parser.getText();
						if (t != null) {
							str.append(t);
						}
						if (parser.getEventType() == XmlPullParser.END_TAG
								&& name.equals(parser.getName())) {
							break;
						} else if (parser.getEventType() != XmlPullParser.END_TAG
								&& parser.isEmptyElementTag()
								&& name.equals(parser.getName())) {
							break;
						}
					}
				}
				ll = parser.getLineNumber();
				lc = parser.getColumnNumber();
				parser.next();
			}
			Log.debug("Received: " + str.toString());
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return str.length() > 0 ? str.toString() : null;
	}
}