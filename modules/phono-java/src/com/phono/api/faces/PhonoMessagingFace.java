/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.api.faces;

/**
 *
 * @author tim
 */
public interface PhonoMessagingFace {
    void onMessage(PhonoMessageFace message);
    void send(String to, String body);
}
