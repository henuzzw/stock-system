package ai.openclaw.stockweb.order;

import ai.openclaw.stockweb.account.OrderView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrderService {
    private static final int DEFAULT_LIMIT = 100;
    private static final Set<String> ALLOWED_SIDES = Set.of("BUY", "SELL");
    private static final Set<String> ALLOWED_ORDER_TYPES = Set.of("LIMIT", "MARKET");

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderView placeOrder(long userId, PlaceOrderRequest request) {
        if (request == null) {
            throw new OrderValidationException("Request body is required");
        }

        OrderSymbolView symbol = resolveSymbol(request);
        String side = normalizeEnum(request.side(), "side", ALLOWED_SIDES);
        String orderType = normalizeEnum(request.orderType(), "orderType", ALLOWED_ORDER_TYPES);
        BigDecimal quantity = requirePositive(request.quantity(), "quantity");
        BigDecimal price = requirePositive(request.price(), "price");

        if ("SELL".equals(side)) {
            BigDecimal availableQuantity = orderRepository.findAvailableQuantity(userId, symbol.id())
                    .orElse(BigDecimal.ZERO);
            if (availableQuantity.compareTo(quantity) < 0) {
                throw new OrderValidationException("SELL quantity exceeds available position quantity");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        OrderView order = new OrderView();
        order.setUserId(userId);
        order.setStrategyRunId(null);
        order.setSymbolId(symbol.id());
        order.setSide(side);
        order.setOrderType(orderType);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setStatus("NEW");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        long orderId = orderRepository.insertOrder(order);
        return getOrder(userId, orderId);
    }

    public List<OrderView> listOrders(long userId) {
        return orderRepository.findOrdersByUserId(userId, DEFAULT_LIMIT);
    }

    public OrderView getOrder(long userId, long orderId) {
        return orderRepository.findOrderByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    private OrderSymbolView resolveSymbol(PlaceOrderRequest request) {
        if (request.symbolId() != null) {
            return orderRepository.findSymbolById(request.symbolId())
                    .orElseThrow(() -> new OrderValidationException("Symbol not found"));
        }

        String symbol = normalizeCode(request.symbol());
        if (symbol.isBlank()) {
            throw new OrderValidationException("symbol or symbolId is required");
        }

        return orderRepository.findSymbolByCode(symbol)
                .orElseThrow(() -> new OrderValidationException("Symbol not found"));
    }

    private String normalizeEnum(String raw, String fieldName, Set<String> allowedValues) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(value)) {
            throw new OrderValidationException(fieldName + " must be one of " + allowedValues);
        }
        return value;
    }

    private BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private String normalizeCode(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.matches("\\d+")) {
            return String.format("%06d", Integer.parseInt(value));
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
