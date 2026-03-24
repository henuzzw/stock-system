package ai.openclaw.stockweb.order;

import ai.openclaw.stockweb.account.OrderView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Test
    void placeBuyOrderCreatesNewOrder() {
        OrderRepository repository = mock(OrderRepository.class);
        OrderService service = new OrderService(repository);

        when(repository.findSymbolByCode("600519"))
                .thenReturn(Optional.of(new OrderSymbolView(10L, "600519", "Kweichow Moutai")));
        when(repository.insertOrder(any(OrderView.class))).thenReturn(42L);

        OrderView saved = new OrderView();
        saved.setId(42L);
        saved.setUserId(7L);
        saved.setSymbolId(10L);
        saved.setCode("600519");
        saved.setName("Kweichow Moutai");
        saved.setSide("BUY");
        saved.setOrderType("LIMIT");
        saved.setPrice(new BigDecimal("1234.5600"));
        saved.setQuantity(new BigDecimal("10.0000"));
        saved.setFilledQuantity(BigDecimal.ZERO);
        saved.setStatus("NEW");
        when(repository.findOrderByIdAndUserId(42L, 7L)).thenReturn(Optional.of(saved));

        OrderView result = service.placeOrder(
                7L,
                new PlaceOrderRequest(null, "600519", "buy", "limit", new BigDecimal("1234.5600"), new BigDecimal("10.0000"))
        );

        assertEquals(Long.valueOf(42L), result.getId());
        assertEquals("BUY", result.getSide());
        assertEquals("LIMIT", result.getOrderType());
        assertEquals("NEW", result.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getFilledQuantity()));
    }

    @Test
    void placeSellOrderRejectsQuantityAboveAvailablePosition() {
        OrderRepository repository = mock(OrderRepository.class);
        OrderService service = new OrderService(repository);

        when(repository.findSymbolByCode("000001"))
                .thenReturn(Optional.of(new OrderSymbolView(1L, "000001", "Ping An Bank")));
        when(repository.findAvailableQuantity(9L, 1L))
                .thenReturn(Optional.of(new BigDecimal("5.0000")));

        OrderValidationException error = assertThrows(
                OrderValidationException.class,
                () -> service.placeOrder(
                        9L,
                        new PlaceOrderRequest(null, "000001", "SELL", "MARKET", new BigDecimal("12.3400"), new BigDecimal("6.0000"))
                )
        );

        assertEquals("SELL quantity exceeds available position quantity", error.getMessage());
    }
}
