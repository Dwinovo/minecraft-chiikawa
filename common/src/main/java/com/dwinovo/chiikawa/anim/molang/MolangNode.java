package com.dwinovo.chiikawa.anim.molang;

/**
 * Compiled Molang expression. Each node is a tiny pure-function: given a
 * {@link MolangContext}, return a double. The AST is built once at resource
 * load by {@link com.dwinovo.chiikawa.anim.compile.MolangCompiler} and shared
 * between all entities playing the animation — this matches GeckoLib's
 * caching of parsed expressions but with a far smaller surface area.
 *
 * <h2>Scope</h2>
 * Only the operators / functions / variables actually used in the chiikawa
 * animation files are supported (see {@code MolangCompiler} for the full
 * grammar). Everything else parses to a {@link Const}{@code (0)} so we can
 * fail soft and warn rather than crash on unfamiliar input.
 */
public sealed interface MolangNode {

    double eval(MolangContext ctx);

    /** Numeric literal. */
    record Const(double value) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return value; }
    }

    /** Variable lookup by pre-resolved slot index (see {@link MolangContext}). */
    record Var(int slot) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return ctx.vars[slot]; }
    }

    /** Unary negation. */
    record Neg(MolangNode arg) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return -arg.eval(ctx); }
    }

    record Add(MolangNode l, MolangNode r) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return l.eval(ctx) + r.eval(ctx); }
    }

    record Sub(MolangNode l, MolangNode r) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return l.eval(ctx) - r.eval(ctx); }
    }

    record Mul(MolangNode l, MolangNode r) implements MolangNode {
        @Override public double eval(MolangContext ctx) { return l.eval(ctx) * r.eval(ctx); }
    }

    record Div(MolangNode l, MolangNode r) implements MolangNode {
        @Override public double eval(MolangContext ctx) {
            double rv = r.eval(ctx);
            return rv == 0.0 ? 0.0 : l.eval(ctx) / rv;
        }
    }

    /** Function call with pre-resolved function id (see {@link MolangFn}). */
    record FuncCall(int fnId, MolangNode[] args) implements MolangNode {
        @Override public double eval(MolangContext ctx) {
            return MolangFn.invoke(fnId, args, ctx);
        }
    }
}
