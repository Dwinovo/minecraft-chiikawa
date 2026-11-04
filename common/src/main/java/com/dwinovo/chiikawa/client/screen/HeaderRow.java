package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Chip;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Where the things on the pet screen's banner's first row go, for the words they have: the pet's name, its job and its
 * licence from the left, its pocket money and its keenness from the right. The words are not the same width in every
 * language or for every pet, so the row is worked out from them: the right-hand badges are as wide as their words and the
 * left-hand things take what is left. Nothing here needs the game, so a test can ask.
 *
 * <p>When the row is crowded the name is what gives: it is drawn at twice the size when it fits so, at the size of the rest
 * when it does not, and cut short last. The job and the licence give up some of their words before that, but never all.
 * Every x here is from the panel's left edge.
 *
 * @param nameScale how many times the size of the font the name is drawn at
 * @param nameWidth how wide the name is drawn, at that size, the most it may take; a name that is wider is cut
 * @param jobWidth how wide the job's chip is, its picture and its words
 * @param rankWidth how wide the licence's chip is, nothing when there is none
 * @param moneyX where the pocket money's badge starts
 * @param moneyWidth how wide it is
 * @param eagerX where the keenness's badge starts, nothing when the pet is not keen
 * @param eagerWidth how wide it is, nothing when the pet is not keen
 */
public record HeaderRow(int nameScale, int nameWidth, int jobWidth, int rankWidth, int moneyX, int moneyWidth, int eagerX,
                        int eagerWidth) {
    /** The air between the name and the first chip. */
    static final int NAME_GAP = UiStyle.GAP;
    /** The air between one chip and the next. */
    static final int CHIP_GAP = UiStyle.TIGHT;
    /** The fewest pixels of words a chip keeps when the row is crowded: enough for a word's first letters. */
    static final int LEAST_WORDS = 14;
    /** The name's size when it fits. */
    static final int BIG = 2;

    /** Where the job's chip starts. */
    public int jobX() {
        return PetScreenLayout.TEXT_X + nameWidth + NAME_GAP;
    }

    /** Where the licence's chip starts. */
    public int rankX() {
        return jobX() + jobWidth + CHIP_GAP;
    }

    /** How much room the words of a chip this wide have. */
    public static int wordsIn(int chipWidth) {
        return chipWidth - Chip.overhead();
    }

    /**
     * @param textWidth how wide the game draws a string
     * @param rank the words on the licence's chip, when there is a licence to show
     * @param eager how long the pet's keenness has left, when it is keen
     */
    public static HeaderRow place(ToIntFunction<String> textWidth, String name, String job, Optional<String> rank, String money,
            Optional<String> eager) {
        int right = PetScreenLayout.RIGHT;
        int eagerWidth = eager.map(words -> Chip.overhead() + textWidth.applyAsInt(words)).orElse(0);
        int eagerX = eager.isPresent() ? right - eagerWidth : 0;
        if (eager.isPresent()) {
            right = eagerX - CHIP_GAP;
        }
        int moneyWidth = Chip.overhead() + textWidth.applyAsInt(money);
        int moneyX = right - moneyWidth;
        int room = moneyX - UiStyle.GAP - PetScreenLayout.TEXT_X;

        int jobWidth = Chip.overhead() + textWidth.applyAsInt(job);
        int rankWidth = rank.map(words -> Chip.overhead() + textWidth.applyAsInt(words)).orElse(0);
        int nameRoom = room - NAME_GAP - jobWidth - (rank.isPresent() ? CHIP_GAP + rankWidth : 0);
        int nameWide = textWidth.applyAsInt(name);

        int scale = nameWide * BIG <= nameRoom ? BIG : 1;
        int nameWidth = nameWide * scale;
        if (nameWidth > nameRoom) {
            // Crowded: the chips give up some of their words, down to a few letters, before the name gives up any.
            int over = nameWidth - nameRoom;
            int fromJob = Math.min(over, Math.max(0, jobWidth - Chip.overhead() - LEAST_WORDS));
            jobWidth -= fromJob;
            over -= fromJob;
            if (rank.isPresent()) {
                int fromRank = Math.min(over, Math.max(0, rankWidth - Chip.overhead() - LEAST_WORDS));
                rankWidth -= fromRank;
                over -= fromRank;
            }
            nameWidth = Math.max(0, nameWidth - over);
        }
        return new HeaderRow(scale, nameWidth, jobWidth, rankWidth, moneyX, moneyWidth, eagerX, eagerWidth);
    }
}
