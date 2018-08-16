/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.test.internal.protocol.serialmessage;

import static org.junit.Assert.assertEquals;

import org.mockito.Mockito;
import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessageType;
import org.openhab.binding.zwave.internal.protocol.messages.ZWaveCommandProcessor;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;

public class ZWaveCommandProcessorTest {

    void ProcessResponse(ZWaveCommandProcessor handler, byte[] packetData) {
        ByteMessage msg = new ByteMessage(packetData);
        assertEquals(true, msg.isValid);
        assertEquals(ByteMessageType.Response, msg.getMessageType());

        // Mock the controller so we can get any events
        ZWaveController controller = Mockito.mock(ZWaveController.class);
        // argument = ArgumentCaptor.forClass(ZWaveEvent.class);
        // Mockito.doNothing().when(controller).notifyEventListeners(argument.capture());

        // ZWaveTransaction transaction = new ZWaveTransactionBuilder(msg).build();

        try {
            handler.handleResponse(controller, msg, msg);
        } catch (ZWaveByteMessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
