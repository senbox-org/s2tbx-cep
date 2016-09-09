package org.esa.snap.s2tbx.cep;

/**
 * Helper class for constants.
 *
 * @author Cosmin Cara
 */
public class Constants {
    public static final String PARAM_MASTER_FOLDER = "mf";
    public static final String PARAM_MASTER_SHARE = "ms";
    public static final String PARAM_SHARE_MOUNT = "smf";
    public static final String PARAM_INPUT = "in";
    public static final String PARAM_INPUT_LOOKFOR_FOLDERS = "f";
    public static final String PARAM_OUTPUT = "out";
    public static final String PARAM_SLAVE_OPERATORS = "sops";
    public static final String PARAM_MASTER_OPERATORS = "mops";
    public static final String PARAM_TIMEOUT = "w";
    public static final String PARAM_USER = "u";
    public static final String PARAM_PASSWORD = "p";
    public static final String PARAM_SLAVES = "s";
    public static final String PARAM_RESUME_MASTER = "r";
    public static final String PARAM_USE_L1C = "l1c";
    public static final String PARAM_USE_L2A = "l2a";
    public static final int DEFAULT_TIMEOUT = 15;
    public static final String DEFAULT_PERMISSIONS = "rwxr-xr-x";
    public static final String KEY_SLAVE_NODE_PREFIX = "slave.node";
    public static final String SLAVE_COMMAND_LINE_TEMPLATE_LINUX = "slave.command.line.template.linux";
    public static final String SLAVE_COMMAND_LINE_TEMPLATE_WINDOWS = "slave.command.line.template.windows";
    public static final String MASTER_COMMAND_LINE_TEMPLATE_LINUX = "master.command.line.template.linux";
    public static final String MASTER_COMMAND_LINE_TEMPLATE_WINDOWS = "master.command.line.template.windows";
    public static final String MASTER_GPT_PATH_LINUX = "master.gpt.path.linux";
    public static final String MASTER_GPT_PATH_WINDOWS = "master.gpt.path.windows";
    public static final String SLAVE_GPT_PATH_LINUX = "slave.gpt.path.linux";
    public static final String SLAVE_GPT_PATH_WINDOWS = "slave.gpt.path.windows";
    public static final String SLAVE_USERNAME = "slave.username";
    public static final String SLAVE_PASSWORD = "slave.password";
    public static final String PLACEHOLDER_GPT = "$gpt";
    public static final String PLACEHOLDER_INPUT_FOLDER = "$in";
    public static final String PLACEHOLDER_INPUT_FILE = "$inputFile";
    public static final String PLACEHOLDER_SHARED_FOLDER = "$smf";
    public static final String PLACEHOLDER_OUTPUT_FOLDER = "$out";
    public static final String PLACEHOLDER_MASTER_INPUT = "$files";
    public static final String PLACEHOLDER_MASTER_OPT = "$opt";
    public static final String CONST_WINDOWS = "windows";
    public static final String CONST_LINUX = "linux";
    public static final String SHELL_COMMAND_SEPARATOR = ";";
}
