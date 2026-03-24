package ai.openclaw.stockweb.trade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    @Test
    void listTradesReturnsRepositoryResults() {
        TradeRepository repository = mock(TradeRepository.class);
        TradeService service = new TradeService(repository);

        TradeView trade = new TradeView();
        trade.setId(42L);
        trade.setUserId(7L);
        trade.setCode("600519");
        trade.setName("Kweichow Moutai");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("10.0000"));
        trade.setPrice(new BigDecimal("1234.5600"));
        trade.setAmount(new BigDecimal("12345.6000"));
        when(repository.findTradesByUserId(7L, 100)).thenReturn(List.of(trade));

        List<TradeView> result = service.listTrades(7L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(42L), result.get(0).getId());
    }

    @Test
    void getTradeRejectsForeignTradeAccess() {
        TradeRepository repository = mock(TradeRepository.class);
        TradeService service = new TradeService(repository);

        when(repository.findTradeByIdAndUserId(88L, 7L)).thenReturn(Optional.empty());

        TradeNotFoundException error = assertThrows(
                TradeNotFoundException.class,
                () -> service.getTrade(7L, 88L)
        );

        assertEquals("Trade not found", error.getMessage());
    }
}
