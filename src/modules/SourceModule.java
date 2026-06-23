package modules;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
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

    public boolean exchangeAndSaveSSLCerts()
    {
        if (port != 443) return false;

        try
        {
            URL target = new URI(url).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) target.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.connect();

            java.security.cert.Certificate[] certs = conn.getServerCertificates();
            conn.disconnect();

            if (certs != null && certs.length > 0)
            {
                String host = target.getHost();
                String folder = "src/modules/" + topic + "/certs";
                Files.createDirectories(Paths.get(folder));

                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);

                for (int i = 0; i < certs.length; i++)
                {
                    ks.setCertificateEntry(host + "-" + i, certs[i]);
                }

                String ksPath = folder + "/" + host + ".jks";
                try (FileOutputStream fos = new FileOutputStream(ksPath))
                {
                    ks.store(fos, "changeit".toCharArray());
                }

                System.out.println("[SSL] Saved " + certs.length + " cert(s) for " + host + " -> " + ksPath);
                return true;
            }
        }
        catch (Exception e)
        {
            System.out.println("[SSL] Failed for " + name + ": " + e.getMessage());
        }
        return false;
    }

    private static class CertCaptureTrustManager implements X509TrustManager
    {
        private final X509TrustManager delegate;
        private X509Certificate[] chain;

        CertCaptureTrustManager(X509TrustManager delegate)
        {
            this.delegate = delegate;
        }

        public X509Certificate[] getChain() { return chain; }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            delegate.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            this.chain = chain;
            delegate.checkServerTrusted(chain, authType);
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return delegate.getAcceptedIssuers();
        }
    }

    public boolean connect()
    {
        try
        {
            URL target = new URI(url).toURL();
            HttpURLConnection conn = openConnection(target);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
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
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

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
