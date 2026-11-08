package com.udhr.health;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Is the database reachable right now? Checked at most every 3 seconds, so
 * every request can ask without slowing anything down. Used by
 * DatabaseGuardFilter (a friendly message instead of raw errors) and by the
 * system status endpoints.
 */
@Component
public class DatabaseHealth {

    private static final long CACHE_MILLIS = 3_000;

    private final DataSource dataSource;
    private volatile boolean up = true;
    private volatile long checkedAt = 0;
    private volatile String product = "unknown";

    public DatabaseHealth(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isUp() {
        long now = System.currentTimeMillis();
        if (now - checkedAt > CACHE_MILLIS) {
            synchronized (this) {
                if (now - checkedAt > CACHE_MILLIS) {
                    up = check();
                    checkedAt = System.currentTimeMillis();
                }
            }
        }
        return up;
    }

    /** Forget the cached answer (after an error, or for the status screen). */
    public boolean checkNow() {
        synchronized (this) {
            up = check();
            checkedAt = System.currentTimeMillis();
            return up;
        }
    }

    public String product() {
        return product;
    }

    private boolean check() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            product = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
