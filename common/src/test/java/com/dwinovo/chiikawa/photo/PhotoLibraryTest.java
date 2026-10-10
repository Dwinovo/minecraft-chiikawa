package com.dwinovo.chiikawa.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoLibraryTest {
    @TempDir
    Path world;

    @Test
    void aPhotoIsKeptAndReadBackWhole() {
        PhotoLibrary library = new PhotoLibrary(world.resolve("data/chiikawa/photos"));
        UUID photo = UUID.randomUUID();

        assertTrue(library.save(photo, new byte[] {1, 2, 3}));

        assertTrue(library.has(photo));
        assertArrayEquals(new byte[] {1, 2, 3}, library.read(photo).orElseThrow());
    }

    @Test
    void aPhotoIsAFileNamedByItsId() {
        Path folder = world.resolve("photos");
        UUID photo = UUID.randomUUID();

        new PhotoLibrary(folder).save(photo, new byte[] {9});

        assertTrue(Files.isRegularFile(folder.resolve(photo + ".png")));
    }

    @Test
    void aPhotoThatIsNotThereIsNothing() {
        PhotoLibrary library = new PhotoLibrary(world.resolve("never-made"));
        UUID photo = UUID.randomUUID();

        assertFalse(library.has(photo));
        assertTrue(library.read(photo).isEmpty());
    }

    @Test
    void savingLeavesNothingElseInTheFolder() throws IOException {
        Path folder = world.resolve("photos");
        PhotoLibrary library = new PhotoLibrary(folder);

        library.save(UUID.randomUUID(), new byte[] {1});
        library.save(UUID.randomUUID(), new byte[] {2});

        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(2, files.filter(file -> file.getFileName().toString().endsWith(".png")).count());
        }
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(0, files.filter(file -> file.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void aPhotoSavedAgainIsReplaced() {
        PhotoLibrary library = new PhotoLibrary(world.resolve("photos"));
        UUID photo = UUID.randomUUID();
        library.save(photo, new byte[] {1});

        library.save(photo, new byte[] {2, 2});

        assertArrayEquals(new byte[] {2, 2}, library.read(photo).orElseThrow());
    }
}
