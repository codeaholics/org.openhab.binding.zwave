/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.handler;

import static org.openhab.binding.zwave.ZWaveBindingConstants.*;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZWaveByteHandler} is responsible for handling communications to the ZWave stick.
 * <p>
 * The {@link ZWaveByteHandler} is a SmartHome bridge. It handles the serial communications, and provides a number of
 * channels that feed back serial communications statistics.
 *
 * @author Danny Yates - based on previous ZWaveSerialHandler by Chris Jackson
 * @author Chris Jackson
 */
public abstract class ZWaveByteHandler extends ZWaveControllerHandler {

    private Logger logger = LoggerFactory.getLogger(ZWaveByteHandler.class);

    private final int SEARCH_SOF = 0;
    private final int SEARCH_LEN = 1;
    private final int SEARCH_DAT = 2;

    private int rxState = SEARCH_SOF;
    private int messageLength;
    private int rxLength;
    private byte[] rxBuffer;

    private static final int SOF = 0x01;
    private static final int ACK = 0x06;
    private static final int NAK = 0x15;
    private static final int CAN = 0x18;

    private static byte[] ACK_FRAME = { ACK };
    private static byte[] NAK_FRAME = { NAK };

    private int sofCount = 0;
    private int canCount = 0;
    private int nakCount = 0;
    private int ackCount = 0;
    private int oofCount = 0;
    private int cseCount = 0;

    public ZWaveByteHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if(channelUID.getId().equals(CHANNEL_1)) {
        // TODO: handle command
        // }
    }

    protected abstract void sendBytes(byte[] bytes);

    protected final void ack() {
        sendBytes(ACK_FRAME);
    }

    protected final void nak() {
        sendBytes(NAK_FRAME);
    }

    protected final void handleByte(int nextByte) {
        // If byte value is -1, this is a timeout
        if (nextByte == -1) {
            if (rxState != SEARCH_SOF) {
                // If we're not searching for a new frame when we get a timeout, something bad happened
                logger.debug("Receive Timeout - Sending NAK");
                rxState = SEARCH_SOF;
            }
            return;
        }

        switch (rxState) {
            case SEARCH_SOF:
                switch (nextByte) {
                    case SOF:
                        logger.trace("Received SOF");

                        // Keep track of statistics
                        sofCount++;
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_SOF), new DecimalType(sofCount));
                        rxState = SEARCH_LEN;
                        break;

                    case ACK:
                        // Keep track of statistics
                        ackCount++;
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_ACK), new DecimalType(ackCount));
                        logger.trace("Received ACK");
                        break;

                    case NAK:
                        // A NAK means the CRC was incorrectly received by the controller
                        nakCount++;
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_NAK), new DecimalType(nakCount));
                        logger.debug("Protocol error (NAK), discarding");

                        // TODO: Add NAK processing
                        break;

                    case CAN:
                        // The CAN means that the controller dropped the frame
                        canCount++;
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_CAN), new DecimalType(canCount));
                        logger.debug("Protocol error (CAN), resending");

                        // TODO: Add CAN processing (Resend?)
                        break;

                    default:
                        oofCount++;
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_OOF), new DecimalType(oofCount));
                        logger.debug(String.format("Protocol error (OOF). Got 0x%02X.", nextByte));
                        // Let the timeout deal with sending the NAK
                        break;
                }
                break;

            case SEARCH_LEN:
                // Sanity check the frame length
                if (nextByte < 4 || nextByte > 64) {
                    logger.debug("Frame length is out of limits ({})", nextByte);

                    break;
                }
                messageLength = (nextByte & 0xff) + 2;

                rxBuffer = new byte[messageLength];
                rxBuffer[0] = SOF;
                rxBuffer[1] = (byte) nextByte;
                rxLength = 2;
                rxState = SEARCH_DAT;
                break;

            case SEARCH_DAT:
                rxBuffer[rxLength] = (byte) nextByte;
                rxLength++;

                if (rxLength < messageLength) {
                    break;
                }

                logger.debug("Receive Message = {}", ByteMessage.bb2hex(rxBuffer));
                ByteMessage recvMessage = new ByteMessage(rxBuffer);
                if (recvMessage.isValid) {
                    logger.trace("Message is valid, sending ACK");
                    ack();

                    incomingMessage(recvMessage);
                } else {
                    cseCount++;
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_SERIAL_CSE), new DecimalType(cseCount));

                    logger.debug("Message is invalid, discarding");
                    nak();
                }

                rxState = SEARCH_SOF;
                break;
        }
    }
}
