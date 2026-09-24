package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Finds the in-game cases and hands them to the game. The game keeps a test in two
 * places: its code as a test function, and the test itself — its floor, its time limit,
 * the batch it runs in — as a test instance, which NeoForge lets a mod add once the data
 * packs are read. A batch is a test environment, whose setup is the batch's
 * {@link BeforeBatch} methods.
 *
 * <p>The cases are the {@link GameTest} methods of the {@link GameTestHolder} classes,
 * found through NeoForge's scan of the mod's classes as NeoForge found them itself before
 * the game took its tests over. Nothing is registered unless the game is running tests.
 */
public final class GameTestCatalog {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition>> ENVIRONMENTS =
        DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, Constants.MOD_ID);

    static {
        ENVIRONMENTS.register("batch", () -> Batch.CODEC);
    }

    private static final List<Case> CASES = new ArrayList<>();
    private static final Map<String, List<MethodHandle>> BEFORE_BATCH = new HashMap<>();

    private GameTestCatalog() {
    }

    /** Files every case with the game, when the game is running tests. */
    public static void register(IEventBus modEventBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        for (Class<?> holder : holders()) {
            String namespace = holder.getAnnotation(GameTestHolder.class).value();
            for (Method method : holder.getDeclaredMethods()) {
                GameTest test = method.getAnnotation(GameTest.class);
                if (test != null) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace,
                        holder.getSimpleName().toLowerCase(Locale.ROOT) + "." + method.getName());
                    MethodHandle handle = handle(method);
                    FUNCTIONS.register(id.getPath(), () -> helper -> run(handle, helper));
                    CASES.add(new Case(id, namespace, test));
                }
                BeforeBatch before = method.getAnnotation(BeforeBatch.class);
                if (before != null) {
                    BEFORE_BATCH.computeIfAbsent(before.batch(), batch -> new ArrayList<>()).add(handle(method));
                }
            }
        }
        FUNCTIONS.register(modEventBus);
        ENVIRONMENTS.register(modEventBus);
        modEventBus.addListener(GameTestCatalog::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Map<String, Holder<TestEnvironmentDefinition>> batches = new HashMap<>();
        for (Case each : CASES) {
            Holder<TestEnvironmentDefinition> batch = batches.computeIfAbsent(each.test().batch(),
                name -> event.registerEnvironment(ResourceLocation.fromNamespaceAndPath(each.namespace(), name), new Batch(name)));
            TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(batch,
                ResourceLocation.fromNamespaceAndPath(each.namespace(), each.test().template()), each.test().timeoutTicks(),
                0, true);
            event.registerTest(each.id(), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, each.id()), data));
        }
    }

    private static List<Class<?>> holders() {
        List<Class<?>> holders = new ArrayList<>();
        ModList.get().getModFileById(Constants.MOD_ID).getFile().getScanResult()
            .getAnnotatedBy(GameTestHolder.class, ElementType.TYPE)
            .forEach(data -> {
                try {
                    holders.add(Class.forName(data.clazz().getClassName()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("a game test holder went missing: " + data.clazz(), e);
                }
            });
        return holders;
    }

    private static MethodHandle handle(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(method + " must be static");
        }
        try {
            return MethodHandles.publicLookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(method + " must be public", e);
        }
    }

    /** Runs a case or a batch's setup, letting what it throws — a failed assertion above all — go as it was thrown. */
    private static void run(MethodHandle handle, Object argument) {
        try {
            handle.invoke(argument);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /** One case, as its annotation files it. */
    private record Case(ResourceLocation id, String namespace, GameTest test) {
    }

    /** A batch: the world settled by the batch's {@link BeforeBatch} methods before its cases run. */
    private record Batch(String name) implements TestEnvironmentDefinition {
        static final MapCodec<Batch> CODEC = Codec.STRING.fieldOf("batch").xmap(Batch::new, Batch::name);

        @Override
        public void setup(ServerLevel level) {
            BEFORE_BATCH.getOrDefault(name, List.of()).forEach(handle -> run(handle, level));
        }

        @Override
        public MapCodec<Batch> codec() {
            return CODEC;
        }
    }
}
