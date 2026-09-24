package com.dwinovo.chiikawa.voice;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.network.VoicePayloads.PetSpeechPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A pet saying something out loud (design 0.1.1, section 3). The server decides whether it
 * speaks up and which line it says, so everyone near it hears the same thing, and tells the
 * players within earshot; their games put the words in a bubble over its head and open its
 * mouth. Who the pet belongs to makes no difference: a pet talks to the world, not to a
 * screen.
 */
public final class PetSpeech {
    /** How far a pet is heard: as far as the game carries a sound at full volume. */
    public static final double HEARING_RANGE = 16.0;
    /** How long a line stays up over a pet's head, long enough to read a few words. */
    public static final int TALK_TICKS = 60;
    /** The two ways every pet opens its mouth to talk. */
    public static final List<String> MOUTHS = List.of("open_mouth1", "open_mouth2");

    private PetSpeech() {
    }

    /**
     * The pet says something fitting for the moment, if it has anything to say then, is not
     * still getting over the last thing it said, and is not one voice too many in a crowd.
     * Server side; a no-op on the client.
     *
     * @return the translation key of what it said, or nothing when it kept quiet
     */
    public static Optional<String> say(AbstractPet pet, VoiceMoment moment) {
        if (!(pet.level() instanceof ServerLevel level) || !pet.isAlive()) {
            return Optional.empty();
        }
        PetVoice voice = PetVoices.of(pet.getType());
        long now = level.getGameTime();
        boolean coolingDown = pet.getBrain().getMemory(InitMemory.LAST_SAID.get())
            .filter(said -> now - said.gameTime() < voice.cooldownTicks())
            .isPresent();
        if (coolingDown) {
            return Optional.empty();
        }
        Optional<String> line = voice.draw(moment, pet.getRandom());
        if (line.isEmpty() || talkingNearby(level, pet, now) >= voice.crowdLimit()) {
            return Optional.empty();
        }
        pet.getBrain().setMemory(InitMemory.LAST_SAID.get(), new Said(line.get(), now));
        PetSpeechPayload payload = new PetSpeechPayload(pet.getId(), line.get());
        // Sent as the game sends a sound: to every player near enough to hear it.
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pet) <= HEARING_RANGE * HEARING_RANGE
                    && Services.NETWORK.canReceive(player, payload.id())) {
                Services.NETWORK.sendToClient(player, payload);
            }
        }
        return line;
    }

    /** How many other pets within earshot of this one have a line over their heads. */
    private static long talkingNearby(ServerLevel level, AbstractPet pet, long now) {
        return level.getEntitiesOfClass(AbstractPet.class, pet.getBoundingBox().inflate(HEARING_RANGE),
                other -> other != pet && other.distanceToSqr(pet) <= HEARING_RANGE * HEARING_RANGE)
            .stream()
            .filter(other -> other.getBrain().getMemory(InitMemory.LAST_SAID.get())
                .filter(said -> now - said.gameTime() < TALK_TICKS)
                .isPresent())
            .count();
    }

    /**
     * Which of {@link #MOUTHS} a line is said with. Worked out from the line, so everyone
     * who hears it sees the pet say it the same way.
     */
    public static String mouth(String line) {
        return MOUTHS.get(Math.floorMod(line.hashCode(), MOUTHS.size()));
    }

    /**
     * What a pet last said and when: why it keeps quiet for a while afterwards, and how the
     * pets around it know it is talking.
     *
     * @param line the translation key of what it said
     * @param gameTime when it said it
     */
    public record Said(String line, long gameTime) {
    }

    /**
     * A line a player's game heard a pet say, and when, by the pet's own tick count.
     *
     * @param line the translation key of what it said
     * @param since the pet's tick count when it started saying it
     */
    public record Heard(String line, int since) {
    }
}
