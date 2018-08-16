/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.handler;

import static org.openhab.binding.zwave.ZWaveBindingConstants.CONFIGURATION_PORT;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zwave.ZWaveBindingConstants;
import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link ZWaveSerialHandler} is responsible for the serial communications to the ZWave stick.
 * <p>
 * The {@link ZWaveSerialHandler} is a SmartHome bridge. It handles the serial communications, and provides a number of
 * channels that feed back serial communications statistics.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ZWaveSerialHandler extends ZWaveByteHandler {

    private Logger logger = LoggerFactory.getLogger(ZWaveSerialHandler.class);

    private String portId;

    private SerialPort serialPort;

    private static final int SERIAL_RECEIVE_TIMEOUT = 250;

    private ZWaveReceiveThread receiveThread;

    public ZWaveSerialHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing ZWave serial controller.");

        portId = (String) getConfig().get(CONFIGURATION_PORT);

        if (portId == null || portId.length() == 0) {
            logger.error("ZWave port is not set.");
            return;
        }

        super.initialize();
        logger.info("Connecting to serial port '{}'", portId);
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portId);
            CommPort commPort = portIdentifier.open("org.openhab.binding.zwave", 2000);
            serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.enableReceiveThreshold(1);
            serialPort.enableReceiveTimeout(SERIAL_RECEIVE_TIMEOUT);
            logger.debug("Starting receive thread");
            receiveThread = new ZWaveReceiveThread();
            receiveThread.start();

            // RXTX serial port library causes high CPU load
            // Start event listener, which will just sleep and slow down event loop
            serialPort.addEventListener(receiveThread);
            serialPort.notifyOnDataAvailable(true);

            logger.info("Serial port is initialized");

            initializeNetwork();
        } catch (NoSuchPortException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ZWaveBindingConstants.getI18nConstant(ZWaveBindingConstants.OFFLINE_SERIAL_EXISTS, portId));
        } catch (PortInUseException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ZWaveBindingConstants.getI18nConstant(ZWaveBindingConstants.OFFLINE_SERIAL_INUSE, portId));
        } catch (UnsupportedCommOperationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ZWaveBindingConstants.getI18nConstant(ZWaveBindingConstants.OFFLINE_SERIAL_UNSUPPORTED, portId));
        } catch (TooManyListenersException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ZWaveBindingConstants.getI18nConstant(ZWaveBindingConstants.OFFLINE_SERIAL_LISTENERS, portId));
        }
    }

    /**
     * Closes the connection to the ZWave controller.
     */
    @Override
    public void dispose() {
        if (receiveThread != null) {
            receiveThread.interrupt();
            try {
                receiveThread.join();
            } catch (InterruptedException e) {
            }
            receiveThread = null;
        }
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
        }
        logger.info("Stopped ZWave serial handler");

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if(channelUID.getId().equals(CHANNEL_1)) {
        // TODO: handle command
        // }
    }

    /**
     * ZWave controller Receive Thread. Takes care of receiving all messages.
     * It uses a semaphore to synchronize communication with the sending thread.
     */
    private class ZWaveReceiveThread extends Thread implements SerialPortEventListener {

        private final Logger logger = LoggerFactory.getLogger(ZWaveReceiveThread.class);

        ZWaveReceiveThread() {
            super("ZWaveReceiveThread");
        }

        @Override
        public void serialEvent(SerialPortEvent arg0) {
            try {
                logger.trace("RXTX library CPU load workaround, sleep forever");
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }
        }

        /**
         * Run method. Runs the actual receiving process.
         */
        @Override
        public void run() {
            logger.debug("Starting ZWave thread: Receive");
            try {
                // Send a NAK to resynchronise communications
                nak();

                while (!interrupted()) {
                    int nextByte;

                    try {
                        nextByte = serialPort.getInputStream().read();
                    } catch (IOException e) {
                        logger.error("Got I/O exception {} during receiving. exiting thread.", e.getLocalizedMessage());
                        break;
                    }

                    handleByte(nextByte);
                }
            } catch (Exception e) {
                logger.error("Exception during ZWave thread: Receive {}", e.getMessage());
            }
            logger.debug("Stopped ZWave thread: Receive");

            serialPort.removeEventListener();
        }
    }

    @Override
    public void sendPacket(ByteMessage serialMessage) {
        byte[] buffer = serialMessage.getMessageBuffer();

        if (serialPort == null) {
            logger.debug("NODE {}: Port closed sending REQUEST Message = {}", serialMessage.getMessageNode(),
                    ByteMessage.bb2hex(buffer));
            return;
        }

        logger.debug("NODE {}: Sending REQUEST Message = {}", serialMessage.getMessageNode(),
                ByteMessage.bb2hex(buffer));

        sendBytes(buffer);
    }

    @Override
    protected void sendBytes(byte[] buffer) {

        try {
            synchronized (serialPort.getOutputStream()) {
                serialPort.getOutputStream().write(buffer);
                serialPort.getOutputStream().flush();
                logger.trace("Message SENT");
            }
        } catch (IOException e) {
            logger.error("Got I/O exception {} during sending. exiting thread.", e.getLocalizedMessage());
        }
    }
}
