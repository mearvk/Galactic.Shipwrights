package utils;

public class GalacticShipwright implements Runnable
{
    @Override
    public void run()
    {
        // Run Reacher to gather content from sources
        new Reacher().reach();

        // Run DictionaryProfiler to catalog words and definitions
        new DictionaryProfiler().profile();

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
}
