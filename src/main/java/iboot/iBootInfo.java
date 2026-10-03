package iboot;

import ghidra.app.util.bin.ByteProvider;
import ghidra.util.exception.InvalidInputException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class iBootInfo {
    // The format of 'iBoot' files changed as of version '6603', and again as of
    // macOS 27 (or maybe macOS 26.4).
    private static final long DESCRIPTION_OFFSET_IBOOT = 0x200;
    private static final long DESCRIPTION_OFFSET_MBOOT = 0x280;
    private static final int DESCRIPTION_SIZE = 0x40;
    private static final long EDITION_OFFSET_IBOOT = 0x240;
    private static final long EDITION_OFFSET_MBOOT = 0x2C0;
    private static final int EDITION_LENGTH = 0x40;
    private static final long VERSION_OFFSET_IBOOT = 0x280;
    private static final long VERSION_OFFSET_MBOOT = 0x300;
    private static final long VERSION_SIZE = 0x40;
    private static final long BASE_ADDRESS_OFFSET_IBOOT_OLD = 0x318;
    private static final long BASE_ADDRESS_OFFSET_IBOOT_NEW = 0x300;
    private static final long BASE_ADDRESS_OFFSET_MBOOT = 0x380;
    private static final int BASE_ADDRESS_SIZE = 8;
    private static final int NEW_VERSION_IBOOT = 6603;

    private final String description;
    private final String description_iBoot;
    private final String description_mBoot;
    private final String edition;
    private final String edition_iBoot;
    private final String edition_mBoot;
    private final String version;
    private final String version_iBoot;
    private final String version_mBoot;
    private final String version_prefix;
    private final byte[] baseAddressArea;
    private final byte[] baseAddressArea_iBoot;
    private final byte[] baseAddressArea_mBoot;

    public iBootInfo(ByteProvider provider) throws IOException, InvalidInputException {
        this.description_iBoot =
            new String(provider.readBytes(DESCRIPTION_OFFSET_IBOOT, DESCRIPTION_SIZE),
                       StandardCharsets.US_ASCII);
        this.description_mBoot =
            new String(provider.readBytes(DESCRIPTION_OFFSET_MBOOT, DESCRIPTION_SIZE),
                       StandardCharsets.US_ASCII);
        this.edition_iBoot =
            new String(provider.readBytes(EDITION_OFFSET_IBOOT, EDITION_LENGTH),
                       StandardCharsets.US_ASCII);
        this.edition_mBoot =
            new String(provider.readBytes(EDITION_OFFSET_MBOOT, EDITION_LENGTH),
                       StandardCharsets.US_ASCII);
        this.version_iBoot =
            new String(provider.readBytes(VERSION_OFFSET_IBOOT, VERSION_SIZE),
                       StandardCharsets.US_ASCII);
        this.version_mBoot =
            new String(provider.readBytes(VERSION_OFFSET_MBOOT, VERSION_SIZE),
                       StandardCharsets.US_ASCII);

        long minimalBaseAddressOffset =
            Math.min(BASE_ADDRESS_OFFSET_IBOOT_NEW, BASE_ADDRESS_OFFSET_IBOOT_OLD);
        long maximalBaseAddressOffset =
            Math.max(BASE_ADDRESS_OFFSET_IBOOT_NEW, BASE_ADDRESS_OFFSET_IBOOT_OLD);
        this.baseAddressArea_iBoot =
            provider.readBytes(minimalBaseAddressOffset,
                               maximalBaseAddressOffset - minimalBaseAddressOffset + BASE_ADDRESS_SIZE);
        this.baseAddressArea_mBoot =
            provider.readBytes(BASE_ADDRESS_OFFSET_MBOOT, BASE_ADDRESS_SIZE);

        if (version_iBoot.startsWith(Consts.VERSION_PREFIX_IBOOT)) {
            this.version_prefix = Consts.VERSION_PREFIX_IBOOT;
            this.version = this.version_iBoot;
            this.description = this.description_iBoot;
            this.edition = this.edition_iBoot;
            this.baseAddressArea = this.baseAddressArea_iBoot;
        } else if (version_mBoot.startsWith(Consts.VERSION_PREFIX_MBOOT)) {
            this.version_prefix = Consts.VERSION_PREFIX_MBOOT;
            this.version = this.version_mBoot;
            this.description = this.description_mBoot;
            this.edition = this.edition_mBoot;
            this.baseAddressArea = this.baseAddressArea_mBoot;
        } else {
            throw new InvalidInputException();
        }
    }

    /**
     * @return the binary's iBoot stage (SecureROM, LLB, iBoot, ...)
     * @throws InvalidInputException in case the binary is not a valid iBoot stage
     */
    public String getStage() throws InvalidInputException {
        for (String stage : Consts.STAGES) {
            if (description.startsWith(stage + " for ")) {
                return stage;
            }
        }
        throw new InvalidInputException();
    }

    /**
     * @return the name of the device the binary was built for. In SecureROM images this is the name of SoC, otherwise
     *         the name of the board
     * @throws InvalidInputException in case the binary is not a valid iBoot stage
     */
    public String getDevice() throws InvalidInputException {
        String descriptionWithoutType = this.description.substring((this.getStage() + " for ").length());
        return descriptionWithoutType.substring(0, descriptionWithoutType.indexOf(',')).toLowerCase();
    }

    /**
     * @return whether or not the binary is supported by this extension
     * @throws InvalidInputException in case the binary is not a valid iBoot stage
     */
    public boolean is64Bit() throws InvalidInputException {
        String device = this.getDevice();
        for (String unsupportedDevice : Consts.DEVICES_32BIT) {
            if (device.equals(unsupportedDevice)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the binary's iBoot version
     */
    public String getVersion() {
        return this.version.substring(version_prefix.length());
    }

    /**
     * @return the binary's "edition", this is normally "RELEASE"
     */
    public String getEdition() {
        return this.edition;
    }

    /**
     * @return the binary's base address as deduced by it's version
     * @throws InvalidInputException in case the binary is not a valid iBoot stage
     */
    public long getBaseAddress() throws InvalidInputException {
        String versionString = this.getVersion();
        if (versionString.contains(".")) {
            versionString = versionString.split("\\.")[0];
        }

        if (Integer.parseInt(versionString) < NEW_VERSION_IBOOT) {
            return Utils.toLittleEndianLong(
                this.baseAddressArea,
                (int)(BASE_ADDRESS_OFFSET_IBOOT_OLD - BASE_ADDRESS_OFFSET_IBOOT_NEW),
                BASE_ADDRESS_SIZE);
        } else {
            return Utils.toLittleEndianLong(this.baseAddressArea, 0, BASE_ADDRESS_SIZE);
        }
    }
}
