package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Tells owners, at sunset the evening before, that tomorrow is an exam day: only the owners
 * who have a pet about that may sit it, since for anyone else it is nothing to them.
 */
public final class ExamNotices {
    private ExamNotices() {
    }

    /** Each server tick; does anything only at the moment the sun sets. */
    public static void tickServer(MinecraftServer server) {
        long dayTime = server.overworld().getDayTime();
        if (QualificationExam.timeOfDay(dayTime) != QualificationExam.EVE_REMINDER_AT) {
            return;
        }
        for (Map.Entry<ResourceLocation, Qualification> entry : Qualifications.all().entrySet()) {
            if (!QualificationExam.isExamEve(entry.getValue(), dayTime)) {
                continue;
            }
            Set<UUID> owners = new HashSet<>();
            for (ServerLevel level : server.getAllLevels()) {
                for (AbstractPet pet : level.getEntities(EntityTypeTest.forClass(AbstractPet.class),
                        pet -> pet.isTame() && QualificationExam.maySit(entry.getValue(), pet.licences().get(entry.getKey())))) {
                    if (pet.getOwnerUUID() != null) {
                        owners.add(pet.getOwnerUUID());
                    }
                }
            }
            for (UUID owner : owners) {
                ServerPlayer player = server.getPlayerList().getPlayer(owner);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.chiikawa.exam.tomorrow",
                        PetExams.name(entry.getKey())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                }
            }
        }
    }
}
