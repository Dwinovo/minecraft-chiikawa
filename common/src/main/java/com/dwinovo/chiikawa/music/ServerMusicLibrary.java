package com.dwinovo.chiikawa.music;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerMusicLibrary implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "wav");

    private final MinecraftServer server;
    private final Path root;
    private final Path musicDir;
    private final Path cacheDir;
    private final Path configFile;
    private final Path tracksFile;
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "chiikawa-music-import");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, MusicTrack> tracks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ImportOutcome>> imports = new ConcurrentHashMap<>();
    private final Map<String, CachedOpusTrack> loadedTracks = new ConcurrentHashMap<>();
    private ChiikawaMusicConfig config = ChiikawaMusicConfig.DEFAULT;

    public ServerMusicLibrary(MinecraftServer server) {
        this.server = server;
        this.root = server.getFile("config/chiikawa");
        this.musicDir = root.resolve("music");
        this.cacheDir = root.resolve("music-cache");
        this.configFile = root.resolve("music.json");
        this.tracksFile = cacheDir.resolve("tracks.json");
    }

    public void init() {
        try {
            Files.createDirectories(musicDir);
            Files.createDirectories(cacheDir);
            config = loadConfig();
            loadTracks();
            scan();
            saveTracks();
        } catch (Exception ex) {
            Constants.LOG.error("[chiikawa-music] Failed to initialize music library", ex);
        }
    }

    public void tick() {
        finishImports();
    }

    public ChiikawaMusicConfig config() {
        return config;
    }

    public boolean rescan() {
        try {
            config = loadConfig();
            scan();
            saveTracks();
            return true;
        } catch (Exception ex) {
            Constants.LOG.warn("[chiikawa-music] Failed to rescan music library", ex);
            return false;
        }
    }

    public List<MusicTrackView> catalog() {
        return tracks.values().stream()
            .sorted(Comparator.comparing(MusicTrack::title, String.CASE_INSENSITIVE_ORDER))
            .map(MusicTrackView::of)
            .toList();
    }

    public Optional<MusicTrack> getTrack(String trackId) {
        MusicTrack track = tracks.get(trackId);
        return track == null || track.status() != MusicTrackStatus.READY ? Optional.empty() : Optional.of(track);
    }

    public Optional<CachedOpusTrack> loadCachedTrack(String trackId) {
        MusicTrack track = tracks.get(trackId);
        if (track == null || track.status() != MusicTrackStatus.READY) {
            return Optional.empty();
        }
        try {
            return Optional.of(loadedTracks.computeIfAbsent(trackId, ignored -> {
                try {
                    return CmsTrackFile.read(cacheDir.resolve(track.cacheFile()));
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        } catch (IllegalStateException ex) {
            // Corrupt/truncated cache: mark the track failed so we don't retry the bad file
            // on every play attempt (which previously could loop forever).
            Constants.LOG.warn("[chiikawa-music] Failed to read cached track {}; marking it failed", trackId, ex);
            loadedTracks.remove(trackId);
            MusicTrack failed = tracks.get(trackId);
            if (failed != null) {
                tracks.put(trackId, failed.failed("corrupt cache: " + ex.getMessage()));
            }
            return Optional.empty();
        }
    }

    private ChiikawaMusicConfig loadConfig() throws IOException {
        if (!Files.exists(configFile)) {
            writeJson(configFile, ChiikawaMusicConfig.CODEC.encodeStart(JsonOps.INSTANCE, ChiikawaMusicConfig.DEFAULT).getOrThrow());
            return ChiikawaMusicConfig.DEFAULT;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonElement json = JsonParser.parseReader(reader);
            return ChiikawaMusicConfig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(Constants.LOG::warn)
                .orElse(ChiikawaMusicConfig.DEFAULT);
        }
    }

    private void loadTracks() throws IOException {
        if (!Files.exists(tracksFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(tracksFile)) {
            JsonElement json = JsonParser.parseReader(reader);
            MusicTrack.LIST_CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(Constants.LOG::warn)
                .orElse(List.of())
                .forEach(track -> tracks.put(track.trackId(), track));
        }
    }

    public void scan() throws IOException {
        if (!config.enabled()) {
            return;
        }
        Set<String> seenTrackIds = new HashSet<>();
        try (Stream<Path> stream = Files.list(musicDir)) {
            for (Path source : stream.filter(Files::isRegularFile).toList()) {
                if (!isSupported(source)) {
                    continue;
                }
                String hash = sha1(source);
                String trackId = createTrackId(source, hash);
                seenTrackIds.add(trackId);
                String cacheName = trackId + ".cms";
                MusicTrack existing = tracks.get(trackId);
                if (existing != null
                        && existing.status() == MusicTrackStatus.READY
                        && hash.equals(existing.sourceHash())
                        && Files.exists(cacheDir.resolve(existing.cacheFile()))) {
                    continue;
                }
                MusicTrack track = new MusicTrack(trackId, title(source), 0, 0, MusicTrackStatus.IMPORTING,
                    hash, source.getFileName().toString(), cacheName, "");
                tracks.put(trackId, track);
                scheduleImport(track);
            }
        }
        tracks.keySet().removeIf(trackId -> !seenTrackIds.contains(trackId));
        loadedTracks.keySet().removeIf(trackId -> !seenTrackIds.contains(trackId));
    }

    private void scheduleImport(MusicTrack track) {
        if (imports.containsKey(track.trackId())) {
            return;
        }
        CompletableFuture<ImportOutcome> future = CompletableFuture.supplyAsync(() -> {
            try {
                MusicImporter.ImportResult result = MusicImporter.importTrack(
                    musicDir.resolve(track.sourceFile()),
                    cacheDir.resolve(track.cacheFile()),
                    config
                );
                return ImportOutcome.success(track.trackId(), result);
            } catch (Exception ex) {
                return ImportOutcome.failure(track.trackId(), ex.getMessage());
            }
        }, importExecutor);
        imports.put(track.trackId(), future);
    }

    private void finishImports() {
        boolean changed = false;
        for (Map.Entry<String, CompletableFuture<ImportOutcome>> entry : List.copyOf(imports.entrySet())) {
            CompletableFuture<ImportOutcome> future = entry.getValue();
            if (!future.isDone()) {
                continue;
            }
            imports.remove(entry.getKey());
            ImportOutcome outcome = future.join();
            MusicTrack track = tracks.get(outcome.trackId());
            if (track == null) {
                continue;
            }
            if (outcome.error() == null) {
                tracks.put(track.trackId(), track.ready(outcome.result().durationTicks(), outcome.result().frameCount(), track.cacheFile()));
                Constants.LOG.info("[chiikawa-music] Imported {}", track.sourceFile());
            } else {
                tracks.put(track.trackId(), track.failed(outcome.error()));
                Constants.LOG.warn("[chiikawa-music] Failed to import {}: {}", track.sourceFile(), outcome.error());
            }
            changed = true;
        }
        if (changed) {
            try {
                saveTracks();
                broadcastCatalogUpdate();
            } catch (IOException ex) {
                Constants.LOG.warn("[chiikawa-music] Failed to save track cache", ex);
            }
        }
    }

    private void broadcastCatalogUpdate() {
        List<MusicTrackView> nextCatalog = catalog();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getMainHandItem().is(InitItems.MUSIC_BOX.get())) {
                Services.NETWORK.sendToClient(player, new MusicCatalogPayload(0, nextCatalog, false));
            }
            if (player.getOffhandItem().is(InitItems.MUSIC_BOX.get())) {
                Services.NETWORK.sendToClient(player, new MusicCatalogPayload(1, nextCatalog, false));
            }
        }
    }

    private void saveTracks() throws IOException {
        List<MusicTrack> ordered = tracks.values().stream()
            .sorted(Comparator.comparing(MusicTrack::title, String.CASE_INSENSITIVE_ORDER))
            .toList();
        writeJson(tracksFile, MusicTrack.LIST_CODEC.encodeStart(JsonOps.INSTANCE, ordered).getOrThrow());
    }

    private static void writeJson(Path path, JsonElement json) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(json, writer);
        }
    }

    private static boolean isSupported(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String title(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String createTrackId(Path source, String hash) {
        String base = title(source).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_");
        if (base.isBlank()) {
            base = "track";
        }
        return base + "-" + hash.substring(0, 8);
    }

    private static String sha1(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() {
        importExecutor.shutdownNow();
    }

    private record ImportOutcome(String trackId, MusicImporter.ImportResult result, String error) {
        static ImportOutcome success(String trackId, MusicImporter.ImportResult result) {
            return new ImportOutcome(trackId, result, null);
        }

        static ImportOutcome failure(String trackId, String error) {
            return new ImportOutcome(trackId, null, error == null ? "unknown error" : error);
        }
    }
}
