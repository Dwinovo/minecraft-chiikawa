package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.objectweb.asm.Type;

/**
 * Hands the game the cases in the {@link GameTestHolder} classes the way it has taken them
 * since 1.21.5: each case a test function and a test instance named after its class and
 * method, as the game named them before, and each batch a test environment that settles
 * the world by the batch's {@link BeforeBatch} methods and puts it back by its
 * {@link AfterBatch} methods. Cases of one environment run as one batch. The cases are the
 * {@link GameTest} methods of the holders and the {@link GeneratedGameTest}s their
 * {@link GameTestGenerator} methods make.
 *
 * <p>Only while the game runs its cases, as NeoForge only collected holders then.
 */
public final class GameTestRegistration {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition>> ENVIRONMENT_TYPES =
        DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, Constants.MOD_ID);
    private static final Type HOLDER = Type.getType(GameTestHolder.class);

    /** Every case found, in the order the holders were scanned. */
    private static final List<Case> CASES = new ArrayList<>();
    /** How each batch settles its world, by batch name. */
    private static final Map<String, List<Method>> SETTLERS = new HashMap<>();
    /** How each batch puts its world back, by batch name. */
    private static final Map<String, List<Method>> RESTORERS = new HashMap<>();

    static {
        ENVIRONMENT_TYPES.register("batch", () -> Batch.CODEC);
    }

    private GameTestRegistration() {
    }

    public static void register(IEventBus modEventBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        ModList.get().getAllScanData().forEach(scan -> scan.getAnnotations().stream()
            .filter(annotation -> HOLDER.equals(annotation.annotationType()))
            .forEach(annotation -> collect(load(annotation.clazz().getClassName()))));
        FUNCTIONS.register(modEventBus);
        ENVIRONMENT_TYPES.register(modEventBus);
        modEventBus.addListener(GameTestRegistration::registerTests);
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("A game test holder vanished: " + className, e);
        }
    }

    private static void collect(Class<?> holder) {
        String namespace = holder.getAnnotation(GameTestHolder.class).value();
        PrefixGameTestTemplate prefix = holder.getAnnotation(PrefixGameTestTemplate.class);
        String className = holder.getSimpleName().toLowerCase(Locale.ROOT);
        for (Method method : holder.getDeclaredMethods()) {
            GameTest test = method.getAnnotation(GameTest.class);
            if (test != null) {
                requireStatic(method);
                file(namespace, className, prefix, method.getName(), test.template(), test.batch(), test.timeoutTicks(),
                    helper -> invoke(method, helper));
            }
            if (method.isAnnotationPresent(GameTestGenerator.class)) {
                requireStatic(method);
                generated(method).forEach(each -> file(namespace, className, prefix, each.name(), each.template(),
                    each.batch(), each.timeoutTicks(), each.body()));
            }
            BeforeBatch before = method.getAnnotation(BeforeBatch.class);
            if (before != null) {
                requireStatic(method);
                SETTLERS.computeIfAbsent(before.batch(), batch -> new ArrayList<>()).add(method);
            }
            AfterBatch after = method.getAnnotation(AfterBatch.class);
            if (after != null) {
                requireStatic(method);
                RESTORERS.computeIfAbsent(after.batch(), batch -> new ArrayList<>()).add(method);
            }
        }
    }

    /** Files one case: its code as a test function now, its instance when the game takes the event. */
    private static void file(String namespace, String className, PrefixGameTestTemplate prefix, String method,
            String template, String batch, int timeoutTicks, Consumer<GameTestHelper> body) {
        String name = className + "." + method.toLowerCase(Locale.ROOT);
        String structure = prefix == null || prefix.value() ? className + "." + template : template;
        FUNCTIONS.register(name, () -> helper -> {
            BatchYards.note(batch, helper);
            body.accept(helper);
        });
        CASES.add(new Case(Identifier.fromNamespaceAndPath(namespace, name),
            Identifier.fromNamespaceAndPath(namespace, structure), batch, timeoutTicks));
    }

    @SuppressWarnings("unchecked")
    private static Collection<GeneratedGameTest> generated(Method method) {
        try {
            return (Collection<GeneratedGameTest>) method.invoke(null);
        } catch (InvocationTargetException e) {
            throw thrown(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Map<String, Holder<TestEnvironmentDefinition>> environments = new HashMap<>();
        for (Case test : CASES) {
            Holder<TestEnvironmentDefinition> environment = environments.computeIfAbsent(test.batch(), batch ->
                event.registerEnvironment(Identifier.fromNamespaceAndPath(test.id().getNamespace(), batch),
                    new Batch(batch)));
            event.registerTest(test.id(), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, test.id()),
                new TestData<>(environment, test.template(), test.timeoutTicks(), 0, true)));
        }
    }

    private static void requireStatic(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("A game test method must be static: " + method);
        }
    }

    /** Runs a case or a settler, handing on whatever it threw as the game's own would be. */
    private static void invoke(Method method, Object argument) {
        try {
            method.invoke(null, argument);
        } catch (InvocationTargetException e) {
            throw thrown(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RuntimeException thrown(InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException runtime) {
            return runtime;
        }
        if (e.getCause() instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(e.getCause());
    }

    /**
     * @param id the case's name, which is its test function's name too
     * @param template the structure it runs on
     */
    private record Case(Identifier id, Identifier template, String batch, int timeoutTicks) {
    }

    /**
     * A batch's world, settled by its {@link BeforeBatch} methods when the batch starts and put back by its
     * {@link AfterBatch} methods once it has run, and its yards cleared.
     */
    private record Batch(String name) implements TestEnvironmentDefinition {
        static final MapCodec<Batch> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Batch::name)
        ).apply(instance, Batch::new));

        @Override
        public void setup(ServerLevel level) {
            SETTLERS.getOrDefault(name, List.of()).forEach(settler -> invoke(settler, level));
        }

        @Override
        public void teardown(ServerLevel level) {
            RESTORERS.getOrDefault(name, List.of()).forEach(restorer -> invoke(restorer, level));
            BatchYards.clear(name, level);
        }

        @Override
        public MapCodec<Batch> codec() {
            return CODEC;
        }
    }
}
