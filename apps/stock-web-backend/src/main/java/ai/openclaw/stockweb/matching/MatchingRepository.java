package ai.openclaw.stockweb.matching;

import ai.openclaw.stockweb.account.AccountView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.mapper.MatchingMapper;
import ai.openclaw.stockweb.trade.TradeView;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MatchingRepository {
    private final MatchingMapper mapper;

    public MatchingRepository(MatchingMapper mapper) {
        this.mapper = mapper;
    }

    public List<MatchableOrderView> findOpenOrdersByUserId(long userId) {
        return mapper.findOpenOrdersByUserId(userId);
    }

    public Optional<MatchableOrderView> lockOrderByIdAndUserId(long orderId, long userId) {
        return mapper.lockOrderByIdAndUserId(orderId, userId);
    }

    public Optional<AccountView> lockAccountByUserId(long userId) {
        return mapper.lockAccountByUserId(userId);
    }

    public Optional<PositionView> lockPositionByUserIdAndSymbolId(long userId, long symbolId) {
        return mapper.lockPositionByUserIdAndSymbolId(userId, symbolId);
    }

    public Optional<MatchPriceView> findLatestMinutePrice(long symbolId, LocalDate tradeDate, LocalDateTime asOf) {
        return mapper.findLatestMinutePrice(symbolId, tradeDate, asOf);
    }

    public Optional<MatchPriceView> findLatestDailyPrice(long symbolId) {
        return mapper.findLatestDailyPrice(symbolId);
    }

    public void updateOrderAsFilled(long orderId, BigDecimal filledQuantity, BigDecimal avgFillPrice, LocalDateTime updatedAt) {
        mapper.updateOrderAsFilled(orderId, filledQuantity, avgFillPrice, updatedAt);
    }

    public void insertTrade(TradeView trade) {
        mapper.insertTrade(trade);
    }

    public void updateAccountCashBalance(long userId, BigDecimal delta) {
        mapper.updateAccountCashBalance(userId, delta);
    }

    public void insertPosition(PositionView position) {
        mapper.insertPosition(position);
    }

    public void updatePosition(PositionView position) {
        mapper.updatePosition(position);
    }

    public void deletePositionById(long id) {
        mapper.deletePositionById(id);
    }

    public MatchingRunView insertMatchingRun(MatchingRunView run) {
        mapper.insertMatchingRun(run);
        return run;
    }

    public Optional<MatchingRunView> findLatestMatchingRunByUserId(long userId) {
        return mapper.findLatestMatchingRunByUserId(userId);
    }
}
