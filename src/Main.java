import utils.DictionaryProfiler;
import utils.GalacticShipwright;
import utils.GuildServer;
import utils.ModuleInstallServer;
import utils.JWSTFJ21Masquerade;

public class Main
{
    // GalacticShipwright settings
    public static final String KNOWN_SERVERS_FILE = "configuration/known.port.20000.servers.xml";
    public static final String KNOWN_MODULE_SERVERS_FILE = "configuration/known.port.49152.servers.xml";
    public static final int RELAY_PORT = 20000;
    public static final int MODULE_INSTALL_PORT = 2000;
    public static final double MIN_INTEREST_SCORE = 0.7;

    // DictionaryProfiler settings
    public static final String DP_OUTPUT_FILE = "dictionaries/dictionary.list.txt";
    public static final String DP_SCAN_DIR = "src/edifiction";
    public static final String[] DP_DEFINITION_SOURCES = {
        "https://en.wiktionary.org/w/api.php",
        "https://api.dictionaryapi.dev/api/v2/entries/en/"
    };

    // GuildServer settings
    public static final int GS_PORT = 10001;
    public static final int GS_REDUNDANCY = 3;
    public static final int GS_MONITOR_INTERVAL = 5000;

    // Components
    public static final DictionaryProfiler DICTIONARYPROFILER = new DictionaryProfiler(DP_OUTPUT_FILE, DP_SCAN_DIR, DP_DEFINITION_SOURCES);
    public static final GuildServer GUILDSERVER = new GuildServer(1);
    public static final ModuleInstallServer MODULE_INSTALL_SERVER = new ModuleInstallServer();
    public static final JWSTFJ21Masquerade MASQUERADE = new JWSTFJ21Masquerade();
    public static final GalacticShipwright SELF = new GalacticShipwright();

    public static void main(String[] args)
    {
        GalacticShipwright.BRIDGE.DICTIONARYPROFILER = DICTIONARYPROFILER;

        GalacticShipwright.BRIDGE.GUILDSERVER = GUILDSERVER;

        GalacticShipwright.SELF = SELF;

        GalacticShipwright.BRIDGE.start();

        // Start ModuleInstallServer on port 2000
        Thread moduleThread = new Thread(MODULE_INSTALL_SERVER, "ModuleInstallServer");
        moduleThread.setDaemon(true);
        moduleThread.start();

        // JWSTFJ21 integration phase — attempts masquerade, falls back to standalone
        Thread masqueradeThread = new Thread(MASQUERADE, "JWSTFJ21Masquerade");
        masqueradeThread.setDaemon(true);
        masqueradeThread.start();

        SELF.start();
    }
}
