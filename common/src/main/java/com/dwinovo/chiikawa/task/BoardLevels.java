package com.dwinovo.chiikawa.task;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

/**
 * How far an owner can pay a labor board up, and what each level gives: the slips it puts
 * up a day, and what it costs to reach. Loaded from
 * {@code data/chiikawa/labor_board/levels.json} by {@link BoardLevelsLoader}, so a pack
 * can have more levels, dearer ones or busier ones; the mod's own are generated.
 *
 * <p>Server-side only. A player's game is told what it needs — the level, today's slips,
 * the next level's price — with the board's screen.
 *
 * @param levels the first level first
 */
public record BoardLevels(List<Level> levels) {
    /** Where a board stands the day it is placed: the first of the levels. */
    public static final int FIRST_LEVEL = 1;
    /**
     * The most slips one level can put up. Which of a board's slips still hang is sent to
     * players as the bits of one int.
     */
    public static final int MOST_SLIPS = Integer.SIZE - 1;
    /** What boards go by with no levels loaded: a single level that puts nothing up. */
    public static final BoardLevels NONE = new BoardLevels(List.of(new Level(0, 0)));

    public static final Codec<BoardLevels> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<BoardLevels>create(instance -> instance.group(
        ExtraCodecs.nonEmptyList(Level.CODEC.listOf()).fieldOf("levels").forGetter(BoardLevels::levels)
    ).apply(instance, BoardLevels::new)), BoardLevels::priced);

    private static volatile BoardLevels current = NONE;

    public BoardLevels {
        levels = List.copyOf(levels);
    }

    /** @return the levels the loaded data packs give every board */
    public static BoardLevels current() {
        return current;
    }

    static void replace(BoardLevels levels) {
        current = levels;
    }

    /** @return as far as an owner can take a board */
    public int top() {
        return FIRST_LEVEL + levels.size() - 1;
    }

    /**
     * The level a board is at, whatever its save says: a pack that took levels away since
     * leaves the board at the top of the ones there are, and gives the rest back if it
     * puts them back.
     */
    public int clamp(int level) {
        return Mth.clamp(level, FIRST_LEVEL, top());
    }

    /** How many slips a board of this level puts up a day. */
    public int slipsAt(int level) {
        return levels.get(clamp(level) - FIRST_LEVEL).slips();
    }

    /**
     * @return what it costs to take a board from this level to the next, or 0 at the top,
     *         which is also how a screen knows there is nothing left to buy
     */
    public int priceAfter(int level) {
        int at = clamp(level);
        return at >= top() ? 0 : levels.get(at + 1 - FIRST_LEVEL).price();
    }

    /** A level anyone could have for nothing would read as the top one: every level past the first has a price. */
    private static DataResult<BoardLevels> priced(BoardLevels board) {
        for (int i = 1; i < board.levels.size(); i++) {
            if (board.levels.get(i).price() <= 0) {
                int level = FIRST_LEVEL + i;
                return DataResult.error(() -> "level " + level + " has no price");
            }
        }
        return DataResult.success(board);
    }

    /**
     * @param slips how many slips a board of this level puts up a day
     * @param price what the owner pays to reach it from the level below; a
     *              board starts at the first level, so the first's is never asked
     */
    public record Level(int slips, int price) {
        static final Codec<Level> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, MOST_SLIPS).fieldOf("slips").forGetter(Level::slips),
            ExtraCodecs.strictOptionalField(ExtraCodecs.NON_NEGATIVE_INT, "price", 0).forGetter(Level::price)
        ).apply(instance, Level::new));
    }
}
