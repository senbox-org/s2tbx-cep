package org.esa.snap.s2tbx.cep.executors;

import org.esa.snap.s2tbx.cep.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Base class for process executors
 *
 * @author Cosmin Cara
 */
public abstract class Executor implements Runnable {

    protected String host;
    protected int port;
    protected String user;
    protected String password;
    protected volatile boolean isStopped;
    protected volatile boolean wasCancelled;
    protected List<String> arguments;
    protected Logger.CustomLogger logger;
    protected volatile int retCode = Integer.MAX_VALUE;
    protected boolean asSuperUser;
    protected CountDownLatch counter;

    public static Executor create(ExecutorType type, String host, int port, List<String> arguments, CountDownLatch synchronisationCounter) {
        return create(type, host, port, arguments, false, synchronisationCounter, "exec");
    }

    public static Executor create(ExecutorType type, String host, int port, List<String> arguments, boolean asSuperUser, CountDownLatch synchronisationCounter) {
        return create(type, host, port, arguments, asSuperUser, synchronisationCounter, "exec");
    }

    public static Executor create(ExecutorType type, String host, int port, List<String> arguments, boolean asSuperUser, CountDownLatch synchronisationCounter, String mode) {
        Executor executor = null;
        switch (type) {
            case PROCESS:
                executor = new ProcessExecutor(host, arguments, asSuperUser, synchronisationCounter);
                break;
            case SSH2:
                executor = new SSHExecutor(host, port, arguments, asSuperUser, synchronisationCounter, mode);
                break;
        }
        return executor;
    }

    /**
     * Constructs an executor with given arguments and a shared latch counter.
     *
     * @param host      The execution node name
     * @param args          The arguments
     * @param sharedCounter The shared latch counter.
     */
    public Executor(String host, int port, List<String> args, boolean asSU, CountDownLatch sharedCounter) {
        this.isStopped = false;
        this.wasCancelled = false;
        this.host = host;
        this.port = port;
        this.arguments = args;
        this.counter = sharedCounter;
        this.asSuperUser = asSU;
        logger = Logger.getRootLogger();
    }

    /**
     * Returns the process exit code.
     */
    public int getReturnCode() { return this.retCode; }

    public String getHost() { return this.host; }

    public int getPort() { return this.port; }

    /**
     * Signals the stop of the execution.
     */
    public void stop() {
        this.isStopped = true;
    }

    /**
     * Checks if the process is/has stopped.
     */
    public boolean isStopped() {
        return this.isStopped;
    }

    public boolean hasCompleted() { return this.retCode != Integer.MAX_VALUE; }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void run() {
        Instant start = Instant.now();
        try {
            retCode = execute(null, true);
            logger.info("[%s] completed %s", host, retCode == 0 ? "OK" : "NOK (code " + String.valueOf(retCode) + ")");
            if (this.counter != null) {
                counter.countDown();
                logger.info(String.format("Active nodes: %s", this.counter.getCount()));
            }
        } catch (Exception e) {
            retCode = -255;
            logger.error("[%s] produced an error: %s", host, e.getMessage());
        } finally {
            Instant end = Instant.now();
            long seconds = Duration.between(start, end).getSeconds();
            long hours = seconds / 3600;
            seconds -= hours * 3600;
            long minutes = seconds / 60;
            seconds -= minutes * 60;
            logger.info(String.format("[%s] Execution took %02dh%02dm%02ds", host, hours, minutes, seconds));
        }
    }

    /**
     * Performs the actual execution, optionally logging the execution output and returning the output as a list
     * of messages.
     *
     * @param outLines      The holder for output messages
     * @param logMessages   If <code>true</code>, the output will be logged
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public abstract int execute(List<String> outLines, boolean logMessages) throws Exception;
}
