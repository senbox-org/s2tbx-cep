package org.esa.snap.s2tbx.cep.executors;

import org.esa.snap.s2tbx.cep.util.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Base class for process executors
 *
 * @author Cosmin Cara
 */
public abstract class Executor implements Runnable {

    protected String host;
    protected String user;
    protected String password;
    protected volatile boolean isStopped;
    protected volatile boolean wasCancelled;
    protected List<String> arguments;
    protected Logger.CustomLogger logger;
    protected int retCode;
    protected boolean asSuperUser;
    protected CountDownLatch counter;

    public static Executor create(ExecutorType type, String host, List<String> arguments, CountDownLatch synchronisationCounter) {
        return create(type, host, arguments, false, synchronisationCounter, "exec");
    }

    public static Executor create(ExecutorType type, String host, List<String> arguments, boolean asSuperUser, CountDownLatch synchronisationCounter) {
        return create(type, host, arguments, asSuperUser, synchronisationCounter, "exec");
    }

    public static Executor create(ExecutorType type, String host, List<String> arguments, boolean asSuperUser, CountDownLatch synchronisationCounter, String mode) {
        Executor executor = null;
        switch (type) {
            case PROCESS:
                executor = new ProcessExecutor(host, arguments, asSuperUser, synchronisationCounter);
                break;
            case SSH2:
                executor = new SSHExecutor(host, arguments, asSuperUser, synchronisationCounter, mode);
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
    public Executor(String host, List<String> args, boolean asSU, CountDownLatch sharedCounter) {
        this.isStopped = false;
        this.wasCancelled = false;
        this.host = host;
        this.arguments = args;
        this.counter = sharedCounter;
        this.asSuperUser = asSU;
        logger = Logger.getRootLogger();
    }

    /**
     * Returns the process exit code.
     */
    public int getReturnCode() { return this.retCode; }

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

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void run() {
        try {
            retCode = execute(null, true);
            logger.info("[[%s]] completed %s", host, retCode == 0 ? "OK" : "NOK (code " + String.valueOf(retCode) + ")");
        } catch (Exception e) {
            logger.error("[[%s]] produced an error: %s", host, e.getMessage());
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
