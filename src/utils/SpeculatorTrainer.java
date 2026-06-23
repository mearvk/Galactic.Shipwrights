package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Trains the Speculator AI by building a local knowledge model from edifiction data.
 * Produces a trained model file (trainer/speculator.model) containing:
 * - Term frequency vectors per source
 * - Topic correlation matrix
 * - Known patterns and their projected outcomes
 */
public class SpeculatorTrainer
{
    private static final String TRAINER_DIR = "trainer";
    private static final String MODEL_FILE = TRAINER_DIR + "/speculator.model";
    private static final String CORPUS_DIR = "src/edifiction";

    private Map<String, Map<String, Integer>> sourceTermVectors = new LinkedHashMap<>();
    private Map<String, Double> topicWeights = new LinkedHashMap<>();
    private Map<String, Set<String>> correlations = new LinkedHashMap<>();

    public boolean isTrained()
    {
        return Files.exists(Paths.get(MODEL_FILE));
    }

    public void train()
    {
        System.out.println("[Trainer] Starting training...");

        // Phase 1: Build term vectors from corpus
        buildTermVectors();

        // Phase 2: Compute topic weights (TF-IDF style)
        computeTopicWeights();

        // Phase 3: Discover correlations between sources
        discoverCorrelations();

        // Phase 4: Save model
        saveModel();

        System.out.println("[Trainer] Training complete. Model saved -> " + MODEL_FILE);
    }

    private void buildTermVectors()
    {
        Path dir = Paths.get(CORPUS_DIR);
        if (!Files.exists(dir))
        {
            System.out.println("[Trainer] No corpus found at " + CORPUS_DIR + ". Run Reacher first.");
            return;
        }

        try
        {
            Files.walk(dir)
                .filter(p -> p.toString().endsWith(".data") || p.toString().endsWith(".txt"))
                .forEach(p -> {
                    try
                    {
                        String source = p.getParent().getFileName().toString();
                        String content = Files.readString(p);
                        Map<String, Integer> termVec = sourceTermVectors
                            .computeIfAbsent(source, k -> new HashMap<>());

                        Matcher m = Pattern.compile("\\b([a-zA-Z]{4,})\\b").matcher(content);
                        while (m.find())
                        {
                            String term = m.group(1).toLowerCase();
                            termVec.merge(term, 1, Integer::sum);
                        }
                    }
                    catch (Exception e) { /* skip */ }
                });

            System.out.println("[Trainer] Built vectors for " + sourceTermVectors.size() + " sources.");
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void computeTopicWeights()
    {
        // Aggregate term frequencies across all sources
        Map<String, Integer> globalFreq = new HashMap<>();
        Map<String, Integer> docFreq = new HashMap<>(); // in how many sources does term appear

        for (Map<String, Integer> vec : sourceTermVectors.values())
        {
            Set<String> seen = new HashSet<>();
            for (Map.Entry<String, Integer> entry : vec.entrySet())
            {
                globalFreq.merge(entry.getKey(), entry.getValue(), Integer::sum);
                if (seen.add(entry.getKey()))
                {
                    docFreq.merge(entry.getKey(), 1, Integer::sum);
                }
            }
        }

        // TF-IDF: weight = freq * log(numSources / docFreq)
        int numSources = sourceTermVectors.size();
        for (Map.Entry<String, Integer> entry : globalFreq.entrySet())
        {
            int df = docFreq.getOrDefault(entry.getKey(), 1);
            double tfidf = entry.getValue() * Math.log((double) numSources / df);
            if (tfidf > 5.0)
            {
                topicWeights.put(entry.getKey(), tfidf);
            }
        }

        System.out.println("[Trainer] Computed weights for " + topicWeights.size() + " significant terms.");
    }

    private void discoverCorrelations()
    {
        // Find terms that co-occur across multiple sources
        List<String> sources = new ArrayList<>(sourceTermVectors.keySet());

        for (int i = 0; i < sources.size(); i++)
        {
            for (int j = i + 1; j < sources.size(); j++)
            {
                Set<String> common = new HashSet<>(sourceTermVectors.get(sources.get(i)).keySet());
                common.retainAll(sourceTermVectors.get(sources.get(j)).keySet());

                // Only keep significant shared terms
                Set<String> significant = new HashSet<>();
                for (String term : common)
                {
                    if (topicWeights.containsKey(term))
                    {
                        significant.add(term);
                    }
                }

                if (significant.size() >= 5)
                {
                    String key = sources.get(i) + "<->" + sources.get(j);
                    correlations.put(key, significant);
                }
            }
        }

        System.out.println("[Trainer] Discovered " + correlations.size() + " source correlations.");
    }

    private void saveModel()
    {
        try
        {
            Files.createDirectories(Paths.get(TRAINER_DIR));
            StringBuilder model = new StringBuilder();

            model.append("# Speculator Model v1.0\n");
            model.append("# Generated: ").append(java.time.LocalDateTime.now()).append("\n");
            model.append("# Sources: ").append(sourceTermVectors.size()).append("\n");
            model.append("# Weighted Terms: ").append(topicWeights.size()).append("\n");
            model.append("# Correlations: ").append(correlations.size()).append("\n\n");

            // Save top weighted terms
            model.append("[TOPIC_WEIGHTS]\n");
            topicWeights.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(500)
                .forEach(e -> model.append(e.getKey()).append("=").append(String.format("%.4f", e.getValue())).append("\n"));

            // Save source term counts
            model.append("\n[SOURCE_PROFILES]\n");
            for (Map.Entry<String, Map<String, Integer>> entry : sourceTermVectors.entrySet())
            {
                model.append(entry.getKey()).append(":").append(entry.getValue().size()).append(" terms\n");
            }

            // Save correlations
            model.append("\n[CORRELATIONS]\n");
            for (Map.Entry<String, Set<String>> entry : correlations.entrySet())
            {
                model.append(entry.getKey()).append(":").append(entry.getValue().size()).append(" shared [");
                int count = 0;
                for (String term : entry.getValue())
                {
                    if (count++ >= 10) { model.append("..."); break; }
                    if (count > 1) model.append(",");
                    model.append(term);
                }
                model.append("]\n");
            }

            Files.writeString(Paths.get(MODEL_FILE), model.toString());

            // Persist weights to MySQL if configured
            saveWeightsToDatabase();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void saveWeightsToDatabase()
    {
        try
        {
            Path dbConfig = Paths.get("src/database.xml");
            if (!Files.exists(dbConfig)) return;

            javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory
                .newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(dbConfig.toFile());
            org.w3c.dom.Element conn = (org.w3c.dom.Element) doc.getElementsByTagName("connection").item(0);

            String host = conn.getElementsByTagName("host").item(0).getTextContent();
            int port = Integer.parseInt(conn.getElementsByTagName("port").item(0).getTextContent());
            String user = conn.getElementsByTagName("user").item(0).getTextContent();
            String password = conn.getElementsByTagName("password").item(0).getTextContent();
            String installerId = conn.getElementsByTagName("installerId").item(0).getTextContent();

            if (installerId == null || installerId.isEmpty()) return;

            SpeculatorDB sdb = new SpeculatorDB(host, port, user, password, installerId);
            if (sdb.connect())
            {
                topicWeights.entrySet().stream()
                    .sorted((a, b2) -> Double.compare(b2.getValue(), a.getValue()))
                    .limit(100)
                    .forEach(e -> sdb.savePosit(
                        "Term '" + e.getKey() + "' has TF-IDF weight " + String.format("%.4f", e.getValue()),
                        "Computed from " + sourceTermVectors.size() + " source corpus",
                        Math.min(1.0, e.getValue() / 100.0),
                        "SpeculatorTrainer"
                    ));

                for (Map.Entry<String, Set<String>> entry : correlations.entrySet())
                {
                    String[] parts = entry.getKey().split("<->");
                    if (parts.length == 2)
                    {
                        sdb.saveCorrelation(parts[0], parts[1],
                            Math.min(1.0, entry.getValue().size() / 50.0),
                            entry.getValue().size() + " shared significant terms");
                    }
                }

                sdb.close();
                System.out.println("[Trainer] Weights saved to MySQL (ShipWrights).");
            }
        }
        catch (Exception e)
        {
            System.out.println("[Trainer] DB save skipped: " + e.getMessage());
        }
    }

    /**
     * Loads the trained model for use by Speculator.
     */
    public Map<String, Double> loadTopicWeights()
    {
        if (!isTrained()) return Collections.emptyMap();

        Map<String, Double> weights = new LinkedHashMap<>();
        try
        {
            List<String> lines = Files.readAllLines(Paths.get(MODEL_FILE));
            boolean inWeights = false;
            for (String line : lines)
            {
                if (line.equals("[TOPIC_WEIGHTS]")) { inWeights = true; continue; }
                if (line.startsWith("[") && inWeights) break;
                if (inWeights && line.contains("="))
                {
                    String[] parts = line.split("=");
                    weights.put(parts[0], Double.parseDouble(parts[1]));
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        return weights;
    }
}
