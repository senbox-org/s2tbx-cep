package org.esa.snap.s2tbx.cep;

import org.apache.commons.cli.*;
import org.esa.snap.s2tbx.cep.executors.Executor;
import org.esa.snap.s2tbx.cep.executors.ExecutorType;
import org.esa.snap.s2tbx.cep.util.Logger;
import org.esa.snap.s2tbx.cep.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple cloud executor for Sentinel-2 Toolbox.
 *
 * @author Cosmin Cara
 */
public class S2TbxRemoteExecutor {

    private static Options options;
    private static Properties props;
    private static Path masterSharedFolder;
    private static Path slaveMountFolder;

    static {
        options = new Options();
        options.addOption(Option.builder(Constants.PARAM_SHARE_MOUNT)
                .longOpt("slavemountfolder")
                .argName("slave.mount.folder")
                .desc("The slave local folder where the master shared folder is mount")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_MASTER_SHARE)
                .longOpt("mastershare")
                .argName("master.shared.folder")
                .desc("The shared folder (residing on the master node) that is visible to all slave nodes")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_INPUT)
                .longOpt("input")
                .argName("input.folder")
                .desc("The folder (relative to the common shared folder) from which the products are to be processed")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_OUTPUT)
                .longOpt("output")
                .argName("output.folder")
                .desc("The folder (relative to the common shared folder) in which the processed products are to be placed")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_SLAVE_OPERATOR)
                .longOpt("slaveOp")
                .argName("slave.operator")
                .desc("Name of the slave SNAP operator")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_SLAVE_ARGUMENTS)
                .longOpt("slaveOpArgs")
                .argName("slave.operator.arguments")
                .desc("Parameters of the slave SNAP operator")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_MASTER_OPERATOR)
                .longOpt("masterOp")
                .argName("master.operator")
                .desc("Name of the master SNAP operator")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_MASTER_ARGUMENTS)
                .longOpt("masterOpArgs")
                .argName("master.operator.arguments")
                .desc("Parameters of the master SNAP operator")
                .hasArg()
                .required()
                .build());
        options.addOption(Option.builder(Constants.PARAM_TIMEOUT)
                .longOpt("wait")
                .argName("wait.for.slave")
                .desc("The amount of time (in minutes) to wait for a slave node to complete its execution")
                .hasArg()
                .optionalArg(true)
                .build());
        options.addOption(Option.builder(Constants.PARAM_USER)
                .longOpt("user")
                .argName("user")
                .desc("The user name used to connect to remote slave nodes")
                .hasArg()
                .optionalArg(true)
                .build());
        options.addOption(Option.builder(Constants.PARAM_PASSWORD)
                .longOpt("password")
                .argName("password")
                .desc("The password of the user used to connect to remote slave nodes")
                .hasArg()
                .optionalArg(true)
                .build());
        Path folder = new File(S2TbxRemoteExecutor.class.getProtectionDomain().getCodeSource().getLocation().getFile()).toPath();
        props = new Properties();
        try (FileInputStream fis = new FileInputStream(folder.resolve("topology.config").toFile())) {
            props.load(fis);
        } catch (IOException e) {
            try (InputStream in = S2TbxRemoteExecutor.class.getResourceAsStream("topology.config")) {
                props.load(in);
            } catch (IOException e1) {
                System.out.println("The topology.config file was not found in the jar!");
            }
            System.out.println("The topology.config file could not be found alongside the jar! Will use the embedded one.");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < options.getRequiredOptions().size() * 2) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("s2tbx-cep", options);
            System.exit(0);
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        Path folder = new File(S2TbxRemoteExecutor.class.getProtectionDomain().getCodeSource().getLocation().getFile()).toPath();
        Logger.initialize(folder.resolve("s2cep.log").toAbsolutePath().toString());
        Logger.CustomLogger logger = Logger.getRootLogger();

        LinkedHashMap<String, String> nodes = new LinkedHashMap<>();
        String commonUser = null, commonPassword = null;
        String osSuffix = getOSSuffix();

        Map<String, CommandTemplate> templates = new HashMap<String, CommandTemplate>() {{
            put(Constants.CONST_WINDOWS, new CommandTemplate());
            put(Constants.CONST_LINUX, new CommandTemplate());
        }};

        Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString().toLowerCase();
            if (key.startsWith(Constants.KEY_SLAVE_NODE_PREFIX)) {
                nodes.put(props.getProperty(key), key.substring(key.lastIndexOf(".") + 1));
            } else if (key.equals(Constants.SLAVE_COMMAND_LINE_TEMPLATE_LINUX)) {
                templates.get(Constants.CONST_LINUX).slaveExecCommand = props.getProperty(key);
            } else if (key.equals(Constants.SLAVE_COMMAND_LINE_TEMPLATE_WINDOWS)) {
                templates.get(Constants.CONST_WINDOWS).slaveExecCommand = props.getProperty(key);
            } else if (key.equals(Constants.MASTER_COMMAND_LINE_TEMPLATE_LINUX)) {
                templates.get(Constants.CONST_LINUX).masterExecCommand = props.getProperty(key);
            } else if (key.equals(Constants.MASTER_COMMAND_LINE_TEMPLATE_WINDOWS)) {
                templates.get(Constants.CONST_WINDOWS).masterExecCommand = props.getProperty(key);
            } else if (key.equals(Constants.MASTER_GPT_PATH_LINUX)) {
                templates.get(Constants.CONST_LINUX).masterGptCommand = props.getProperty(key);
            } else if (key.equals(Constants.MASTER_GPT_PATH_WINDOWS)) {
                templates.get(Constants.CONST_WINDOWS).masterGptCommand = props.getProperty(key);
            } else if (key.equals(Constants.SLAVE_GPT_PATH_LINUX)) {
                templates.get(Constants.CONST_LINUX).slaveGptCommand = props.getProperty(key);
            } else if (key.equals(Constants.SLAVE_GPT_PATH_WINDOWS)) {
                templates.get(Constants.CONST_WINDOWS).slaveGptCommand = props.getProperty(key);
            } else if (key.equals(Constants.SLAVE_USERNAME)) {
                commonUser = props.getProperty(key);
            } else if (key.equals(Constants.SLAVE_PASSWORD)) {
                commonPassword = props.getProperty(key);
            }
        }
        masterSharedFolder = Paths.get(commandLine.getOptionValue(Constants.PARAM_MASTER_SHARE));
        slaveMountFolder = Paths.get(commandLine.getOptionValue(Constants.PARAM_SHARE_MOUNT));

        String inputFolder = commandLine.getOptionValue(Constants.PARAM_INPUT);
        String outputFolder = commandLine.getOptionValue(Constants.PARAM_OUTPUT);
        String slaveOpName = commandLine.getOptionValue(Constants.PARAM_SLAVE_OPERATOR);
        String slaveOpArgs = commandLine.getOptionValue(Constants.PARAM_SLAVE_ARGUMENTS).replaceAll("\"", "");
        String masterOpName = commandLine.getOptionValue(Constants.PARAM_MASTER_OPERATOR);
        String masterOpArgs = commandLine.getOptionValue(Constants.PARAM_MASTER_ARGUMENTS).replaceAll("\"", "");
        int waitTimeout;
        if (commandLine.hasOption(Constants.PARAM_TIMEOUT)) {
            waitTimeout = Integer.parseInt(commandLine.getOptionValue(Constants.PARAM_TIMEOUT));
        } else {
            waitTimeout = Constants.DEFAULT_TIMEOUT;
        }
        if (commandLine.hasOption(Constants.PARAM_USER)) {
            commonUser = commandLine.getOptionValue(Constants.PARAM_USER);
        }
        if (commandLine.hasOption(Constants.PARAM_PASSWORD)) {
            commonPassword = commandLine.getOptionValue(Constants.PARAM_PASSWORD);
        }
        logger.info("Retrieving the list of available operators");
        List<String> arg = new ArrayList<>();
        arg.add(templates.get(osSuffix).masterGptCommand);
        Executor gptExecutor = Executor.create(ExecutorType.PROCESS, "master", arg, null);
        List<String> outLines = new ArrayList<>();
        gptExecutor.execute(outLines, false);
        Set<String> opNames = extractOperatorNames(outLines);
        if (!opNames.contains(slaveOpName)) {
            logger.error("Operator %s is not registered", slaveOpName);
            System.exit(-3);
        }
        if (!opNames.contains(masterOpName)) {
            logger.error("Operator %s is not registered", masterOpName);
            System.exit(-4);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Path> productFolders = Utilities.listFiles(
                Constants.CONST_WINDOWS.equals(osSuffix) ?
                        masterSharedFolder.resolve(inputFolder) :
                        slaveMountFolder.resolve(inputFolder),
                1);
        int nodeIndex = 0;
        Map<String, List<String>> jobArguments = new HashMap<>();
        List<String> outFiles = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>(nodes.keySet());
        for (Path productFolder : productFolders) {
            try {
                Optional<Path> xmlFile = Utilities.findFirst(productFolder, ".xml");
                if (xmlFile.isPresent() && Files.exists(xmlFile.get())) {
                    logger.info("Found candidate metadata file " + xmlFile.get().toString());
                    String node = nodeNames.get(nodeIndex);
                    String nodeOS = nodes.get(node);
                    Path relativeXmlPath = (resolve(inputFolder, osSuffix)).relativize(xmlFile.get().toAbsolutePath());
                    final String transformedCmdLine = String.format(
                            templates.get(nodeOS).slaveExecCommand
                                    .replace(Constants.PLACEHOLDER_GPT, templates.get(nodeOS).slaveGptCommand)
                                    .replace(Constants.PLACEHOLDER_SLAVE_OPERATOR, slaveOpName)
                                    .replace(Constants.PLACEHOLDER_SLAVE_ARGS, slaveOpArgs)
                                    .replace(Constants.PLACEHOLDER_INPUT_FOLDER, normalizePath(resolve(inputFolder, nodeOS), nodeOS))
                                    .replace(Constants.PLACEHOLDER_OUTPUT_FOLDER, normalizePath(resolve(outputFolder, nodeOS), nodeOS)),
                            normalizePath(relativeXmlPath, nodeOS),
                            "result_" + String.valueOf(nodeIndex + 1));
                    jobArguments.put(node, new ArrayList<String>() {{
                        add(transformedCmdLine);
                    }});
                    outFiles.add(resolve(outputFolder, nodeOS).resolve("result_" + String.valueOf(nodeIndex + 1) + ".tif").toString());
                    nodeIndex = (nodeIndex == nodes.size() - 1) ? 0 : nodeIndex + 1;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        CountDownLatch sharedCounter = new CountDownLatch(jobArguments.size());
        for (Map.Entry<String, List<String>> entry : jobArguments.entrySet()) {
            Executor sshExecutor = Executor.create(ExecutorType.SSH2, entry.getKey(), entry.getValue(), sharedCounter);
            sshExecutor.setUser(commonUser);
            sshExecutor.setPassword(commonPassword);
            executorService.submit(sshExecutor);
        }
        try {
            sharedCounter.await(waitTimeout, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Operation timed out");
        }
        String masterCmdLine = templates.get(osSuffix).masterExecCommand
                                        .replace(Constants.PLACEHOLDER_GPT, templates.get(osSuffix).masterGptCommand)
                                        .replace(Constants.PLACEHOLDER_MASTER_OPERATOR, masterOpName)
                                        .replace(Constants.PLACEHOLDER_MASTER_ARGS, masterOpArgs + String.join(" ", outFiles))
                                        .replace(Constants.PLACEHOLDER_OUTPUT_FOLDER, normalizePath(resolve(outputFolder, osSuffix), osSuffix));
        sharedCounter = new CountDownLatch(1);
        executorService.submit(Executor.create(ExecutorType.PROCESS, "master", Arrays.asList(masterCmdLine.split(" ")), sharedCounter));
        try {
            sharedCounter.await(waitTimeout * outFiles.size(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Operation timed out");
        }
        executorService.shutdown();
        System.exit(0);
    }

    private static Set<String> extractOperatorNames(List<String> lines) {
        Set<String> opNames = new HashSet<>();
        boolean markBegin = false;
        for (String line : lines) {
            if (markBegin) {
                opNames.add(line.trim().substring(0, line.trim().indexOf(" ")).trim());
            } else {
                markBegin = line.trim().startsWith("Operators:");
            }
        }
        return opNames;
    }

    private static String getOSSuffix() {
        String sysName = System.getProperty("os.name").toLowerCase();
        if (sysName.contains(Constants.CONST_WINDOWS)) {
            return Constants.CONST_WINDOWS;
        } else if (sysName.contains(Constants.CONST_LINUX)) {
            return Constants.CONST_LINUX;
        } else {
            throw new UnsupportedOperationException("OS not supported");
        }
    }

    private static Path resolve(String path, String os) {
        return Constants.CONST_WINDOWS.equals(os) ?
                    masterSharedFolder.resolve(path) :
                    slaveMountFolder.resolve(path);
    }

    private static String normalizePath(Path path, String os) {
        String stringPath = path.toString();
        if (stringPath.endsWith("\\") || stringPath.endsWith("/")) {
            stringPath = stringPath.substring(0, stringPath.length() - 1);
        }
        return Constants.CONST_WINDOWS.equals(os) ?
                stringPath.replaceAll("/", "\\") :
                stringPath.replaceAll("\\\\", "/");
    }

    private static class CommandTemplate {
        public String masterGptCommand;
        public String masterExecCommand;
        public String slaveGptCommand;
        public String slaveExecCommand;
    }
}
