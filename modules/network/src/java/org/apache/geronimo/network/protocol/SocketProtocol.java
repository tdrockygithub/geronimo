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

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.geronimo.network.SelectionEventListner;
import org.apache.geronimo.network.SelectorManager;


/**
 * @version $Revision: 1.1 $ $Date: 2004/03/10 02:14:27 $
 */
public class SocketProtocol implements AcceptableProtocol, SelectionEventListner {

    final static private Log log = LogFactory.getLog(SocketProtocol.class);

    private Protocol up;

    private SocketChannel acceptedSocketChannel;
    private SocketChannel socketChannel;
    private SocketAddress address;
    private SocketAddress socketInterface;

    private long timeout;
    private Mutex sendMutex = new Mutex();
    private SelectorManager selectorManager;
    private SelectionKey selectionKey;

    private long created;
    private long lastUsed;

    private final int STARTED = 0;
    private final int STOPPED = 1;
    private int state = STOPPED;


    public Protocol getUp() {
        return up;
    }

    public void setUp(Protocol up) {
        this.up = up;
    }

    public Protocol getDown() {
        throw new NoSuchMethodError("Socket protocol is at the bottom");
    }

    public void setDown(Protocol down) {
        throw new NoSuchMethodError("Socket protocol is at the bottom");
    }

    public void clearLinks() {
        up = null;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.address = address;
    }

    public SocketAddress getInterface() {
        return socketInterface;
    }

    public void setInterface(SocketAddress socketInterface) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.socketInterface = socketInterface;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.timeout = timeout;
    }

    public SelectorManager getSelectorManager() {
        return selectorManager;
    }

    public void setSelectorManager(SelectorManager selectorManager) {
        if (state == STARTED) throw new IllegalStateException("Protocol already started");
        this.selectorManager = selectorManager;
    }

    public boolean isDone() {
        return state == STOPPED;
    }

    public long getCreated() {
        return created;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public Protocol cloneProtocol() throws CloneNotSupportedException {
        return (Protocol) super.clone();
    }

    public void doStart() throws ProtocolException {
        if (address == null && acceptedSocketChannel == null) throw new IllegalStateException("No address set");

        log.trace("Starting");
        if (acceptedSocketChannel == null) {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                if (socketInterface != null) socketChannel.socket().bind(socketInterface);
                socketChannel.socket().setReuseAddress(true);
                socketChannel.connect(address);
            } catch (SocketException e) {
                state = STOPPED;
                throw new ProtocolException(e);
            } catch (IOException e) {
                state = STOPPED;
                throw new ProtocolException(e);
            }
        } else {
            socketChannel = acceptedSocketChannel;
        }

        try {
            socketChannel.configureBlocking(false);
            selectionKey = selectorManager.register(socketChannel, SelectionKey.OP_READ, this);
            log.trace("OP_READ " + selectionKey);
        } catch (ClosedChannelException e) {
            state = STOPPED;
            throw new ProtocolException(e);
        } catch (IOException e) {
            state = STOPPED;
            throw new ProtocolException(e);
        }

        created = System.currentTimeMillis();
        lastUsed = System.currentTimeMillis();
        state = STARTED;
    }

    public void doStop() throws ProtocolException {
        log.trace("Stopping");
        close();
        state = STOPPED;
    }

    public void sendUp(UpPacket packet) throws ProtocolException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    public void sendDown(DownPacket packet) throws ProtocolException {
        if (state == STOPPED) throw new IllegalStateException("Protocol is not started");

        lastUsed = System.currentTimeMillis();

        try {
            log.trace("AQUIRING " + sendMutex);
            if (!sendMutex.attempt(timeout)) throw new ProtocolException("Send timeout.");
            log.trace("AQUIRED " + sendMutex);


            Collection patcketBuffers = packet.getBuffers();

            int n = patcketBuffers.size();
            sendBuffer = new ByteBuffer[n + 1];
            int size = 0;
            Iterator iter = patcketBuffers.iterator();
            for (int i = 1; iter.hasNext(); i++) {
                sendBuffer[i] = (ByteBuffer) iter.next();
                size += sendBuffer[i].remaining();
            }

            sendBuffer[0] = ByteBuffer.allocate(4);
            sendBuffer[0].putInt(size);
            sendBuffer[0].flip();

            log.trace("OP_READ, OP_WRITE " + selectionKey);
            selectorManager.setInterestOps(selectionKey, SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);

        } catch (InterruptedException e) {
            log.debug("Communications error, closing connection: ", e);
            close();
            throw new ProtocolException(e);
        }
    }

    public synchronized void selectionEvent(SelectionKey selection) {
        synchronized (this) {
            try {
                if (selection.isWritable())
                    serviceWrite();
                if (selection.isReadable())
                    serviceRead();
            } catch (CancelledKeyException e) {
                // who knows, by the time we get here,
                // the key could have been canceled.
            }
        }
    }

    ByteBuffer[] sendBuffer;
    ByteBuffer headerBuffer = ByteBuffer.allocate(4);
    ByteBuffer bodyBuffer = null;

    private void serviceWrite() {
        try {
            long count = socketChannel.write(sendBuffer);
            log.trace("Wrote " + count);

            for (int i = 0; i < sendBuffer.length; i++) {
                if (sendBuffer[i].hasRemaining()) {
                    // not all was delivered in this call setup selector
                    // so we setup to finish sending async.
                    log.trace("OP_READ, OP_WRITE " + selectionKey);
                    selectorManager.setInterestOps(selectionKey, SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);

                    return;
                }
            }


            // We are done writing.
            log.trace("OP_READ " + selectionKey);
            selectorManager.setInterestOps(selectionKey, SelectionKey.OP_READ, 0);

            log.trace("RELEASING " + sendMutex);
            sendMutex.release();
            log.trace("RELEASED " + sendMutex);
        } catch (IOException e) {
            log.debug("Communications error, closing connection: ", e);
            close();
        }
    }

    public void serviceRead() {
        boolean tracing = log.isTraceEnabled();

        if (tracing) log.trace("ReadDataAction triggered.");

        lastUsed = System.currentTimeMillis();

        try {
            while (true) {

                // Are we reading the header??
                if (headerBuffer.hasRemaining()) {
                    if (tracing)
                        log.trace("Reading header");

                    long count = socketChannel.read(headerBuffer);
                    log.trace("HEADER Read " + count);

                    if (count == -1) {
                        close();
                        return;
                    }

                    if (headerBuffer.hasRemaining()) break; // not done reading the header.

                    headerBuffer.flip();

                    int size = headerBuffer.getInt();
                    log.trace("Gotta get " + size);

                    if (size == 0) {
                        headerBuffer.clear();
                        log.trace("OP_READ " + selectionKey);
                        selectorManager.setInterestOps(selectionKey, SelectionKey.OP_READ, 0);
                        return;
                    }

                    bodyBuffer = ByteBuffer.allocate(size);

                    bodyBuffer.clear();
                    bodyBuffer.limit(size);
                }
                // Are we reading the body??
                if (bodyBuffer.hasRemaining()) {
                    if (tracing)
                        log.trace("Reading body");

                    long count = socketChannel.read(bodyBuffer);
                    log.trace("BODY Read " + count);
                    log.trace("BODY remaining " + bodyBuffer.remaining());

                    if (bodyBuffer.hasRemaining())
                        break; // not done reading the body.

                    bodyBuffer.flip();

                    UpPacket packet = new UpPacket();
                    packet.setBuffer(bodyBuffer);

                    bodyBuffer = null;

                    up.sendUp(packet);

                    headerBuffer.clear();
                }
            }
            log.trace("OP_READ " + selectionKey);
            selectorManager.setInterestOps(selectionKey, SelectionKey.OP_READ, 0);
            if (tracing) log.trace("No more data available to be read.");
        } catch (ClosedChannelException e) {
            // who knows, by the time we get here,
            // the channel could have been closed.
        } catch (IOException e) {
            log.debug("Communications error, closing connection: ", e);
            close();
        } catch (ProtocolException e) {
            log.debug("Communications error, closing connection: ", e);
            close();
        }
    }

    public void close() {
        synchronized (this) {
            if (socketChannel != null) {
                log.trace("Closing");
                try {
                    selectionKey.cancel();
                    socketChannel.close();
                } catch (Throwable e) {
                    log.info("Closing error: ", e);
                }
                log.trace("Closed");
            }
            state = STOPPED;
        }
    }

    public void accept(SocketChannel socketChannel) {
        this.acceptedSocketChannel = socketChannel;
    }
}
