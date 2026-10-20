package com.dwinovo.chiikawa.qualification;

/**
 * How an exam went, as the pet is told once its results are out.
 *
 * @param passed whether it passed
 * @param rank the rank of the grade it sat for, as the owner reads it
 */
public record ExamResult(boolean passed, int rank) {
}
