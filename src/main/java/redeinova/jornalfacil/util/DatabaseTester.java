package redeinova.jornalfacil.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;

@Component
public class DatabaseTester implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Override
    public void run(String... args) throws Exception {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Conexão com MySQL bem-sucedida!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Falha na conexão com MySQL: " + e.getMessage());
        }
    }
}