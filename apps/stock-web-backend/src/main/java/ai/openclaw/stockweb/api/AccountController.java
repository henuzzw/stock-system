package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.account.AccountPositionView;
import ai.openclaw.stockweb.account.AccountRepository;
import ai.openclaw.stockweb.account.AccountSummaryView;
import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@CrossOrigin
public class AccountController {
    private final AuthService authService;
    private final AccountRepository accountRepository;

    public AccountController(AuthService authService, AccountRepository accountRepository) {
        this.authService = authService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/positions")
    public ResponseEntity<?> positions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            List<AccountPositionView> positions = accountRepository.findPositionsByUserId(user.id());
            return ResponseEntity.ok(positions);
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            AccountSummaryView summary = accountRepository.findSummaryByUserId(user.id());
            return ResponseEntity.ok(summary);
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }
}
