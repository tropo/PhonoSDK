package org.minijingle.jingle;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 *
 * @author tim
 */
@XStreamAlias("custom-header")
class CustomHeader {

    @XStreamAsAttribute
    private String name, data;

    CustomHeader(String n, String v) {
        name = n;
        data = v;
    }
    public String getData(){
        return data;
    }
    public String getName(){
        return name;
    }
    public void setName(String n){
        name = n;
    }
    public void setData(String v){
        data = v;
    }
}
