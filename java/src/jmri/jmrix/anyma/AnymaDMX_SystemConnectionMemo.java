package jmri.jmrix.anyma;

import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import jmri.InstanceManager;
import jmri.Light;
import jmri.LightManager;
import jmri.Manager.NameValidity;
import jmri.Sensor;
import jmri.jmrix.AbstractNode;
import jmri.jmrix.SystemConnectionMemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal SystemConnectionMemo for anyma dmx systems.
 *
 * @author George Warner Copyright (C) 2017
 * @since 4.9.6
 */
public class AnymaDMX_SystemConnectionMemo extends SystemConnectionMemo {

    private boolean configured = false;

    public AnymaDMX_SystemConnectionMemo() {
        this("DX", AnymaDMX_ConnectionTypeList.ANYMA_DMX); // default to "DX" prefix
        log.debug("* Constructor()");
    }

    public AnymaDMX_SystemConnectionMemo(@Nonnull String prefix, @Nonnull String userName) {
        super(prefix, userName);

        log.debug("* Constructor ({}, {})", prefix, userName);

        register(); // registers general type
        InstanceManager.store(this, AnymaDMX_SystemConnectionMemo.class); // also register as specific type
    }

    private AnymaDMX_TrafficController trafficController = null;

    /**
     * 
     * @return 
     */
    protected AnymaDMX_TrafficController getTrafficController() {
        return trafficController;
    }

    protected void setTrafficController(AnymaDMX_TrafficController trafficController) {
        this.trafficController = trafficController;
    }

//    private AnymaDMX_UsbPortAdapter trafficController = null;
//
//    protected AnymaDMX_UsbPortAdapter getAdapter() {
//        return trafficController;
//    }
//
//    protected void setAdapter(AnymaDMX_UsbPortAdapter trafficController) {
//        this.trafficController = trafficController;
//    }

    /**
     * Public static method to the user name for a valid system name.
     *
     * @return "" (null string) if the system name is not valid or does not
     *         exist
     */
    public String getUserNameFromSystemName(String systemName) {
        log.debug("* getUserNameFromSystemName('{}')", systemName);
        String result = "";        // not any known light
        int offset = checkSystemPrefix(systemName);
        if (offset > 0) {
            if (systemName.length() > offset) {
                if (systemName.charAt(offset) == 'L') {
                    Light lgt = null;
                    lgt = InstanceManager.lightManagerInstance().getBySystemName(systemName);
                    if (lgt != null) {
                        result = lgt.getUserName();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Public static method to parse a anyma dmx system name and return the
     * channel number. Notes:
     * <ul>
     * <li>Channels are numbered from 1 to 512.</li>
     * <li>Does not check whether that node is defined on current system.</li>
     * </ul>
     *
     * @return 0 if an error is found.
     */
    public int getChannelFromSystemName(String systemName) {
        int result = 0;
        log.debug("* getChannelFromSystemName('{}')", systemName);

        int offset = checkSystemPrefix(systemName);
        if (offset > 1) {
            if (validSystemNameFormat(systemName, systemName.charAt(offset)) == NameValidity.VALID) {
                // Find the beginning of the channel number field
                int k = 0;
                for (int i = offset; i < systemName.length(); i++) {
                    if (systemName.charAt(i) == 'L') {
                        k = i + 1;
                        break;
                    }
                }
                if (k > offset) {    // k = position of "L" char in name
                    try {
                        result = Integer.parseInt(systemName.substring(k));
                    } catch (Exception e) {
                        log.warn("invalid character in channel number field of anyma dmx system name: {}", systemName);
                    }
                }
            } else {
                log.error("No point in normalizing if a valid system name format is not present");
            }
        } else {
            log.error("invalid system prefix in anyma dmx system name: {}", systemName);
        }
        return result;
    }

    /**
     * Public static method to check and skip the System Prefix string on a
     * system name.
     *
     * @return offset of the 1st character past the prefix, or -1 if not valid
     *         for this connection
     */
    public int checkSystemPrefix(String systemName) {
        log.debug("* checkSystemPrefix('{}')", systemName);
        int result = -1;
        if (systemName.startsWith(getSystemPrefix())) {
            result = getSystemPrefix().length();
        }
        return result;
    }

    /**
     * Public static method to test if a anyma dmx output channel is free for
     * assignment. Test is not performed if the node address or channel number
     * is invalid.
     *
     * @return "" (empty string) if the specified output channel is free for
     *         assignment, else returns the system name of the conflicting
     *         assignment.
     */
    public String isOutputChannelFree(int nAddress, int channelNum) {
        log.debug("* isOutputChannelFree({}, {})", nAddress, channelNum);

        if ((nAddress < 0) || (nAddress > 127)) {
            log.warn("invalid node address in free channel test");
            return "";
        }
        if ((channelNum < 1) || (channelNum > 512)) {
            log.warn("invalid channel number in free channel test");
            return "";
        }

        Light lgt = null;
        String sysName = makeSystemName("L", nAddress, channelNum);
        lgt = InstanceManager.lightManagerInstance().getBySystemName(sysName);
        if (lgt != null) {
            return sysName;
        }
        String altName = convertSystemNameToAlternate(sysName);
        lgt = InstanceManager.lightManagerInstance().getBySystemName(altName);
        if (lgt != null) {
            return altName;
        }
        // not assigned to a light
        return "";
    }

    /**
     * Public static method to normalize a anyma dmx system name.
     * <P>
     * This routine is used to ensure that each system name is uniquely linked
     * to one anyma dmx channel, by removing any extra zeros inserted by the
     * user.
     *
     * @return "" (empty string) if the supplied system name does not have a
     *         valid format. Otherwise a normalized name is returned in the same
     *         format as the input name.
     */
    public String normalizeSystemName(String systemName) {
        log.debug("* normalizeSystemName('{}')", systemName);
        int offset = checkSystemPrefix(systemName);
        if (offset < 1) {
//            log.error("invalid system prefix in anyma dmx system name: {}", systemName); // fix test first
            return "";
        }
        if (validSystemNameFormat(systemName, systemName.charAt(offset)) != NameValidity.VALID) {
            // No point in normalizing if a valid system name format is not present
            return "";
        }
        String nName = "";
        String s = "";
        int k = 0;
        boolean noB = true;
        for (int i = offset + 1; (i < systemName.length()) && noB; i++) {
            if (systemName.charAt(i) == 'B') {
                s = systemName.substring(offset + 1, i);
                k = i + 1;
                noB = false;
            }
        }
        if (noB) {
            int num = Integer.valueOf(systemName.substring(offset + 1)).intValue();
            int nAddress = num / 1000;
            int channelNum = num - (nAddress * 1000);
            nName = systemName.substring(0, offset + 1) + Integer.toString((nAddress * 1000) + channelNum);
        } else {
            int nAddress = Integer.valueOf(s).intValue();
            int channelNum = Integer.parseInt(systemName.substring(k, systemName.length()));
            nName = systemName.substring(0, offset + 1) + Integer.toString(nAddress) + "B" + Integer.toString(channelNum);
        }
        return nName;
    }

    /**
     * Public static method to convert one format anyma dmx system name to the
     * alternate format.
     *
     * @return "" (empty string) if the supplied system name does not have a
     *         valid format, or if there is no representation in the alternate
     *         naming scheme
     */
    public String convertSystemNameToAlternate(String systemName) {
        log.debug("* convertSystemNameToAlternate('{}')", systemName);
        int offset = checkSystemPrefix(systemName);
        if (offset < 1) {
            log.error("invalid system prefix in anyma dmx system name: {}", systemName);
            return "";
        }
        if (validSystemNameFormat(systemName, systemName.charAt(offset)) != NameValidity.VALID) {
            // No point in trying if a valid system name format is not present
            return "";
        }
        String altName = "";
        String s = "";
        int k = 0;
        boolean noB = true;
        for (int i = offset + 1; (i < systemName.length()) && noB; i++) {
            if (systemName.charAt(i) == 'B') {
                s = systemName.substring(offset + 1, i);
                k = i + 1;
                noB = false;
            }
        }
        if (noB) {
            int num = Integer.valueOf(systemName.substring(offset + 1)).intValue();
            int nAddress = num / 1000;
            int channelNum = num - (nAddress * 1000);
            altName = systemName.substring(0, offset + 1) + Integer.toString(nAddress) + "B" + Integer.toString(channelNum);
        } else {
            int nAddress = Integer.valueOf(s).intValue();
            int channelNum = Integer.parseInt(systemName.substring(k, systemName.length()));
            if (channelNum > 999) {
                // channel number is out-of-range for a CLnnnxxx address
                return "";
            }
            altName = systemName.substring(0, offset + 1) + Integer.toString((nAddress * 1000) + channelNum);
        }
        return altName;
    }

    /**
     * Public static method to validate system name format. Does not check
     * whether that node is defined on current system.
     *
     * @return enum indicating current validity, which might be just as a prefix
     */
    public NameValidity validSystemNameFormat(String systemName, char type) {
        log.debug("* validSystemNameFormat('{}', '{}')", systemName, type);
        int offset = checkSystemPrefix(systemName);
        if (offset < 1) {
            log.error("invalid system prefix in anyma dmx system name: {}", systemName);
            return NameValidity.INVALID;
        }
        if (systemName.charAt(offset) != type) {
            log.error("invalid type character in anyma dmx system name: {}", systemName);
            return NameValidity.INVALID;
        }
        String s = "";
        int k = 0;
        boolean noB = true;
        for (int i = offset + 1; (i < systemName.length()) && noB; i++) {
            if (systemName.charAt(i) == 'B') {
                s = systemName.substring(offset + 1, i);
                k = i + 1;
                noB = false;
            }
        }
        if (noB) {
            // This is a CLnnnxxx pattern address
            int num;
            try {
                num = Integer.valueOf(systemName.substring(offset + 1)).intValue();
            } catch (Exception e) {
                log.debug("invalid character in number field of anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            if ((num < 1) || (num >= 128000)) {
                log.debug("number field out of range in anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            if ((num - ((num / 1000) * 1000)) == 0) {
                log.debug("channel number not in range 1 - 999 in anyma dmx system name: {}", systemName);
                if (systemName.length() <= offset + 6) {
                    return NameValidity.VALID_AS_PREFIX_ONLY;
                    // may become valid by adding 1 or more digits > 0
                } else { // unless systemName.length() > offset + 6
                    return NameValidity.INVALID;
                }
            }
        } else {
            // This is a CLnBxxx pattern address
            if (s.length() == 0) {
                log.debug("no node address before 'B' in anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            int num;
            try {
                num = Integer.valueOf(s).intValue();
            } catch (Exception e) {
                log.debug("invalid character in node address field of anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            if ((num < 0) || (num >= 128)) {
                log.debug("node address field out of range in anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            try {
                num = Integer.parseInt(systemName.substring(k));
            } catch (Exception e) {
                log.debug("invalid character in channel number field of anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
            if (num == 0) {
                return NameValidity.VALID_AS_PREFIX_ONLY;
                // may become valid by adding 1 or more digits > 0, all zeros will be removed later so total length irrelevant
            }
            if ((num < 1) || (num > 512)) {
                log.debug("channel number field out of range in anyma dmx system name: {}", systemName);
                return NameValidity.INVALID;
            }
        } // TODO add format check for CLnn:xxx format
        return NameValidity.VALID;
    }

    /**
     * Public static method to test if a anyma dmx input channel is free for
     * assignment. Test is not performed if the node address is invalid or
     * channel number is greater than 512.
     *
     * @return "" (empty string) if the specified input channel is free for
     *         assignment, else returns the system name of the conflicting
     *         assignment.
     */
    public String isInputChannelFree(int nAddress, int channelNum) {
        log.debug("* isInputChannelFree({}, {})", nAddress, channelNum);
        if ((nAddress < 0) || (nAddress > 127)) {
            log.warn("invalid node address in free channel test");
            return "";
        }
        if ((channelNum < 1) || (channelNum > 512)) {
            log.warn("invalid channel number in free channel test");
            return "";
        }
        Sensor s = null;
        String sysName = "";
        sysName = makeSystemName("S", nAddress, channelNum);
        s = InstanceManager.sensorManagerInstance().getBySystemName(sysName);
        if (s != null) {
            return sysName;
        }
        String altName = "";
        altName = convertSystemNameToAlternate(sysName);
        s = InstanceManager.sensorManagerInstance().getBySystemName(altName);
        if (s != null) {
            return altName;
        }
        // not assigned to a sensor
        return "";
    }

    /**
     * Public static method to construct a anyma dmx system name from type
     * character, node address, and channel number.
     * <p>
     * If the supplied character is not valid, or the node address is out of the
     * 0 - 127 range, or the channel number is out of the 1 - 512 range, an
     * error message is logged and the null string "" is returned.
     *
     * @return a system name in the CLnnnxxx, CTnnnxxx, or CSnnnxxx format if
     *         the channel number is 1 - 999. If the channel number is 1000 -
     *         512, the system name is returned in the CLnnnBxxxx, CTnnnBxxxx,
     *         or CSnnnBxxxx format. The returned name is normalized.
     */
    public String makeSystemName(String type, int nAddress, int channelNum) {
        log.debug("* makeSystemName('{}', {}, {})", type, nAddress, channelNum);
        String nName = "";
        if (type.equals("L")) {
            if ((nAddress >= 0) && (nAddress < 128)) {
                if ((channelNum >= 1) && (channelNum <= 512)) {
                    nName = getSystemPrefix() + nAddress + type + Integer.toString(channelNum);
                } else {
                    log.warn("invalid channel number proposed for system name");
                    return nName;
                }
            } else {
                log.warn("invalid node address proposed for system name");
                return nName;
            }
        } else {
            log.error("invalid type character proposed for system name");
            return nName;
        }
        return nName;
    }

    /**
     * Public static method to parse a anyma dmx system name and return the Usb
     * Node.
     *
     * @return 'null' if invalid systemName format or if the node is not found
     */
    public AbstractNode getNodeFromSystemName(String systemName) {
        log.debug("* getNodeFromSystemName('{}')", systemName);
        // get the node address
        int ua = getNodeAddressFromSystemName(systemName);
        if (ua == -1) {
            return null;
        }
        return getNodeFromAddress(ua);
    }

    synchronized public AbstractNode getNodeFromAddress(int addr) {
        return null; //getAdapter();
    }

    /**
     * Public static method to validate anyma dmx system name for configuration.
     * Does validate node number and system prefix.
     *
     * @return 'true' if system name has a valid meaning in current
     *         configuration, else returns 'false'.
     */
    public boolean validSystemNameConfig(String systemName, char type) {
        log.debug("* validSystemNameConfig('{}', '{}')", systemName, type);
        if (validSystemNameFormat(systemName, type) != NameValidity.VALID) {
            // No point in trying if a valid system name format is not present
            return false;
        }
//        AbstractNode node = getNodeFromSystemName(systemName);
//        if (node == null) {
//            // The node indicated by this system address is not present
//            return false;
//        }
        int channel = getChannelFromSystemName(systemName);
        if (type == 'L') {
            if ((channel <= 0) || (channel > 512)) {
                // The channel is not valid for this defined Usb node
                return false;
            }
        } else {
            log.error("Invalid type specification in validSystemNameConfig call");
            return false;
        }
        // System name has passed all tests
        return true;
    }

    /**
     * Public static method to parse a anyma dmx system name and return the Usb
     * Node Address
     * <p>
     * Nodes are numbered from 0 - 127. Does not check whether that node is
     * defined on current system.
     *
     * @return '-1' if invalid systemName format or if the node is not found.
     */
    public int getNodeAddressFromSystemName(String systemName) {
        int result = -1;    // assume failure (pessimist!)
        log.debug("* getNodeAddressFromSystemName('{}')", systemName);
        int offset = checkSystemPrefix(systemName);
        if (offset > 1) {
            if (systemName.charAt(offset) == 'L') {
                int num = Integer.valueOf(systemName.substring(offset + 1)).intValue();
                if (num > 0) {
                    result = num;
                } else {
                    log.warn("invalid anyma dmx system name: " + systemName);
                }
            } else {
                log.error("invalid character in header field of system name: {}", systemName);
            }
        }
        return result;
    }

    @Override
    public boolean provides(Class<?> type) {
        boolean result = false;
        log.debug("* provides('{}')", type);
        if (!getDisabled()) {
            if (!configured) {
                configureManagers();
            }
            if (type.equals(LightManager.class)) {
                result = true;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<?> T) {
        T result = null; // nothing by default
        log.debug("* get({})", T.toString());
        if (!getDisabled()) {
            if (!configured) {
                configureManagers();
            }
            if (T.equals(LightManager.class)) {
                return (T) getLightManager();
            }
        }
        return result;
    }

    /**
     * Configure the common managers for anyma dmx connections. This puts the
     * common manager config in one place.
     */
    public void configureManagers() {
        log.debug("* configureManagers()");
        InstanceManager.setLightManager(getLightManager());

        if (configured) {
            log.warn("configureManagers for a second time", new Exception("traceback"));
        }
        configured = true;
    }

    protected UsbLightManager lightManager;

    public UsbLightManager getLightManager() {
        log.debug("* getLightManager()");
        UsbLightManager result = null;
        if (!getDisabled()) {
            if (lightManager == null) {
                lightManager = new UsbLightManager(this);
                InstanceManager.setLightManager(lightManager);
            }
            result = lightManager;
        }
        return result;
    }

    @Override
    protected ResourceBundle getActionModelResourceBundle() {
        log.debug("* getActionModelResourceBundle()");
        return null; //ResourceBundle.getBundle("jmri.jmrix.anyma.AnymaDMX_Bundle");
    }

    @Override
    public void dispose() {
        log.debug("* dispose()");
        InstanceManager.deregister(this, AnymaDMX_SystemConnectionMemo.class);
        if (lightManager != null) {
            InstanceManager.deregister(lightManager, UsbLightManager.class);
        }
        super.dispose();
    }

    private final static Logger log
            = LoggerFactory.getLogger(AnymaDMX_SystemConnectionMemo.class);
}
