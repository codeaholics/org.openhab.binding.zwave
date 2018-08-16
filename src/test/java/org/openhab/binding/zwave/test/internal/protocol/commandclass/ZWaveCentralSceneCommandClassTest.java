/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.test.internal.protocol.commandclass;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveBatteryCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCentralSceneCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;

/**
 * Test cases for {@link ZWaveBatteryCommandClass}.
 *
 * @author Chris Jackson - Initial version
 */
public class ZWaveCentralSceneCommandClassTest extends ZWaveCommandClassTest {

    @Test
    public void getValueMessage() {
        ZWaveCentralSceneCommandClass cls = (ZWaveCentralSceneCommandClass) getCommandClass(CommandClass.CENTRAL_SCENE);
        ByteMessage msg;

        byte[] expectedResponseV1 = { 1, 9, 0, 19, 99, 2, 91, 1, 0, 0, -34 };
        cls.setVersion(1);
        msg = cls.getValueMessage();
        assertTrue(Arrays.equals(msg.getMessageBuffer(), expectedResponseV1));
    }

    @Test
    public void processScene1() {
        byte[] packetData = { 0x01, 0x0B, 0x00, 0x04, 0x00, 0x08, 0x05, 0x5B, 0x03, 0x03, 0x03, 0x01, (byte) 0xA4 };

        List<ZWaveEvent> events = processCommandClassMessage(packetData);

        assertEquals(events.size(), 1);
        ZWaveCommandClassValueEvent event = (ZWaveCommandClassValueEvent) events.get(0);

        assertEquals(event.getCommandClass(), CommandClass.CENTRAL_SCENE);
        assertEquals(event.getValue(), new BigDecimal("1.3"));
    }

    @Test
    public void processScene2() {
        byte[] packetData = { 0x01, 0x0B, 0x00, 0x04, 0x00, 0x08, 0x05, 0x5B, 0x03, 0x05, 0x05, 0x01, (byte) 0xA4 };

        List<ZWaveEvent> events = processCommandClassMessage(packetData);

        assertEquals(events.size(), 1);
        ZWaveCommandClassValueEvent event = (ZWaveCommandClassValueEvent) events.get(0);

        assertEquals(event.getCommandClass(), CommandClass.CENTRAL_SCENE);
        assertEquals(event.getValue(), new BigDecimal("1.5"));
    }
}
