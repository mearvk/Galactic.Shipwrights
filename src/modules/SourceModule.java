package modules;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;

public class SourceModule
{
    private String name;
    private String url;
    private String topic;
    private int port;

    public SourceModule(String name, String url, String topic)
    {
        this.name = name;
        this.url = url;
        this.topic = topic;
        this.port = url.startsWith("https") ? 443 : 80;
    }

    public boolean connect()
    {
        try
        {
            URL target = new URI(url).toURL();
            HttpURLConnection conn = openConnection(target);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 400;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String poll()
    {
        try
        {
            URL target = new URI(url).toURL();
            HttpURLConnection conn = openConnection(target);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

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
            e.printStackTrace();
            return null;
        }
    }

    public void save(String content)
    {
        try
        {
            String folder = "src/" + topic;
            Files.createDirectories(Paths.get(folder));
            String filename = LocalDate.now() + "." + topic + ".data";
            Files.writeString(Paths.get(folder, filename), content);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private HttpURLConnection openConnection(URL target) throws Exception
    {
        if (port == 443)
        {
            HttpsURLConnection conn = (HttpsURLConnection) target.openConnection();
            return conn;
        }
        else
        {
            return (HttpURLConnection) target.openConnection();
        }
    }

    public int getPort()
    {
        return port;
    }

    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url;
    }

    public String getTopic()
    {
        return topic;
    }
}
