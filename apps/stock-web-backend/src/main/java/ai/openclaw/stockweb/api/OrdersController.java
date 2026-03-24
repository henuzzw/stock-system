package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.order.OrderNotFoundException;
import ai.openclaw.stockweb.order.OrderService;
import ai.openclaw.stockweb.order.OrderValidationException;
import ai.openclaw.stockweb.order.PlaceOrderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrdersController {
    private final AuthService authService;
    private final OrderService orderService;

    public OrdersController(AuthService authService, OrderService orderService) {
        this.authService = authService;
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) PlaceOrderRequest request
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(user.id(), request));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        } catch (OrderValidationException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(orderService.listOrders(user.id()));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable long id
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(orderService.getOrder(user.id(), id));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        } catch (OrderNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(false, ex.getMessage()));
        }
    }
}
