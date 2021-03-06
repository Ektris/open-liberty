/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.Before;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApp;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class AbstractSpringTests {
    static enum AppConfigType {
        DROPINS_SPR,
        DROPINS_ROOT,
        SPRING_BOOT_APP_TAG
    }

    public static final String SPRING_BOOT_15_APP_BASE = "com.ibm.ws.springboot.support.version15.test.app.jar";
    public static final String SPRING_BOOT_20_APP_BASE = "com.ibm.ws.springboot.support.version20.test.app-0.0.1-SNAPSHOT.jar";

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.springboot.support.fat.SpringBootTests");
    public static final AtomicBoolean serverStarted = new AtomicBoolean();
    public static final Collection<RemoteFile> dropinFiles = new ArrayList<>();

    @AfterClass
    public static void stopServer() throws Exception {
        serverStarted.set(false);
        try {
            server.stopServer();
        } finally {
            for (RemoteFile remoteFile : dropinFiles) {
                remoteFile.delete();
            }
            dropinFiles.clear();
        }
    }

    public abstract Set<String> getFeatures();

    public abstract String getApplication();

    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_SPR;
    }

    @Before
    public void configureServer() throws Exception {
        if (serverStarted.compareAndSet(false, true)) {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            features.clear();
            features.addAll(getFeatures());
            RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + getApplication());
            switch (getApplicationConfigType()) {
                case DROPINS_SPR: {
                    new File(new File(server.getServerRoot()), "dropins/spr/").mkdirs();
                    appFile.copyToDest(server.getFileFromLibertyServerRoot("dropins/spr/"));
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/spr/"), appFile.getName());
                    appFile.copyToDest(dest);
                    dropinFiles.add(dest);
                    break;
                }
                case DROPINS_ROOT: {
                    new File(new File(server.getServerRoot()), "dropins/").mkdirs();
                    String appName = appFile.getName();
                    appName = appName.substring(0, appName.length() - 3) + "spr";
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/"), appName);
                    appFile.copyToDest(dest);
                    dropinFiles.add(dest);
                    break;
                }
                case SPRING_BOOT_APP_TAG: {
                    List<SpringBootApp> apps = config.getSpringBootApps();
                    SpringBootApp app = new SpringBootApp();
                    app.setLocation(appFile.getName());
                    app.setName("testName");
                    apps.add(app);
                    break;
                }
                default:
                    break;
            }

            server.updateServerConfiguration(config);
            server.startServer(true, false);
        }
    }
}
