import utils.DictionaryProfiler;
import utils.GalacticShipwright;
import utils.GuildServer;

public class Main
{
    public static DictionaryProfiler dictionaryProfiler = new DictionaryProfiler();

    public static void main(String[] args)
    {
        // Save dictionary on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutdown - saving dictionary...");
            dictionaryProfiler.saveDictionary();
        }, "ShutdownHook-Dictionary"));

        // Start triple-redundant Guild server on port 10001
        GuildServer.startAll();

        // Start the Shipwright pipeline on its own thread
        GalacticShipwright galactic_shipwright = new GalacticShipwright(dictionaryProfiler);
        Thread thread = new Thread(galactic_shipwright, "GalacticShipwright");
        thread.start();
    }
}
