package server.database;

import common.models.Billboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import server.database.billboard.BillboardHandler;

import java.sql.Connection;
import java.util.List;

public class DBTest {

    Connection connection;
    @BeforeEach
    public void createDatabase() throws Exception {
        connection = ConnectionFactory.getConnection();
    }

    @Test
    public void testFetch() throws Exception {
        BillboardHandler billboards = new BillboardHandler(connection);
        List<Billboard> test = billboards.getAll(false);
        for (int i = 0; i < test.size(); i++) {
            System.out.println(test.get(i).name);
        }
    }
}
