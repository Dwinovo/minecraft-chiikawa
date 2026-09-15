package com.dwinovo.chiikawa.command;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSwitchLog;
import com.dwinovo.chiikawa.init.InitMemory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /chiikawa debug intent} inspects the intent selector of the pet under the
 * player's crosshair: every candidate with its permission, condition and score,
 * and, once {@code log on} is set for that pet, its recent intent switches.
 */
public final class ChiikawaDebugCommand {
    private static final double TARGET_RANGE = 16.0;
    private static final SimpleCommandExceptionType NO_PET =
        new SimpleCommandExceptionType(Component.literal("[chiikawa-intent] Look at a pet within 16 blocks"));

    private ChiikawaDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chiikawa")
            .then(Commands.literal("debug")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("intent")
                    .executes(context -> showCandidates(context.getSource()))
                    .then(Commands.literal("log")
                        .executes(context -> showLog(context.getSource()))
                        .then(Commands.literal("on").executes(context -> setLogging(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setLogging(context.getSource(), false)))))));
    }

    private static int showCandidates(CommandSourceStack source) throws CommandSyntaxException {
        AbstractPet pet = targetedPet(source);
        IntentSelector.Snapshot snapshot = IntentSelector.describe(pet);
        MutableComponent header = Component.literal("[chiikawa-intent] ").append(pet.getDisplayName()).append(" running: ");
        header.append(snapshot.running()
            .map(running -> Component.literal(running.id() + " for " + (snapshot.gameTime() - running.startTick()) + "t"))
            .orElse(Component.literal("none")));
        header.append(Component.literal(" (" + snapshot.phase().getSerializedName() + ")"));
        source.sendSuccess(() -> header, false);
        for (IntentSelector.CandidateView view : snapshot.candidates()) {
            MutableComponent line = Component.literal(" " + view.intent().id() + " ")
                .append(flag("allowed", view.allowed()))
                .append(" ")
                .append(flag("check", view.check().ok()));
            if (view.check().reasonKey() != null) {
                line.append(Component.literal(" (").append(Component.translatable(view.check().reasonKey())).append(")"));
            }
            line.append(Component.literal(String.format(Locale.ROOT, " score %.2f", view.score())));
            source.sendSuccess(() -> line, false);
        }
        return snapshot.candidates().size();
    }

    private static int showLog(CommandSourceStack source) throws CommandSyntaxException {
        AbstractPet pet = targetedPet(source);
        IntentSwitchLog log = pet.getBrain().getMemory(InitMemory.INTENT_SWITCH_LOG.get()).orElse(null);
        if (log == null) {
            source.sendFailure(Component.literal("[chiikawa-intent] Switch log is off for this pet, use /chiikawa debug intent log on"));
            return 0;
        }
        List<IntentSwitchLog.Entry> entries = log.entries();
        source.sendSuccess(() -> Component.literal("[chiikawa-intent] ").append(pet.getDisplayName())
            .append(" last " + entries.size() + " switches:"), false);
        for (IntentSwitchLog.Entry entry : entries) {
            String top = entry.top().stream()
                .map(scored -> String.format(Locale.ROOT, "%s %.2f", scored.id(), scored.score()))
                .collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal(" " + entry.gameTime() + ": " + name(entry.from()) + " -> "
                + name(entry.to()) + " [" + entry.cause() + "] " + top), false);
        }
        return entries.size();
    }

    private static int setLogging(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        AbstractPet pet = targetedPet(source);
        if (enabled) {
            if (!pet.getBrain().hasMemoryValue(InitMemory.INTENT_SWITCH_LOG.get())) {
                pet.getBrain().setMemory(InitMemory.INTENT_SWITCH_LOG.get(), new IntentSwitchLog());
            }
        } else {
            pet.getBrain().eraseMemory(InitMemory.INTENT_SWITCH_LOG.get());
        }
        source.sendSuccess(() -> Component.literal("[chiikawa-intent] Switch log " + (enabled ? "on" : "off") + " for ")
            .append(pet.getDisplayName()), false);
        return 1;
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
