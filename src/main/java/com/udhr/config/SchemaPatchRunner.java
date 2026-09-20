package com.udhr.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Patches schema constraints that ddl-auto=update won't touch on its own —
 * it only adds missing tables/columns, it never relaxes an existing column's
 * NOT NULL constraint. audit_logs.staff_id was originally required, but a
 * patient viewing their own record (PatientPortalService) has no staff actor
 * at all, so it needs to be nullable. Runs after Hibernate's own schema
 * update, so on a brand-new database the table already exists with the
 * column nullable (from the current entity mapping) and this is a no-op;
 * on an existing database it relaxes the leftover constraint.
 */
@Component
public class SchemaPatchRunner implements CommandLineRunner {

    private final DataSource dataSource;

    public SchemaPatchRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            String productName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (productName.contains("postgresql")) {
                stmt.execute("ALTER TABLE audit_logs ALTER COLUMN staff_id DROP NOT NULL");
            } else {
                stmt.execute("ALTER TABLE audit_logs MODIFY COLUMN staff_id BIGINT NULL");
            }
        } catch (Exception e) {
            System.out.println("SchemaPatchRunner: skipped audit_logs.staff_id patch (" + e.getMessage() + ")");
        }
    }
}
