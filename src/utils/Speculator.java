package utils;

import com.google.gson.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

public class Speculator
{
    private String scanDir;
    private String knownServersFile;
    private List<ServerInfo> servers = new ArrayList<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Speculator()
    {
        this.scanDir = "src/edifiction";
        this.knownServersFile = "src/known.port.20000.servers.xml";
    }

    public void speculate()
    {
        System.out.println("[Speculator] Starting analysis...");

        // Load known servers
        loadServers();

        // Study files in edifiction
        Map<String, List<String>> findings = studyFiles();

        // Generate speculations (pattern analysis, correlations, future projections)
        List<Speculation> speculations = generateSpeculations(findings);

        System.out.println("[Speculator] Generated " + speculations.size() + " speculations.");

        // Send interesting results to known servers
        for (Speculation spec : speculations)
        {
            if (spec.interestScore >= 0.5)
            {
                sendToServers(spec);
            }
        }

        // Save local report
        saveReport(speculations);
    }

    private Map<String, List<String>> studyFiles()
    {
        Map<String, List<String>> findings = new LinkedHashMap<>();
        Path dir = Paths.get(scanDir);
        if (!Files.exists(dir)) return findings;

        try
        {
            Files.walk(dir)
                .filter(p -> p.toString().endsWith(".data") || p.toString().endsWith(".txt"))
                .forEach(p -> {
                    try
                    {
                        String content = Files.readString(p);
                        String source = p.getParent().getFileName().toString();
                        List<String> insights = analyzeContent(content, source);
                        if (!insights.isEmpty())
                        {
                            findings.computeIfAbsent(source, k -> new ArrayList<>()).addAll(insights);
                        }
                    }
                    catch (Exception e) { /* skip */ }
                });
        }
        catch (Exception e) { e.printStackTrace(); }

        System.out.println("[Speculator] Studied " + findings.size() + " sources.");
        return findings;
    }

    /**
     * Analyzes content for patterns, topics of interest, and actionable data.
     */
    private List<String> analyzeContent(String content, String source)
    {
        List<String> insights = new ArrayList<>();

        // Extract key phrases (repeated significant terms)
        Map<String, Integer> termFreq = new HashMap<>();
        Matcher m = Pattern.compile("\\b([a-zA-Z]{5,})\\b").matcher(content);
        while (m.find())
        {
            String word = m.group(1).toLowerCase();
            termFreq.merge(word, 1, Integer::sum);
        }

        // High-frequency terms indicate topics of significance
        termFreq.entrySet().stream()
            .filter(e -> e.getValue() >= 5)
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(20)
            .forEach(e -> insights.add("TOPIC:" + e.getKey() + ":" + e.getValue()));

        // Extract URLs (resources to follow up on)
        Matcher urlMatcher = Pattern.compile("https?://[^\\s\"'<>]+").matcher(content);
        Set<String> urls = new LinkedHashSet<>();
        while (urlMatcher.find() && urls.size() < 10)
        {
            urls.add(urlMatcher.group());
        }
        for (String url : urls)
        {
            insights.add("RESOURCE:" + url);
        }

        // Extract titles/headings (structural data)
        Matcher titleMatcher = Pattern.compile("\\[\\d+\\]\\s+(.+?)\\s*\\|").matcher(content);
        while (titleMatcher.find())
        {
            insights.add("ITEM:" + titleMatcher.group(1).trim());
        }

        return insights;
    }

    /**
     * Generates speculations based on cross-referencing findings from multiple sources.
     * Uses frequency analysis, correlation detection, and pattern projection.
     */
    private List<Speculation> generateSpeculations(Map<String, List<String>> findings)
    {
        List<Speculation> speculations = new ArrayList<>();

        // Aggregate all topics across sources
        Map<String, Integer> globalTopics = new HashMap<>();
        Map<String, Set<String>> topicSources = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : findings.entrySet())
        {
            String source = entry.getKey();
            for (String insight : entry.getValue())
            {
                if (insight.startsWith("TOPIC:"))
                {
                    String[] parts = insight.split(":");
                    String topic = parts[1];
                    int freq = Integer.parseInt(parts[2]);
                    globalTopics.merge(topic, freq, Integer::sum);
                    topicSources.computeIfAbsent(topic, k -> new HashSet<>()).add(source);
                }
            }
        }

        // Cross-source topics are more interesting
        for (Map.Entry<String, Set<String>> entry : topicSources.entrySet())
        {
            if (entry.getValue().size() >= 2)
            {
                String topic = entry.getKey();
                int freq = globalTopics.get(topic);
                double score = Math.min(1.0, (entry.getValue().size() * 0.2) + (freq * 0.01));

                Speculation spec = new Speculation();
                spec.topic = topic;
                spec.insight = "Topic '" + topic + "' appears across " + entry.getValue().size() +
                    " sources (" + String.join(", ", entry.getValue()) + ") with frequency " + freq;
                spec.interestScore = score;
                spec.projection = projectFuture(topic, freq, entry.getValue().size());
                spec.timestamp = LocalDateTime.now().toString();
                speculations.add(spec);
            }
        }

        // Sort by interest score
        speculations.sort((a, b) -> Double.compare(b.interestScore, a.interestScore));
        return speculations;
    }

    /**
     * Projects future relevance based on topic frequency and source diversity.
     */
    private String projectFuture(String topic, int frequency, int sourceCount)
    {
        if (frequency > 50 && sourceCount > 3)
            return "HIGH RELEVANCE - Topic likely central to domain; recommend deep research and resource allocation.";
        else if (frequency > 20 && sourceCount >= 2)
            return "GROWING RELEVANCE - Topic emerging across multiple domains; monitor and collect more data.";
        else
            return "POTENTIAL RELEVANCE - Early signal detected; worth tracking in future scans.";
    }

    private void sendToServers(Speculation spec)
    {
        String payload = gson.toJson(spec);

        for (ServerInfo server : servers)
        {
            if (!server.enabled) continue;

            try
            {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(server.address, server.port), 5000);
                OutputStream out = socket.getOutputStream();
                out.write(payload.getBytes());
                out.write('\n');
                out.flush();
                socket.close();
                System.out.println("[Speculator] Sent to " + server.name + " (" + server.address + ":" + server.port + ")");
            }
            catch (Exception e)
            {
                System.out.println("[Speculator] Could not reach " + server.name + ": " + e.getMessage());
            }
        }
    }

    private void saveReport(List<Speculation> speculations)
    {
        try
        {
            String folder = "src/edifiction/_speculations";
            Files.createDirectories(Paths.get(folder));
            String filename = java.time.LocalDate.now() + ".speculations.json";
            Path filePath = Paths.get(folder, filename);
            if (!Files.exists(filePath))
            {
                Files.writeString(filePath, gson.toJson(speculations));
                System.out.println("[Speculator] Report saved -> " + filePath);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void loadServers()
    {
        try
        {
            File file = new File(knownServersFile);
            if (!file.exists()) return;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(file);
            NodeList serverNodes = doc.getElementsByTagName("server");

            for (int i = 0; i < serverNodes.getLength(); i++)
            {
                Element el = (Element) serverNodes.item(i);
                ServerInfo info = new ServerInfo();
                info.name = el.getElementsByTagName("name").item(0).getTextContent();
                info.address = el.getElementsByTagName("address").item(0).getTextContent();
                info.port = Integer.parseInt(el.getElementsByTagName("port").item(0).getTextContent());
                info.enabled = Boolean.parseBoolean(el.getElementsByTagName("enabled").item(0).getTextContent());
                servers.add(info);
            }
            System.out.println("[Speculator] Loaded " + servers.size() + " known server(s).");
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ===== Data Classes =====

    static class Speculation
    {
        String topic;
        String insight;
        double interestScore;
        String projection;
        String timestamp;
    }

    static class ServerInfo
    {
        String name;
        String address;
        int port;
        boolean enabled;
    }
}
