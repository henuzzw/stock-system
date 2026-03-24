package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.trade.TradeService;
import ai.openclaw.stockweb.trade.TradeView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradesControllerTest {

    @Test
    void listTradesReturnsCurrentUsersTrades() {
        AuthService authService = mock(AuthService.class);
        TradeService tradeService = mock(TradeService.class);
        TradesController controller = new TradesController(authService, tradeService);

        when(authService.getCurrentUser("Bearer demo-token"))
                .thenReturn(new UserBasicInfo(5L, "demo", LocalDateTime.now()));

        TradeView trade = new TradeView();
        trade.setId(7L);
        trade.setUserId(5L);
        trade.setCode("000858");
        trade.setName("Wuliangye");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("100.0000"));
        trade.setPrice(new BigDecimal("130.5000"));
        trade.setAmount(new BigDecimal("13050.0000"));
        when(tradeService.listTrades(5L)).thenReturn(List.of(trade));

        ResponseEntity<?> response = controller.listTrades("Bearer demo-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(trade), response.getBody());
    }

    @Test
    void getTradeReturnsUnauthorizedWithoutValidToken() {
        AuthService authService = mock(AuthService.class);
        TradeService tradeService = mock(TradeService.class);
        TradesController controller = new TradesController(authService, tradeService);

        when(authService.getCurrentUser(null)).thenThrow(new AuthException("Missing Authorization header"));

        ResponseEntity<?> response = controller.getTrade(null, 10L);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Missing Authorization header", ((ErrorResponse) response.getBody()).message());
    }
}
