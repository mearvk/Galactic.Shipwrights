package utils;

import java.io.*;
import java.net.*;

/**
 * Triple-redundant server listening on port 10001.
 * Three identical server threads accept connections for resilience.
 */
public class GuildServer implements Runnable
{
    private static final int PORT = 10001;
    private static final int REDUNDANCY = 3;
    private final int instance;
    private ServerSocket serverSocket;

    public GuildServer(int instance)
    {
        this.instance = instance;
    }

    public static void startAll()
    {
        for (int i = 1; i <= REDUNDANCY; i++)
        {
            if (i == 1)
            {
                // Primary binds the port
                Thread t = new Thread(new GuildServer(i), "GuildServer-" + i);
                t.setDaemon(true);
                t.start();
            }
            else
            {
                // Redundant instances monitor and take over if primary fails
                Thread t = new Thread(new GuildServerMonitor(i), "GuildServer-Monitor-" + i);
                t.setDaemon(true);
                t.start();
            }
        }
        System.out.println("[GuildServer] Triple-redundant server started on port " + PORT);
    }

    @Override
    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[GuildServer-" + instance + "] Listening on port " + PORT);

            while (!Thread.interrupted())
            {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(() -> handleClient(client), "GuildClient-" + client.getPort());
                handler.setDaemon(true);
                handler.start();
            }
        }
        catch (Exception e)
        {
            System.out.println("[GuildServer-" + instance + "] Stopped: " + e.getMessage());
        }
    }

    private void handleClient(Socket client)
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            out.println("GALACTIC SHIPWRIGHTS GUILD - PORT 10001");
            out.println("STATUS: OPERATIONAL");
            out.println("READY");

            String line;
            while ((line = in.readLine()) != null)
            {
                if (line.equalsIgnoreCase("STATUS"))
                {
                    out.println("GUILD SERVER ACTIVE | INSTANCE " + instance + " | REDUNDANCY " + REDUNDANCY);
                }
                else if (line.equalsIgnoreCase("QUIT"))
                {
                    out.println("FAREWELL");
                    break;
                }
                else
                {
                    out.println("ACK: " + line);
                }
            }
            client.close();
        }
        catch (Exception e) { /* client disconnected */ }
    }

    /**
     * Monitor thread that watches the primary and takes over if it fails.
     */
    static class GuildServerMonitor implements Runnable
    {
        private final int instance;

        GuildServerMonitor(int instance)
        {
            this.instance = instance;
        }

        @Override
        public void run()
        {
            while (!Thread.interrupted())
            {
                try
                {
                    Thread.sleep(5000);
                    // Check if primary is alive
                    Socket probe = new Socket();
                    probe.connect(new InetSocketAddress("127.0.0.1", PORT), 1000);
                    probe.close();
                }
                catch (InterruptedException e) { break; }
                catch (Exception e)
                {
                    // Primary is down, take over
                    System.out.println("[GuildServer-Monitor-" + instance + "] Primary down. Taking over...");
                    new GuildServer(instance).run();
                    break;
                }
            }
        }
    }
}
