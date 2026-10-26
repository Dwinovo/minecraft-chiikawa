package com.dwinovo.chiikawa.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Decision;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.Site;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestRests;
import com.dwinovo.chiikawa.testing.TestTargets;
import com.dwinovo.chiikawa.testing.TestView;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What the pet screen is told: what the pet would do next, and why it is not at work. */
class PetOutlookTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final PetAnchor FREE = TestAnchors.free(HOME);
    private static final List<ResourceLocation> FARM = List.of(ErrandIds.WANDER, WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER);
    private static final Set<ResourceLocation> WORK = Set.of(WorkData.HARVEST, WorkData.PLANT, WorkData.DELIVER, WorkData.MELEE);
    private static final long NOW = 5000L;
    private static final int PET = 17;

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void aFarmerWithNoSeedsWouldPottersAboutAndSaysItIsShortOfSeeds() {
        TestView view = TestView.at(HOME, FREE);
        PetMind.Look look = look(FARM, view, Optional.empty());

        PetOutlookPayload outlook = PetOutlookPayload.of(PET, look, whyNot(look));

        assertEquals(new PetOutlookPayload(PET, Optional.of(ErrandIds.WANDER), Optional.of("intent.chiikawa.fail.no_farmland"),
            Optional.of("tag.item.chiikawa.entity_plant_crops")), outlook);
    }

    @Test
    void aFarmerWithSeedsAndNothingToDoSaysWhatItFoundAndNothingIsShort() {
        TestView view = TestView.at(HOME, FREE).carryingSeeds();
        PetMind.Look look = look(FARM, view, Optional.empty());

        PetOutlookPayload outlook = PetOutlookPayload.of(PET, look, whyNot(look));

        assertEquals(Optional.of("intent.chiikawa.fail.no_crop"), outlook.whyNotKey());
        assertEquals(Optional.empty(), outlook.lackKey());
    }

    @Test
    void aPetAtWorkHasNoReasonItIsNotAndWouldNotDoWhatItIsAlreadyDoing() {
        GlobalPos crop = GlobalPos.of(OVERWORLD, new BlockPos(3, 64, 0));
        TestView view = TestView.at(HOME, FREE).targets(TestTargets.of(Optional.empty(), Optional.of(crop), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        Running reaping = running(WorkData.HARVEST, new Site(crop));
        PetMind.Look look = look(FARM, view, Optional.of(reaping));

        PetOutlookPayload outlook = PetOutlookPayload.of(PET, look, whyNot(look));

        assertEquals(new PetOutlookPayload(PET, Optional.of(ErrandIds.WANDER), Optional.empty(), Optional.empty()), outlook);
        // Were it on none, harvesting is the first thing it would do.
        assertEquals(Optional.of(WorkData.HARVEST), PetOutlookPayload.of(PET, look(FARM, view, Optional.empty()), Optional.empty()).next());
    }

    @Test
    void theHighestPriorityThatCanStartIsWhatComesNextWhateverTheScores() {
        TestView view = TestView.at(HOME, FREE).targets(TestTargets.of(Optional.of(TestTargets.foe(UUID.randomUUID(),
            GlobalPos.of(OVERWORLD, new BlockPos(2, 64, 0)), Ground.SELF, Ground.OWNER, Ground.NEARBY)), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())).armedWithArrows();

        assertEquals(Optional.of(WorkData.MELEE),
            PetOutlookPayload.of(PET, look(List.of(ErrandIds.WANDER, WorkData.MELEE), view, Optional.empty()), Optional.empty()).next());
    }

    @Test
    void withNothingThatCouldStartThereIsNothingNext() {
        PetOutlookPayload outlook = PetOutlookPayload.of(PET, look(List.of(WorkData.HARVEST), TestView.at(HOME, FREE), Optional.empty()),
            Optional.empty());

        assertEquals(Optional.empty(), outlook.next());
    }

    @Test
    void whatTheScreenIsToldComesBackAsItWasSent() {
        PetOutlookPayload outlook = new PetOutlookPayload(PET, Optional.of(WorkData.HARVEST), Optional.of("intent.chiikawa.fail.no_crop"),
            Optional.of("tag.item.minecraft.arrows"));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        PetOutlookPayload.STREAM_CODEC.encode(buffer, outlook);

        assertEquals(outlook, PetOutlookPayload.STREAM_CODEC.decode(buffer));
    }

    private static Optional<WhyNot> whyNot(PetMind.Look look) {
        return WhyNot.of(look.trace(), WORK::contains, NOW);
    }

    /** How each errand would fare for the pet if it were on none, as the pet's mind looks. */
    private static PetMind.Look look(List<ResourceLocation> ids, PetView view, Optional<Running> running) {
        DecisionTrace trace = new DecisionTrace();
        List<ErrandType<?, ?>> offered = ids.stream().<ErrandType<?, ?>>map(TestErrands::errand).toList();
        Decision.decide(offered, view, category -> true, TestRests.noRests(), Integer.MIN_VALUE, null, NOW, trace);
        return new PetMind.Look(running, NOW, DayPhase.DAY, trace);
    }

    private static Running running(ResourceLocation errand, Site plan) {
        return new Running(TestErrands.errand(errand), plan, ErrandSettings.of(errand).orElseThrow(), 0, NOW, Set.of());
    }
}
