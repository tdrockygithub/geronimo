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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.geronimo.network.SelectorManager;
import org.apache.geronimo.system.ClockPool;
import org.apache.geronimo.system.ThreadPool;


/**
 * @version $Revision: 1.1 $ $Date: 2004/03/10 02:14:29 $
 */
public class ProtocolStackTest extends TestCase {

    public void test() throws Exception {
        ThreadPool tp = new ThreadPool();
        tp.setKeepAliveTime(1 * 1000);
        tp.setMinimumPoolSize(5);
        tp.setMaximumPoolSize(25);
        tp.setPoolName("TP");
        tp.doStart();

        ClockPool cp = new ClockPool();
        cp.setPoolName("CP");
        cp.doStart();

        SelectorManager sm = new SelectorManager();
        sm.setThreadPool(tp);
        sm.setThreadName("SM");
        sm.setTimeout(500);
        sm.doStart();

        AcceptableProtocolStack s = new AcceptableProtocolStack();

        SocketProtocol spt = new SocketProtocol();
        spt.setTimeout(10 * 1000);
        spt.setSelectorManager(sm);

        s.push(spt);

        TestProtocol test = new TestProtocol();
        test.setValue("SimpleTest");
        test.setThreadPool(tp);
        test.setClockPool(cp);
        test.setSelectorManager(sm);
        s.push(test);

        ProtocolFactory pf = new ProtocolFactory();
        pf.setClockPool(cp);
        pf.setMaxAge(Long.MAX_VALUE);
        pf.setMaxInactivity(1 * 60 * 60 * 1000);
        pf.setReclaimPeriod(10 * 1000);
        pf.setTemplate(s);

        ServerSocketAcceptor ssa = new ServerSocketAcceptor();
        ssa.setSelectorManager(sm);
        ssa.setTimeOut(5 * 1000);
        ssa.setUri(new URI("async://localhost:8081/?tcp.nodelay=true&tcp.backlog=5#"));
        ssa.setAcceptorListener(pf);
        ssa.doStart();

        SocketProtocol sp = new SocketProtocol();
        sp.setUp(new Protocol() {
            public Protocol getUp() {
                throw new NoSuchMethodError();
            }

            public void setUp(Protocol up) {
                throw new NoSuchMethodError();
            }

            public Protocol getDown() {
                throw new NoSuchMethodError();
            }

            public void setDown(Protocol down) {
                throw new NoSuchMethodError();
            }

            public void clearLinks() {
            }

            public Protocol cloneProtocol() throws CloneNotSupportedException {
                return (Protocol) super.clone();
            }

            public void doStart() {
            }

            public void doStop() {
            }

            public void sendUp(UpPacket packet) {
            }

            public void sendDown(DownPacket packet) {
            }

        });

        sp.setTimeout(1000 * 1000);  //todo set back to 10s
        sp.setInterface(new InetSocketAddress("localhost", 0));
        sp.setAddress(new InetSocketAddress("localhost", 8081));
        sp.setSelectorManager(sm);

        sp.doStart();


        sp.sendDown(getDatagramPacket());
        sp.sendDown(getDatagramPacket());
        sp.sendDown(getDatagramPacket());

        DatagramDownPacket packet = getDatagramPacket();
        sp.sendDown(packet);
        sp.sendDown(packet);

        Thread.sleep(5 * 1000);

        sp.doStop();

        ssa.doStop();

        pf.doStop();

        spt.doStop();

        sm.doStop();

        cp.doStop();

        tp.doStop();
    }
    
    static volatile long id = 0;

    public DatagramDownPacket getDatagramPacket() {
        DatagramDownPacket packet = new DatagramDownPacket();
        ArrayList list = new ArrayList();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        CharBuffer b = buffer.asCharBuffer();
        b.put("Hello! " + id++);

        buffer.position(b.position() * 2);
        buffer.flip();
        list.add(buffer);

        packet.setBuffers(list);

        return packet;
    }
}
