package com.dwinovo.chiikawa.anim.compile;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.dwinovo.chiikawa.anim.molang.MolangFn;
import com.dwinovo.chiikawa.anim.molang.MolangNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser that turns a Molang expression string into a
 * {@link MolangNode} AST. Run once at resource load — the AST is shared
 * between all entities playing the animation.
 *
 * <h2>Grammar</h2>
 * <pre>
 *   expr      = term (('+' | '-') term)*
 *   term      = unary (('*' | '/') unary)*
 *   unary     = '-' unary | primary
 *   primary   = NUMBER | identifier | identifier '(' args? ')' | '(' expr ')'
 *   args      = expr (',' expr)*
 * </pre>
 *
 * <h2>Soft failure</h2>
 * Unknown variables / functions / unparseable input log a warning and yield
 * {@code Const(0)} rather than throwing. The animation still plays — the
 * affected channel just stays at zero. This keeps a single bad expression
 * from breaking the entire pipeline.
 */
public final class MolangCompiler {

    private final String src;
    private int pos;

    private MolangCompiler(String src) {
        this.src = src;
    }

    /** Compiles {@code input}; returns {@code Const(0)} on parse failure. */
    public static MolangNode compile(String input) {
        if (input == null || input.isBlank()) return new MolangNode.Const(0);
        try {
            MolangCompiler p = new MolangCompiler(input.trim());
            MolangNode root = p.parseExpr();
            p.skipWhitespace();
            if (p.pos != p.src.length()) {
                Constants.LOG.warn("[chiikawa-anim] trailing input in molang '{}' at {}", input, p.pos);
                return new MolangNode.Const(0);
            }
            return root;
        } catch (RuntimeException ex) {
            Constants.LOG.warn("[chiikawa-anim] failed to parse molang '{}': {}", input, ex.getMessage());
            return new MolangNode.Const(0);
        }
    }

    private MolangNode parseExpr() {
        MolangNode left = parseTerm();
        while (true) {
            skipWhitespace();
            char c = peekChar();
            if (c == '+') { pos++; left = new MolangNode.Add(left, parseTerm()); }
            else if (c == '-') { pos++; left = new MolangNode.Sub(left, parseTerm()); }
            else break;
        }
        return left;
    }

    private MolangNode parseTerm() {
        MolangNode left = parseUnary();
        while (true) {
            skipWhitespace();
            char c = peekChar();
            if (c == '*') { pos++; left = new MolangNode.Mul(left, parseUnary()); }
            else if (c == '/') { pos++; left = new MolangNode.Div(left, parseUnary()); }
            else break;
        }
        return left;
    }

    private MolangNode parseUnary() {
        skipWhitespace();
        if (peekChar() == '-') {
            pos++;
            return new MolangNode.Neg(parseUnary());
        }
        return parsePrimary();
    }

    private MolangNode parsePrimary() {
        skipWhitespace();
        char c = peekChar();
        if (c == '(') {
            pos++;
            MolangNode inner = parseExpr();
            skipWhitespace();
            expect(')');
            return inner;
        }
        if (isDigit(c) || c == '.') {
            return parseNumber();
        }
        if (isIdentStart(c)) {
            String id = parseIdent();
            skipWhitespace();
            if (peekChar() == '(') {
                pos++;
                List<MolangNode> args = parseArgList();
                expect(')');
                return makeFnCall(id, args);
            }
            return makeVarRef(id);
        }
        throw new RuntimeException("unexpected '" + c + "' at " + pos);
    }

    private List<MolangNode> parseArgList() {
        List<MolangNode> args = new ArrayList<>(3);
        skipWhitespace();
        if (peekChar() == ')') return args;
        args.add(parseExpr());
        while (true) {
            skipWhitespace();
            if (peekChar() == ',') { pos++; args.add(parseExpr()); }
            else break;
        }
        return args;
    }

    private MolangNode parseNumber() {
        int start = pos;
        while (pos < src.length() && (isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        String num = src.substring(start, pos);
        try {
            return new MolangNode.Const(Double.parseDouble(num));
        } catch (NumberFormatException ex) {
            throw new RuntimeException("bad number '" + num + "' at " + start);
        }
    }

    private String parseIdent() {
        int start = pos;
        while (pos < src.length() && isIdentPart(src.charAt(pos))) {
            pos++;
        }
        return src.substring(start, pos);
    }

    private MolangNode makeFnCall(String name, List<MolangNode> args) {
        int fnId = resolveFn(name);
        if (fnId == MolangFn.FN_UNKNOWN) {
            Constants.LOG.warn("[chiikawa-anim] unknown molang function '{}'; using 0", name);
            return new MolangNode.Const(0);
        }
        int required = MolangFn.requiredArity(fnId);
        if (args.size() != required) {
            Constants.LOG.warn("[chiikawa-anim] molang '{}' expects {} arg(s) but got {}; using 0",
                    name, required, args.size());
            return new MolangNode.Const(0);
        }
        return new MolangNode.FuncCall(fnId, args.toArray(new MolangNode[0]));
    }

    private MolangNode makeVarRef(String name) {
        int slot = MolangContext.resolveSlot(name);
        if (slot < 0) {
            Constants.LOG.warn("[chiikawa-anim] unknown molang variable '{}'; using 0", name);
            return new MolangNode.Const(0);
        }
        return new MolangNode.Var(slot);
    }

    /** Case-insensitive: {@code Math.sin} and {@code math.sin} both resolve. */
    private static int resolveFn(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return switch (lower) {
            case "math.cos"   -> MolangFn.FN_COS;
            case "math.sin"   -> MolangFn.FN_SIN;
            case "math.lerp"  -> MolangFn.FN_LERP;
            case "math.exp"   -> MolangFn.FN_EXP;
            case "math.clamp" -> MolangFn.FN_CLAMP;
            default -> MolangFn.FN_UNKNOWN;
        };
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new RuntimeException("expected '" + c + "' at " + pos);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peekChar() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c) || c == '.';
    }
}
