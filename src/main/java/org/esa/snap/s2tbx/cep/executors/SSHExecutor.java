package org.esa.snap.s2tbx.cep.executors;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Executor class based on JSch. It uses SSH2 for remote host connection and command invocation.
 *
 * @author Cosmin Cara
 */
public class SSHExecutor extends Executor {

    private String mode;

    public SSHExecutor(String host, List<String> args, CountDownLatch sharedCounter) {
        super(host, args, sharedCounter);
        this.mode = "exec";
    }

    public SSHExecutor(String host, List<String> args, CountDownLatch sharedCounter, String mode) {
        super(host, args, sharedCounter);
        this.mode = mode;
    }

    @Override
    public int execute(List<String> outLines, boolean logMessages) throws IOException, InterruptedException, JSchException {
        BufferedReader outReader;
        int ret = -1;
        Session session = null;
        Channel channel = null;
        try {
            String cmdLine = String.join(" ", arguments);
            logger.info("Invoking on " + host + ": " + cmdLine);
            JSch jSch = new JSch();
            //jSch.setKnownHosts("D:\\known_hosts");
            session = jSch.getSession(this.user, this.host, 22);
            session.setUserInfo(new UserInfo(this.password));
            session.setPassword(password.getBytes());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel(this.mode);
            ((ChannelExec) channel).setCommand(cmdLine);
            channel.setInputStream(null);
            InputStream inputStream = channel.getInputStream();
            ((ChannelExec) channel).setErrStream(new ByteArrayOutputStream() {
                @Override
                public synchronized void write(byte[] b, int off, int len) {
                    String message = new String(b, off, len).replaceAll("\n", "");
                    if (message.length() > 0) {
                        logger.warn("[" + host + "] " + message);
                    }
                }
            });
            channel.connect();
            outReader = new BufferedReader(new InputStreamReader(inputStream));
            while (!isStopped()) {
                while (!isStopped && outReader.ready()) {
                    String line = outReader.readLine();
                    if (line != null && !"".equals(line.trim())) {
                        if (outLines != null) {
                            outLines.add(line);
                        }
                        if (logMessages) {
                            this.logger.info(line);
                        }
                    }
                }
                if (channel.isClosed()) {
                    if (inputStream.available() > 0)
                        continue;
                    stop();
                } else {
                    Thread.yield();
                }
            }
            ret = channel.getExitStatus();
        } catch (IOException | JSchException e) {
            logger.error("Invocation of %s failed: %s", host, e.getMessage());
            wasCancelled = true;
            throw e;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            if (counter != null) {
                counter.countDown();
                logger.info("Remaining active nodes: %s", counter.getCount());
            }
        }
        return ret;
    }

    private class UserInfo implements com.jcraft.jsch.UserInfo {

        private String pwd;

        UserInfo(String pass) {
            this.pwd = pass;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return pwd;
        }

        @Override
        public boolean promptPassword(String s) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return false;
        }

        @Override
        public boolean promptYesNo(String s) {
            return false;
        }

        @Override
        public void showMessage(String s) {

        }
    }
}
