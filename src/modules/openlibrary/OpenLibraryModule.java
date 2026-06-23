package modules.openlibrary;

import modules.SourceModule;

public class OpenLibraryModule extends SourceModule
{
    // Search API: https://openlibrary.org/search.json?q=TERM
    // Returns JSON with docs[] array containing title, author_name, key, etc.
    // Rate limit: 1 req/sec unidentified, 3 req/sec with User-Agent header

    private static final String SEARCH_URL = "https://openlibrary.org/search.json?q=";

    public OpenLibraryModule()
    {
        super("Open Library", "https://openlibrary.org", "general");
    }

    public String search(String query)
    {
        String searchUrl = SEARCH_URL + query.replace(" ", "+");
        return fetch(searchUrl);
    }

    public String fetchWork(String workKey)
    {
        // workKey like /works/OL15626917W
        String workUrl = getUrl() + workKey + ".json";
        return fetch(workUrl);
    }

    private String fetch(String urlStr)
    {
        try
        {
            java.net.URL target = new java.net.URI(urlStr).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GalacticShipwrights/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
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
            e.printStackTrace();
            return null;
        }
    }
}
