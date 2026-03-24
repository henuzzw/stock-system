package ai.openclaw.stockweb.matching;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class MatchingServiceTest {

    @Test
    void runAggregatesResultsAndPersistsSummary() {
        MatchingRepository repository = mock(MatchingRepository.class);
        MatchingOrderExecutor executor = mock(MatchingOrderExecutor.class);
        MatchingService service = new MatchingService(repository, executor);

        MatchableOrderView first = new MatchableOrderView();
        first.setId(1L);
        MatchableOrderView second = new MatchableOrderView();
        second.setId(2L);

        when(repository.findOpenOrdersByUserId(8L)).thenReturn(List.of(first, second));
        when(executor.process(8L, 1L)).thenReturn(MatchingOrderResult.filled(new BigDecimal("123.4500")));
        when(executor.process(8L, 2L)).thenReturn(MatchingOrderResult.skipped());
        when(repository.insertMatchingRun(any(MatchingRunView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchingRunView run = service.run(8L);

        assertEquals(2, run.getScanned());
        assertEquals(1, run.getFilled());
        assertEquals(1, run.getSkipped());
        assertEquals(0, new BigDecimal("123.4500").compareTo(run.getTotalAmount()));

        ArgumentCaptor<MatchingRunView> captor = ArgumentCaptor.forClass(MatchingRunView.class);
        verify(repository).insertMatchingRun(captor.capture());
        MatchingRunView saved = captor.getValue();
        assertEquals(Long.valueOf(8L), saved.getUserId());
        assertEquals(2, saved.getScanned());
        assertEquals(1, saved.getFilled());
        assertEquals(1, saved.getSkipped());
    }
}
