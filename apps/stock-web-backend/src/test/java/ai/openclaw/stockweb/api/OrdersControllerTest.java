package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.account.OrderView;
import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.order.OrderService;
import ai.openclaw.stockweb.order.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdersControllerTest {

    @Test
    void placeOrderReturnsCreatedForAuthenticatedUser() {
        AuthService authService = mock(AuthService.class);
        OrderService orderService = mock(OrderService.class);
        OrdersController controller = new OrdersController(authService, orderService);

        when(authService.getCurrentUser("Bearer demo-token"))
                .thenReturn(new UserBasicInfo(3L, "demo", LocalDateTime.now()));

        OrderView order = new OrderView();
        order.setId(88L);
        order.setUserId(3L);
        order.setSymbolId(11L);
        order.setCode("600036");
        order.setName("China Merchants Bank");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setPrice(new BigDecimal("32.1000"));
        order.setQuantity(new BigDecimal("100.0000"));
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setStatus("NEW");
        when(orderService.placeOrder(eq(3L), any(PlaceOrderRequest.class))).thenReturn(order);

        ResponseEntity<?> response = controller.placeOrder(
                "Bearer demo-token",
                new PlaceOrderRequest(null, "600036", "BUY", "LIMIT", new BigDecimal("32.1000"), new BigDecimal("100.0000"))
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(order, response.getBody());
    }

    @Test
    void listOrdersReturnsUnauthorizedWithoutValidToken() {
        AuthService authService = mock(AuthService.class);
        OrderService orderService = mock(OrderService.class);
        OrdersController controller = new OrdersController(authService, orderService);

        when(authService.getCurrentUser(null)).thenThrow(new AuthException("Missing Authorization header"));

        ResponseEntity<?> response = controller.listOrders(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Missing Authorization header", ((ErrorResponse) response.getBody()).message());
    }

    @Test
    void listOrdersReturnsCurrentUsersOrders() {
        AuthService authService = mock(AuthService.class);
        OrderService orderService = mock(OrderService.class);
        OrdersController controller = new OrdersController(authService, orderService);

        when(authService.getCurrentUser("Bearer demo-token"))
                .thenReturn(new UserBasicInfo(5L, "demo", LocalDateTime.now()));

        OrderView order = new OrderView();
        order.setId(7L);
        order.setUserId(5L);
        order.setCode("000858");
        order.setName("Wuliangye");
        when(orderService.listOrders(5L)).thenReturn(List.of(order));

        ResponseEntity<?> response = controller.listOrders("Bearer demo-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(order), response.getBody());
    }
}
