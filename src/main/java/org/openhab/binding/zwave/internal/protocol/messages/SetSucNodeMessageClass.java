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
public class SetSucNodeMessageClass extends ZWaveCommandProcessor {
    private final static Logger logger = LoggerFactory.getLogger(SetSucNodeMessageClass.class);

    public ByteMessage doRequest(int nodeId, SUCType type) {
        logger.debug("NODE {}: SetSucNodeID node as {}", nodeId, type.toString());

        // Queue the request
        ByteMessage newMessage = new ByteMessage(MessageClass.SetSucNodeID, ByteMessageType.Request,
                MessageClass.SetSucNodeID, ByteMessagePriority.High);
        byte[] newPayload = new byte[5];
        newPayload[0] = (byte) nodeId;
        switch (type) {
            case NONE:
                newPayload[1] = 0;
                newPayload[3] = 0;
                break;
            case BASIC:
                newPayload[1] = 1;
                newPayload[3] = 0;
                break;
            case SERVER:
                newPayload[1] = 1;
                newPayload[3] = 1;
                break;
        }

        newPayload[2] = 0; // Low power option = false
        newPayload[4] = 1; // Callback!!!
        newMessage.setMessagePayload(newPayload);
        return newMessage;
    }

    @Override
    public boolean handleResponse(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        int nodeId = lastSentMessage.getMessagePayloadByte(0);

        logger.debug("NODE {}: SetSucNodeID node response.", nodeId);

        if (incomingMessage.getMessagePayloadByte(0) != 0x00) {
            logger.debug("NODE {}: SetSucNodeID command OK.", nodeId);
        } else {
            logger.debug("NODE {}: SetSucNodeID command failed.", nodeId);
            checkTransactionComplete(lastSentMessage, incomingMessage);
        }

        return true;
    }

    @Override
    public boolean handleRequest(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        int nodeId = lastSentMessage.getMessagePayloadByte(0);

        logger.debug("NODE {}: SetSucNodeID node request.", nodeId);

        checkTransactionComplete(lastSentMessage, incomingMessage);
        if (incomingMessage.getMessagePayloadByte(1) != 0x00) {
            logger.debug("NODE {}: SetSucNodeID failed with error 0x{}.", nodeId,
                    Integer.toHexString(incomingMessage.getMessagePayloadByte(1)));

            return false;
        } else {
            return true;
        }
    }

    public enum SUCType {
        NONE,
        BASIC,
        SERVER
    }

}
