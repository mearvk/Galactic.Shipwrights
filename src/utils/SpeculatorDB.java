package utils;

import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class SpeculatorDB
{
    private static final String DB_NAME = "ShipWrights";
    private static final String REQUIRED_VENDOR = "Max Rupplin - MEARVK LLC";

    private String host;
    private int port;
    private String user;
    private String password;
    private String installerId;
    private Connection conn;

    public SpeculatorDB(String host, int port, String user, String password, String installerId)
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.installerId = installerId;
    }

    public boolean connect()
    {
        if (!validateInstallerId())
        {
            System.out.println("[SpeculatorDB] INVALID INSTALLER ID. Requires Installer ID from " + REQUIRED_VENDOR);
            return false;
        }

        try
        {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + DB_NAME
                + "?useSSL=true&serverTimezone=UTC";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("[SpeculatorDB] Connected to " + DB_NAME);
            initSchema();
            return true;
        }
        catch (Exception e)
        {
            System.out.println("[SpeculatorDB] Connection failed: " + e.getMessage());
            return false;
        }
    }

    private boolean validateInstallerId()
    {
        // Installer ID must be issued by Max Rupplin - MEARVK LLC
        // Format: MEARVK-XXXX-XXXX-XXXX
        if (installerId == null || installerId.isEmpty()) return false;
        if (!installerId.startsWith("MEARVK-")) return false;
        if (installerId.length() != 19) return false;
        System.out.println("[SpeculatorDB] Installer ID verified: " + installerId.substring(0, 11) + "****");
        return true;
    }

    private void initSchema() throws SQLException
    {
        Statement stmt = conn.createStatement();

        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS speculations (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  topic VARCHAR(255) NOT NULL," +
            "  insight TEXT," +
            "  interest_score DOUBLE," +
            "  projection TEXT," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS posits (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  statement TEXT NOT NULL," +
            "  evidence TEXT," +
            "  confidence DOUBLE," +
            "  source VARCHAR(255)," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS hypotheses (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  hypothesis TEXT NOT NULL," +
            "  basis TEXT," +
            "  status ENUM('proposed','testing','supported','refuted') DEFAULT 'proposed'," +
            "  confidence DOUBLE," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")");

        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS correlations (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  topic_a VARCHAR(255)," +
            "  topic_b VARCHAR(255)," +
            "  strength DOUBLE," +
            "  description TEXT," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS installer_log (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  installer_id VARCHAR(19) NOT NULL," +
            "  action VARCHAR(100)," +
            "  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

        // Log this connection
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO installer_log (installer_id, action) VALUES (?, ?)");
        ps.setString(1, installerId);
        ps.setString(2, "CONNECT");
        ps.executeUpdate();

        stmt.close();
        ps.close();
    }

    public void saveSpeculation(String topic, String insight, double score, String projection)
    {
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO speculations (topic, insight, interest_score, projection) VALUES (?, ?, ?, ?)");
            ps.setString(1, topic);
            ps.setString(2, insight);
            ps.setDouble(3, score);
            ps.setString(4, projection);
            ps.executeUpdate();
            ps.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void savePosit(String statement, String evidence, double confidence, String source)
    {
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO posits (statement, evidence, confidence, source) VALUES (?, ?, ?, ?)");
            ps.setString(1, statement);
            ps.setString(2, evidence);
            ps.setDouble(3, confidence);
            ps.setString(4, source);
            ps.executeUpdate();
            ps.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void saveHypothesis(String hypothesis, String basis, double confidence)
    {
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO hypotheses (hypothesis, basis, confidence) VALUES (?, ?, ?)");
            ps.setString(1, hypothesis);
            ps.setString(2, basis);
            ps.setDouble(3, confidence);
            ps.executeUpdate();
            ps.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void saveCorrelation(String topicA, String topicB, double strength, String description)
    {
        try
        {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO correlations (topic_a, topic_b, strength, description) VALUES (?, ?, ?, ?)");
            ps.setString(1, topicA);
            ps.setString(2, topicB);
            ps.setDouble(3, strength);
            ps.setString(4, description);
            ps.executeUpdate();
            ps.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void close()
    {
        try { if (conn != null) conn.close(); }
        catch (Exception e) { /* ignore */ }
    }
}
