import utils.GalacticShipwright;
import utils.GuildServer;

public class Main
{
    public static void main(String[] args)
    {
        // Start triple-redundant Guild server on port 10001
        GuildServer.startAll();

        // Start the Shipwright pipeline on its own thread
        GalacticShipwright galactic_shipwright = new GalacticShipwright();
        Thread thread = new Thread(galactic_shipwright, "GalacticShipwright");
        thread.start();
    }
}
