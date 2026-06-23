package modules.gutenberg;

import modules.SourceModule;
import com.google.gson.*;

public class GutenbergModule extends SourceModule
{
    // Gutendex API: https://gutendex.com/books/?search=TERM
    // Returns JSON with count, results[] containing id, title, authors, formats, subjects, etc.
    // Supports: ?search=, ?topic=, ?languages=, ?author_year_start=, ?author_year_end=, ?sort=popular/ascending/descending

    private static final String GUTENDEX_URL = "https://gutendex.com/books/";

    public GutenbergModule()
    {
        super("Project Gutenberg", "https://www.gutenberg.org", "literature");
    }

    public String search(String query)
    {
        return fetch(GUTENDEX_URL + "?search=" + query.replace(" ", "%20"));
    }

    public String searchByTopic(String topic)
    {
        return fetch(GUTENDEX_URL + "?topic=" + topic.replace(" ", "%20"));
    }

    public String getPopular()
    {
        return fetch(GUTENDEX_URL + "?sort=popular");
    }

    public String getBook(int id)
    {
        return fetch(GUTENDEX_URL + id);
    }

    private String fetch(String urlStr)
    {
        try
        {
            java.net.URL target = new java.net.URI(urlStr).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "GalacticShipwrights/1.0");

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
