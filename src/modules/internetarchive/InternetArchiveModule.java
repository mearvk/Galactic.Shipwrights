package modules.internetarchive;

import modules.SourceModule;

public class InternetArchiveModule extends SourceModule
{
    // Advanced Search API: https://archive.org/advancedsearch.php?q=QUERY&fl[]=identifier&fl[]=title&output=json&rows=50
    // Uses Lucene query syntax; supports field:value, ranges, boolean operators
    // Metadata API: https://archive.org/metadata/IDENTIFIER

    private static final String SEARCH_URL = "https://archive.org/advancedsearch.php?q=";
    private static final String SEARCH_SUFFIX = "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=date&output=json&rows=50";

    public InternetArchiveModule()
    {
        super("Internet Archive", "https://archive.org", "archives");
    }

    public String search(String query)
    {
        String searchUrl = SEARCH_URL + query.replace(" ", "+") + SEARCH_SUFFIX;
        return fetch(searchUrl);
    }

    public String fetchMetadata(String identifier)
    {
        String metaUrl = getUrl() + "/metadata/" + identifier;
        return fetch(metaUrl);
    }

    private String fetch(String urlStr)
    {
        try
        {
            java.net.URL target = new java.net.URI(urlStr).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
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
