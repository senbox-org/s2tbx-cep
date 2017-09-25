package org.esa.snap.s2tbx.cep;

import org.apache.commons.cli.*;
import org.esa.snap.s2tbx.cep.config.*;
import org.esa.snap.s2tbx.cep.executors.Executor;
import org.esa.snap.s2tbx.cep.executors.ExecutorType;
import org.esa.snap.s2tbx.cep.graph.GraphDescriptor;
import org.esa.snap.s2tbx.cep.graph.GraphNode;
import org.esa.snap.s2tbx.cep.graph.GraphParser;
import org.esa.snap.s2tbx.cep.util.Logger;
import org.esa.snap.s2tbx.cep.util.Sentinel2NameHelper;
import org.esa.snap.s2tbx.cep.util.Utilities;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
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
import java.util.stream.Collectors;

/**
 * Simple cloud executor for Sentinel-2 Toolbox.
 *
 * A working example (if platform configured) of command line invocation (no line breaks):
 *
 * java -jar s2tbx-cep-1.0.jar
 *          -ms \\s2tb-cep-01.c-s.ro\share
 *          -smf /mnt/share
 *          -u admin -p password
 *          -in products
 *          -out .
 *          -w 15
 *          -sops NdviOp{"-PnirFactor=1.0F -PnirSourceBand=B8 -PredFactor=1.0F -PredSourceBand=B4"}|Resample{"-PtargetResolution=60 -PresampleOnPyramidLevels=false"}
 *          -mops Mosaic{"-Pvariables=[variable[name=ndvi,expression=ndvi];variable[name=ndvi_flags,expression=ndvi_flags]]
 *                        -Pcombine=OR -PpixelSizeX=60.0 -PpixelSizeY=60.0 -PwestBound=38.5 -PnorthBound=10.3 -PeastBound=42.9 -PsouthBound=7.7
 *                        -Pcrs=PROJCS['WGS 84 / UTM zone 36N', GEOGCS['WGS 84', DATUM['World Geodetic System 1984',
 *                                  SPHEROID['WGS 84', 6378137.0, 298.257223563, AUTHORITY['EPSG','7030']], AUTHORITY['EPSG','6326']],
 *                                  PRIMEM['Greenwich', 0.0, AUTHORITY['EPSG','8901']], UNIT['degree', 0.017453292519943295], AXIS['Geodetic longitude', EAST],
 *                                  AXIS['Geodetic latitude', NORTH], AUTHORITY['EPSG','4326']], PROJECTION['Transverse_Mercator', AUTHORITY['EPSG','9807']],
 *                                  PARAMETER['central_meridian', 9.0], PARAMETER['latitude_of_origin', 0.0], PARAMETER['scale_factor', 0.9996],
 *                                  PARAMETER['false_easting', 500000.0], PARAMETER['false_northing', 0.0], UNIT['m', 1.0], AXIS['Easting', EAST], AXIS['Northing', NORTH],
 *                                  AUTHORITY['EPSG','32632']]"}
 *          -s s2tb-cep-01:linux,s2tb-cep-02:linux
 *          -t 32TMK
 *          -startdate 2017-07-01
 *          -enddate 2017-07-05
 *
 * @author Cosmin Cara
 */
public class S2TbxRemoteExecutor {

    private static Options options;
    private static Properties props;
    private static Path masterLocalFolder;
    private static Path masterSharedFolder;
    private static Path slaveMountFolder;
    private static final ExecutorService executorService;

    static {
        options = new Options();
        options.addOption(Option.builder(Constants.PARAM_CFG_FILE)
                                  .longOpt("cfg")
                                  .argName("filename")
                                  .desc("XML configuration file describing the topology and search parameters")
                                  .hasArg()
                                  .required()
                                  .build());

        options.addOption(Option.builder(Constants.PARAM_SLAVE_GRAPH_FILE)
                                  .longOpt("sg")
                                  .argName("filename")
                                  .desc("XML graph file for slave operations ")
                                  .hasArg()
                                  .required()
                                  .build());
        options.addOption(Option.builder(Constants.PARAM_MASTER_GRAPH_FILE)
                                  .longOpt("mg")
                                  .argName("filename")
                                  .desc("XML graph file for master operations ")
                                  .hasArg()
                                  .optionalArg(true)
                                  .build());
        options.addOption(Option.builder(Constants.PARAM_RESUME_MASTER)
                .longOpt("resume")
                .argName("resume.master")
                .desc("Resumes the execution for the master node only")
                .hasArg(false)
                .optionalArg(true)
                .build());
        options.addOption(Option.builder(Constants.PARAM_NO_DOWNLOAD)
                                  .longOpt("nd")
                                  .argName("")
                                  .desc("Don't perform the download")
                                  .hasArg(false)
                                  .optionalArg(true)
                                  .build());

        props = new Properties();
        try (InputStream in = S2TbxRemoteExecutor.class.getResourceAsStream("template.properties")) {
            props.load(in);
        } catch (IOException e1) {
            System.out.println("The template.properties file was not found in the jar!");
        }
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < options.getRequiredOptions().size() * 2) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar s2tbx-cep-1.0.jar", options);
            System.exit(0);
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        Path folder = new File(S2TbxRemoteExecutor.class.getProtectionDomain().getCodeSource().getLocation().getFile()).toPath();
        Logger.initialize(folder.resolveSibling("s2cep.log").toAbsolutePath().toString());
        Logger.CustomLogger logger = Logger.getRootLogger();

        printCommandLine(commandLine);

        LinkedHashMap<String, Node> nodes = new LinkedHashMap<>();
        String commonUser, commonPassword;
        String osSuffix = getOSSuffix();

        Map<String, CommandTemplate> templates = initializeCommandTemplates();

        String cfgFile = commandLine.getOptionValue(Constants.PARAM_CFG_FILE);
        Execution execution;
        try (InputStream inputStream = Files.newInputStream(Paths.get(cfgFile))) {
            execution = Serializer.deserialize(new StreamSource(inputStream));
        }
        if (execution == null) {
            throw new IllegalArgumentException("Cannot read the configuration file");
        }

        final Master master = execution.getConfiguration().getMaster();
        masterLocalFolder = Paths.get(master.getLocalFolder());
        masterSharedFolder = Paths.get(master.getShare());
        int waitTimeout = Integer.parseInt(master.getWaitTime());

        final Slave slave = execution.getConfiguration().getSlave();
        final Search search = execution.getSearch();
        slaveMountFolder = Paths.get(slave.getShareMount());
        String inputFolder = slave.getInputFolder();
        String outputFolder = slave.getOutputFolder();
        boolean useL1c = "l1c".equalsIgnoreCase(search.getProductType());
        boolean useL2a = "l2a".equalsIgnoreCase(search.getProductType());
        boolean lookForFolders = search.getLookForFolders();
        commonUser = slave.getUser();
        commonPassword = slave.getPassword();
        final String user = commonUser;
        final String password = commonPassword;

        String startDate = search.getStartDate();
        String endDate = search.getEndDate();
        String[] tiles = search.getTiles().split(" ");

        GraphDescriptor slaveGraph;
        try (InputStream inputStream = Files.newInputStream(Paths.get(commandLine.getOptionValue(Constants.PARAM_SLAVE_GRAPH_FILE)))) {
            slaveGraph = GraphParser.parse(inputStream);
        }

        GraphDescriptor masterGraph = null;
        if (commandLine.hasOption(Constants.PARAM_MASTER_GRAPH_FILE)) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(commandLine.getOptionValue(Constants.PARAM_MASTER_GRAPH_FILE)))) {
                masterGraph = GraphParser.parse(inputStream);
            }
        }

        logger.info(String.format("Searching for Sentinel-2 %s products between %s and %s, containing tiles %s",
                                  useL1c ? "L1C" : "L2A",
                                  startDate,
                                  endDate,
                                  String.join(",", tiles)));
        Path path = Constants.CONST_WINDOWS.equals(osSuffix) ?
                masterSharedFolder.resolve(inputFolder) : slaveMountFolder.resolve(inputFolder);
        String[] cmdLine = new String[] { "--out", path.toString(),
                                        "--tiles", String.join(" ", tiles),
                                        "--startdate", startDate,
                                        "--enddate", endDate,
                                        "--aws",
                                        "--store", "AWS",
                                        "--mode", "RESUME",
                                        "--preops",
                                        "--s2pt", useL1c ? "S2MSI1C" : "S2MSI2Ap" };
        if (!commandLine.hasOption(Constants.PARAM_NO_DOWNLOAD)) {
            ro.cs.products.Executor.execute(cmdLine);
        }
        logger.info(String.format("Scanning %s", path));
        List<Path> productFolders = Utilities.listFiles(path, 1);
        Files.createDirectories(masterLocalFolder.resolve(outputFolder));
        ensurePermissions(osSuffix, master.getName(), master.getSshPort(), masterLocalFolder.resolve(outputFolder), user, password);
        List<Path> inputFiles = new ArrayList<>();
        for (Path productFolder : productFolders) {
            Optional<Path> inputFile = Optional.empty();
            if (!lookForFolders) {
                if (useL1c) {
                    inputFile = Utilities.findFirst(productFolder, Sentinel2NameHelper.getL1CMetadataPatterns());
                }
                if (useL2a) {
                    inputFile = Utilities.findFirst(productFolder, Sentinel2NameHelper.getL2AMetadataPatterns());
                }
            } else {
                if (useL1c && Sentinel2NameHelper.isL1C(productFolder.toAbsolutePath().toString())) {
                    inputFile = Optional.of(productFolder);
                }
                if (useL2a && Sentinel2NameHelper.isL2A(productFolder.toAbsolutePath().toString())) {
                    inputFile = Optional.of(productFolder);
                }
            }
            if (inputFile.isPresent() && Files.exists(inputFile.get())) {
                logger.info("Found candidate input " + inputFile.get().toString());
                inputFiles.add(resolve(inputFolder, osSuffix).relativize(inputFile.get().toAbsolutePath()));
            }
        }
        logger.info(String.format("%s products found", inputFiles.size()));

        for (Node node : slave.getNodes()) {
            nodes.put(node.getName(), node);
        }

        int nodeIndex = 0;
        Map<Map.Entry<String, Integer>, List<String>> jobArguments = new HashMap<>();
        List<String> outFiles = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>(nodes.keySet());
        /*
         * Check that the shared folder is mount on slaves
         */
        CountDownLatch sharedCounter;
        /*CountDownLatch sharedCounter = new CountDownLatch(nodeNames.size());
        for (Map.Entry<String, String> node : nodes.entrySet()) {
            checkPrerequisites(node.getKey(), node.getValue(), commonUser, commonPassword, sharedCounter);
        }
        try {
            sharedCounter.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Operation timed out");
        }*/
        /*
         * Create job arguments for slaves
         */

        int counter = 0;
        GraphNode firstNode = slaveGraph.getNode(0);
        boolean isSen2CorOrThree = "Sen2Cor".equals(firstNode.getOperator()) ||
                (masterGraph != null && "Sen2Three".equals(masterGraph.getNode(0).getOperator()));
        for (Path inputFile : inputFiles) {
            try {
                counter++;
                String nodeName = nodeNames.get(nodeIndex);
                Node node = nodes.get(nodeName);
                String nodeOS = node.getOs();
                String outFile = resolve(outputFolder, nodeOS).resolve("result_" + String.valueOf(counter) + ".tif").toString();
                String transformedCmdLine = templates.get(nodeOS).slaveExecCommand;
                if (isSen2CorOrThree) {
                    transformedCmdLine = transformedCmdLine.replace(Constants.SLAVE_CMD_OUTPUT_SECTION, "");
                }
                String gptPath = node.getGptLocation();
                transformedCmdLine= String.format(transformedCmdLine
                                .replace(Constants.PLACEHOLDER_GPT, gptPath != null ? gptPath : templates.get(nodeOS).slaveGptCommand)
                                .replace(Constants.PLACEHOLDER_SHARED_FOLDER, normalizePath(slaveMountFolder, nodeOS))
                                .replace(Constants.PLACEHOLDER_INPUT_FILE, outFile)
                                .replace(Constants.PLACEHOLDER_INPUT_FOLDER, normalizePath(resolve(inputFolder, nodeOS), nodeOS))
                                .replace(Constants.PLACEHOLDER_OUTPUT_FOLDER, normalizePath(resolve(outputFolder, nodeOS), nodeOS)),
                        String.valueOf(counter),
                        normalizePath(inputFile, nodeOS),
                        "result_" + String.valueOf(counter));
                if (shouldInsertReadOp(slaveGraph)) {
                    slaveGraph.insertNode(0, "Read", "\"-Pfile=" + normalizePath(resolve(inputFolder, nodeOS).resolve(inputFile), nodeOS) + "\"");
                } else {
                    if ("Read".equals(firstNode.getOperator())) {
                        firstNode.setArgument("file", normalizePath(resolve(inputFolder, nodeOS).resolve(inputFile), nodeOS));
                    } else if ("Sen2Cor".equals(firstNode.getOperator())) {
                        firstNode.setArgument("sourceFolder", normalizePath(resolve(inputFolder, nodeOS).resolve(inputFile), nodeOS));
                    }
                }
                GraphNode writeNode = slaveGraph.getNode("Write");
                if (writeNode != null) {
                    writeNode.setArgument("file", normalizePath(resolve(outputFolder, nodeOS).resolve("result_" + String.valueOf(counter) + ".tif"), nodeOS));
                    writeNode.setArgument("formatName", node.getOutputFormat());
                }
                Files.write(masterLocalFolder.resolve("slaveGraph" + String.valueOf(counter) + ".xml"), slaveGraph.toString().getBytes());
                String finalTransformedCmdLine = transformedCmdLine;
                jobArguments.put(new AbstractMap.SimpleEntry<String, Integer>(nodeName, node.getSshPort()), new ArrayList<String>() {{
                    add(finalTransformedCmdLine);
                }});
                outFiles.add(outFile);
                nodeIndex = (nodeIndex == nodes.size() - 1) ? 0 : nodeIndex + 1;
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        /*
         * Execute the jobs on slaves
         */
        if (!commandLine.hasOption(Constants.PARAM_RESUME_MASTER)) {
            Set<Executor> processes = new HashSet<>();
            sharedCounter = new CountDownLatch(jobArguments.size());
            for (Map.Entry<Map.Entry<String, Integer>, List<String>> entry : jobArguments.entrySet()) {
                Executor sshExecutor = Executor.create(ExecutorType.SSH2,
                        entry.getKey().getKey(), entry.getKey().getValue(),
                        entry.getValue(), sharedCounter);
                sshExecutor.setUser(commonUser);
                sshExecutor.setPassword(commonPassword);
                executorService.submit(sshExecutor);
                processes.add(sshExecutor);
            }
            try {
                sharedCounter.await(waitTimeout, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.warn("Operation timed out");
            }
            processes.stream()
                     .filter(executor -> !executor.hasCompleted())
                     .forEach(executor ->
                             logger.warn("[" + executor.getHost() + "] Node still running. Its output will not be complete."));
            processes.clear();
        }
        /*
         * Execute the master job
         */
        if (masterGraph != null) {
            boolean isMosaic = "Mosaic".equals(masterGraph.getNode(0).getOperator());
            isSen2CorOrThree = "Sen2Cor".equals(masterGraph.getNode(0).getOperator()) ||
                    "Sen2Three".equals(masterGraph.getNode(0).getOperator());
            if (isMosaic) {
                Files.write(masterLocalFolder.resolve("masterGraph.xml"), masterGraph.getNode(0).parametersToString().getBytes());
            } else {
                if (isSen2CorOrThree) {
                    masterGraph.getNode(0).setArgument("sourceFolder", normalizePath(resolve(inputFolder, osSuffix), osSuffix));
                }
                Files.write(masterLocalFolder.resolve("masterGraph.xml"), masterGraph.toString().getBytes());
            }
            if (outFiles.size() > 0) {
                outFiles = outFiles.stream()
                        .map(name -> masterLocalFolder.resolve(slaveMountFolder.relativize(Paths.get(name))).toString())
                        .collect(Collectors.toList());
                outFiles.forEach(f -> ensurePermissions(osSuffix, master.getName(), master.getSshPort(), f, user, password));
            } else {
                ensurePermissions(osSuffix, master.getName(), master.getSshPort(), masterLocalFolder, user, password);
            }
            String masterCmdLine = templates.get(osSuffix).masterExecCommand;
            if (isSen2CorOrThree) {
                masterCmdLine = masterCmdLine.replace(Constants.MASTER_CMD_OUTPUT_SECTION, "");
            }
            String gptPath = master.getGptLocation();
            masterCmdLine = masterCmdLine.replace(Constants.PLACEHOLDER_GPT, gptPath != null ? gptPath : templates.get(osSuffix).masterGptCommand)
                    .replace(Constants.PLACEHOLDER_MASTER_OPT, isMosaic ? "Mosaic -p " : "")
                    .replace(Constants.PLACEHOLDER_INPUT_FOLDER, normalizePath(masterLocalFolder, osSuffix))
                    .replace(Constants.PLACEHOLDER_MASTER_INPUT, !isSen2CorOrThree ? String.join(" ", outFiles) : "")
                    .replace(Constants.PLACEHOLDER_OUTPUT_FOLDER, !isSen2CorOrThree ? normalizePath(resolve(outputFolder, osSuffix), osSuffix) : "");
            sharedCounter = new CountDownLatch(1);
            executorService.submit(Executor.create(ExecutorType.PROCESS, master.getName(), master.getSshPort(), Arrays.asList(masterCmdLine.split(" ")), sharedCounter));
            try {
                sharedCounter.await(waitTimeout * outFiles.size(), TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.warn("Operation timed out");
            }
        }
        /*
         * Do the cleanup on slaves
         */
        /*sharedCounter = new CountDownLatch(nodeNames.size());
        for (Map.Entry<String, String> node : nodes.entrySet()) {
            cleanup(node.getKey(), node.getValue(), commonUser, commonPassword, sharedCounter);
        }
        try {
            sharedCounter.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Operation timed out");
        }*/

        executorService.shutdown();
        System.exit(0);
    }

    private static Map<String, CommandTemplate> initializeCommandTemplates() {
        Map<String, CommandTemplate> templates = new HashMap<String, CommandTemplate>() {{
            put(Constants.CONST_WINDOWS, new CommandTemplate());
            put(Constants.CONST_LINUX, new CommandTemplate());
        }};

        Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString().toLowerCase();
            switch (key) {
                case Constants.SLAVE_COMMAND_LINE_TEMPLATE_LINUX:
                    templates.get(Constants.CONST_LINUX).slaveExecCommand = props.getProperty(key);
                    break;
                case Constants.SLAVE_COMMAND_LINE_TEMPLATE_WINDOWS:
                    templates.get(Constants.CONST_WINDOWS).slaveExecCommand = props.getProperty(key);
                    break;
                case Constants.MASTER_COMMAND_LINE_TEMPLATE_LINUX:
                    templates.get(Constants.CONST_LINUX).masterExecCommand = props.getProperty(key);
                    break;
                case Constants.MASTER_COMMAND_LINE_TEMPLATE_WINDOWS:
                    templates.get(Constants.CONST_WINDOWS).masterExecCommand = props.getProperty(key);
                    break;
                case Constants.MASTER_GPT_PATH_LINUX:
                    templates.get(Constants.CONST_LINUX).masterGptCommand = props.getProperty(key);
                    break;
                case Constants.MASTER_GPT_PATH_WINDOWS:
                    templates.get(Constants.CONST_WINDOWS).masterGptCommand = props.getProperty(key);
                    break;
                case Constants.SLAVE_GPT_PATH_LINUX:
                    templates.get(Constants.CONST_LINUX).slaveGptCommand = props.getProperty(key);
                    break;
                case Constants.SLAVE_GPT_PATH_WINDOWS:
                    templates.get(Constants.CONST_WINDOWS).slaveGptCommand = props.getProperty(key);
                    break;
            }
        }
        return templates;
    }

    private static void ensurePermissions(String nodeType, String nodeName, int port, Path path, String usr, String pwd) {
        Executor executor = Executor.create(ExecutorType.SSH2,
                nodeName, port,
                new ArrayList<String>() {{
                    add("chmod");
                    if (Files.isDirectory(path)) {
                        add("-R");
                    }
                    add("0777");
                    add(normalizePath(path, nodeType));
                }},
                true,
                null);
        executor.setUser(usr);
        executor.setPassword(pwd);
        executorService.submit(executor);
    }

    private static void ensurePermissions(String nodeType, String host, int port, String path, String usr, String pwd) {
        ensurePermissions(nodeType, host, port, Paths.get(path), usr, pwd);
    }

    private static void checkPrerequisites(String nodeName, int port, String nodeType, String usr, String pwd, CountDownLatch sharedCounter) {
        Executor executor = Executor.create(ExecutorType.SSH2,
                nodeName, port,
                new ArrayList<String>() {{
                    add("mkdir");
                    add(normalizePath(slaveMountFolder, nodeType));
                    add(Constants.SHELL_COMMAND_SEPARATOR);
                    add("chmod");
                    add("0777");
                    add(normalizePath(slaveMountFolder, nodeType));
                    add(Constants.SHELL_COMMAND_SEPARATOR);
                    add("mount.cifs");
                    add(normalizePath(masterSharedFolder, nodeType));
                    add(normalizePath(slaveMountFolder, nodeType));
                    add("-o");
                    add(String.format("user=%s,password=%s,file_mode=0777,dir_mode=0777,noperm", usr, pwd));
                }},
                true,
                sharedCounter);
        executor.setUser(usr);
        executor.setPassword(pwd);
        executorService.submit(executor);
    }

    private static void cleanup(String nodeName, int port, String nodeType, String usr, String pwd, CountDownLatch sharedCounter) {
        Executor executor = Executor.create(ExecutorType.SSH2,
                nodeName, port,
                new ArrayList<String>() {{
                    add("umount");
                    add(normalizePath(slaveMountFolder, nodeType));
                    add(Constants.SHELL_COMMAND_SEPARATOR);
                    add("rmdir");
                    add(normalizePath(slaveMountFolder, nodeType));
                }},
                true,
                sharedCounter);
        executor.setUser(usr);
        executor.setPassword(pwd);
        executorService.submit(executor);
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

    private static void printCommandLine(CommandLine cmdLine) {
        for (Option option : cmdLine.getOptions()) {
            Logger.getRootLogger().info(option.getOpt() + "=" + option.getValue());
        }
    }

    private static boolean shouldInsertReadOp(GraphDescriptor graph) {
        String operator = graph.getNode(0).getOperator();
        return !("Read".equals(operator) || "Sen2Cor".equals(operator));
    }

    private static class CommandTemplate {
        String masterGptCommand;
        String masterExecCommand;
        String slaveGptCommand;
        String slaveExecCommand;
    }
}
