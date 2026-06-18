package com.dwinovo.chiikawa.command;

import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ChiikawaMusicCommand {
    private ChiikawaMusicCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chiikawa")
            .then(Commands.literal("music")
                .then(Commands.literal("rescan")
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                    .executes(context -> rescan(context.getSource())))));
    }

    private static int rescan(CommandSourceStack source) {
        boolean ok = ServerMusicSystem.library(source.getServer()).rescan();
        if (ok) {
            source.sendSuccess(() -> Component.literal("[chiikawa-music] Rescanned config/chiikawa/music"), false);
            return 1;
        }
        source.sendFailure(Component.literal("[chiikawa-music] Failed to rescan music library"));
        return 0;
    }
}
