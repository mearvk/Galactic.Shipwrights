package utils;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class DictionaryProfiler
{
    private String outputFile;
    private String scanDir;
    private String[] definitionSources;
    private TreeMap<String, String> dictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public DictionaryProfiler()
    {
        this.outputFile = "dictionaries/dictionary.list.txt";
        this.scanDir = "src/edifiction";
        this.definitionSources = new String[]{
            "https://api.dictionaryapi.dev/api/v2/entries/en/"
        };
    }

    public DictionaryProfiler(String outputFile, String scanDir, String[] definitionSources)
    {
        this.outputFile = outputFile;
        this.scanDir = scanDir;
        this.definitionSources = definitionSources;
    }

    public void profile()
    {
        System.out.println("[DictionaryProfiler] Starting...");

        // Load existing dictionary
        loadDictionary();
        System.out.println("[DictionaryProfiler] Loaded " + dictionary.size() + " existing entries.");

        // Scan for new words
        Set<String> newWords = scanForWords();
        int added = 0;
        for (String word : newWords)
        {
            if (!dictionary.containsKey(word))
            {
                dictionary.put(word, "");
                added++;
            }
        }
        System.out.println("[DictionaryProfiler] Found " + added + " new words. Total: " + dictionary.size());

        // Look up definitions for undefined words
        int defined = 0;
        for (Map.Entry<String, String> entry : dictionary.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                String def = lookupDefinition(entry.getKey());
                if (def != null && !def.isEmpty())
                {
                    entry.setValue(def);
                    defined++;
                }
            }
        }
        System.out.println("[DictionaryProfiler] Defined " + defined + " words.");

        // Save alphabetically
        saveDictionary();
        System.out.println("[DictionaryProfiler] Saved to " + outputFile);
    }

    /**
     * Add a word encountered during processing. If definition is found later, it will be completed.
     */
    public void addWord(String word)
    {
        if (word != null && word.length() > 2 && !dictionary.containsKey(word))
        {
            dictionary.put(word, "");
        }
    }

    /**
     * Add a word with its definition.
     */
    public void addWord(String word, String definition)
    {
        if (word != null && word.length() > 2)
        {
            dictionary.put(word, definition != null ? definition : "");
            saveDictionary();
        }
    }

    private Set<String> scanForWords()
    {
        Set<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Path dir = Paths.get(scanDir);
        if (!Files.exists(dir)) return words;

        try
        {
            Files.walk(dir)
                .filter(p -> p.toString().endsWith(".data") || p.toString().endsWith(".txt"))
                .forEach(p -> {
                    try
                    {
                        String content = Files.readString(p);
                        extractWords(content, words);
                    }
                    catch (Exception e) { /* skip unreadable files */ }
                });
        }
        catch (Exception e) { e.printStackTrace(); }

        return words;
    }

    private void extractWords(String content, Set<String> words)
    {
        // Extract words 3+ chars, alpha only
        Matcher m = Pattern.compile("\\b([a-zA-Z]{3,})\\b").matcher(content);
        while (m.find())
        {
            String word = m.group(1).toLowerCase();
            // Skip common stopwords
            if (!isStopword(word))
            {
                words.add(word);
            }
        }
    }

    private boolean isStopword(String w)
    {
        return Set.of("the", "and", "for", "are", "but", "not", "you", "all",
            "can", "had", "her", "was", "one", "our", "out", "has", "have",
            "this", "that", "with", "from", "they", "been", "said", "each",
            "which", "their", "will", "other", "about", "many", "then",
            "them", "these", "some", "would", "make", "like", "into",
            "than", "its", "over", "such", "after", "also", "did", "any",
            "new", "could", "very", "when", "what", "your", "how", "were",
            "http", "https", "www", "com", "org", "html", "json", "xml").contains(w);
    }

    private String lookupDefinition(String word)
    {
        // Try free dictionary API first
        String json = fetchRaw("https://api.dictionaryapi.dev/api/v2/entries/en/" + word);
        if (json != null && json.startsWith("["))
        {
            try
            {
                JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                JsonObject first = arr.get(0).getAsJsonObject();
                if (first.has("meanings"))
                {
                    JsonArray meanings = first.getAsJsonArray("meanings");
                    StringBuilder def = new StringBuilder();
                    for (int i = 0; i < Math.min(meanings.size(), 3); i++)
                    {
                        JsonObject meaning = meanings.get(i).getAsJsonObject();
                        String partOfSpeech = meaning.get("partOfSpeech").getAsString();
                        JsonArray defs = meaning.getAsJsonArray("definitions");
                        if (defs.size() > 0)
                        {
                            String d = defs.get(0).getAsJsonObject().get("definition").getAsString();
                            if (def.length() > 0) def.append("; ");
                            def.append("(").append(partOfSpeech).append(") ").append(d);
                        }
                    }
                    return def.toString();
                }
            }
            catch (Exception e) { /* fall through */ }
        }
        return null;
    }

    private void loadDictionary()
    {
        Path path = Paths.get(outputFile);
        if (!Files.exists(path)) return;

        try
        {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines)
            {
                if (line.contains(" : "))
                {
                    int sep = line.indexOf(" : ");
                    String word = line.substring(0, sep).trim();
                    String def = line.substring(sep + 3).trim();
                    dictionary.put(word, def);
                }
                else if (line.contains(" : [undefined]") || line.trim().length() > 0)
                {
                    String word = line.replace(" : [undefined]", "").trim();
                    if (word.length() > 0) dictionary.put(word, "");
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void saveDictionary()
    {
        try
        {
            Files.createDirectories(Paths.get(outputFile).getParent());
            StringBuilder sb = new StringBuilder();
            char currentLetter = 0;
            for (Map.Entry<String, String> entry : dictionary.entrySet())
            {
                char first = Character.toUpperCase(entry.getKey().charAt(0));
                if (first != currentLetter)
                {
                    if (currentLetter != 0) sb.append("\n");
                    sb.append("=== ").append(first).append(" ===\n");
                    currentLetter = first;
                }
                String def = entry.getValue().isEmpty() ? "[undefined]" : entry.getValue();
                sb.append(entry.getKey()).append(" : ").append(def).append("\n");
            }
            Files.writeString(Paths.get(outputFile), sb.toString());
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private String fetchRaw(String urlStr)
    {
        try
        {
            URL target = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "GalacticShipwrights/1.0");

            if (conn.getResponseCode() >= 400) { conn.disconnect(); return null; }

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
        catch (Exception e) { return null; }
    }
}
