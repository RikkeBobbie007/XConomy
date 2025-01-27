package me.yic.xconomy.utils;/*
 *  This file (DatabaseConnection.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import com.zaxxer.hikari.HikariDataSource;
import me.yic.xconomy.XConomy;
import me.yic.xconomy.info.DataBaseConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public class DatabaseConnection {
    @SuppressWarnings("FieldCanBeLocal")
    private final String driver = "org.spongepowered.api.service.sql.SqlService";
    //============================================================================================
    private final File dataFolder = new File(XConomy.getInstance().configDir.toFile(), "playerdata");
    private String url = "";
    private final int maxPoolSize = DataBaseConfig.config.getNode("Pool-Settings", "maximum-pool-size").getInt();
    private final int minIdle = DataBaseConfig.config.getNode("Pool-Settings", "minimum-idle").getInt();
    private final int maxLife = DataBaseConfig.config.getNode("Pool-Settings", "maximum-lifetime").getInt();
    private final Long idleTime = DataBaseConfig.config.getNode("Pool-Settings", "idle-timeout").getLong();
    private boolean secon = false;
    //============================================================================================
    public int waittimeout = 10;
    //============================================================================================
    public File userdata = new File(dataFolder, "data.db");
    //============================================================================================
    private Connection connection = null;
    private HikariDataSource hikari = null;
    private boolean isfirstry = true;

    private void createNewHikariConfiguration() {
        hikari = new HikariDataSource();
        hikari.setPoolName("XConomy");
        hikari.setJdbcUrl(url);
        hikari.setUsername(XConomy.DConfig.getuser());
        hikari.setPassword(XConomy.DConfig.getpass());
        hikari.setMaximumPoolSize(maxPoolSize);
        hikari.setMinimumIdle(minIdle);
        hikari.setMaxLifetime(maxLife);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("userServerPrepStmts", "true");
        if (hikari.getMinimumIdle() < hikari.getMaximumPoolSize()) {
            hikari.setIdleTimeout(idleTime);
        } else {
            hikari.setIdleTimeout(0);
        }
    }


    public boolean setGlobalConnection() {
        url = XConomy.DConfig.geturl();
        try {
            if (XConomy.DConfig.EnableConnectionPool) {
                createNewHikariConfiguration();
                Connection connection = getConnection();
                closeHikariConnection(connection);
            } else {
                Class.forName(driver);
                switch (XConomy.DConfig.getStorageType()) {
                    case 1:
                        connection = DriverManager.getConnection("jdbc:sqlite:" + userdata.toString());
                        break;
                    case 2:
                        connection = DriverManager.getConnection(url, XConomy.DConfig.getuser(), XConomy.DConfig.getpass());
                        break;
                }
            }

            if (secon) {
                XConomy.DConfig.loggersysmess("重新连接成功");
            } else {
                secon = true;
            }
            return true;

        } catch (SQLException e) {
            XConomy.getInstance().logger("无法连接到数据库-----", 1, null);
            e.printStackTrace();
            close();
            return false;

        } catch (ClassNotFoundException e) {
            XConomy.getInstance().logger("JDBC驱动加载失败", 1, null);
        }

        return false;
    }

    public Connection getConnectionAndCheck() {
        if (!canConnect()) {
            return null;
        }
        try {
            return getConnection();
        } catch (SQLException e1) {
            if (isfirstry) {
                isfirstry = false;
                close();
                return getConnectionAndCheck();
            } else {
                isfirstry = true;
                XConomy.getInstance().logger("无法连接到数据库-----", 1, null);
                close();
                e1.printStackTrace();
                return null;
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (XConomy.DConfig.EnableConnectionPool) {
            return hikari.getConnection();
        } else {
            return connection;
        }
    }

    public boolean canConnect() {
        try {
            if (XConomy.DConfig.EnableConnectionPool) {
                if (hikari == null) {
                    return setGlobalConnection();
                }

                if (hikari.isClosed()) {
                    return setGlobalConnection();
                }

            } else {
                if (connection == null) {
                    return setGlobalConnection();
                }

                if (connection.isClosed()) {
                    return setGlobalConnection();
                }

                if (XConomy.DConfig.getStorageType() == 2) {
                    if (!connection.isValid(waittimeout)) {
                        secon = false;
                        return setGlobalConnection();
                    }
                }
            }
        } catch (SQLException e) {
            Arrays.stream(e.getStackTrace()).forEach(d -> XConomy.getInstance().logger(null, 1, d.toString()));
            return false;
        }
        return true;
    }

    public void closeHikariConnection(Connection connection) {
        if (!XConomy.DConfig.EnableConnectionPool) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (hikari != null) {
                hikari.close();
            }
        } catch (SQLException e) {
            XConomy.DConfig.loggersysmess("连接断开失败");
            e.printStackTrace();
        }
    }
}
