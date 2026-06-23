package modules.wikipedia;

import modules.SourceModule;

public class WikipediaModule extends SourceModule
{
    // MediaWiki API: https://en.wikipedia.org/w/api.php?action=opensearch&search=TERM&format=json
    // For full content: action=query&titles=TITLE&prop=extracts&explaintext&format=json
    // For search results: action=query&list=search&srsearch=TERM&format=json

    private static final String OPENSEARCH_URL = "https://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=";
    private static final String QUERY_URL = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&explaintext&titles=";
    private static final String SEARCH_URL = "https://en.wikipedia.org/w/api.php?action=query&format=json&list=search&srsearch=";

    public WikipediaModule()
    {
        super("Wikipedia", "https://en.wikipedia.org", "general");
    }

    public String search(String query)
    {
        String searchUrl = SEARCH_URL + query.replace(" ", "+");
        return fetch(searchUrl);
    }

    public String opensearch(String query)
    {
        String url = OPENSEARCH_URL + query.replace(" ", "+");
        return fetch(url);
    }

    public String fetchArticle(String title)
    {
        String url = QUERY_URL + title.replace(" ", "_");
        return fetch(url);
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
