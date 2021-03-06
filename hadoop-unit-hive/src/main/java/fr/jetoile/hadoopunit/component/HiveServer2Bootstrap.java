/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.jetoile.hadoopunit.component;

import com.github.sakserv.minicluster.impl.HiveLocalServer2;
import com.github.sakserv.minicluster.util.FileUtils;
import com.github.sakserv.minicluster.util.WindowsLibsUtils;
import fr.jetoile.hadoopunit.*;
import fr.jetoile.hadoopunit.exception.BootstrapException;
import fr.jetoile.hadoopunit.exception.NotFoundServiceException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

public class HiveServer2Bootstrap implements BootstrapHadoop {
    static final private Logger LOGGER = LoggerFactory.getLogger(HiveServer2Bootstrap.class);

    private HiveLocalServer2 hiveLocalServer2;

    private State state = State.STOPPED;

    private Configuration configuration;
    private String host;
    private int port;
    private String derbyDirectory;
    private String scratchDirectory;
    private String warehouseDirectory;
    private String zookeeperConnectionString;
    private String hostMetastore;
    private int portMetastore;

    private String hdfsUri;

    public HiveServer2Bootstrap() {
        if (hiveLocalServer2 == null) {
            try {
                configuration = HadoopUtils.INSTANCE.loadConfigFile(null);
                loadConfig();
            } catch (BootstrapException e) {
                LOGGER.error("unable to load configuration", e);
            }
        }
    }

    public HiveServer2Bootstrap(URL url) {
        if (hiveLocalServer2 == null) {
            try {
                configuration = HadoopUtils.INSTANCE.loadConfigFile(url);
                loadConfig();
            } catch (BootstrapException e) {
                LOGGER.error("unable to load configuration", e);
            }
        }
    }

    @Override
    public ComponentMetadata getMetadata() {
        return new HiveServer2Metadata();
    }

    @Override
    public String getProperties() {
        return "\n \t\t\t port:" + port;
    }

    private void loadConfig() throws BootstrapException {
        host = configuration.getString(HiveConfig.HIVE_SERVER2_HOSTNAME_KEY);
        port = configuration.getInt(HiveConfig.HIVE_SERVER2_PORT_KEY);
        hostMetastore = configuration.getString(HiveConfig.HIVE_METASTORE_HOSTNAME_KEY);
        portMetastore = configuration.getInt(HiveConfig.HIVE_METASTORE_PORT_KEY);
        derbyDirectory = configuration.getString(HiveConfig.HIVE_METASTORE_DERBY_DB_DIR_KEY);
        scratchDirectory = configuration.getString(HiveConfig.HIVE_SCRATCH_DIR_KEY);
        warehouseDirectory = configuration.getString(HiveConfig.HIVE_WAREHOUSE_DIR_KEY);
        zookeeperConnectionString = configuration.getString(ZookeeperConfig.ZOOKEEPER_HOST_KEY) + ":" + configuration.getInt(ZookeeperConfig.ZOOKEEPER_PORT_KEY);
        hdfsUri = "hdfs://" + configuration.getString(HdfsConfig.HDFS_NAMENODE_HOST_KEY) + ":" + configuration.getString(HdfsConfig.HDFS_NAMENODE_PORT_KEY);
    }

    @Override
    public void loadConfig(Map<String, String> configs) {
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_SERVER2_HOSTNAME_KEY))) {
            host = configs.get(HiveConfig.HIVE_SERVER2_HOSTNAME_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_SERVER2_PORT_KEY))) {
            port = Integer.parseInt(configs.get(HiveConfig.HIVE_SERVER2_PORT_KEY));
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_METASTORE_HOSTNAME_KEY))) {
            hostMetastore = configs.get(HiveConfig.HIVE_METASTORE_HOSTNAME_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_METASTORE_PORT_KEY))) {
            portMetastore = Integer.parseInt(configs.get(HiveConfig.HIVE_METASTORE_PORT_KEY));
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_METASTORE_DERBY_DB_DIR_KEY))) {
            derbyDirectory = configs.get(HiveConfig.HIVE_METASTORE_DERBY_DB_DIR_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_SCRATCH_DIR_KEY))) {
            scratchDirectory = configs.get(HiveConfig.HIVE_SCRATCH_DIR_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(HiveConfig.HIVE_WAREHOUSE_DIR_KEY))) {
            warehouseDirectory = configs.get(HiveConfig.HIVE_WAREHOUSE_DIR_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(ZookeeperConfig.ZOOKEEPER_HOST_KEY)) && StringUtils.isNotEmpty(configs.get(ZookeeperConfig.ZOOKEEPER_PORT_KEY))) {
            zookeeperConnectionString = configs.get(ZookeeperConfig.ZOOKEEPER_HOST_KEY) + ":" + configs.get(ZookeeperConfig.ZOOKEEPER_PORT_KEY);
        }
        if (StringUtils.isNotEmpty(configs.get(HdfsConfig.HDFS_NAMENODE_HOST_KEY)) && StringUtils.isNotEmpty(configs.get(HdfsConfig.HDFS_NAMENODE_PORT_KEY))) {
            hdfsUri = "hdfs://" + configs.get(HdfsConfig.HDFS_NAMENODE_HOST_KEY) + ":" + Integer.parseInt(configs.get(HdfsConfig.HDFS_NAMENODE_PORT_KEY));
        }
    }


    private void cleanup() {
        FileUtils.deleteFolder(derbyDirectory);
        FileUtils.deleteFolder(derbyDirectory.substring(derbyDirectory.lastIndexOf("/") + 1));

    }

    private void build() {
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to load Calcite JDBC driver", e);
        }

        hiveLocalServer2 = new HiveLocalServer2.Builder()
                .setHiveMetastoreDerbyDbDir(derbyDirectory)
                .setHiveServer2Hostname(host)
                .setHiveServer2Port(port)
                .setHiveMetastoreHostname(hostMetastore)
                .setHiveMetastorePort(portMetastore)
                .setHiveWarehouseDir(warehouseDirectory)
                .setHiveScratchDir(scratchDirectory)
                .setHiveConf(buildHiveConf())
                .setZookeeperConnectionString(zookeeperConnectionString)
                .build();

    }

    private HiveConf buildHiveConf() {
        // Handle Windows
        WindowsLibsUtils.setHadoopHome();

        HiveConf hiveConf = new HiveConf();
        hiveConf.set("fs.defaultFS", hdfsUri);
//        hiveConf.set(HiveConf.ConfVars.HIVE_TXN_MANAGER.varname, "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager");
//        hiveConf.set(HiveConf.ConfVars.HIVE_COMPACTOR_INITIATOR_ON.varname, "true");
//        hiveConf.set(HiveConf.ConfVars.HIVE_COMPACTOR_WORKER_THREADS.varname, "5");
//        hiveConf.set("hive.root.logger", "DEBUG,console");
//        hiveConf.setIntVar(HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES, 3);
//        hiveConf.set(HiveConf.ConfVars.PREEXECHOOKS.varname, "");
//        hiveConf.set(HiveConf.ConfVars.POSTEXECHOOKS.varname, "");
//        System.setProperty(HiveConf.ConfVars.PREEXECHOOKS.varname, " ");
//        System.setProperty(HiveConf.ConfVars.POSTEXECHOOKS.varname, " ");

        return hiveConf;
    }

    @Override
    public Bootstrap start() {
        if (state == State.STOPPED) {
            state = State.STARTING;
            LOGGER.info("{} is starting", this.getClass().getName());

            build();
            try {
                hiveLocalServer2.start();
            } catch (Exception e) {
                LOGGER.error("unable to add hiveserver2", e);
            }
            state = State.STARTED;
            LOGGER.info("{} is started", this.getClass().getName());
        }
        return this;
    }

    @Override
    public Bootstrap stop() {
        if (state == State.STARTED) {
            state = State.STOPPING;
            LOGGER.info("{} is stopping", this.getClass().getName());
            try {
                hiveLocalServer2.stop(true);
            } catch (Exception e) {
                LOGGER.error("unable to stop hiveserver2", e);
            }
            cleanup();
            state = State.STOPPED;
            LOGGER.info("{} is stopped", this.getClass().getName());
        }
        return this;
    }

    @Override
    public org.apache.hadoop.conf.Configuration getConfiguration() {
        return hiveLocalServer2.getHiveConf();
    }

}
