package ai.openclaw.stockweb.matching;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MatchingOrderResult(
        Long orderId,
        Long symbolId,
        String code,
        String name,
        String side,
        String orderType,
        String orderStatus,
        BigDecimal limitPrice,
        BigDecimal requestedQuantity,
        BigDecimal filledQuantity,
        BigDecimal remainingQuantity,
        boolean filled,
        BigDecimal amount,
        BigDecimal marketPrice,
        String priceSource,
        LocalDateTime matchedAt,
        MatchingSkipReason skipReason
) {
    public static MatchingOrderResult filled(
            MatchableOrderView order,
            BigDecimal remainingQuantity,
            MatchPriceView matchPrice,
            BigDecimal amount
    ) {
        return new MatchingOrderResult(
                order.getId(),
                order.getSymbolId(),
                order.getCode(),
                order.getName(),
                order.getSide(),
                order.getOrderType(),
                "FILLED",
                order.getPrice(),
                order.getQuantity(),
                order.getFilledQuantity() == null ? remainingQuantity : order.getFilledQuantity().add(remainingQuantity),
                remainingQuantity,
                true,
                amount,
                matchPrice == null ? null : matchPrice.getPrice(),
                matchPrice == null ? null : matchPrice.getSource(),
                matchPrice == null ? null : matchPrice.getMatchedAt(),
                null
        );
    }

    public static MatchingOrderResult skipped(
            MatchableOrderView order,
            BigDecimal remainingQuantity,
            MatchPriceView matchPrice,
            MatchingSkipReason skipReason
    ) {
        return new MatchingOrderResult(
                order == null ? null : order.getId(),
                order == null ? null : order.getSymbolId(),
                order == null ? null : order.getCode(),
                order == null ? null : order.getName(),
                order == null ? null : order.getSide(),
                order == null ? null : order.getOrderType(),
                order == null ? null : order.getStatus(),
                order == null ? null : order.getPrice(),
                order == null ? null : order.getQuantity(),
                order == null ? null : order.getFilledQuantity(),
                remainingQuantity,
                false,
                BigDecimal.ZERO,
                matchPrice == null ? null : matchPrice.getPrice(),
                matchPrice == null ? null : matchPrice.getSource(),
                matchPrice == null ? null : matchPrice.getMatchedAt(),
                skipReason
        );
    }

    public static MatchingOrderResult processingError(MatchableOrderView order) {
        BigDecimal requestedQuantity = order == null || order.getQuantity() == null ? BigDecimal.ZERO : order.getQuantity();
        BigDecimal filledQuantity = order == null || order.getFilledQuantity() == null ? BigDecimal.ZERO : order.getFilledQuantity();
        return skipped(order, requestedQuantity.subtract(filledQuantity), null, MatchingSkipReason.PROCESSING_ERROR);
    }
}
