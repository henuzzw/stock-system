package ai.openclaw.stockweb.matching;

import ai.openclaw.stockweb.account.AccountView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.trade.TradeView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchingOrderExecutorTest {

    @Test
    void buyOrderFillsAndCreatesTradeAndPosition() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView order = order(42L, 7L, 10L, "BUY", "LIMIT", "150.0000", "10.0000", "0.0000", "NEW");
        MatchPriceView minutePrice = matchPrice("100.0000", LocalDateTime.of(2026, 3, 24, 14, 1));
        AccountView account = new AccountView();
        account.setUserId(7L);
        account.setCashBalance(new BigDecimal("5000.00"));

        when(repository.lockOrderByIdAndUserId(42L, 7L)).thenReturn(Optional.of(order));
        when(repository.findLatestMinutePrice(eq(10L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(minutePrice));
        when(repository.lockAccountByUserId(7L)).thenReturn(Optional.of(account));
        when(repository.lockPositionByUserIdAndSymbolId(7L, 10L)).thenReturn(Optional.empty());

        MatchingOrderResult result = executor.process(7L, order);

        assertTrue(result.filled());
        assertEquals(0, new BigDecimal("1000.0000").compareTo(result.amount()));
        assertEquals("FILLED", result.orderStatus());
        assertEquals("TEST", result.priceSource());
        assertEquals(0, new BigDecimal("100.0000").compareTo(result.marketPrice()));
        verify(repository).updateOrderAsFilled(eq(42L), eq(new BigDecimal("10.0000")), eq(new BigDecimal("100.0000")), any(LocalDateTime.class));
        verify(repository).updateAccountCashBalance(7L, new BigDecimal("-1000.00"));

        ArgumentCaptor<TradeView> tradeCaptor = ArgumentCaptor.forClass(TradeView.class);
        verify(repository).insertTrade(tradeCaptor.capture());
        TradeView trade = tradeCaptor.getValue();
        assertEquals(Long.valueOf(42L), trade.getOrderId());
        assertEquals("BUY", trade.getSide());
        assertEquals(0, new BigDecimal("10.0000").compareTo(trade.getQuantity()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(trade.getPrice()));
        assertEquals(0, new BigDecimal("1000.0000").compareTo(trade.getAmount()));

        ArgumentCaptor<PositionView> positionCaptor = ArgumentCaptor.forClass(PositionView.class);
        verify(repository).insertPosition(positionCaptor.capture());
        PositionView position = positionCaptor.getValue();
        assertEquals(0, new BigDecimal("10.0000").compareTo(position.getQuantity()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(position.getAvailableQuantity()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(position.getAvgCost()));
    }

    @Test
    void buyOrderSkipsWhenCashBalanceIsInsufficient() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView order = order(52L, 9L, 11L, "BUY", "MARKET", "0", "10.0000", "0.0000", "NEW");
        MatchPriceView minutePrice = matchPrice("100.0000", LocalDateTime.of(2026, 3, 24, 14, 2));
        AccountView account = new AccountView();
        account.setUserId(9L);
        account.setCashBalance(new BigDecimal("999.99"));

        when(repository.lockOrderByIdAndUserId(52L, 9L)).thenReturn(Optional.of(order));
        when(repository.findLatestMinutePrice(eq(11L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(minutePrice));
        when(repository.lockAccountByUserId(9L)).thenReturn(Optional.of(account));

        MatchingOrderResult result = executor.process(9L, order);

        assertFalse(result.filled());
        assertEquals(MatchingSkipReason.INSUFFICIENT_CASH, result.skipReason());
        assertEquals(0, new BigDecimal("100.0000").compareTo(result.marketPrice()));
        verify(repository, never()).updateOrderAsFilled(any(Long.class), any(BigDecimal.class), any(BigDecimal.class), any(LocalDateTime.class));
        verify(repository, never()).insertTrade(any(TradeView.class));
        verify(repository, never()).updateAccountCashBalance(any(Long.class), any(BigDecimal.class));
    }

    @Test
    void partialSellOrderFillsRemainingQuantityOnly() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView order = order(77L, 5L, 3L, "SELL", "LIMIT", "14.0000", "10.0000", "4.0000", "PARTIAL");
        MatchPriceView dailyPrice = matchPrice("15.0000", LocalDateTime.of(2026, 3, 24, 0, 0), "DAILY");
        PositionView position = new PositionView();
        position.setId(90L);
        position.setUserId(5L);
        position.setSymbolId(3L);
        position.setQuantity(new BigDecimal("10.0000"));
        position.setAvailableQuantity(new BigDecimal("6.0000"));
        position.setAvgCost(new BigDecimal("11.0000"));

        when(repository.lockOrderByIdAndUserId(77L, 5L)).thenReturn(Optional.of(order));
        when(repository.findLatestMinutePrice(eq(3L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(repository.findLatestDailyPrice(3L)).thenReturn(Optional.of(dailyPrice));
        when(repository.lockPositionByUserIdAndSymbolId(5L, 3L)).thenReturn(Optional.of(position));

        MatchingOrderResult result = executor.process(5L, order);

        assertTrue(result.filled());
        assertEquals(0, new BigDecimal("90.0000").compareTo(result.amount()));
        assertEquals("DAILY", result.priceSource());
        assertEquals(0, new BigDecimal("6.0000").compareTo(result.remainingQuantity()));
        verify(repository).updateOrderAsFilled(eq(77L), eq(new BigDecimal("10.0000")), eq(new BigDecimal("15.0000")), any(LocalDateTime.class));
        verify(repository).updateAccountCashBalance(5L, new BigDecimal("90.00"));

        ArgumentCaptor<TradeView> tradeCaptor = ArgumentCaptor.forClass(TradeView.class);
        verify(repository).insertTrade(tradeCaptor.capture());
        assertEquals(0, new BigDecimal("6.0000").compareTo(tradeCaptor.getValue().getQuantity()));

        ArgumentCaptor<PositionView> positionCaptor = ArgumentCaptor.forClass(PositionView.class);
        verify(repository).updatePosition(positionCaptor.capture());
        PositionView updated = positionCaptor.getValue();
        assertEquals(0, new BigDecimal("4.0000").compareTo(updated.getQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getAvailableQuantity()));
        assertEquals(0, new BigDecimal("11.0000").compareTo(updated.getAvgCost()));
    }

    @Test
    void limitOrderSkipsWhenMarketDoesNotReachLimit() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView order = order(88L, 4L, 6L, "BUY", "LIMIT", "10.0000", "5.0000", "0.0000", "NEW");
        MatchPriceView minutePrice = matchPrice("10.5000", LocalDateTime.of(2026, 3, 24, 14, 3));

        when(repository.lockOrderByIdAndUserId(88L, 4L)).thenReturn(Optional.of(order));
        when(repository.findLatestMinutePrice(eq(6L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(minutePrice));

        MatchingOrderResult result = executor.process(4L, order);

        assertFalse(result.filled());
        assertEquals(MatchingSkipReason.LIMIT_NOT_REACHED, result.skipReason());
        assertEquals(0, new BigDecimal("10.5000").compareTo(result.marketPrice()));
        verify(repository, never()).updateOrderAsFilled(any(Long.class), any(BigDecimal.class), any(BigDecimal.class), any(LocalDateTime.class));
    }

    @Test
    void orderSkipsWhenNoPriceExists() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView order = order(91L, 2L, 8L, "SELL", "MARKET", "0", "2.0000", "0.0000", "NEW");

        when(repository.lockOrderByIdAndUserId(91L, 2L)).thenReturn(Optional.of(order));
        when(repository.findLatestMinutePrice(eq(8L), any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(repository.findLatestDailyPrice(8L)).thenReturn(Optional.empty());

        MatchingOrderResult result = executor.process(2L, order);

        assertFalse(result.filled());
        assertEquals(MatchingSkipReason.NO_PRICE, result.skipReason());
        assertEquals(null, result.marketPrice());
    }

    @Test
    void orderSkipsWhenNoLongerOpen() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = new MatchingOrderExecutor(repository);

        MatchableOrderView snapshot = order(99L, 3L, 7L, "BUY", "MARKET", "0", "1.0000", "0.0000", "NEW");
        MatchableOrderView locked = order(99L, 3L, 7L, "BUY", "MARKET", "0", "1.0000", "1.0000", "FILLED");

        when(repository.lockOrderByIdAndUserId(99L, 3L)).thenReturn(Optional.of(locked));

        MatchingOrderResult result = executor.process(3L, snapshot);

        assertFalse(result.filled());
        assertEquals(MatchingSkipReason.ORDER_NOT_OPEN, result.skipReason());
        assertEquals("FILLED", result.orderStatus());
    }

    private MatchableOrderView order(
            long id,
            long userId,
            long symbolId,
            String side,
            String orderType,
            String price,
            String quantity,
            String filledQuantity,
            String status
    ) {
        MatchableOrderView order = new MatchableOrderView();
        order.setId(id);
        order.setUserId(userId);
        order.setSymbolId(symbolId);
        order.setCode("000001");
        order.setName("Ping An");
        order.setSide(side);
        order.setOrderType(orderType);
        order.setPrice(new BigDecimal(price));
        order.setQuantity(new BigDecimal(quantity));
        order.setFilledQuantity(new BigDecimal(filledQuantity));
        order.setStatus(status);
        return order;
    }

    private MatchPriceView matchPrice(String price, LocalDateTime matchedAt) {
        return matchPrice(price, matchedAt, "TEST");
    }

    private MatchPriceView matchPrice(String price, LocalDateTime matchedAt, String source) {
        MatchPriceView view = new MatchPriceView();
        view.setPrice(new BigDecimal(price));
        view.setMatchedAt(matchedAt);
        view.setSource(source);
        return view;
    }
}
