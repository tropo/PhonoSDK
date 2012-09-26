/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.api.faces;

/**
 *
 * @author tim
 */
public interface PhonoMessageFace {
    public String getFrom();
    public String getBody();
    public void reply(String body);
}
