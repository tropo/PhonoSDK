package org.minijingle.xmpp.smack.parser;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import org.jivesoftware.smack.packet.IQ;

public class XStreamIQ<T> extends IQ {

    private Object element;

    final static XStream stream = new XStream(new DomDriver()) {
        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new MapperWrapper(next) {
                public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                    return definedIn != Object.class && super.shouldSerializeMember(definedIn, fieldName);
                }
            };
        }
    };

    static {
        stream.autodetectAnnotations(true);
    }

    public static XStream getStream() {
        return stream;
    }

    public XStreamIQ(final T element) {
        this.element = element;
    }

    public T getElement() {
        return (T) element;
    }

    public String getChildElementXML() {
        return this.element.toString();
    }
}
