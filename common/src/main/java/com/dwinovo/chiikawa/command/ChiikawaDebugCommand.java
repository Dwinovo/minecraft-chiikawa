package com.dwinovo.chiikawa.command;

import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.camera.CameraSettings;
import com.dwinovo.chiikawa.camera.FilmRolls;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.photo.PhotoUploads;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /chiikawa debug intent} inspects the mind of the pet under the player's
 * crosshair: the slip it carries and the errand it is on, how every errand fares in a
 * decision made now (why it cannot start, or its priority and score), and with
 * {@code log} what it started and ended recently. {@code /chiikawa debug board} lists the day's slips of the labor board under
 * the crosshair. {@code /chiikawa debug photo <entity>} has the player's game draw a photo
 * from an entity's eyes, and puts it on the roll of film in hand, or on a roll of its own.
 */
public final class ChiikawaDebugCommand {
    private static final double TARGET_RANGE = 16.0;
    private static final SimpleCommandExceptionType NO_PET =
        new SimpleCommandExceptionType(Component.literal("[chiikawa-intent] Look at a pet within 16 blocks"));
    private static final SimpleCommandExceptionType NO_BOARD =
        new SimpleCommandExceptionType(Component.literal("[chiikawa-board] Look at a labor board within 16 blocks"));

    private static final SimpleCommandExceptionType NO_CAMERA =
        new SimpleCommandExceptionType(Component.literal("[chiikawa-photo] No camera settings are loaded"));
    private static final SimpleCommandExceptionType OTHER_DIMENSION =
        new SimpleCommandExceptionType(Component.literal("[chiikawa-photo] That entity is in another dimension"));

    private ChiikawaDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chiikawa")
            .then(Commands.literal("debug")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("intent")
                    .executes(context -> showCandidates(context.getSource()))
                    .then(Commands.literal("log")
                        .executes(context -> showLog(context.getSource()))))
                .then(Commands.literal("board")
                    .executes(context -> showBoard(context.getSource())))
                .then(Commands.literal("photo")
                    .then(Commands.argument("entity", EntityArgument.entity())
                        .executes(context -> takePhoto(context.getSource(), EntityArgument.getEntity(context, "entity")))))));
    }

    /**
     * Asks the player's own game to draw a picture from the entity's eyes, and puts it on the
     * roll of film in their hand, if it has room, or gives them a roll with it once it has been
     * sent back and kept. It is the camera's whole path without a pet
     * in it, for looking at what the picture comes out as.
     */
    private static int takePhoto(CommandSourceStack source, Entity viewpoint) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Whichever camera is loaded first by name: what is being looked at is the picture, not the camera.
        CameraSettings camera = CameraSettings.all().entrySet().stream().min(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue).orElseThrow(NO_CAMERA::create);
        if (viewpoint.level() != player.level()) {
            throw OTHER_DIMENSION.create();
        }
        PhotoSystem.of(source.getServer()).request(player, viewpoint, camera, new PhotoUploads.Listener() {
            @Override
            public void stored(UUID photo) {
                if (!player.hasDisconnected()) {
                    PhotoData shot = PhotoData.taken(photo, PhotoSubject.SCENERY, "", player.level(),
                        viewpoint.getDisplayName().getString());
                    Item film = PhotoScout.filmOf(camera);
                    // On the roll in hand if it has room, otherwise on a roll of its own.
                    if (FilmRolls.isOpen(player.getMainHandItem(), film)) {
                        FilmRolls.add(player.getMainHandItem(), shot);
                    } else {
                        player.getInventory().placeItemBackInInventory(FilmRolls.begin(film, camera.exposures(), shot));
                    }
                    player.sendSystemMessage(Component.literal("[chiikawa-photo] Photo " + photo + " is in the library"));
                }
            }

            @Override
            public void failed(UUID photo) {
                player.sendSystemMessage(Component.literal("[chiikawa-photo] Photo " + photo + " did not come back"));
            }
        });
        source.sendSuccess(() -> Component.literal("[chiikawa-photo] Asked your game for a photo from ")
            .append(viewpoint.getDisplayName()).append("'s eyes"), false);
        return 1;
    }

    private static int showCandidates(CommandSourceStack source) throws CommandSyntaxException {
        AbstractPet pet = targetedPet(source);
        PetMind.Look look = pet.getMind().look((ServerLevel) pet.level());
        MutableComponent header = Component.literal("[chiikawa-intent] ").append(pet.getDisplayName()).append(" running: ");
        header.append(look.running()
            .map(running -> Component.literal(running.errand() + " for " + (look.gameTime() - running.startedAt()) + "t"))
            .orElse(Component.literal("none")));
        header.append(Component.literal(" (" + look.phase().getSerializedName() + ")"));
        header.append(Component.literal(pet.getTask()
            .map(task -> " slip " + task.type() + " " + task.progress() + "/" + task.target())
            .orElse(" no slip")));
        source.sendSuccess(() -> header, false);
        for (DecisionTrace.Line entry : look.trace().lines()) {
            boolean startable = entry.verdict().equals(DecisionTrace.STARTABLE);
            MutableComponent line = Component.literal(" " + entry.errand() + " p" + entry.priority() + " ")
                .append(flag("startable", startable));
            if (!startable) {
                line.append(Component.literal(" (").append(entry.verdict().startsWith("intent.")
                    ? Component.translatable(entry.verdict()) : Component.literal(entry.verdict())).append(")"));
            }
            entry.score().ifPresent(score -> line.append(Component.literal(String.format(Locale.ROOT, " score %.2f", score))));
            source.sendSuccess(() -> line, false);
        }
        return look.trace().lines().size();
    }

    private static int showLog(CommandSourceStack source) throws CommandSyntaxException {
        AbstractPet pet = targetedPet(source);
        List<DecisionLog.Entry> entries = pet.getMind().log().entries();
        source.sendSuccess(() -> Component.literal("[chiikawa-intent] ").append(pet.getDisplayName())
            .append(" last " + entries.size() + " happenings:"), false);
        for (DecisionLog.Entry entry : entries) {
            String text = switch (entry) {
                case DecisionLog.Switched switched -> name(switched.from()) + " -> " + name(switched.to())
                    + " [" + switched.cause() + "] " + switched.top().stream()
                        .map(scored -> String.format(Locale.ROOT, "%s p%d %.2f", scored.errand(), scored.priority(), scored.score()))
                        .collect(Collectors.joining(", "));
                case DecisionLog.Ended ended -> name(ended.errand()) + " ended " + ended.outcome()
                    + " after " + ended.ranTicks() + "t";
            };
            source.sendSuccess(() -> Component.literal(" " + entry.gameTime() + ": " + text), false);
        }
        return entries.size();
    }

    private static int showBoard(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.pick(TARGET_RANGE, 1.0F, false) instanceof BlockHitResult hit)
                || !(player.serverLevel().getBlockEntity(hit.getBlockPos()) instanceof LaborBoardBlockEntity board)) {
            throw NO_BOARD.create();
        }
        long gameTime = player.serverLevel().getGameTime();
        List<BoardSlot> slots = board.today();
        source.sendSuccess(() -> Component.literal("[chiikawa-board] " + slots.size() + " slips today:"), false);
        for (BoardSlot slot : slots) {
            String state = slot.claim().map(claim -> "taken by " + claim.describe())
                .orElseGet(() -> slot.reservation().filter(held -> held.until() > gameTime)
                    .map(held -> "held by " + held.pet() + " for " + (held.until() - gameTime) + "t")
                    .orElse("open"));
            source.sendSuccess(() -> Component.literal(" " + slot.slip().type() + " x" + slot.slip().target()
                + " for " + slot.slip().capability() + ": " + state), false);
        }
        return slots.size();
    }

    private static AbstractPet targetedPet(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(TARGET_RANGE);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach),
            player.getBoundingBox().expandTowards(reach).inflate(1.0), entity -> entity instanceof AbstractPet,
            TARGET_RANGE * TARGET_RANGE);
        if (hit == null) {
            throw NO_PET.create();
        }
        return (AbstractPet) hit.getEntity();
    }

    private static Component flag(String label, boolean value) {
        return Component.literal(label + (value ? " yes" : " no"))
            .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static String name(ResourceLocation id) {
        return id == null ? "none" : id.toString();
    }
}
