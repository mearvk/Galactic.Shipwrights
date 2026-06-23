package utils;

import modules.SourceModule;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class Reacher
{
    private String sourcesPath;

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
            NodeList sources = doc.getElementsByTagName("source");

            for (int i = 0; i < sources.getLength(); i++)
            {
                Element source = (Element) sources.item(i);
                String name = source.getElementsByTagName("name").item(0).getTextContent();
                String urlStr = source.getElementsByTagName("url").item(0).getTextContent();
                String topic = source.getElementsByTagName("topic").item(0).getTextContent();

                SourceModule module = new SourceModule(name, urlStr, topic);

                System.out.println("[Reacher] Connecting to: " + name + " (" + urlStr + ") port " + module.getPort());

                if (module.getPort() == 443)
                {
                    module.exchangeAndSaveSSLCerts();
                }

                if (module.connect())
                {
                    System.out.println("[Reacher] " + name + " -> CONNECTED. Polling...");
                    String content = module.poll();
                    if (content != null)
                    {
                        module.save(content);
                        System.out.println("[Reacher] " + name + " -> SAVED (" + content.length() + " bytes)");
                    }
                    else
                    {
                        System.out.println("[Reacher] " + name + " -> POLL FAILED (null response)");
                    }
                }
                else
                {
                    System.out.println("[Reacher] " + name + " -> CONNECTION FAILED");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
