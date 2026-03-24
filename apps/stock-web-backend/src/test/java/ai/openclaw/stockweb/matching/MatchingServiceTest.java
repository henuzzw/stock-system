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
        first.setCode("600000");
        MatchableOrderView second = new MatchableOrderView();
        second.setId(2L);
        second.setCode("600001");

        MatchPriceView price = new MatchPriceView();
        price.setPrice(new BigDecimal("12.3450"));
        price.setSource("DAILY");

        when(repository.findOpenOrdersByUserId(8L)).thenReturn(List.of(first, second));
        when(executor.process(8L, first)).thenReturn(MatchingOrderResult.filled(first, new BigDecimal("10.0000"), price, new BigDecimal("123.4500")));
        when(executor.process(8L, second)).thenReturn(MatchingOrderResult.skipped(second, new BigDecimal("5.0000"), price, MatchingSkipReason.LIMIT_NOT_REACHED));
        when(repository.insertMatchingRun(any(MatchingRunView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchingRunView run = service.run(8L);

        assertEquals(2, run.getScanned());
        assertEquals(1, run.getFilled());
        assertEquals(1, run.getSkipped());
        assertEquals(0, new BigDecimal("123.4500").compareTo(run.getTotalAmount()));
        assertEquals(2, run.getResults().size());
        assertEquals(MatchingSkipReason.LIMIT_NOT_REACHED, run.getResults().get(1).skipReason());

        ArgumentCaptor<MatchingRunView> captor = ArgumentCaptor.forClass(MatchingRunView.class);
        verify(repository).insertMatchingRun(captor.capture());
        MatchingRunView saved = captor.getValue();
        assertEquals(Long.valueOf(8L), saved.getUserId());
        assertEquals(2, saved.getScanned());
        assertEquals(1, saved.getFilled());
        assertEquals(1, saved.getSkipped());
        assertEquals(2, saved.getResults().size());
    }
}
