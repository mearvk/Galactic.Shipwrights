import utils.Reacher;
import utils.DictionaryProfiler;

public class Main
{
    public static void main(String[] args)
    {
        // Run Reacher to gather content from sources
        new Reacher().reach();

        // Run DictionaryProfiler to catalog words and definitions
        new DictionaryProfiler().profile();
    }
}
