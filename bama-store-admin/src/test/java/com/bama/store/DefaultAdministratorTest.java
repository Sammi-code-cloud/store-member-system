package com.bama.store;

import com.bama.store.config.DefaultAdministrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DefaultAdministratorTest {
    JdbcTemplate jdbc;
    DefaultAdministrator initializer;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:bootstrap_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/schema-h2.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds);
        new com.bama.store.config.AdminSchemaUpgrade(ds,jdbc).run(null);
        initializer = new DefaultAdministrator(jdbc,encoder);
        ReflectionTestUtils.setField(initializer,"phone","13800009999");
        ReflectionTestUtils.setField(initializer,"password","bootstrap-test-only");
    }
    @Test void emptyDatabaseGetsOneAdministratorAndCompletePermissionsWithoutBusinessData() {
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_staff",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_permission",Integer.class)).isEqualTo(18);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_role_permission",Integer.class)).isEqualTo(18);
        assertThat(encoder.matches("bootstrap-test-only",jdbc.queryForObject("SELECT password FROM t_staff",String.class))).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_store",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_member",Integer.class)).isZero();
        String changed = encoder.encode("changed-by-administrator");
        jdbc.update("UPDATE t_staff SET password=?",changed);
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT password FROM t_staff",String.class)).isEqualTo(changed);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_staff_role",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_role_permission",Integer.class)).isEqualTo(18);
    }
    @Test void firstActiveStoreMakesAdministratorVisibleWithoutMovingAnAssignedAdministrator() {
        initializer.run(null);
        jdbc.update("INSERT INTO t_store(id,name,status) VALUES(9,'Closed',0),(10,'First',1),(11,'Second',1)");
        initializer.attachToFirstStore(9L);
        assertThat(jdbc.queryForObject("SELECT store_id FROM t_staff",Long.class)).isNull();
        initializer.attachToFirstStore(10L);
        initializer.attachToFirstStore(11L);
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT store_id FROM t_staff",Long.class)).isEqualTo(10L);
        assertThat(jdbc.queryForList("SELECT store_id FROM t_staff_store",Long.class)).containsExactly(10L);
    }
    @Test void neverPromotesAnotherEmployeeWithTheConfiguredPhone() {
        jdbc.update("INSERT INTO t_staff(staff_no,name,phone,password,status) VALUES('OTHER','Other','13800009999','unused',1)");
        assertThatThrownBy(()->initializer.run(null)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_staff_role",Integer.class)).isZero();
    }
    @Test void supportsIndependentAdminLoginWithoutUsingAnEmployeePhone() {
        ReflectionTestUtils.setField(initializer,"phone","admin");
        initializer.run(null);
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT phone FROM t_staff WHERE staff_no='BM-ADMIN'",String.class)).isEqualTo("admin");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_staff",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_role_permission",Integer.class)).isEqualTo(18);
    }
    @Test void disabledOrDeletedAdministratorIsNotRecreatedOrEnabled() {
        initializer.run(null);
        jdbc.update("UPDATE t_staff SET status=0");
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT status FROM t_staff",Integer.class)).isZero();
        jdbc.update("UPDATE t_staff SET deleted=1");
        initializer.run(null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_staff",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT deleted FROM t_staff",Integer.class)).isEqualTo(1);
    }
}
