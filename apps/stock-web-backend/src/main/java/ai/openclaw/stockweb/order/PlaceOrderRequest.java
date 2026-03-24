package ai.openclaw.stockweb.order;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        Long symbolId,
        String symbol,
        String side,
        String orderType,
        BigDecimal price,
        BigDecimal quantity
) {
}
