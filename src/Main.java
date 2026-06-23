import utils.Reacher;
import utils.DictionaryProfiler;
import utils.Speculator;
import utils.SpeculatorTrainer;

public class Main
{
    public static void main(String[] args)
    {
        // Run Reacher to gather content from sources
        new Reacher().reach();

        // Run DictionaryProfiler to catalog words and definitions
        new DictionaryProfiler().profile();

        // Train the Speculator if not already trained
        SpeculatorTrainer trainer = new SpeculatorTrainer();
        if (!trainer.isTrained())
        {
            System.out.println("[Main] Speculator not trained. Running trainer...");
            trainer.train();
        }
        else
        {
            System.out.println("[Main] Speculator model found. Skipping training.");
        }

        // Run Speculator to analyze findings and share with known servers
        new Speculator().speculate();
    }
}
