package org.esa.snap.s2tbx.cep.util;

import java.util.regex.Pattern;

/**
 * @author Cosmin Cara
 */
public class Sentinel2NameHelper {
    private static final Pattern L1C_Product_V13 =
            Pattern.compile("(S2[A-B])_(OPER)_(PRD)_(MSIL1C)_(PDMC)_(\\d{8}T\\d{6})_(R\\d{3})_(V\\d{8}T\\d{6})_(\\d{8}T\\d{6})(?:.SAFE)?");
    private static final Pattern L1C_Product_V14 =
            Pattern.compile("(S2[A-B])_(MSIL1C)_(\\d{8}T\\d{6})_(N\\d{4})_(R\\d{3})_(T\\d{2}\\w{3})_(\\d{8}T\\d{6})(?:.SAFE)?");
    private static final Pattern L2A_Product_V13 =
            Pattern.compile("(S2[A-B])_(OPER|USER)_(PRD)_(MSIL2A)_(PDMC)_(\\d{8}T\\d{6})_(R\\d{3})_(V\\d{8}T\\d{6})_(\\d{8}T\\d{6})(?:.SAFE)?");
    private static final Pattern L2A_Product_V14 =
            Pattern.compile("(S2[A-B])_(MSIL2A)_(\\d{8}T\\d{6})_(N\\d{4})_(R\\d{3})_(T\\d{2}\\w{3})_(\\d{8}T\\d{6})(?:.SAFE)?");
    private static final Pattern L1C_Tile_V13 =
            Pattern.compile("(S2[A-B])_(OPER)_(MSI)_(L1C)_(TL)_(\\w{3})__(\\d{8}T\\d{6})_(A\\d{6})_(T\\d{2}\\w{3})_(N\\d{2}.\\d{2})");
    private static final Pattern L1C_Metadata_V13 =
            Pattern.compile(".*(S2[A-B])_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9]{3})L1C_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})([A-Z|0-9|_]+).(xml|XML)");
    private static final Pattern L1C_Metadata_V14 =
            Pattern.compile("MTD_MSIL1C.xml");
    private static final Pattern L2A_Metadata_V13 =
            Pattern.compile(".*(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9]{3})L2A_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})([A-Z|0-9|_]+).(xml|XML)");
    private static final Pattern L2A_Metadata_V14 =
            Pattern.compile("MTD_MSIL2A.xml");

    public static boolean isL1C(String productFolder) {
        return L1C_Product_V13.matcher(productFolder).matches() || L1C_Product_V14.matcher(productFolder).matches() ||
            L1C_Tile_V13.matcher(productFolder).matches();
    }

    public static boolean isL2A(String productFolder) {
        return L2A_Product_V13.matcher(productFolder).matches() || L2A_Product_V14.matcher(productFolder).matches();
    }

    public static Pattern[] getL1CMetadataPatterns() {
        return new Pattern[] {L1C_Metadata_V13, L1C_Metadata_V14 };
    }

    public static Pattern[] getL2AMetadataPatterns() {
        return new Pattern[] {L2A_Metadata_V13, L2A_Metadata_V14 };
    }
}
