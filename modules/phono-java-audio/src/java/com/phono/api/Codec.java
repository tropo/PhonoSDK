/*
 * Copyright 2011 Voxeo Corp.
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

package com.phono.api;

public class Codec {
    // pt, name, rate, (ptime)
    final public int pt;
    final public String name;
    final public int rate;
    final public int ptime;
    final public long iaxcn;
    public Codec(int pt_,String name_,int rate_,int ptime_, long iaxcn_){
        pt = pt_;
        name = name_;
        rate = rate_;
        ptime = ptime_;
        iaxcn = iaxcn_;
    }
}
