/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.messages;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller.
 * <p>
 * This class is the base class for the serial message class. It handles the request from the application, and the
 * processing of the responses from the controller.
 * <p>
 * When a message is sent to the controller, the controller responds with a RESPONSE.
 * <p>
 * When the controller has further data, it responds with a REQUEST.
 * <p>
 * These calls map to the handleResponse and handleRequest methods which must be overridden by the individual classes.
 *
 * @author Chris Jackson
 */
public abstract class ZWaveCommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ZWaveCommandProcessor.class);

    private static HashMap<ByteMessage.MessageClass, Class<? extends ZWaveCommandProcessor>> messageMap = null;
    protected boolean transactionComplete = false;
    /**
     * Map of Long (received time) and {@link ByteMessage}. Use TreeMap so it's sorted from oldest to newest
     */
    private final Map<Long, ByteMessage> incomingMessageTable = new TreeMap<Long, ByteMessage>();

    public ZWaveCommandProcessor() {
    }

    /**
     * Checks if the processor marked the transaction as complete
     *
     * @return true is the transaction was completed.
     */
    public boolean isTransactionComplete() {
        return transactionComplete;
    }

    /**
     * Perform a check to see if this is the expected reply and we can complete the transaction
     *
     * @param lastSentMessage The original message we sent to the controller
     * @param incomingMessage The response from the controller
     */
    protected void checkTransactionComplete(ByteMessage lastSentMessage, ByteMessage latestIncomingMessage) {
        // Put the message in our table so it will be processed now or later
        incomingMessageTable.put(System.currentTimeMillis(), latestIncomingMessage);

        // First, check if we're waiting for an ACK from the controller
        // This is used for multi-stage transactions to ensure we get all parts of the
        // transaction before completing.
        if (lastSentMessage == null || lastSentMessage.isAckPending()) {
            logger.debug("Checking transaction complete: Message has Ack Pending: {}", lastSentMessage);
            // Return until we get the ack, then come back and compare. This is necessary since, per ZWaveSendThread,
            // we sometimes get the response before the ack. See ZWaveSendThreadcomment starting with "A transaction
            // consists of (up to) 4 parts"
            return;
        }

        logger.debug("Checking transaction complete: Sent {}", lastSentMessage.toString());
        final Iterator<Map.Entry<Long, ByteMessage>> iter = incomingMessageTable.entrySet().iterator();
        final long expired = System.currentTimeMillis() - 10000; // Discard responses from 10 seconds ago or longer
        while (iter.hasNext()) {
            final Map.Entry<Long, ByteMessage> entry = iter.next();
            // Check if it's expired
            if (entry.getKey() < expired) {
                iter.remove();
                continue;
            }
            final ByteMessage incomingMessage = entry.getValue();
            logger.debug("Checking transaction complete: Recv {}", incomingMessage.toString());
            final boolean ignoreTransmissionCompleteMismatch = false; // TODO: change
            if (incomingMessage.getMessageClass() == lastSentMessage.getExpectedReply()
                    && !incomingMessage.isTransactionCanceled()) {
                logger.debug(
                        "Checking transaction complete: class={}, callback id={}, expected={}, cancelled={}        transaction complete!",
                        incomingMessage.getMessageClass(), lastSentMessage.getCallbackId(),
                        lastSentMessage.getExpectedReply(), incomingMessage.isTransactionCanceled());
                transactionComplete = true;
                return;
            } else if (ignoreTransmissionCompleteMismatch) {
                logger.debug(
                        "Checking transaction complete: class={}, callback id={}, expected={}, cancelled={}      MISMATCH IGNORED",
                        incomingMessage.getMessageClass(), lastSentMessage.getCallbackId(),
                        lastSentMessage.getExpectedReply(), incomingMessage.isTransactionCanceled());
                transactionComplete = true; // TODO: this was to test if this was preventing successful security
                                            // pairing, fix properly and remove
                return;
            } else {
                logger.debug(
                        "Checking transaction complete: class={}, callback id={}, expected={}, cancelled={}      MISMATCH",
                        incomingMessage.getMessageClass(), lastSentMessage.getCallbackId(),
                        lastSentMessage.getExpectedReply(), incomingMessage.isTransactionCanceled());
            }
        }
    }

    /**
     * Method for handling the response from the controller
     *
     * @param zController the ZWave controller
     * @param lastSentMessage The original message we sent to the controller
     * @param incomingMessage The response from the controller
     * @return
     * @throws ZWaveByteMessageException
     */
    public boolean handleResponse(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        logger.warn("TODO: {} unsupported RESPONSE.", incomingMessage.getMessageClass().getLabel());
        return false;
    }

    /**
     * Method for handling the request from the controller
     *
     * @param zController the ZWave controller
     * @param lastSentMessage The original message we sent to the controller
     * @param incomingMessage The response from the controller
     * @return
     * @throws ZWaveByteMessageException
     */
    public boolean handleRequest(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        logger.warn("TODO: {} unsupported REQUEST.", incomingMessage.getMessageClass().getLabel());
        return false;
    }

    /**
     * Returns the message processor for the specified message class
     *
     * @param serialMessage The message class required to be processed
     * @return The message processor
     */
    public static ZWaveCommandProcessor getMessageDispatcher(ByteMessage.MessageClass serialMessage) {
        if (messageMap == null) {
            messageMap = new HashMap<ByteMessage.MessageClass, Class<? extends ZWaveCommandProcessor>>();
            messageMap.put(ByteMessage.MessageClass.AddNodeToNetwork, AddNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.ApplicationCommandHandler,
                    ApplicationCommandMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.ApplicationUpdate, ApplicationUpdateMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.AssignReturnRoute, AssignReturnRouteMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.AssignSucReturnRoute,
                    AssignSucReturnRouteMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.DeleteReturnRoute, DeleteReturnRouteMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.DeleteSUCReturnRoute,
                    DeleteSucReturnRouteMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.EnableSuc, EnableSucMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.GetRoutingInfo, GetRoutingInfoMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.GetVersion, GetVersionMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.GetSucNodeId, GetSucNodeIdMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.GetControllerCapabilities,
                    GetControllerCapabilitiesMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.IdentifyNode, IdentifyNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.MemoryGetId, MemoryGetIdMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.RemoveFailedNodeID, RemoveFailedNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.IsFailedNodeID, IsFailedNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.RemoveNodeFromNetwork, RemoveNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.ReplaceFailedNode, ReplaceFailedNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.RequestNetworkUpdate,
                    RequestNetworkUpdateMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.RequestNodeInfo, RequestNodeInfoMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.RequestNodeNeighborUpdate,
                    RequestNodeNeighborUpdateMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SendData, SendDataMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SerialApiGetCapabilities,
                    SerialApiGetCapabilitiesMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SerialApiGetInitData,
                    SerialApiGetInitDataMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SerialApiSetTimeouts,
                    SerialApiSetTimeoutsMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SerialApiSoftReset, SerialApiSoftResetMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SetSucNodeID, SetSucNodeMessageClass.class);
            messageMap.put(ByteMessage.MessageClass.SetDefault, ControllerSetDefaultMessageClass.class);
        }

        Constructor<? extends ZWaveCommandProcessor> constructor;
        try {
            if (messageMap.get(serialMessage) == null) {
                logger.warn("SerialMessage class {} is not implemented!", serialMessage.getLabel());
                return null;
            }
            constructor = messageMap.get(serialMessage).getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            logger.debug("Command processor error");
        }

        return null;
    }
}
