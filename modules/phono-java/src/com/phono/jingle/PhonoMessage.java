/*
 * Copyright 2012 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.phono.jingle;


/**
 * Represents a Phono Message
 * @author tim
 */
public class PhonoMessage  {

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
