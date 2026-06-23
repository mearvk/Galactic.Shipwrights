import utils.GalacticShipwright;

public class Main
{
    public static void main(String[] args)
    {
        GalacticShipwright galactic_shipwright = new GalacticShipwright();

        Thread thread = new Thread(galactic_shipwright, "GalacticShipwright");

        thread.start();
    }
}
