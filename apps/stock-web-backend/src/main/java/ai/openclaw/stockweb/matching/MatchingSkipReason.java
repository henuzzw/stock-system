package ai.openclaw.stockweb.matching;

public enum MatchingSkipReason {
    NO_PRICE,
    LIMIT_NOT_REACHED,
    INSUFFICIENT_CASH,
    INSUFFICIENT_POSITION,
    ORDER_NOT_OPEN,
    PROCESSING_ERROR
}
