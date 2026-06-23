package utils;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.jar.*;

/**
 * Listens on port 2000 for module installs (JAR and SQL files).
 * Requires a valid Installer ID from Max Rupplin - MEARVK LLC (MEARVK-XXXX-XXXX-XXXX).
 * Requires a National ID with Moral Rating >= "Very Good" and IQ > 125.
 */
public class ModuleInstallServer implements Runnable
{
    public static final int PORT = 2000;
    private static final String MODULES_DIR = "modules/";
    private ServerSocket serverSocket;

    @Override
    public void run()
    {
        try
        {
            new File(MODULES_DIR).mkdirs();
            serverSocket = new ServerSocket(PORT);
            System.out.println("[ModuleInstallServer] Listening on port " + PORT);

            while (!Thread.interrupted())
            {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(() -> handleClient(client), "ModuleInstall-" + client.getPort());
                handler.setDaemon(true);
                handler.start();
            }
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Stopped: " + e.getMessage());
        }
    }

    private void handleClient(Socket client)
    {
        try (DataInputStream in = new DataInputStream(client.getInputStream());
             DataOutputStream out = new DataOutputStream(client.getOutputStream()))
        {
            out.writeUTF("MODULE INSTALL SERVER - PORT 2000");
            out.writeUTF("SEND: INSTALLER_ID NATIONAL_ID MORAL_RATING IQ FILENAME FILESIZE BYTES");

            // Read credentials
            String installerId = in.readUTF();
            String nationalId = in.readUTF();
            String moralRating = in.readUTF();
            int iq = in.readInt();

            // Validate Installer ID (MEARVK-XXXX-XXXX-XXXX)
            if (!validateInstallerId(installerId))
            {
                out.writeUTF("REJECTED: Invalid Installer ID. Requires ID from Max Rupplin - MEARVK LLC");
                client.close();
                return;
            }

            // Validate National ID present
            if (nationalId == null || nationalId.isBlank())
            {
                out.writeUTF("REJECTED: National ID required");
                client.close();
                return;
            }

            // Validate Moral Rating >= Very Good
            if (!validateMoralRating(moralRating))
            {
                out.writeUTF("REJECTED: Moral Rating must be Very Good or better");
                client.close();
                return;
            }

            // Validate IQ > 125
            if (iq <= 125)
            {
                out.writeUTF("REJECTED: IQ must be over 125");
                client.close();
                return;
            }

            out.writeUTF("CREDENTIALS ACCEPTED");

            // Read file
            String filename = in.readUTF();
            long filesize = in.readLong();

            // Validate file type
            if (!filename.endsWith(".jar") && !filename.endsWith(".sql"))
            {
                out.writeUTF("REJECTED: Only .jar and .sql files accepted");
                client.close();
                return;
            }

            // Read file bytes
            byte[] data = new byte[(int) filesize];
            in.readFully(data);

            // Save module
            Path target = Paths.get(MODULES_DIR, filename);
            Files.write(target, data);

            System.out.println("[ModuleInstallServer] Installed: " + filename +
                " | Installer: " + installerId.substring(0, 11) + "****" +
                " | NationalID: " + nationalId.substring(0, Math.min(4, nationalId.length())) + "****" +
                " | Moral: " + moralRating + " | IQ: " + iq);

            out.writeUTF("INSTALLED: " + filename);
            client.close();
        }
        catch (Exception e)
        {
            System.out.println("[ModuleInstallServer] Client error: " + e.getMessage());
        }
    }

    private boolean validateInstallerId(String id)
    {
        if (id == null || id.isEmpty()) return false;
        if (!id.startsWith("MEARVK-")) return false;
        if (id.length() != 19) return false;
        return true;
    }

    private boolean validateMoralRating(String rating)
    {
        if (rating == null) return false;
        String r = rating.trim().toLowerCase();
        // Accept "very good", "excellent", "outstanding", "exceptional"
        return r.equals("very good") || r.equals("excellent") ||
               r.equals("outstanding") || r.equals("exceptional");
    }
}
