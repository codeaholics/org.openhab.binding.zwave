/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.messages;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.MessageClass;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessagePriority;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessageType;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller
 *
 * @author Chris Jackson
 */
public class SerialApiSetTimeoutsMessageClass extends ZWaveCommandProcessor {
    private final static Logger logger = LoggerFactory.getLogger(SerialApiSetTimeoutsMessageClass.class);

    public ByteMessage doRequest(int ackTimeout, int byteTimeout) {
        // Queue the request
        ByteMessage newMessage = new ByteMessage(MessageClass.SerialApiSetTimeouts, ByteMessageType.Request,
                MessageClass.SerialApiSetTimeouts, ByteMessagePriority.High);

        byte[] newPayload = { (byte) ackTimeout, (byte) byteTimeout };

        newMessage.setMessagePayload(newPayload);
        return newMessage;
    }

    @Override
    public boolean handleResponse(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        logger.debug("Got SerialApiSetTimeouts response. ACK={}, BYTE={}", incomingMessage.getMessagePayloadByte(0),
                incomingMessage.getMessagePayloadByte(1));

        checkTransactionComplete(lastSentMessage, incomingMessage);

        return true;
    }
}
