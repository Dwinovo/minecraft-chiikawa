package com.dwinovo.chiikawa.client.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientSettingsTest {
    @TempDir
    Path folder;

    @Test
    void aMissingFileIsWrittenWithTheDefaultsInIt() throws IOException {
        Path file = folder.resolve("config/chiikawa/client.json");

        ClientSettings read = ClientSettings.read(file);

        assertEquals(ClientSettings.DEFAULT, read);
        assertTrue(Files.readString(file).contains("\"speech_bubbles\": true"), "the file has nothing to change in it");
    }

    @Test
    void whatIsWrittenIsWhatIsRead() {
        Path file = folder.resolve("client.json");
        new ClientSettings(false).write(file);

        assertFalse(ClientSettings.read(file).speechBubbles());
    }

    @Test
    void aFieldLeftOutIsTheDefault() throws IOException {
        Path file = folder.resolve("client.json");
        Files.writeString(file, "{}");

        assertEquals(ClientSettings.DEFAULT, ClientSettings.read(file));
    }

    @Test
    void aFileThatCannotBeReadLeavesTheDefaultsAndIsLeftAlone() throws IOException {
        Path file = folder.resolve("client.json");
        Files.writeString(file, "{ \"speech_bubbles\": ");

        assertEquals(ClientSettings.DEFAULT, ClientSettings.read(file));
        assertEquals("{ \"speech_bubbles\": ", Files.readString(file));
    }

    @Test
    void aWrongTypeIsNotMistakenForAnAnswer() throws IOException {
        Path file = folder.resolve("client.json");
        Files.writeString(file, "{ \"speech_bubbles\": \"maybe\" }");

        assertEquals(ClientSettings.DEFAULT, ClientSettings.read(file));
    }
}
