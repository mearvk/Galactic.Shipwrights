package modules.arxiv;

import modules.SourceModule;

public class ArxivModule extends SourceModule
{
    // arXiv API: http://export.arxiv.org/api/query?search_query=QUERY&start=0&max_results=50
    // Returns Atom XML feed with entries containing title, summary, authors, links
    // Supports field prefixes: ti: (title), au: (author), abs: (abstract), cat: (category)
    // Boolean: AND, OR, ANDNOT

    private static final String SEARCH_URL = "http://export.arxiv.org/api/query?search_query=";
    private static final String SEARCH_SUFFIX = "&start=0&max_results=50";

    public ArxivModule()
    {
        super("arXiv", "https://arxiv.org", "science");
    }

    public String search(String query)
    {
        String searchUrl = SEARCH_URL + "all:" + query.replace(" ", "+") + SEARCH_SUFFIX;
        return fetch(searchUrl);
    }

    public String searchByTitle(String title)
    {
        String searchUrl = SEARCH_URL + "ti:" + title.replace(" ", "+") + SEARCH_SUFFIX;
        return fetch(searchUrl);
    }

    public String searchByAuthor(String author)
    {
        String searchUrl = SEARCH_URL + "au:" + author.replace(" ", "+") + SEARCH_SUFFIX;
        return fetch(searchUrl);
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
