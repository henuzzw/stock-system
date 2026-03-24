package ai.openclaw.stockweb.account;

import ai.openclaw.stockweb.mapper.AccountMapper;
import ai.openclaw.stockweb.trade.TradeView;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AccountRepository {
    private static final BigDecimal INITIAL_CASH = new BigDecimal("10000000.00");

    private final AccountMapper mapper;

    public AccountRepository(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void createDefaultAccount(long userId) {
        AccountView account = new AccountView(userId, INITIAL_CASH, INITIAL_CASH);
        mapper.insertAccount(account);
    }

    public List<AccountPositionView> findPositionsByUserId(long userId) {
        return mapper.findPositionsByUserId(userId);
    }

    public AccountSummaryView findSummaryByUserId(long userId) {
        return mapper.findSummaryByUserId(userId);
    }

    public List<OrderView> findOrdersByUserId(long userId) {
        return mapper.findOrdersByUserId(userId, 100);
    }

    public List<TradeView> findTradesByUserId(long userId) {
        return mapper.findTradesByUserId(userId, 100);
    }

    public List<StrategyRunView> findStrategyRunsByUserId(long userId) {
        return mapper.findStrategyRunsByUserId(userId, 100);
    }

    public List<AccountPositionView> findDailyPlansByUserId(long userId) {
        return mapper.findDailyPlansByUserId(userId, 100);
    }

    public List<OrderView> findOrdersByUserId(long userId, int limit) {
        return mapper.findOrdersByUserId(userId, limit);
    }

    public List<TradeView> findTradesByUserId(long userId, int limit) {
        return mapper.findTradesByUserId(userId, limit);
    }

    public List<StrategyRunView> findStrategyRunsByUserId(long userId, int limit) {
        return mapper.findStrategyRunsByUserId(userId, limit);
    }

    public List<AccountPositionView> findDailyPlansByUserId(long userId, int limit) {
        return mapper.findDailyPlansByUserId(userId, limit);
    }

    @Transactional
    public void updateAccountCashBalance(long userId, BigDecimal delta) {
        mapper.updateAccountCashBalance(userId, delta);
    }

    @Transactional
    public void upsertPosition(long userId, long symbolId, BigDecimal quantity, BigDecimal availableQuantity, BigDecimal avgCost) {
        PositionView position = new PositionView(userId, symbolId, quantity, availableQuantity, avgCost, LocalDateTime.now(), LocalDateTime.now());
        mapper.upsertPosition(position);
    }

    @Transactional
    public long createOrder(long userId, long strategyRunId, long symbolId, String orderType, String side,
                            BigDecimal quantity, BigDecimal price, String status) {
        LocalDateTime now = LocalDateTime.now();
        OrderView order = new OrderView();
        order.setUserId(userId);
        order.setStrategyRunId(strategyRunId <= 0 ? null : strategyRunId);
        order.setSymbolId(symbolId);
        order.setSide(side);
        order.setOrderType(orderType);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setFilledQuantity("FILLED".equalsIgnoreCase(status) ? quantity : BigDecimal.ZERO);
        order.setStatus(status);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        mapper.insertOrder(order);
        return order.getId();
    }

    @Transactional
    public long createTrade(long userId, long strategyRunId, long symbolId, String side,
                            BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        TradeView trade = new TradeView(userId, null, strategyRunId, symbolId, side, quantity, price, amount, LocalDateTime.now());
        mapper.insertTrade(trade);
        return trade.id();
    }
}
