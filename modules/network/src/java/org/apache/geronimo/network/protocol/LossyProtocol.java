/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.network.protocol;

import java.util.Random;


/**
 * @version $Revision: 1.1 $ $Date: 2004/03/10 02:14:27 $
 */
public class LossyProtocol extends AbstractProtocol {

    private final int STARTED = 0;
    private final int STOPPED = 1;
    private int state = STARTED;

    private Random upRandom = new Random();
    private Random downRandom = new Random();
    private float upLoss;
    private float downLoss;


    public float getUpLoss() {
        return upLoss;
    }

    public void setUpLoss(float upLoss) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.upLoss = upLoss;
    }

    public float getDownLoss() {
        return downLoss;
    }

    public void setDownLoss(float downLoss) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.downLoss = downLoss;
    }

    public void doStart() throws ProtocolException {
        state = STARTED;
    }

    public void doStop() throws ProtocolException {
        state = STOPPED;
    }

    public void sendUp(UpPacket packet) throws ProtocolException {
        if (state == STOPPED) throw new IllegalStateException("Protocol is not started");

        if (upRandom.nextFloat() > upLoss) getUp().sendUp(packet);
    }

    public void sendDown(DownPacket packet) throws ProtocolException {
        if (state == STOPPED) throw new IllegalStateException("Protocol is not started");

        if (downRandom.nextFloat() > downLoss) getDown().sendDown(packet);
    }

}
