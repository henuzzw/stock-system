package ai.openclaw.stockweb.matching;

import ai.openclaw.stockweb.account.AccountView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.trade.TradeView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Component
public class MatchingOrderExecutor {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> OPEN_STATUSES = Set.of("NEW", "PARTIAL");

    private final MatchingRepository repository;

    public MatchingOrderExecutor(MatchingRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MatchingOrderResult process(long userId, long orderId) {
        MatchableOrderView order = repository.lockOrderByIdAndUserId(orderId, userId).orElse(null);
        if (order == null || !OPEN_STATUSES.contains(order.getStatus())) {
            return MatchingOrderResult.skipped();
        }

        BigDecimal requestedQuantity = defaultQuantity(order.getQuantity());
        BigDecimal alreadyFilled = defaultQuantity(order.getFilledQuantity());
        BigDecimal remainingQuantity = requestedQuantity.subtract(alreadyFilled);
        if (remainingQuantity.compareTo(ZERO) <= 0) {
            return MatchingOrderResult.skipped();
        }

        LocalDateTime now = LocalDateTime.now(MARKET_ZONE);
        MatchPriceView matchPrice = repository.findLatestMinutePrice(order.getSymbolId(), now.toLocalDate(), now)
                .or(() -> repository.findLatestDailyPrice(order.getSymbolId()))
                .orElse(null);
        if (matchPrice == null || matchPrice.getPrice() == null || matchPrice.getPrice().compareTo(ZERO) <= 0) {
            return MatchingOrderResult.skipped();
        }

        if (!isPriceMatchable(order, matchPrice.getPrice())) {
            return MatchingOrderResult.skipped();
        }

        BigDecimal tradeAmount = remainingQuantity.multiply(matchPrice.getPrice()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal cashDelta = tradeAmount.setScale(2, RoundingMode.HALF_UP);

        if ("BUY".equals(order.getSide())) {
            AccountView account = repository.lockAccountByUserId(userId).orElse(null);
            if (account == null || account.getCashBalance() == null || account.getCashBalance().compareTo(cashDelta) < 0) {
                return MatchingOrderResult.skipped();
            }
            applyBuy(order, remainingQuantity, matchPrice, tradeAmount, cashDelta, now);
            return MatchingOrderResult.filled(tradeAmount);
        }

        if ("SELL".equals(order.getSide())) {
            PositionView position = repository.lockPositionByUserIdAndSymbolId(userId, order.getSymbolId()).orElse(null);
            if (!hasEnoughPosition(position, remainingQuantity)) {
                return MatchingOrderResult.skipped();
            }
            applySell(order, position, remainingQuantity, matchPrice, tradeAmount, cashDelta, now);
            return MatchingOrderResult.filled(tradeAmount);
        }

        return MatchingOrderResult.skipped();
    }

    private void applyBuy(
            MatchableOrderView order,
            BigDecimal remainingQuantity,
            MatchPriceView matchPrice,
            BigDecimal tradeAmount,
            BigDecimal cashDelta,
            LocalDateTime now
    ) {
        PositionView position = repository.lockPositionByUserIdAndSymbolId(order.getUserId(), order.getSymbolId()).orElse(null);

        repository.updateOrderAsFilled(order.getId(), order.getQuantity(), matchPrice.getPrice(), now);
        repository.insertTrade(buildTrade(order, remainingQuantity, matchPrice, tradeAmount));
        repository.updateAccountCashBalance(order.getUserId(), cashDelta.negate());

        if (position == null) {
            PositionView newPosition = new PositionView();
            newPosition.setUserId(order.getUserId());
            newPosition.setSymbolId(order.getSymbolId());
            newPosition.setQuantity(remainingQuantity);
            newPosition.setAvailableQuantity(remainingQuantity);
            newPosition.setAvgCost(matchPrice.getPrice().setScale(4, RoundingMode.HALF_UP));
            newPosition.setCreatedAt(now);
            newPosition.setUpdatedAt(now);
            repository.insertPosition(newPosition);
            return;
        }

        BigDecimal currentQuantity = defaultQuantity(position.getQuantity());
        BigDecimal currentAvailable = defaultQuantity(position.getAvailableQuantity());
        BigDecimal currentAvgCost = position.getAvgCost() == null ? ZERO : position.getAvgCost();
        BigDecimal nextQuantity = currentQuantity.add(remainingQuantity);
        BigDecimal nextAvailable = currentAvailable.add(remainingQuantity);
        BigDecimal nextAvgCost = nextQuantity.compareTo(ZERO) <= 0
                ? ZERO.setScale(4, RoundingMode.HALF_UP)
                : currentQuantity.multiply(currentAvgCost)
                        .add(remainingQuantity.multiply(matchPrice.getPrice()))
                        .divide(nextQuantity, 4, RoundingMode.HALF_UP);

        position.setQuantity(nextQuantity);
        position.setAvailableQuantity(nextAvailable);
        position.setAvgCost(nextAvgCost);
        position.setUpdatedAt(now);
        repository.updatePosition(position);
    }

    private void applySell(
            MatchableOrderView order,
            PositionView position,
            BigDecimal remainingQuantity,
            MatchPriceView matchPrice,
            BigDecimal tradeAmount,
            BigDecimal cashDelta,
            LocalDateTime now
    ) {
        repository.updateOrderAsFilled(order.getId(), order.getQuantity(), matchPrice.getPrice(), now);
        repository.insertTrade(buildTrade(order, remainingQuantity, matchPrice, tradeAmount));
        repository.updateAccountCashBalance(order.getUserId(), cashDelta);

        BigDecimal nextQuantity = defaultQuantity(position.getQuantity()).subtract(remainingQuantity);
        BigDecimal nextAvailable = defaultQuantity(position.getAvailableQuantity()).subtract(remainingQuantity);
        if (nextQuantity.compareTo(ZERO) <= 0) {
            repository.deletePositionById(position.getId());
            return;
        }

        position.setQuantity(nextQuantity);
        position.setAvailableQuantity(nextAvailable.max(ZERO));
        position.setUpdatedAt(now);
        repository.updatePosition(position);
    }

    private TradeView buildTrade(
            MatchableOrderView order,
            BigDecimal quantity,
            MatchPriceView matchPrice,
            BigDecimal tradeAmount
    ) {
        TradeView trade = new TradeView();
        trade.setOrderId(order.getId());
        trade.setUserId(order.getUserId());
        trade.setStrategyRunId(null);
        trade.setSymbolId(order.getSymbolId());
        trade.setSide(order.getSide());
        trade.setQuantity(quantity);
        trade.setPrice(matchPrice.getPrice());
        trade.setAmount(tradeAmount);
        trade.setCreatedAt(matchPrice.getMatchedAt());
        return trade;
    }

    private boolean isPriceMatchable(MatchableOrderView order, BigDecimal marketPrice) {
        if (!"LIMIT".equalsIgnoreCase(order.getOrderType())) {
            return true;
        }
        BigDecimal limitPrice = order.getPrice();
        if (limitPrice == null || limitPrice.compareTo(ZERO) <= 0) {
            return true;
        }
        if ("BUY".equals(order.getSide())) {
            return marketPrice.compareTo(limitPrice) <= 0;
        }
        if ("SELL".equals(order.getSide())) {
            return marketPrice.compareTo(limitPrice) >= 0;
        }
        return false;
    }

    private boolean hasEnoughPosition(PositionView position, BigDecimal remainingQuantity) {
        if (position == null) {
            return false;
        }
        BigDecimal quantity = defaultQuantity(position.getQuantity());
        BigDecimal availableQuantity = defaultQuantity(position.getAvailableQuantity());
        return quantity.compareTo(remainingQuantity) >= 0 && availableQuantity.compareTo(remainingQuantity) >= 0;
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
