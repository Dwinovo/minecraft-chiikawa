package com.dwinovo.chiikawa.qualification;

/**
 * Why a pet cannot be signed up for an exam at a desk now. The first three are the
 * licence's business, see {@link QualificationExam#whyNot}; the rest are where the pet is,
 * see {@link ExamEnrollment}.
 */
public enum Ineligible {
    /** It holds every grade there is. */
    TOP_GRADE,
    /** It has not done the practice since its last exam. */
    UNPRACTISED,
    /** It is signed up already, or waiting to hear how its last exam went. */
    BUSY,
    /** Its owner told it to stay where it is. */
    STAYING,
    /** The desk is not somewhere it would go from where it is. */
    OUT_OF_REACH
}
