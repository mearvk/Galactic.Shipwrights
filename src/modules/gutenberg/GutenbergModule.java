package modules.gutenberg;

import modules.SourceModule;

public class GutenbergModule extends SourceModule
{
    // Search API: https://www.gutenberg.org/ebooks/search/?query=TERM
    // Returns HTML results; use .txt.utf-8 format for plain text downloads
    // Example: https://www.gutenberg.org/ebooks/11.txt.utf-8

    private static final String SEARCH_URL = "https://www.gutenberg.org/ebooks/search/?query=";

    public GutenbergModule()
    {
        super("Project Gutenberg", "https://www.gutenberg.org", "literature");
    }

    public String search(String query)
    {
        String searchUrl = SEARCH_URL + query.replace(" ", "+");
        return fetch(searchUrl);
    }

    public String fetchBook(int bookId)
    {
        String bookUrl = getUrl() + "/ebooks/" + bookId + ".txt.utf-8";
        return fetch(bookUrl);
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
