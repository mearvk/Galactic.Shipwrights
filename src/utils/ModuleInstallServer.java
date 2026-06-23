package utils;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Listens on port 2000 for module installs and server directory queries.
 * Requires a valid Installer ID from Max Rupplin - MEARVK LLC (MEARVK-XXXX-XXXX-XXXX).
 * Requires a National ID with Moral Rating >= "Very Good" and IQ > 125 for installs.
 * Provides a menu for querying port 20000/49152 server lists and registering JWSTNJ21 servers.
 */
public class ModuleInstallServer implements Runnable
{
    public static final int PORT = 2000;
    private static final String MODULES_DIR = "modules/";
    private static final String CONFIG_FILE = "configuration/port.2000.config.xml";
    private static final String SERVERS_20000 = "configuration/known.port.20000.servers.xml";
    private static final String SERVERS_49152 = "configuration/known.port.49152.servers.xml";
    private static final String REGISTERED_KEYS_DIR = "configuration/registered_keys/";
    private static final String JWSTNJ21_PUBLIC_KEY_URL = "https://github.com/mearvk/Java.Web.Server.Telnet.Front.Java.21/blob/main/psychiatry/secrets/public.key";
    private static final String JWSTNJ21_PUBLIC_KEY = "A49B2C30185AA764129CB057483B29451076C291A86395B6C42938C201A84351B0695B30294862B0192A437C56B27103B268A6495C3019A82B7436CB21095A629348B01C59A263C70A8256B34710A34759B4B27C2CC729A36481745C95BC016B7992B0673B3463033A7A57C1CB566B96C7A0103C292882092A86193A90B72B7039880C5C9CA74344BA19C02A336CC939300002862904B32AA3419248C241C843B5B18418850A88B8AA9CAC6A35705AAB17C3A046C89550203B5982405486C28B0654646A7A1066534A6928593278298AB444145947610C6CB376733AB30912329AB094C50A3043748310704C6750927B19702B464C3495CB03CB6A132256B27328364CC2BBA97231451873C130376C925C2438A2315C363AC125A315119B947A605A140015CC9C56BA337647C06330ACCBA4C03A50A16046951A77C67C65A4CC3632775A439A715B87101A8C99B4028801413AAC874C3419A7976B1668C62677C1110B1267692A5A49C3871200C0C36278964C15481656CCBA6B46640A2690A304215A58B970C6881182B218AB4274568801473B62BA09BA7A5954138CB2155990B3C511473399697661A2093A55355237B6661743382BC6094B627407213B78A11137B3089C14C55770245C5BA02BC2B7C5B454C1A9769B3C9844341BB167701C807CCC895984172893B7A8133A651244A180116940145538AC1062C07166B5482CB5678B3487B6560C8ACAB352194A791A996AA398A66B208CC74617CA0342326886838878B86A849678079894452875C7B43769635B44B112B4CB5C845223883B405A8975B586288076473A95C21797B385A20234A27765A40228309CA9C9087187AB35A0582A27C6BB3762CB776653C8A4752CC25B2C05844985730547C62C351AB6138C145413BC55B31561A7B468870982A7AA0348314C112621B5B35953A0722C546AA0417292A692A2C7A5342803250C14C4229A2AA71ACB33A73CC73B62626811B6985C311279B6B23216CCCB0CA1BBA728B202641880B87A122A07926821142CB5163B57C709BCA124B4164441681676A90A84379B987A9B865B3B4236B664986569271C543C1A9AA2C821B14274C3493C001B2347412560628B622081522A23C393210583198B000340099AB1B33C08164874046B61A06111825C270781A988437953686006B92C3C2213ABA34CA964C4C68561B702938C332906C2621C8A1054CC7AA42BC4BCB842340B325CA26416556BC1CC75A8170B1CA676450A0B36C87C091C5C7A11910A382A8294B059C394600C09AB39BA86591C6250917AA8C838BA3A41B667CC9330965C304891CA090518712C57B4397BCBCAC14C8085B962A56232803CCB9BC9AC93C38B130BCB468BA6BCBCB377CC5AC037A8AA5A87B757529452817BBBC35C9A7453C93B4112CC7300762955519B36B1516A34A60BA3A30C3643CC5B97AA94425A845CA99B390089BC1A36262B548074902BC288C09A4601A848135086BA38AC194511A1BCC24B701B2C6374976722C997C23A25A394747AC3735BCB06CCBA437B7CA87A85B22A8146BB2AA3806C196417A63371900CC64366A55060A2BC555776C894B0BC36005391694CCBC50C33AA4C8A5795C6073105C3841C52A200424564AA04332909C104595BBA9875A6428781B34759B4B27C2CC729A36481745C9ACBB3C4023BCBB0CBAA6158BC86C45B9BA95CA93AA503B8BC430B824B7BB0C261A7BC371AA1B6194A30B1C4B5324C4106C1227AC39744AA4AA5BC861340B48A145C8690623B6CC7BA1BAAA01BAAA31A562CA1BA05697A5229C008B25A36768393526CA38392113337C4421ACBC8BBA7A36441315CCCA4AA5CBA8CB5790CA4BC894C2B4B2376C5AA920C8B7A8CB3B0A5093A048BCABBB30B6CC46006C42BC8CC24C03BC2C76A284534B1AA965749AB00473216853C3C0BC643278A9C1284CC495BCC2A62C6176313C52CC162C92C3278457018CB0CC9A12B26CC696AA48A54BA50A72B8B97C2B0288827CA7920A381BC58814707CCAB069ACBB9B5A4B27C1717311746B316A3C75AA75BA3073CC0CC30CC47A13CC93AA6BCC29C35CA43949BA9BA547AC4C6CC28481A639B0B7BA8702BB2CCAB90BB91CC4C38549CA9C6AC2AC1A47BA1CCAAB2BC48AA59AA25827C7AA48CA1AA460B843105B7BB3AC7C2CA7A02BBCC46AA85188B67B547AAAB2BB4BB99385BC5AC308930C863B5AB6889BB2B1CCBC1CC2B945A46C49CC29221C42BC37BC46924BBCA2137BA65B3392CAABCC23CA27BBA6928CC92C18BA048BB45BCB0663CC6AA03CC597AB3451A07CC3BB9A4C0A4B3BA97A0A9C18BCC7C7691C9BC29A6C0CA6CA2A54AC39171C2BB1A4CCBB59CA75BC49A157CA8BC6A3AA9BA20BC1BB65A4CCBB0B4BCBC9A48BCC0AA0ACBA8B9CBB9BC7A4BC81BBBCAB5BCBA9CC89CA1AAAB01CC5CB4C894BCC1BC9B11CCA2ACBC7B3B0C773C33CCB19BB9BA1BA18BCC611BCCBB5922C0AC6649B2AA4CBB9CB1CC9BC3B1BCBA1A4BC18BB8B8A6BA4A5AC3BC5BA8A2BCC71C35CA66BA5A4A76CCA1B8BCBAA3BCC2BCB9CB18BCBB16CC29CA16541CC76CA069C1A09CB39A4BB09CA83B38A";
    private static final String INSTALLER_TECH_ID = "A49B2C30185AA764129CB057483B29451076C291A86395B6C42938C201A84351B0695B30294862B0192A437C56B27103B268A6495C3019A82B7436CB21095A629348B01C59A263C70A8256B34710A34759B4B27C2CC729A36481745C95BC016B7992B0673B3463033A7A57C1CB566B96C7A0103C292882092A86193A90B72B7039880C5C9CA74344BA19C02A336CC939300002862904B32AA3419248C241C843B5B18418850A88B8AA9CAC6A35705AAB17C3A046C89550203B5982405486C28B0654646A7A1066534A6928593278298AB444145947610C6CB376733AB30912329AB094C50A3043748310704C6750927B19702B464C3495CB03CB6A132256B27328364CC2BBA97231451873C130376C925C2438A2315C363AC125A315119B947A605A140015CC9C56BA337647C06330ACCBA4C03A50A16046951A77C67C65A4CC3632775A439A715B87101A8C99B4028801413AAC874C3419A7976B1668C62677C1110B1267692A5A49C3871200C0C36278964C15481656CCBA6B46640A2690A304215A58B970C6881182B218AB4274568801473B62BA09BA7A5954138CB2155990B3C511473399697661A2093A55355237B6661743382BC6094B627407213B78A11137B3089C14C55770245C5BA02BC2B7C5B454C1A9769B3C9844341BB167701C807CCC895984172893B7A8133A651244A180116940145538AC1062C07166B5482CB5678B3487B6560C8ACAB352194A791A996AA398A66B208CC74617CA0342326886838878B86A849678079894452875C7B43769635B44B112B4CB5C845223883B405A8975B586288076473A95C21797B385A20234A27765A40228309CA9C9087187AB35A0582A27C6BB3762CB776653C8A4752CC25B2C05844985730547C62C351AB6138C145413BC55B31561A7B468870982A7AA0348314C112621B5B35953A0722C546AA0417292A692A2C7A5342803250C14C4229A2AA71ACB33A73CC73B62626811B6985C311279B6B23216CCCB0CA1BBA728B202641880B87A122A07926821142CB5163B57C709BCA124B4164441681676A90A84379B987A9B865B3B4236B664986569271C543C1A9AA2C821B14274C3493C001B2347412560628B622081522A23C393210583198B000340099AB1B33C08164874046B61A06111825C270781A988437953686006B92C3C2213ABA34CA964C4C68561B702938C332906C2621C8A1054CC7AA42BC4BCB842340B325CA26416556BC1CC75A8170B1CA676450A0B36C87C091C5C7A11910A382A8294B059C394600C09AB39BA86591C6250917AA8C838BA3A41B667CC9330965C304891CA090518712C57B4397BCBCAC14C8085B962A56232803CCB9BC9AC93C38B130BCB468BA6BCBCB377CC5AC037A8AA5A87B757529452817BBBC35C9A7453C93B4112CC7300762955519B36B1516A34A60BA3A30C3643CC5B97AA94425A845CA99B390089BC1A36262B548074902BC288C09A4601A848135086BA38AC194511A1BCC24B701B2C6374976722C997C23A25A394747AC3735BCB06CCBA437B7CA87A85B22A8146BB2AA3806C196417A63371900CC64366A55060A2BC555776C894B0BC36005391694CCBC50C33AA4C8A5795C6073105C3841C52A200424564AA04332909C104595BBA9875A6428781B34759B4B27C2CC729A36481745C9ACBB3C4023BCBB0CBAA6158BC86C45B9BA95CA93AA503B8BC430B824B7BB0C261A7BC371AA1B6194A30B1C4B5324C4106C1227AC39744AA4AA5BC861340B48A145C8690623B6CC7BA1BAAA01BAAA31A562CA1BA05697A5229C008B25A36768393526CA38392113337C4421ACBC8BBA7A36441315CCCA4AA5CBA8CB5790CA4BC894C2B4B2376C5AA920C8B7A8CB3B0A5093A048BCABBB30B6CC46006C42BC8CC24C03BC2C76A284534B1AA965749AB00473216853C3C0BC643278A9C1284CC495BCC2A62C6176313C52CC162C92C3278457018CB0CC9A12B26CC696AA48A54BA50A72B8B97C2B0288827CA7920A381BC58814707CCAB069ACBB9B5A4B27C1717311746B316A3C75AA75BA3073CC0CC30CC47A13CC93AA6BCC29C35CA43949BA9BA547AC4C6CC28481A639B0B7BA8702BB2CCAB90BB91CC4C38549CA9C6AC2AC1A47BA1CCAAB2BC48AA59AA25827C7AA48CA1AA460B843105B7BB3AC7C2CA7A02BBCC46AA85188B67B547AAAB2BB4BB99385BC5AC308930C863B5AB6889BB2B1CCBC1CC2B945A46C49CC29221C42BC37BC46924BBCA2137BA65B3392CAABCC23CA27BBA6928CC92C18BA048BB45BCB0663CC6AA03CC597AB3451A07CC3BB9A4C0A4B3BA97A0A9C18BCC7C7691C9BC29A6C0CA6CA2A54AC39171C2BB1A4CCBB59CA75BC49A157CA8BC6A3AA9BA20BC1BB65A4CCBB0B4BCBC9A48BCC0AA0ACBA8B9CBB9BC7A4BC81BBBCAB5BCBA9CC89CA1AAAB01CC5CB4C894BCC1BC9B11CCA2ACBC7B3B0C773C33CCB19BB9BA1BA18BCC611BCCBB5922C0AC6649B2AA4CBB9CB1CC9BC3B1BCBA1A4BC18BB8B8A6BA4A5AC3BC5BA8A2BCC71C35CA66BA5A4A76CCA1B8BCBAA3BCC2BCB9CB18BCBB16CC29CA16541CC76CA069C1A09CB39A4BB09CA83B38A";

    private ServerSocket serverSocket;

    // Config loaded from port.2000.config.xml
    private boolean query20000Enabled = true;
    private boolean query20000RequireNationalId = true;
    private boolean query20000RequireJWSTFJ21 = false;
    private boolean query49152Enabled = true;
    private boolean query49152RequireNationalId = true;
    private boolean query49152RequireJWSTFJ21 = false;
    private boolean registerEnabled = true;
    private boolean registerRequirePublicKey = true;
    private int registerRequiredRank = 4;

    @Override
    public void run()
    {
        new File(MODULES_DIR).mkdirs();
        new File(REGISTERED_KEYS_DIR).mkdirs();
        loadConfig();
        initDatabase();

        try
        {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[ModuleInstallServer] Listening on port " + PORT);

            while (!Thread.interrupted())
            {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(() -> handleClient(client), "ModuleInstall-" + client.getPort());
                handler.setDaemon(true);
                handler.start();
            }
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Stopped: " + e.getMessage());
        }
    }

    private void initDatabase()
    {
        try
        {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/ShipWrights?useSSL=true&serverTimezone=UTC",
                "root", "");

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS jwstnj21_keys (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  installer_tech_id TEXT NOT NULL," +
                "  public_key_url VARCHAR(512) NOT NULL," +
                "  public_key TEXT NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // Store URL and key if not already present
            PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM jwstnj21_keys WHERE installer_tech_id = ?");
            check.setString(1, INSTALLER_TECH_ID);
            var rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0)
            {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO jwstnj21_keys (installer_tech_id, public_key_url, public_key) VALUES (?, ?, ?)");
                ps.setString(1, INSTALLER_TECH_ID);
                ps.setString(2, JWSTNJ21_PUBLIC_KEY_URL);
                ps.setString(3, JWSTNJ21_PUBLIC_KEY);
                ps.executeUpdate();
                ps.close();
                System.out.println("[ModuleInstallServer] Stored JWSTNJ21 public key and URL in database");
            }

            check.close();
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Database init (non-fatal): " + e.getMessage());
        }
    }

    private void loadConfig()
    {
        try
        {
            File f = new File(CONFIG_FILE);
            if (!f.exists()) return;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            doc.getDocumentElement().normalize();

            NodeList q20 = doc.getElementsByTagName("queryPort20000");
            if (q20.getLength() > 0)
            {
                Element e = (Element) q20.item(0);
                query20000Enabled = getBool(e, "enabled", true);
                query20000RequireNationalId = getBool(e, "requireNationalId", true);
                query20000RequireJWSTFJ21 = getBool(e, "requireJWSTFJ21Rank4", false);
            }

            NodeList q49 = doc.getElementsByTagName("queryPort49152");
            if (q49.getLength() > 0)
            {
                Element e = (Element) q49.item(0);
                query49152Enabled = getBool(e, "enabled", true);
                query49152RequireNationalId = getBool(e, "requireNationalId", true);
                query49152RequireJWSTFJ21 = getBool(e, "requireJWSTFJ21Rank4", false);
            }

            NodeList reg = doc.getElementsByTagName("registerJWSTFJ21");
            if (reg.getLength() > 0)
            {
                Element e = (Element) reg.item(0);
                registerEnabled = getBool(e, "enabled", true);
                registerRequirePublicKey = getBool(e, "requirePublicKey", true);
                String rank = getTagValue(e, "requiredRank");
                if (rank != null) registerRequiredRank = Integer.parseInt(rank.trim());
            }
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Config load error: " + e.getMessage());
        }
    }

    private boolean getBool(Element parent, String tag, boolean def)
    {
        String v = getTagValue(parent, tag);
        return v != null ? Boolean.parseBoolean(v.trim()) : def;
    }

    private String getTagValue(Element parent, String tag)
    {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() > 0) return nl.item(0).getTextContent();
        return null;
    }

    private void handleClient(Socket client)
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true))
        {
            out.println("=== GALACTIC SHIPWRIGHTS - PORT 2000 ===");
            out.println("MENU:");
            out.println("  1 - Query port 20000 servers" + (query20000Enabled ? "" : " [DISABLED]"));
            out.println("  2 - Query port 49152 servers" + (query49152Enabled ? "" : " [DISABLED]"));
            out.println("  3 - Install module (JAR/SQL)");
            out.println("  4 - Register Rank " + registerRequiredRank + " JWSTNJ21 server" + (registerEnabled ? "" : " [DISABLED]"));
            out.println("  5 - Quit");
            out.println("SELECT>");

            String choice = in.readLine();
            if (choice == null) { client.close(); return; }

            switch (choice.trim())
            {
                case "1": handleQueryServers(in, out, 20000); break;
                case "2": handleQueryServers(in, out, 49152); break;
                case "3": handleModuleInstall(in, out, client); break;
                case "4": handleRegisterJWSTFJ21(in, out); break;
                default: out.println("GOODBYE"); break;
            }

            client.close();
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Client error: " + e.getMessage());
        }
    }

    private void handleQueryServers(BufferedReader in, PrintWriter out, int port)
    {
        boolean enabled = port == 20000 ? query20000Enabled : query49152Enabled;
        boolean requireNationalId = port == 20000 ? query20000RequireNationalId : query49152RequireNationalId;
        boolean requireJWSTFJ21 = port == 20000 ? query20000RequireJWSTFJ21 : query49152RequireJWSTFJ21;

        if (!enabled)
        {
            out.println("DENIED: Query for port " + port + " servers is disabled by admin");
            return;
        }

        try
        {
            if (requireNationalId)
            {
                out.println("NATIONAL_ID_REQUIRED>");
                String nationalId = in.readLine();
                if (nationalId == null || nationalId.isBlank())
                {
                    out.println("REJECTED: National ID required");
                    return;
                }
                out.println("NATIONAL_ID_ACCEPTED");
            }

            if (requireJWSTFJ21)
            {
                out.println("JWSTNJ21_RANK4_VERIFICATION_REQUIRED>");
                out.println("PROVIDE: HOST PORT");
                String serverInfo = in.readLine();
                if (serverInfo == null || !verifyJWSTFJ21Rank4(serverInfo))
                {
                    out.println("REJECTED: Working Rank 4 JWSTNJ21 server required");
                    return;
                }
                out.println("JWSTNJ21_VERIFIED");
            }

            // Return server list
            String file = port == 20000 ? SERVERS_20000 : SERVERS_49152;
            List<String> entries = parseServerList(file);
            out.println("--- PORT " + port + " SERVERS ---");
            for (String entry : entries) out.println(entry);
            out.println("--- END ---");
        }
        catch (Exception e)
        {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private List<String> parseServerList(String xmlFile)
    {
        List<String> results = new ArrayList<>();
        try
        {
            File f = new File(xmlFile);
            if (!f.exists()) { results.add("NO SERVERS CONFIGURED"); return results; }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            NodeList servers = doc.getElementsByTagName("server");

            for (int i = 0; i < servers.getLength(); i++)
            {
                Element srv = (Element) servers.item(i);
                String name = getTagValue(srv, "name");
                String host = getTagValue(srv, "host");
                String port = getTagValue(srv, "port");
                NodeList addresses = srv.getElementsByTagName("address");

                StringBuilder sb = new StringBuilder();
                sb.append(name != null ? name : "Unknown").append(" | ").append(host != null ? host : "");
                sb.append(" | Port: ").append(port);
                for (int j = 0; j < addresses.getLength(); j++)
                {
                    sb.append(" | IP: ").append(addresses.item(j).getTextContent());
                }
                results.add(sb.toString());
            }
        }
        catch (Exception e) { results.add("ERROR READING SERVER LIST"); }
        return results;
    }

    private boolean verifyJWSTFJ21Rank4(String serverInfo)
    {
        // Attempt to connect to the declared JWSTNJ21 server and verify rank 4
        try
        {
            String[] parts = serverInfo.trim().split("\\s+");
            if (parts.length < 2) return false;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            Socket probe = new Socket();
            probe.connect(new InetSocketAddress(host, port), 5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(probe.getInputStream()));
            PrintWriter w = new PrintWriter(probe.getOutputStream(), true);

            w.println("RANK");
            String response = r.readLine();
            probe.close();

            return response != null && response.contains("4");
        }
        catch (Exception e) { return false; }
    }

    private void handleRegisterJWSTFJ21(BufferedReader in, PrintWriter out)
    {
        if (!registerEnabled)
        {
            out.println("DENIED: JWSTNJ21 registration is disabled by admin");
            return;
        }

        try
        {
            out.println("REGISTER RANK " + registerRequiredRank + " JWSTNJ21 SERVER");
            out.println("https://github.com/mearvk/Java.Web.Server.Telnet.Front.Java.21");

            if (registerRequirePublicKey)
            {
                out.println("PUBLIC_KEY_REQUIRED>");
                out.println("Obtain public key from: " + JWSTNJ21_PUBLIC_KEY_URL);
                out.println("PASTE PUBLIC KEY (single line, base64):");
                String publicKey = in.readLine();
                if (publicKey == null || publicKey.isBlank())
                {
                    out.println("REJECTED: Public key required to register");
                    return;
                }

                out.println("SERVER_HOST>");
                String host = in.readLine();
                out.println("SERVER_PORT>");
                String port = in.readLine();

                if (host == null || host.isBlank() || port == null || port.isBlank())
                {
                    out.println("REJECTED: Host and port required");
                    return;
                }

                // Verify it's actually a rank 4 JWSTNJ21
                if (!verifyJWSTFJ21Rank4(host.trim() + " " + port.trim()))
                {
                    out.println("REJECTED: Could not verify Rank " + registerRequiredRank + " JWSTNJ21 at " + host + ":" + port);
                    return;
                }

                // Store the public key
                String keyFile = REGISTERED_KEYS_DIR + host.trim().replace(".", "_") + "_" + port.trim() + ".pub";
                Files.writeString(Paths.get(keyFile), publicKey.trim());

                System.out.println("[ModuleInstallServer] Registered JWSTNJ21 Rank " + registerRequiredRank +
                    " server: " + host.trim() + ":" + port.trim());
                out.println("REGISTERED: " + host.trim() + ":" + port.trim() + " (Rank " + registerRequiredRank + ")");
            }
        }
        catch (Exception e)
        {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleModuleInstall(BufferedReader in, PrintWriter out, Socket client)
    {
        try
        {
            out.println("MODULE INSTALL - Requires Installer ID from Max Rupplin - MEARVK LLC");
            out.println("INSTALLER_ID>");
            String installerId = in.readLine();

            if (!validateInstallerId(installerId))
            {
                out.println("REJECTED: Invalid Installer ID (format: MEARVK-XXXX-XXXX-XXXX)");
                return;
            }

            out.println("NATIONAL_ID>");
            String nationalId = in.readLine();
            if (nationalId == null || nationalId.isBlank())
            {
                out.println("REJECTED: National ID required");
                return;
            }

            out.println("MORAL_RATING>");
            String moralRating = in.readLine();
            if (!validateMoralRating(moralRating))
            {
                out.println("REJECTED: Moral Rating must be Very Good or better");
                return;
            }

            out.println("IQ>");
            String iqStr = in.readLine();
            int iq;
            try { iq = Integer.parseInt(iqStr.trim()); }
            catch (Exception e) { out.println("REJECTED: Invalid IQ"); return; }

            if (iq <= 125)
            {
                out.println("REJECTED: IQ must be over 125");
                return;
            }

            out.println("CREDENTIALS ACCEPTED");
            out.println("SEND FILE: FILENAME then FILESIZE then BYTES via binary stream");

            // Switch to binary for file transfer
            DataInputStream dis = new DataInputStream(client.getInputStream());
            String filename = dis.readUTF();
            long filesize = dis.readLong();

            if (!filename.endsWith(".jar") && !filename.endsWith(".sql"))
            {
                out.println("REJECTED: Only .jar and .sql files accepted");
                return;
            }

            byte[] data = new byte[(int) filesize];
            dis.readFully(data);

            Path target = Paths.get(MODULES_DIR, filename);
            Files.write(target, data);

            System.out.println("[ModuleInstallServer] Installed: " + filename +
                " | Installer: " + installerId.substring(0, 11) + "****");
            out.println("INSTALLED: " + filename);
        }
        catch (Exception e)
        {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private boolean validateInstallerId(String id)
    {
        if (id == null || id.isEmpty()) return false;
        if (!id.startsWith("MEARVK-")) return false;
        if (id.length() != 19) return false;
        return true;
    }

    private boolean validateMoralRating(String rating)
    {
        if (rating == null) return false;
        String r = rating.trim().toLowerCase();
        return r.equals("very good") || r.equals("excellent") ||
               r.equals("outstanding") || r.equals("exceptional");
    }
}
