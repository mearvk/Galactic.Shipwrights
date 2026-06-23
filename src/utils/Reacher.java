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

                if (module.connect())
                {
                    String content = module.poll();
                    if (content != null)
                    {
                        module.save(content);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
