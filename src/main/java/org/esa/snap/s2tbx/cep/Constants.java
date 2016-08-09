package org.esa.snap.s2tbx.cep;

/**
 * Helper class for constants.
 *
 * @author Cosmin Cara
 */
public class Constants {
    public static final String PARAM_MASTER_SHARE = "ms";
    public static final String PARAM_SHARE_MOUNT = "smf";
    public static final String PARAM_INPUT = "in";
    public static final String PARAM_OUTPUT = "out";
    public static final String PARAM_SLAVE_OPERATOR = "sop";
    public static final String PARAM_SLAVE_ARGUMENTS = "sargs";
    public static final String PARAM_MASTER_OPERATOR = "mop";
    public static final String PARAM_MASTER_ARGUMENTS = "margs";
    public static final String PARAM_TIMEOUT = "w";
    public static final String PARAM_USER = "u";
    public static final String PARAM_PASSWORD = "p";
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
    public static final String PLACEHOLDER_SLAVE_OPERATOR = "$sop";
    public static final String PLACEHOLDER_SLAVE_ARGS = "$sargs";
    public static final String PLACEHOLDER_INPUT_FOLDER = "$in";
    public static final String PLACEHOLDER_OUTPUT_FOLDER = "$out";
    public static final String PLACEHOLDER_MASTER_OPERATOR = "$mop";
    public static final String PLACEHOLDER_MASTER_ARGS = "$margs";
    public static final String CONST_WINDOWS = "windows";
    public static final String CONST_LINUX = "linux";
    public static final String SHELL_COMMAND_SEPARATOR = ";";
}
