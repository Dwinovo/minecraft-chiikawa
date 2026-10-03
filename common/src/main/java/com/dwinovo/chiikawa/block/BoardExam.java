package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.qualification.QualificationExam;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The exam room in front of a labor board: the day an owner last opened an exam here, the
 * seats pets sit it in, and the results the board posts. Kept by the board and saved with
 * it. Who was called to sit is each pet's own business, and each carries it.
 */
public final class BoardExam {
    private static final long NEVER = -1L;

    public static final Codec<BoardExam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.optionalFieldOf("opened_on", NEVER).forGetter(exam -> exam.openedOn),
        ExamSeats.CODEC.optionalFieldOf("seats", new ExamSeats()).forGetter(BoardExam::seats),
        ExamResults.CODEC.optionalFieldOf("results", new ExamResults()).forGetter(BoardExam::results)
    ).apply(instance, BoardExam::new));

    private long openedOn;
    private final ExamSeats seats;
    private final ExamResults results;

    public BoardExam() {
        this(NEVER, new ExamSeats(), new ExamResults());
    }

    private BoardExam(long openedOn, ExamSeats seats, ExamResults results) {
        this.openedOn = openedOn;
        this.seats = seats;
        this.results = results;
    }

    /** An exam was opened here on {@code day}. */
    public void opened(long day) {
        openedOn = day;
    }

    /** Whether an exam opened here is on now: opened today, and not yet closed for the day. */
    public boolean isOn(long dayTime) {
        return openedOn == QualificationExam.day(dayTime)
            && QualificationExam.timeOfDay(dayTime) < QualificationExam.EXAM_UNTIL;
    }

    public ExamSeats seats() {
        return seats;
    }

    public ExamResults results() {
        return results;
    }
}
