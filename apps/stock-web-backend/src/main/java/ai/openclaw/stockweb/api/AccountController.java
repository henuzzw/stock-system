package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.account.AccountPositionView;
import ai.openclaw.stockweb.account.AccountRepository;
import ai.openclaw.stockweb.account.AccountSummaryView;
import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.strategy.ExecuteStrategyRequest;
import ai.openclaw.stockweb.strategy.StrategyExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final StrategyExecutionService strategyExecutionService;

    public AccountController(
            AuthService authService,
            AccountRepository accountRepository,
            StrategyExecutionService strategyExecutionService
    ) {
        this.authService = authService;
        this.accountRepository = accountRepository;
        this.strategyExecutionService = strategyExecutionService;
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

    @GetMapping("/orders")
    public ResponseEntity<?> orders(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(accountRepository.findOrdersByUserId(user.id(), 100));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/trades")
    public ResponseEntity<?> trades(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(accountRepository.findTradesByUserId(user.id(), 100));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/strategy-runs")
    public ResponseEntity<?> strategyRuns(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(accountRepository.findStrategyRunsByUserId(user.id(), 100));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/daily-plan")
    public ResponseEntity<?> dailyPlan(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(accountRepository.findDailyPlansByUserId(user.id(), 100));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @PostMapping("/strategy/execute")
    public ResponseEntity<?> executeStrategy(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) ExecuteStrategyRequest request
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(strategyExecutionService.execute(user.id(), request == null ? null : request.runDate()));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/strategy/latest")
    public ResponseEntity<?> latestStrategy(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(strategyExecutionService.latest(user.id()));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }
}
