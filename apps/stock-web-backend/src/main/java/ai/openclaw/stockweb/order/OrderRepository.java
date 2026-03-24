package ai.openclaw.stockweb.order;

import ai.openclaw.stockweb.account.OrderView;
import ai.openclaw.stockweb.mapper.OrderMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final OrderMapper mapper;

    public OrderRepository(OrderMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<OrderSymbolView> findSymbolById(long symbolId) {
        return mapper.findSymbolById(symbolId);
    }

    public Optional<OrderSymbolView> findSymbolByCode(String code) {
        return mapper.findSymbolByCode(code);
    }

    public Optional<BigDecimal> findAvailableQuantity(long userId, long symbolId) {
        return mapper.findAvailableQuantity(userId, symbolId);
    }

    public List<OrderView> findOrdersByUserId(long userId, int limit) {
        return mapper.findOrdersByUserId(userId, limit);
    }

    public Optional<OrderView> findOrderByIdAndUserId(long id, long userId) {
        return mapper.findOrderByIdAndUserId(id, userId);
    }

    @Transactional
    public long insertOrder(OrderView order) {
        mapper.insertOrder(order);
        return order.getId();
    }
}
