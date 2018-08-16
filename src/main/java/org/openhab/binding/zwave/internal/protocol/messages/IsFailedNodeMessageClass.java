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
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.ZWaveNodeState;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller.
 * It queries the controller to see if the node is on its 'failed nodes' list.
 *
 * @author Chris Jackson
 * @author Wez Hunter
 */
public class IsFailedNodeMessageClass extends ZWaveCommandProcessor {
    private final static Logger logger = LoggerFactory.getLogger(IsFailedNodeMessageClass.class);

    public ByteMessage doRequest(int nodeId) {
        logger.debug("NODE {}: Requesting IsFailedNode status from controller.", nodeId);
        ByteMessage newMessage = new ByteMessage(MessageClass.IsFailedNodeID, ByteMessageType.Request,
                MessageClass.IsFailedNodeID, ByteMessagePriority.High);
        byte[] newPayload = { (byte) nodeId };
        newMessage.setMessagePayload(newPayload);
        return newMessage;
    }

    @Override
    public boolean handleResponse(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        int nodeId = lastSentMessage.getMessagePayloadByte(0);

        ZWaveNode node = zController.getNode(nodeId);
        if (node == null) {
            logger.debug("NODE {}: Failed node message for unknown node", nodeId);
            incomingMessage.setTransactionCanceled();
            return false;
        }

        if (incomingMessage.getMessagePayloadByte(0) != 0x00) {
            logger.warn("NODE {}: Is currently marked as failed by the controller!", nodeId);
            node.setNodeState(ZWaveNodeState.FAILED);
        } else {
            logger.debug("NODE {}: Is currently marked as healthy by the controller", nodeId);
        }

        checkTransactionComplete(lastSentMessage, incomingMessage);

        return true;
    }
}
