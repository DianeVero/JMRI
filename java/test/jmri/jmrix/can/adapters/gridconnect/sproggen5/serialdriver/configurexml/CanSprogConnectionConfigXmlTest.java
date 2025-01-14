package jmri.jmrix.can.adapters.gridconnect.sproggen5.serialdriver.configurexml;

import jmri.jmrix.can.adapters.gridconnect.sproggen5.serialdriver.CanSprogConnectionConfig;
import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * CanSprogConnectionConfigXmlTest.java
 *
 * Test for the CanSprogConnectionConfigXml class
 *
 * @author   Andrew Crosland  Copyright (C) 2020
 */
public class CanSprogConnectionConfigXmlTest extends jmri.jmrix.configurexml.AbstractSerialConnectionConfigXmlTestBase {

    @Test
    public void testCTor() {
        CanSprogConnectionConfigXml c = new CanSprogConnectionConfigXml();
        Assert.assertNotNull("exists",c);
    }

    @BeforeEach
    @Override
    public void setUp() {
        JUnitUtil.setUp();
        xmlAdapter = new CanSprogConnectionConfigXml();
        cc = new CanSprogConnectionConfig();
    }

    @AfterEach
    @Override
    public void tearDown() {
        JUnitUtil.deregisterBlockManagerShutdownTask();
        JUnitUtil.deregisterEditorManagerShutdownTask();
        xmlAdapter = null;
        cc = null;
        JUnitUtil.tearDown();
    }
}
