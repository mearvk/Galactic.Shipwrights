package utils;

import modules.SourceModule;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class Reacher
{
    private String sourcesPath;

    public Reacher()
    {
        this.sourcesPath = "src/sources.xml";
    }

    public Reacher(String sourcesPath)
    {
        this.sourcesPath = sourcesPath;
    }

    public void reach()
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(new File(sourcesPath));
            NodeList sources = doc.getElementsByTagName("source");

            for (int i = 0; i < sources.getLength(); i++)
            {
                Element source = (Element) sources.item(i);
                String name = source.getElementsByTagName("name").item(0).getTextContent();
                String urlStr = source.getElementsByTagName("url").item(0).getTextContent();
                String topic = source.getElementsByTagName("topic").item(0).getTextContent();

                SourceModule module = new SourceModule(name, urlStr, topic);

                System.out.println("[Reacher] Connecting to: " + name + " (" + urlStr + ") port " + module.getPort());

                if (module.getPort() == 443)
                {
                    module.exchangeAndSaveSSLCerts();
                }

                if (module.connect())
                {
                    System.out.println("[Reacher] " + name + " -> CONNECTED.");

                    // Probe what the site supports
                    Map<String, String> capabilities = probe(urlStr);
                    System.out.println("[Reacher] " + name + " -> Capabilities: " + capabilities);

                    // Try API/search endpoints for real data
                    String content = fetchContent(module, urlStr, capabilities);
                    if (content != null)
                    {
                        module.save(content);
                        System.out.println("[Reacher] " + name + " -> SAVED (" + content.length() + " bytes)");
                    }
                    else
                    {
                        System.out.println("[Reacher] " + name + " -> NO CONTENT RETRIEVED");
                    }
                }
                else
                {
                    System.out.println("[Reacher] " + name + " -> CONNECTION FAILED");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Probes a site to determine what HTTP methods and content types it supports,
     * checks for robots.txt, sitemap, API endpoints, and feed availability.
     */
    public Map<String, String> probe(String baseUrl)
    {
        Map<String, String> capabilities = new LinkedHashMap<>();

        // Check allowed HTTP methods via OPTIONS
        capabilities.put("OPTIONS", checkOptions(baseUrl));

        // Check robots.txt for allowed/disallowed paths
        String robotsTxt = fetch(baseUrl + "/robots.txt");
        if (robotsTxt != null && !robotsTxt.contains("<html"))
        {
            capabilities.put("robots.txt", "FOUND");
            String sitemap = extractSitemap(robotsTxt);
            if (sitemap != null) capabilities.put("sitemap", sitemap);
        }
        else
        {
            capabilities.put("robots.txt", "NOT FOUND");
        }

        // Check for common API/feed/search endpoints
        checkEndpoint(capabilities, "api", baseUrl + "/api");
        checkEndpoint(capabilities, "search", baseUrl + "/search");
        checkEndpoint(capabilities, "feed-rss", baseUrl + "/feed");
        checkEndpoint(capabilities, "feed-atom", baseUrl + "/atom.xml");
        checkEndpoint(capabilities, "opensearch", baseUrl + "/opensearch.xml");
        checkEndpoint(capabilities, "oai-pmh", baseUrl + "/oai");

        // Check content types supported
        capabilities.put("content-type", checkContentType(baseUrl));

        return capabilities;
    }

    /**
     * Fetches real content (books, data, articles) based on discovered capabilities.
     */
    private String fetchContent(SourceModule module, String baseUrl, Map<String, String> capabilities)
    {
        StringBuilder allContent = new StringBuilder();

        // Try known search/API patterns based on source URL
        String[] searchPaths = guessSearchPaths(baseUrl);

        for (String path : searchPaths)
        {
            String result = fetch(path);
            if (result != null && result.length() > 100)
            {
                System.out.println("[Reacher]   Found content at: " + path + " (" + result.length() + " bytes)");
                allContent.append("--- SOURCE: ").append(path).append(" ---\n");
                allContent.append(result).append("\n\n");
            }
        }

        // If sitemap found, grab it for catalog of available items
        if (capabilities.containsKey("sitemap") && !capabilities.get("sitemap").equals("NOT FOUND"))
        {
            String sitemapContent = fetch(capabilities.get("sitemap"));
            if (sitemapContent != null)
            {
                allContent.append("--- SITEMAP ---\n").append(sitemapContent).append("\n\n");
            }
        }

        return allContent.length() > 0 ? allContent.toString() : null;
    }

    /**
     * Returns search/data paths to try based on the known source.
     */
    private String[] guessSearchPaths(String baseUrl)
    {
        if (baseUrl.contains("gutenberg"))
        {
            return new String[]{
                baseUrl + "/ebooks/search/?sort_order=downloads",
                baseUrl + "/ebooks/search/?query=science",
                baseUrl + "/ebooks/search/?query=history",
                baseUrl + "/cache/epub/feeds/today.rss"
            };
        }
        else if (baseUrl.contains("openlibrary"))
        {
            return new String[]{
                baseUrl + "/search.json?q=science&limit=25",
                baseUrl + "/search.json?q=history&limit=25",
                baseUrl + "/search.json?q=mathematics&limit=25",
                baseUrl + "/trending/daily.json"
            };
        }
        else if (baseUrl.contains("archive.org"))
        {
            return new String[]{
                baseUrl + "/advancedsearch.php?q=mediatype:texts&fl[]=identifier&fl[]=title&output=json&rows=25",
                baseUrl + "/advancedsearch.php?q=subject:science+AND+mediatype:texts&fl[]=identifier&fl[]=title&output=json&rows=25",
                baseUrl + "/advancedsearch.php?q=subject:history+AND+mediatype:texts&fl[]=identifier&fl[]=title&output=json&rows=25"
            };
        }
        else if (baseUrl.contains("wikipedia"))
        {
            return new String[]{
                baseUrl + "/w/api.php?action=query&format=json&list=search&srsearch=shipbuilding&srlimit=25",
                baseUrl + "/w/api.php?action=query&format=json&list=search&srsearch=naval+architecture&srlimit=25",
                baseUrl + "/w/api.php?action=query&format=json&list=search&srsearch=trade+history&srlimit=25"
            };
        }
        else if (baseUrl.contains("arxiv"))
        {
            return new String[]{
                "http://export.arxiv.org/api/query?search_query=all:engineering&start=0&max_results=25",
                "http://export.arxiv.org/api/query?search_query=all:materials+science&start=0&max_results=25",
                "http://export.arxiv.org/api/query?search_query=all:naval+architecture&start=0&max_results=25"
            };
        }
        // Generic fallback
        return new String[]{
            baseUrl + "/api",
            baseUrl + "/search",
            baseUrl + "/feed"
        };
    }

    private String checkOptions(String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("OPTIONS");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            String allow = conn.getHeaderField("Allow");
            conn.disconnect();
            return allow != null ? allow : "NONE";
        }
        catch (Exception e)
        {
            return "FAILED";
        }
    }

    private void checkEndpoint(Map<String, String> capabilities, String key, String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            conn.disconnect();
            capabilities.put(key, code < 400 ? "AVAILABLE (" + code + ")" : "NOT FOUND (" + code + ")");
        }
        catch (Exception e)
        {
            capabilities.put(key, "UNREACHABLE");
        }
    }

    private String checkContentType(String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            String ct = conn.getContentType();
            conn.disconnect();
            return ct != null ? ct : "unknown";
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }

    private String extractSitemap(String robotsTxt)
    {
        for (String line : robotsTxt.split("\n"))
        {
            if (line.toLowerCase().startsWith("sitemap:"))
            {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        return null;
    }

    private String fetch(String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "GalacticShipwrights/1.0");
            conn.setInstanceFollowRedirects(true);

            if (conn.getResponseCode() >= 400) { conn.disconnect(); return null; }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();
            return sb.toString();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
