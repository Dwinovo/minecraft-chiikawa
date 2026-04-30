package com.dwinovo.chiikawa.anim.molang;

/**
 * Built-in Molang function dispatch. Compiler resolves a function name to one
 * of these integer ids at parse time so {@link MolangNode.FuncCall#eval} can
 * skip any name lookup.
 *
 * <p>Bedrock spec: {@code math.cos} / {@code math.sin} take <b>degrees</b>,
 * not radians. {@code math.lerp} is the standard linear blend. {@code math.exp}
 * is the natural exponent. {@code math.clamp} pins a value into a closed range.
 *
 * <p>The capital-{@code M} form ({@code Math.sin}) is non-standard but emitted
 * by Blockbench in some pipelines; we treat it as a synonym of the lowercase
 * function. See {@link com.dwinovo.chiikawa.anim.compile.MolangCompiler#resolveFn}.
 */
public final class MolangFn {

    public static final int FN_COS    = 0;
    public static final int FN_SIN    = 1;
    public static final int FN_LERP   = 2;
    public static final int FN_EXP    = 3;
    public static final int FN_CLAMP  = 4;

    public static final int FN_UNKNOWN = -1;

    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private MolangFn() {}

    public static double invoke(int fnId, MolangNode[] args, MolangContext ctx) {
        return switch (fnId) {
            case FN_COS   -> Math.cos(args[0].eval(ctx) * DEG_TO_RAD);
            case FN_SIN   -> Math.sin(args[0].eval(ctx) * DEG_TO_RAD);
            case FN_LERP  -> {
                double a = args[0].eval(ctx);
                double b = args[1].eval(ctx);
                double t = args[2].eval(ctx);
                yield a + (b - a) * t;
            }
            case FN_EXP   -> Math.exp(args[0].eval(ctx));
            case FN_CLAMP -> {
                double v  = args[0].eval(ctx);
                double lo = args[1].eval(ctx);
                double hi = args[2].eval(ctx);
                yield v < lo ? lo : (v > hi ? hi : v);
            }
            default -> 0.0;
        };
    }

    public static int requiredArity(int fnId) {
        return switch (fnId) {
            case FN_COS, FN_SIN, FN_EXP -> 1;
            case FN_LERP, FN_CLAMP -> 3;
            default -> -1;
        };
    }
}
