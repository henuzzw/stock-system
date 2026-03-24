package ai.openclaw.stockweb.api;

import ai.openclaw.stockweb.auth.AuthException;
import ai.openclaw.stockweb.auth.AuthService;
import ai.openclaw.stockweb.auth.ErrorResponse;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import ai.openclaw.stockweb.matching.MatchingRunView;
import ai.openclaw.stockweb.matching.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingControllerTest {

    @Test
    void runReturnsCurrentUsersSummary() {
        AuthService authService = mock(AuthService.class);
        MatchingService matchingService = mock(MatchingService.class);
        MatchingController controller = new MatchingController(authService, matchingService);

        MatchingRunView run = new MatchingRunView();
        run.setId(12L);
        run.setUserId(5L);
        run.setScanned(3);
        run.setFilled(2);
        run.setSkipped(1);
        run.setTotalAmount(new BigDecimal("456.7800"));

        when(authService.getCurrentUser("Bearer demo-token"))
                .thenReturn(new UserBasicInfo(5L, "demo", LocalDateTime.now()));
        when(matchingService.run(5L)).thenReturn(run);

        ResponseEntity<?> response = controller.run("Bearer demo-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(run, response.getBody());
    }

    @Test
    void lastReturnsCurrentUsersLastRun() {
        AuthService authService = mock(AuthService.class);
        MatchingService matchingService = mock(MatchingService.class);
        MatchingController controller = new MatchingController(authService, matchingService);

        MatchingRunView run = new MatchingRunView();
        run.setId(14L);
        run.setUserId(6L);
        run.setScanned(1);
        run.setFilled(1);
        run.setSkipped(0);
        run.setTotalAmount(new BigDecimal("123.0000"));

        when(authService.getCurrentUser("Bearer token"))
                .thenReturn(new UserBasicInfo(6L, "demo", LocalDateTime.now()));
        when(matchingService.last(6L)).thenReturn(Optional.of(run));

        ResponseEntity<?> response = controller.last("Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(run, response.getBody());
    }

    @Test
    void runReturnsUnauthorizedWithoutValidToken() {
        AuthService authService = mock(AuthService.class);
        MatchingService matchingService = mock(MatchingService.class);
        MatchingController controller = new MatchingController(authService, matchingService);

        when(authService.getCurrentUser(null)).thenThrow(new AuthException("Missing Authorization header"));

        ResponseEntity<?> response = controller.run(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Missing Authorization header", ((ErrorResponse) response.getBody()).message());
    }
}
