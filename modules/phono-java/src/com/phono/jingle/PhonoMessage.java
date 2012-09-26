/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.jingle;

import com.phono.api.faces.PhonoMessageFace;

/**
 *
 * @author tim
 */
public class PhonoMessage  implements PhonoMessageFace {

    private PhonoMessaging _pmi;
    private String _body;
    private String _from;

    PhonoMessage(PhonoMessaging pmi, String from, String body) {
        _body = body;
        _from = from;
        _pmi = pmi;
    }

    public String getFrom() {
        return _from;
    }

    public String getBody() {
        return _body;
    }

    public void reply(String body) {
        _pmi.send(_from, body);
    }

}
