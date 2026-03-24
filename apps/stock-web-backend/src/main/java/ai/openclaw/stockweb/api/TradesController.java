package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.trade.TradeNotFoundException;
import ai.openclaw.stockweb.trade.TradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin
public class TradesController {
    private final AuthService authService;
    private final TradeService tradeService;

    public TradesController(AuthService authService, TradeService tradeService) {
        this.authService = authService;
        this.tradeService = tradeService;
    }

    @GetMapping
    public ResponseEntity<?> listTrades(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(tradeService.listTrades(user.id()));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrade(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable long id
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(tradeService.getTrade(user.id(), id));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        } catch (TradeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(false, ex.getMessage()));
        }
    }
}
