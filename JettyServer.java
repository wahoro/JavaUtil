package com.hit.fm.common.util;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.Scanner;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class JettyServer {

    private final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

    private int port;
    private String context;
    private String webappPath;
    private int scanIntervalSeconds;
    private boolean jmxEnabled;
    private Server server;
    private WebAppContext webapp;

    public JettyServer(String webappPath, int port, String context) {
        this(webappPath, port, context, 0, false);
    }

    public JettyServer(String webappPath, int port, String context, int scanIntervalSeconds, boolean jmxEnabled) {
        this.webappPath = webappPath;
        this.port = port;
        this.context = context;
        this.scanIntervalSeconds = scanIntervalSeconds;
        this.jmxEnabled = jmxEnabled;
        validateConfig();
    }

    private boolean portAvailable(int port) {
        if (port <= 0) {
            LOGGER.error("Invalid start port: " + port);
            return false;
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    private void validateConfig() {
        if (port < 0 || port > 65536) {
            LOGGER.error("http.port.invalid: " + port);
            return;
        }
        if (context == null) {
            LOGGER.error("http.context.invalid: " + context);
            return;
        }
        if (webappPath == null) {
            LOGGER.error("http.webAppPath.invalid: " + webappPath);
            return;
        }
    }

    public void start() {
        if (server == null || server.isStopped()) {
            try {
                doStart();
            } catch (Exception e) {
                LOGGER.error("http.errOnStart", e);
                //System.exit(1);
            }
        } else {
            LOGGER.warn("http.started.");
            System.out.println("http started");
        }
    }

    public boolean isRunning() {
        if (server == null) {
            return false;
        } else {
            return server.isStarted();
        }
    }

    private void doStart() throws Exception {
        if (!portAvailable(port)) {
            LOGGER.error("http.port.inUse: " + port);
            return;
        }

        System.setProperty("org.eclipse.jetty.util.URI.charset", "UTF-8");
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");

        webapp = new WebAppContext(webappPath, context);
        //webapp.getSessionHandler().getSessionManager().setMaxInactiveInterval(20);
        String tmpdir = System.getProperty("http.io.tmpdir");
        if (tmpdir != null && !tmpdir.isEmpty()) {
            webapp.setAttribute("org.eclipse.jetty.webapp.basetempdir", tmpdir);
        }
        server = new Server();
        server.setHandler(webapp);

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        final BlockingArrayQueue baq = new BlockingArrayQueue(8, 8);
        QueuedThreadPool qtp = new QueuedThreadPool(baq);
        qtp.setMaxThreads(Runtime.getRuntime().availableProcessors() * 60);
        qtp.setMinThreads(Runtime.getRuntime().availableProcessors() * 60);
        qtp.setThreadsPriority(Thread.MAX_PRIORITY);
        connector.setThreadPool(qtp);
        connector.setMaxIdleTime(600000);
        connector.setRequestBufferSize(1024 * 1024);
        server.setConnectors(new Connector[]{connector});


        if (jmxEnabled) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
            server.addBean(mBeanContainer);
        }

        if (scanIntervalSeconds > 0) {
            startFileWatchScanner();
        }

        long ts = System.currentTimeMillis();
        server.start();

        ts = System.currentTimeMillis() - ts;
        LOGGER.info("http.started: " + String.format("%.2f sec", ts / 1000d));

        server.join();
    }

    private void startFileWatchScanner() throws Exception {
        List<File> scanList = new ArrayList<File>();
        scanList.add(new File(webappPath, "WEB-INF"));

        Scanner scanner = new Scanner();
        scanner.setReportExistingFilesOnStartup(false);
        scanner.setScanInterval(scanIntervalSeconds);
        scanner.setScanDirs(scanList);
        scanner.addListener(new Scanner.BulkListener() {
            @Override
            public void filesChanged(List changes) {
                try {
                    LOGGER.info("http.reloading");
                    webapp.stop();
                    webapp.start();
                    LOGGER.info("http.reloaded");
                } catch (Exception e) {
                    LOGGER.error("http.errorOnReload", e);
                }
            }
        });
        LOGGER.info("http.scaner.start: {'interval':" + scanIntervalSeconds + "}");
        scanner.start();
    }
}
