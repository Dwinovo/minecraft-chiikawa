package com.dwinovo.chiikawa.photo;

import com.dwinovo.chiikawa.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * The photos of a world, one PNG each in a folder of the world's own, named by the photo's
 * id. They travel with the save and are all there is to a photo besides the little the item
 * remembers. The names are ids the server made, never anything a player typed, so a name
 * cannot lead out of the folder.
 */
public final class PhotoLibrary {
    private final Path folder;

    /** @param folder where the photos live; made when the first one is saved */
    public PhotoLibrary(Path folder) {
        this.folder = folder;
    }

    /** @return whether it was saved */
    public boolean save(UUID photo, byte[] png) {
        try {
            Files.createDirectories(folder);
            // Written beside its place and moved into it, so a photo is whole or not there.
            Path temporary = folder.resolve(photo + ".png.tmp");
            Files.write(temporary, png);
            Files.move(temporary, fileOf(photo), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            Constants.LOG.warn("[chiikawa-photo] Could not save photo {}", photo, ex);
            return false;
        }
    }

    public Optional<byte[]> read(UUID photo) {
        try {
            return Optional.of(Files.readAllBytes(fileOf(photo)));
        } catch (NoSuchFileException ex) {
            return Optional.empty();
        } catch (IOException ex) {
            Constants.LOG.warn("[chiikawa-photo] Could not read photo {}", photo, ex);
            return Optional.empty();
        }
    }

    public boolean has(UUID photo) {
        return Files.isRegularFile(fileOf(photo));
    }

    private Path fileOf(UUID photo) {
        return folder.resolve(photo + ".png");
    }
}
