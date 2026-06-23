package utils;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Reflective integration engine for JWSTFJ21 (Java.Web.Server.Telnet.Front.Java.21).
 * Discovers JWSTFJ21's PortRegistry, Module, and VirtualChannel classes at runtime.
 * Registers Galactic Shipwrights' listeners under extended virtual ports.
 * Degrades to standalone mode if JWSTFJ21 is not on the classpath.
 *
 * https://github.com/mearvk/Java.Web.Server.Telnet.Front.Java.21
 */
public class JWSTFJ21Masquerade implements Runnable
{
    private static final String CONFIG_FILE = "configuration/jwstfj21-integration.xml";

    private String moduleName = "galactic-shipwrights";
    private int rank = 4;
    private String version = "1.00.0";
    private int retryInterval = 30000;
    private int maxAttempts = 10;
    private String parentHost = "127.0.0.1";
    private int parentPort = 2000;

    private boolean integrated = false;

    // Reflection handles
    private Class<?> portRegistryClass;
    private Class<?> moduleClass;
    private Class<?> virtualChannelClass;
    private Object portRegistryInstance;

    public boolean isIntegrated() { return integrated; }

    @Override
    public void run()
    {
        loadConfig();
        System.out.println("[JWSTFJ21Masquerade] Attempting integration with JWSTFJ21...");

        for (int attempt = 1; attempt <= maxAttempts; attempt++)
        {
            if (attemptIntegration())
            {
                integrated = true;
                System.out.println("[JWSTFJ21Masquerade] INTEGRATED — module '" + moduleName +
                    "' registered at Rank " + rank);
                announceModule();
                return;
            }

            if (attempt < maxAttempts)
            {
                System.out.println("[JWSTFJ21Masquerade] Attempt " + attempt + "/" + maxAttempts +
                    " failed. Retrying in " + (retryInterval / 1000) + "s...");
                try { Thread.sleep(retryInterval); }
                catch (InterruptedException e) { break; }
            }
        }

        System.out.println("[JWSTFJ21Masquerade] STANDALONE — JWSTFJ21 not available. " +
            "All physical listeners operational.");
    }

    private boolean attemptIntegration()
    {
        try
        {
            portRegistryClass = Class.forName("com.mearvk.jwstfj21.PortRegistry");
            moduleClass = Class.forName("com.mearvk.jwstfj21.Module");
            virtualChannelClass = Class.forName("com.mearvk.jwstfj21.VirtualChannel");

            // Get PortRegistry singleton
            Method getInstance = portRegistryClass.getMethod("getInstance");
            portRegistryInstance = getInstance.invoke(null);

            // Register virtual ports
            registerVirtualPorts();

            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
        catch (Exception e)
        {
            System.out.println("[JWSTFJ21Masquerade] Integration error: " + e.getMessage());
            return false;
        }
    }

    private void registerVirtualPorts()
    {
        try
        {
            Method registerVirtualPort = portRegistryClass.getMethod(
                "registerVirtualPort", int.class, int.class, String.class);

            // Parse config for mappings
            File f = new File(CONFIG_FILE);
            if (!f.exists()) return;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            NodeList mappings = doc.getElementsByTagName("mapping");

            for (int i = 0; i < mappings.getLength(); i++)
            {
                Element m = (Element) mappings.item(i);
                int virtualPort = Integer.parseInt(m.getAttribute("virtual"));
                int localPort = Integer.parseInt(m.getAttribute("local"));
                String handler = m.getAttribute("handler");

                registerVirtualPort.invoke(portRegistryInstance, virtualPort, localPort, handler);
                System.out.println("[JWSTFJ21Masquerade] Registered virtual:" + virtualPort +
                    " -> local:" + localPort + " (" + handler + ")");
            }
        }
        catch (Exception e)
        {
            System.out.println("[JWSTFJ21Masquerade] Virtual port registration error: " + e.getMessage());
        }
    }

    private void announceModule()
    {
        try
        {
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(parentHost, parentPort), 5000);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

            out.println("<nwe-route><module>" + moduleName + "</module>" +
                "<rank>" + rank + "</rank>" +
                "<version>" + version + "</version>" +
                "<ports>71001,72000,90000,99152</ports></nwe-route>");

            sock.close();
            System.out.println("[JWSTFJ21Masquerade] Announced to JWSTFJ21 directory at " +
                parentHost + ":" + parentPort);
        }
        catch (Exception e)
        {
            System.out.println("[JWSTFJ21Masquerade] Announcement failed (non-fatal): " + e.getMessage());
        }
    }

    public void deregister()
    {
        if (!integrated || portRegistryInstance == null) return;
        try
        {
            Method deregister = portRegistryClass.getMethod("deregisterModule", String.class);
            deregister.invoke(portRegistryInstance, moduleName);
            integrated = false;
            System.out.println("[JWSTFJ21Masquerade] Deregistered from JWSTFJ21");
        }
        catch (Exception e) { /* silent */ }
    }

    private void loadConfig()
    {
        try
        {
            File f = new File(CONFIG_FILE);
            if (!f.exists()) return;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            doc.getDocumentElement().normalize();

            NodeList moduleNodes = doc.getElementsByTagName("module");
            if (moduleNodes.getLength() > 0)
            {
                Element m = (Element) moduleNodes.item(0);
                NodeList n = m.getElementsByTagName("name");
                if (n.getLength() > 0) moduleName = n.item(0).getTextContent().trim();
                n = m.getElementsByTagName("rank");
                if (n.getLength() > 0) rank = Integer.parseInt(n.item(0).getTextContent().trim());
                n = m.getElementsByTagName("version");
                if (n.getLength() > 0) version = n.item(0).getTextContent().trim();
            }

            NodeList retryNodes = doc.getElementsByTagName("retryPolicy");
            if (retryNodes.getLength() > 0)
            {
                Element r = (Element) retryNodes.item(0);
                NodeList n = r.getElementsByTagName("intervalSeconds");
                if (n.getLength() > 0) retryInterval = Integer.parseInt(n.item(0).getTextContent().trim()) * 1000;
                n = r.getElementsByTagName("maxAttempts");
                if (n.getLength() > 0) maxAttempts = Integer.parseInt(n.item(0).getTextContent().trim());
            }

            NodeList pdNodes = doc.getElementsByTagName("parentDirectory");
            if (pdNodes.getLength() > 0)
            {
                Element pd = (Element) pdNodes.item(0);
                NodeList n = pd.getElementsByTagName("host");
                if (n.getLength() > 0) parentHost = n.item(0).getTextContent().trim();
                n = pd.getElementsByTagName("port");
                if (n.getLength() > 0) parentPort = Integer.parseInt(n.item(0).getTextContent().trim());
            }
        }
        catch (Exception e)
        {
            System.out.println("[JWSTFJ21Masquerade] Config load error: " + e.getMessage());
        }
    }
}
