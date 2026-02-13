package net.runelite.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FixMyClient {
    public static void main(String[] args) throws IOException {
        // Path to your resources folder
        // ADJUST THIS PATH to match your actual project location if needed
        String resourcesPath = "runelite-client/src/main/resources/runelite";

        File dir = new File(resourcesPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File indexFile = new File(dir, "index");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(indexFile))) {
            // Write integer -1 (0xFFFFFFFF).
            // This tells the client "End of list, no more overlays to load".
            out.writeInt(-1);
        }

        System.out.println("Dummy index file created at: " + indexFile.getAbsolutePath());
    }
}