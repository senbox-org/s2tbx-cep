package org.esa.snap.s2tbx.cep;

/**
 * Helper class for constants.
 *
 * @author Cosmin Cara
 */
public class Constants {
    public static final String DEFAULT_PERMISSIONS = "rwxr-xr-x";
    static final String SLAVE_COMMAND_LINE_TEMPLATE_LINUX = "linux.slave.command.line.template";
    static final String SLAVE_COMMAND_LINE_TEMPLATE_WINDOWS = "windows.slave.command.line.template";
    static final String MASTER_COMMAND_LINE_TEMPLATE_LINUX = "linux.master.command.line.template";
    static final String MASTER_COMMAND_LINE_TEMPLATE_WINDOWS = "windows.master.command.line.template";
    static final String MASTER_GPT_PATH_LINUX = "linux.master.gpt.path";
    static final String MASTER_GPT_PATH_WINDOWS = "windows.master.gpt.path";
    static final String SLAVE_GPT_PATH_LINUX = "linux.slave.gpt.path";
    static final String SLAVE_GPT_PATH_WINDOWS = "windows.slave.gpt.path";
    static final String PLACEHOLDER_GPT = "$gpt";
    static final String PLACEHOLDER_INPUT_FOLDER = "$in";
    static final String PLACEHOLDER_INPUT_FILE = "$inputFile";
    static final String PLACEHOLDER_SHARED_FOLDER = "$smf";
    static final String PLACEHOLDER_OUTPUT_FOLDER = "$out";
    static final String PLACEHOLDER_MASTER_INPUT = "$files";
    static final String PLACEHOLDER_MASTER_OPT = "$opt ";
    static final String CONST_WINDOWS = "windows";
    static final String CONST_LINUX = "linux";
    public static final String SHELL_COMMAND_SEPARATOR = ";";
    static final String MASTER_CMD_OUTPUT_SECTION = " -f GeoTIFF-BigTIFF -t $out/master.tif";
    static final String SLAVE_CMD_OUTPUT_SECTION = " -f GeoTIFF-BigTIFF -t $out/%s.tif";
    static final String PARAM_CFG_FILE = "cfg";
    static final String PARAM_SLAVE_GRAPH_FILE = "sg";
    static final String PARAM_MASTER_GRAPH_FILE = "mg";
    static final String PARAM_RESUME_MASTER = "r";
    static final String PARAM_NO_DOWNLOAD = "nd";
}
