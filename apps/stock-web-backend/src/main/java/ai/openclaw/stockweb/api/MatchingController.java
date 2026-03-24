package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.matching.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching")
@CrossOrigin
public class MatchingController {
    private final AuthService authService;
    private final MatchingService matchingService;

    public MatchingController(AuthService authService, MatchingService matchingService) {
        this.authService = authService;
        this.matchingService = matchingService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(matchingService.run(user.id()));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }

    @GetMapping("/last")
    public ResponseEntity<?> last(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            UserBasicInfo user = authService.getCurrentUser(authorizationHeader);
            return ResponseEntity.ok(matchingService.last(user.id()).orElse(null));
        } catch (AuthException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(false, ex.getMessage()));
        }
    }
}
