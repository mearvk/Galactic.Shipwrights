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
            String host = new URI(url).toURL().getHost();
            SSLContext ctx = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            X509TrustManager defaultTm = (X509TrustManager) tmf.getTrustManagers()[0];
            CertCaptureTrustManager captureTm = new CertCaptureTrustManager(defaultTm);

            ctx.init(null, new TrustManager[]{captureTm}, null);
            SSLSocketFactory factory = ctx.getSocketFactory();

            SSLSocket socket = (SSLSocket) factory.createSocket(host, 443);
            socket.setSoTimeout(30000);
            socket.startHandshake();
            socket.close();

            X509Certificate[] chain = captureTm.getChain();
            if (chain != null && chain.length > 0)
            {
                String folder = "src/modules/" + topic + "/certs";
                Files.createDirectories(Paths.get(folder));

                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);

                for (int i = 0; i < chain.length; i++)
                {
                    ks.setCertificateEntry(host + "-" + i, chain[i]);
                }

                String ksPath = folder + "/" + host + ".jks";
                try (FileOutputStream fos = new FileOutputStream(ksPath))
                {
                    ks.store(fos, "changeit".toCharArray());
                }

                System.out.println("[SSL] Saved " + chain.length + " cert(s) for " + host + " -> " + ksPath);
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
