/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Handles the move-to-position window covering command class.
 *
 * @author Chris Jackson
 *
 */
@XStreamAlias("mtpWindowCoveringCommandClass")
public class ZWaveMtpWindowCoveringCommandClass extends ZWaveCommandClass {

    @XStreamOmitField
    private final static Logger logger = LoggerFactory.getLogger(ZWaveMtpWindowCoveringCommandClass.class);

    /**
     * Creates a new instance of the ZWaveMtpWindowCoveringCommandClass class.
     *
     * @param node the node this command class belongs to
     * @param controller the controller to use
     * @param endpoint the endpoint this Command class belongs to
     */
    public ZWaveMtpWindowCoveringCommandClass(ZWaveNode node, ZWaveController controller, ZWaveEndpoint endpoint) {
        super(node, controller, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandClass getCommandClass() {
        return CommandClass.MTP_WINDOW_COVERING;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ZWaveByteMessageException
     */
    @Override
    public void handleApplicationCommandRequest(ByteMessage serialMessage, int offset, int endpoint)
            throws ZWaveByteMessageException {
        logger.debug("NODE {}: Received MTP_WINDOW_COVERING command V{}", getNode().getNodeId(), getVersion());
        int command = serialMessage.getMessagePayloadByte(offset);
        switch (command) {
            default:
                logger.warn(String.format("NODE %d: Unsupported Command %d for command class %s (0x%02X).",
                        getNode().getNodeId(), command, getCommandClass().getLabel(), getCommandClass().getKey()));
                break;
        }
    }
}
