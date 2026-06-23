package utils;

public class GalacticShipwright implements Runnable
{
    public static GalacticShipwright SELF;
    public static final Bridge BRIDGE = new Bridge();

    @Override
    public void run()
    {
        // Run Reacher to gather content from sources
        new Reacher().reach();

        // Run DictionaryProfiler to catalog words and definitions
        BRIDGE.DICTIONARYPROFILER.profile();

        // Train the Speculator if not already trained
        SpeculatorTrainer trainer = new SpeculatorTrainer();
        if (!trainer.isTrained())
        {
            System.out.println("[GalacticShipwright] Speculator not trained. Running trainer...");
            trainer.train();
        }
        else
        {
            System.out.println("[GalacticShipwright] Speculator model found. Skipping training.");
        }

        // Run Speculator to analyze findings and share with known servers
        new Speculator().speculate();

        System.out.println("[GalacticShipwright] Complete.");
    }

    public static class Bridge
    {
        public DictionaryProfiler DICTIONARYPROFILER;
        public GuildServer GUILDSERVER;

        public void start()
        {
            // Register shutdown hooks
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[Bridge] Shutdown - saving dictionary...");
                if (DICTIONARYPROFILER != null) DICTIONARYPROFILER.saveDictionary();
            }, "ShutdownHook-Dictionary"));

            // Start GuildServer
            GuildServer.startAll();
        }
    }
}
