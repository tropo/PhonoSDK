/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.minijingle.jingle.ringing;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 *
 * @author tim
 */
@XStreamAlias("ringing")
public class Ringing {

    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    public final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1:info";
    public static final String XMLNS = "urn:xmpp:jingle:apps:rtp:1:info";
}
