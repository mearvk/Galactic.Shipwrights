package utils;

import modules.SourceModule;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Reacher
{
    private String sourcesPath;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
            org.w3c.dom.NodeList sources = doc.getElementsByTagName("source");

            for (int i = 0; i < sources.getLength(); i++)
            {
                org.w3c.dom.Element source = (org.w3c.dom.Element) sources.item(i);
                String name = source.getElementsByTagName("name").item(0).getTextContent();
                String urlStr = source.getElementsByTagName("url").item(0).getTextContent();
                String topic = source.getElementsByTagName("topic").item(0).getTextContent();
                String type = source.getElementsByTagName("type").item(0).getTextContent();

                SourceModule module = new SourceModule(name, urlStr, topic);

                System.out.println("\n[Reacher] === " + name + " (" + urlStr + ") port " + module.getPort() + " ===");

                if (module.getPort() == 443)
                {
                    module.exchangeAndSaveSSLCerts();
                }

                if (module.connect())
                {
                    System.out.println("[Reacher] CONNECTED.");

                    String content = fetchBySource(name, urlStr, type, topic);

                    if (content != null && content.length() > 100)
                    {
                        String safeName = name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                        String folder = "src/edifiction/" + safeName;
                        Files.createDirectories(Paths.get(folder));
                        String filename = LocalDate.now() + "." + topic + ".data";
                        Path filePath = Paths.get(folder, filename);

                        if (Files.exists(filePath))
                        {
                            System.out.println("[Reacher] SKIPPED (file exists: " + filePath + ")");
                        }
                        else
                        {
                            Files.writeString(filePath, content);
                            System.out.println("[Reacher] SAVED " + content.length() + " bytes -> " + filePath);
                        }
                    }
                    else
                    {
                        System.out.println("[Reacher] NO CONTENT RETRIEVED");
                    }
                }
                else
                {
                    System.out.println("[Reacher] CONNECTION FAILED");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Routes to the appropriate fetcher based on source identity and type.
     */
    private String fetchBySource(String name, String baseUrl, String type, String topic)
    {
        try
        {
            // --- BOOKS & TEXTS ---
            if (baseUrl.contains("gutenberg"))
                return fetchGutenberg(baseUrl);
            if (baseUrl.contains("openlibrary"))
                return fetchOpenLibrary(baseUrl);
            if (baseUrl.contains("archive.org"))
                return fetchInternetArchive(baseUrl);
            if (baseUrl.contains("standardebooks"))
                return fetchStandardEbooks(baseUrl);
            if (baseUrl.contains("hathitrust"))
                return fetchHTML(baseUrl + "/cgi/ls?field1=ocr;q1=public+domain;a=srchls;lmt=25");
            if (baseUrl.contains("perseus"))
                return fetchHTML(baseUrl + "/hopper/collection?collection=Perseus:collection:Greco-Roman");
            if (baseUrl.contains("biodiversitylibrary"))
                return fetchJSON(baseUrl + "/api3?op=GetTitleMetadata&id=1000&format=json");
            if (baseUrl.contains("dp.la"))
                return fetchJSON(baseUrl + "/api/v2/items?q=trade&page_size=25");
            if (baseUrl.contains("publicdomainreview"))
                return fetchHTML(baseUrl + "/collections/");

            // --- ENCYCLOPEDIAS ---
            if (baseUrl.contains("wikipedia"))
                return fetchWikipedia(baseUrl);
            if (baseUrl.contains("wiktionary"))
                return fetchMediaWikiSearch(baseUrl, "shipwright|swain|bove|rudder|architect|nail");
            if (baseUrl.contains("wikisource"))
                return fetchMediaWikiSearch(baseUrl, "ship+building|trade|navigation");
            if (baseUrl.contains("wikibooks"))
                return fetchMediaWikiSearch(baseUrl, "engineering|construction|woodworking");

            // --- PAPERS ---
            if (baseUrl.contains("arxiv"))
                return fetchArxiv();
            if (baseUrl.contains("ncbi.nlm.nih.gov"))
                return fetchJSON("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pmc&term=shipbuilding+OR+naval+architecture&retmax=25&retmode=json");
            if (baseUrl.contains("doaj"))
                return fetchJSON(baseUrl + "/api/search/articles/naval+architecture?page=1&pageSize=25");
            if (baseUrl.contains("semanticscholar"))
                return fetchJSON("https://api.semanticscholar.org/graph/v1/paper/search?query=shipbuilding+materials&limit=25&fields=title,year,authors");
            if (baseUrl.contains("core.ac.uk"))
                return fetchJSON("https://api.core.ac.uk/v3/search/works?q=naval+architecture&limit=25");

            // --- IMAGES & ART ---
            if (baseUrl.contains("collectionapi.metmuseum"))
                return fetchJSON(baseUrl + "/search?q=ship&hasImages=true");
            if (baseUrl.contains("metmuseum.org"))
                return fetchJSON("https://collectionapi.metmuseum.org/public/collection/v1/search?q=ship&hasImages=true");
            if (baseUrl.contains("api.artic.edu"))
                return fetchJSON(baseUrl + "/artworks/search?q=ship&limit=25&fields=id,title,artist_display,date_display,image_id");
            if (baseUrl.contains("api.si.edu"))
                return fetchJSON(baseUrl + "/search?q=ship+building&rows=25");
            if (baseUrl.contains("rijksmuseum"))
                return fetchHTML(baseUrl + "/en/search?q=ship&s=relevance");
            if (baseUrl.contains("europeana"))
                return fetchHTML(baseUrl + "/en/search?query=shipbuilding&page=1");
            if (baseUrl.contains("getty.edu"))
                return fetchHTML(baseUrl + "/art/collection/search?q=ship");
            if (baseUrl.contains("api.unsplash"))
                return fetchJSON(baseUrl + "/search/photos?query=ship+building&per_page=25");
            if (baseUrl.contains("images-api.nasa"))
                return fetchJSON(baseUrl + "/search?q=spacecraft+construction&media_type=image");
            if (baseUrl.contains("commons.wikimedia"))
                return fetchMediaWikiSearch(baseUrl, "shipbuilding|ship+construction");
            if (baseUrl.contains("loc.gov/pictures"))
                return fetchJSON("https://www.loc.gov/pictures/search/?q=ship+building&fo=json&c=25");

            // --- MANUALS & GOVERNMENT ---
            if (baseUrl.contains("loc.gov") && !baseUrl.contains("pictures") && !baseUrl.contains("chronicling"))
                return fetchJSON("https://www.loc.gov/search/?q=shipbuilding+manuals&fo=json&c=25");
            if (baseUrl.contains("chroniclingamerica"))
                return fetchJSON(baseUrl + "/search/pages/results/?andtext=shipwright&format=json&page=1");
            if (baseUrl.contains("govinfo"))
                return fetchHTML(baseUrl + "/search?query=shipbuilding&offset=0");
            if (baseUrl.contains("ntrl.ntis"))
                return fetchHTML(baseUrl + "/NTRL/dashboard/searchResults/titleDetail/");
            if (baseUrl.contains("osti.gov"))
                return fetchJSON("https://www.osti.gov/api/v1/records?title=materials+engineering&rows=25");
            if (baseUrl.contains("data.gov"))
                return fetchJSON("https://catalog.data.gov/api/3/action/package_search?q=engineering&rows=25");
            if (baseUrl.contains("uspto"))
                return fetchHTML(baseUrl + "/patents/search?q=shipbuilding");
            if (baseUrl.contains("everyspec"))
                return fetchHTML(baseUrl + "/MIL-STD/");
            if (baseUrl.contains("openstax"))
                return fetchJSON("https://openstax.org/apps/cms/api/v2/pages/?type=books.Book&fields=title,cover_url&limit=50");
            if (baseUrl.contains("ocw.mit.edu"))
                return fetchJSON("https://ocw.mit.edu/api/v0/courses/?limit=25&topic=Engineering");

            // --- DATA & SCIENCE ---
            if (baseUrl.contains("api.nasa.gov"))
                return fetchJSON(baseUrl + "/planetary/apod?count=10&api_key=DEMO_KEY");
            if (baseUrl.contains("usgs.gov"))
                return fetchHTML(baseUrl + "/science/science-explorer?q=geology");
            if (baseUrl.contains("ncdc.noaa"))
                return fetchHTML(baseUrl + "/cdo-web/");
            if (baseUrl.contains("worldcat"))
                return fetchHTML(baseUrl + "/search?q=shipbuilding&qt=results_page");
            if (baseUrl.contains("opendoar"))
                return fetchHTML(baseUrl + "/search.html?q=engineering");

            // Fallback: try to parse HTML for links
            return fetchHTML(baseUrl);
        }
        catch (Exception e)
        {
            System.out.println("[Reacher] Error fetching " + name + ": " + e.getMessage());
            return null;
        }
    }

    // ===== SPECIALIZED FETCHERS =====

    private String fetchGutenberg(String baseUrl)
    {
        StringBuilder sb = new StringBuilder();
        String[] queries = {"science", "history", "engineering", "navigation", "architecture", "trade", "shipbuilding"};
        String gutendex = "https://gutendex.com/books/";

        for (String q : queries)
        {
            String json = fetchRaw(gutendex + "?search=" + q);
            if (json != null)
            {
                try
                {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    int count = obj.get("count").getAsInt();
                    JsonArray results = obj.getAsJsonArray("results");
                    sb.append("--- GUTENBERG (Gutendex): ").append(q).append(" (").append(count).append(" total) ---\n");
                    for (int i = 0; i < results.size(); i++)
                    {
                        JsonObject book = results.get(i).getAsJsonObject();
                        int id = book.get("id").getAsInt();
                        String title = book.get("title").getAsString();
                        String author = "unknown";
                        if (book.has("authors") && book.getAsJsonArray("authors").size() > 0)
                        {
                            author = book.getAsJsonArray("authors").get(0).getAsJsonObject().get("name").getAsString();
                        }
                        String textUrl = "";
                        if (book.has("formats"))
                        {
                            JsonObject formats = book.getAsJsonObject("formats");
                            if (formats.has("text/plain; charset=utf-8"))
                                textUrl = formats.get("text/plain; charset=utf-8").getAsString();
                            else if (formats.has("text/plain"))
                                textUrl = formats.get("text/plain").getAsString();
                        }
                        sb.append("  [").append(id).append("] ").append(title).append(" | ").append(author);
                        if (!textUrl.isEmpty()) sb.append(" | ").append(textUrl);
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                catch (Exception e) { sb.append(json).append("\n"); }
            }
        }

        // Also get popular books
        String popular = fetchRaw(gutendex + "?sort=popular");
        if (popular != null)
        {
            try
            {
                JsonObject obj = JsonParser.parseString(popular).getAsJsonObject();
                JsonArray results = obj.getAsJsonArray("results");
                sb.append("--- GUTENBERG POPULAR (Top ").append(results.size()).append(") ---\n");
                for (int i = 0; i < results.size(); i++)
                {
                    JsonObject book = results.get(i).getAsJsonObject();
                    int id = book.get("id").getAsInt();
                    String title = book.get("title").getAsString();
                    int downloads = book.get("download_count").getAsInt();
                    sb.append("  [").append(id).append("] ").append(title).append(" (").append(downloads).append(" downloads)\n");
                }
            }
            catch (Exception e) { /* skip */ }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    private String fetchOpenLibrary(String baseUrl)
    {
        StringBuilder sb = new StringBuilder();
        String[] queries = {"shipbuilding", "naval+architecture", "trade+history", "engineering", "woodworking", "carpentry"};
        for (String q : queries)
        {
            String json = fetchRaw(baseUrl + "/search.json?q=" + q + "&limit=25");
            if (json != null)
            {
                try
                {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray docs = obj.getAsJsonArray("docs");
                    sb.append("--- OPEN LIBRARY: ").append(q).append(" (").append(obj.get("numFound")).append(" total) ---\n");
                    for (int i = 0; i < Math.min(docs.size(), 25); i++)
                    {
                        JsonObject book = docs.get(i).getAsJsonObject();
                        String title = book.has("title") ? book.get("title").getAsString() : "untitled";
                        String author = book.has("author_name") ? book.getAsJsonArray("author_name").get(0).getAsString() : "unknown";
                        String key = book.has("key") ? book.get("key").getAsString() : "";
                        int year = book.has("first_publish_year") ? book.get("first_publish_year").getAsInt() : 0;
                        sb.append("  ").append(title).append(" | ").append(author).append(" | ").append(year).append(" | https://openlibrary.org").append(key).append("\n");
                    }
                    sb.append("\n");
                }
                catch (Exception e) { sb.append(json).append("\n"); }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String fetchInternetArchive(String baseUrl)
    {
        StringBuilder sb = new StringBuilder();
        String[] queries = {"shipbuilding", "naval+architecture", "trade", "engineering+manual", "woodworking"};
        for (String q : queries)
        {
            String url = baseUrl + "/advancedsearch.php?q=" + q + "+AND+mediatype:texts&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=year&output=json&rows=25";
            String json = fetchRaw(url);
            if (json != null)
            {
                try
                {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    JsonObject response = obj.getAsJsonObject("response");
                    JsonArray docs = response.getAsJsonArray("docs");
                    sb.append("--- INTERNET ARCHIVE: ").append(q).append(" (").append(response.get("numFound")).append(" total) ---\n");
                    for (int i = 0; i < docs.size(); i++)
                    {
                        JsonObject item = docs.get(i).getAsJsonObject();
                        String title = item.has("title") ? item.get("title").getAsString() : "untitled";
                        String id = item.has("identifier") ? item.get("identifier").getAsString() : "";
                        String creator = item.has("creator") ? item.get("creator").getAsString() : "unknown";
                        sb.append("  ").append(title).append(" | ").append(creator).append(" | https://archive.org/details/").append(id).append("\n");
                    }
                    sb.append("\n");
                }
                catch (Exception e) { sb.append(json).append("\n"); }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String fetchStandardEbooks(String baseUrl)
    {
        String html = fetchRaw(baseUrl + "/ebooks");
        if (html == null) return null;
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        Elements books = doc.select("li[typeof='schema:Book']");
        StringBuilder sb = new StringBuilder("--- STANDARD EBOOKS CATALOG ---\n");
        for (Element book : books)
        {
            String title = book.select("span[property='schema:name']").text();
            String author = book.select("span.author").text();
            String link = book.select("a").attr("href");
            sb.append("  ").append(title).append(" | ").append(author).append(" | ").append(baseUrl).append(link).append("\n");
        }
        return sb.toString();
    }

    private String fetchWikipedia(String baseUrl)
    {
        StringBuilder sb = new StringBuilder();
        String[] terms = {"shipbuilding", "naval+architecture", "shipwright", "trade+wind", "rudder", "keel"};
        for (String term : terms)
        {
            String json = fetchRaw(baseUrl + "/w/api.php?action=query&format=json&list=search&srsearch=" + term + "&srlimit=25");
            if (json != null)
            {
                try
                {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray results = obj.getAsJsonObject("query").getAsJsonArray("search");
                    sb.append("--- WIKIPEDIA: ").append(term).append(" (").append(results.size()).append(" results) ---\n");
                    for (int i = 0; i < results.size(); i++)
                    {
                        JsonObject r = results.get(i).getAsJsonObject();
                        String title = r.get("title").getAsString();
                        String snippet = Jsoup.parse(r.get("snippet").getAsString()).text();
                        sb.append("  ").append(title).append(" - ").append(snippet).append("\n");
                    }
                    sb.append("\n");
                }
                catch (Exception e) { sb.append(json).append("\n"); }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String fetchMediaWikiSearch(String baseUrl, String searchTerms)
    {
        StringBuilder sb = new StringBuilder();
        for (String term : searchTerms.split("\\|"))
        {
            String json = fetchRaw(baseUrl + "/w/api.php?action=query&format=json&list=search&srsearch=" + term.trim() + "&srlimit=10");
            if (json != null)
            {
                try
                {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray results = obj.getAsJsonObject("query").getAsJsonArray("search");
                    sb.append("--- ").append(baseUrl).append(" SEARCH: ").append(term).append(" ---\n");
                    for (int i = 0; i < results.size(); i++)
                    {
                        JsonObject r = results.get(i).getAsJsonObject();
                        sb.append("  ").append(r.get("title").getAsString()).append("\n");
                    }
                    sb.append("\n");
                }
                catch (Exception e) { /* skip */ }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String fetchArxiv()
    {
        StringBuilder sb = new StringBuilder();
        String[] queries = {"naval+architecture", "ship+design", "materials+engineering", "structural+engineering", "woodworking"};
        for (String q : queries)
        {
            String xml = fetchRaw("http://export.arxiv.org/api/query?search_query=all:" + q + "&start=0&max_results=25");
            if (xml != null)
            {
                // Parse Atom XML with Jsoup
                org.jsoup.nodes.Document doc = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
                Elements entries = doc.select("entry");
                sb.append("--- ARXIV: ").append(q).append(" (").append(entries.size()).append(" results) ---\n");
                for (Element entry : entries)
                {
                    String title = entry.select("title").text().replaceAll("\\s+", " ");
                    String id = entry.select("id").text();
                    String summary = entry.select("summary").text();
                    if (summary.length() > 150) summary = summary.substring(0, 150) + "...";
                    sb.append("  ").append(title).append("\n    ").append(id).append("\n    ").append(summary).append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ===== GENERIC FETCHERS =====

    private String fetchJSON(String urlStr)
    {
        String raw = fetchRaw(urlStr);
        if (raw == null) return null;
        try
        {
            JsonElement el = JsonParser.parseString(raw);
            return gson.toJson(el);
        }
        catch (Exception e)
        {
            return raw;
        }
    }

    private String fetchHTML(String urlStr)
    {
        String raw = fetchRaw(urlStr);
        if (raw == null) return null;
        try
        {
            org.jsoup.nodes.Document doc = Jsoup.parse(raw);
            doc.select("script, style, nav, footer, header").remove();
            // Extract links and text
            StringBuilder sb = new StringBuilder();
            sb.append("--- PAGE: ").append(urlStr).append(" ---\n");
            sb.append("TITLE: ").append(doc.title()).append("\n\n");

            Elements links = doc.select("a[href]");
            sb.append("LINKS (").append(links.size()).append("):\n");
            int count = 0;
            for (Element link : links)
            {
                String text = link.text().trim();
                String href = link.attr("abs:href");
                if (text.length() > 3 && href.length() > 5)
                {
                    sb.append("  ").append(text).append(" -> ").append(href).append("\n");
                    if (++count >= 100) break;
                }
            }
            sb.append("\nTEXT CONTENT:\n");
            String bodyText = doc.body() != null ? doc.body().text() : "";
            if (bodyText.length() > 5000) bodyText = bodyText.substring(0, 5000) + "...";
            sb.append(bodyText);
            return sb.toString();
        }
        catch (Exception e)
        {
            return raw;
        }
    }

    private String fetchRaw(String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "GalacticShipwrights/1.0 (research project)");
            conn.setRequestProperty("Accept", "application/json, text/html, application/xml, */*");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code >= 400) { conn.disconnect(); return null; }

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
