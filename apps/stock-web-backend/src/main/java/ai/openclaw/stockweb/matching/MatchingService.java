package ai.openclaw.stockweb.matching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MatchingService {
    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final MatchingRepository repository;
    private final MatchingOrderExecutor orderExecutor;

    public MatchingService(MatchingRepository repository, MatchingOrderExecutor orderExecutor) {
        this.repository = repository;
        this.orderExecutor = orderExecutor;
    }

    public MatchingRunView run(long userId) {
        List<MatchableOrderView> openOrders = repository.findOpenOrdersByUserId(userId);
        int filledCount = 0;
        int skippedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        for (MatchableOrderView order : openOrders) {
            try {
                MatchingOrderResult result = orderExecutor.process(userId, order.getId());
                if (result.filled()) {
                    filledCount++;
                    totalAmount = totalAmount.add(result.amount()).setScale(4, RoundingMode.HALF_UP);
                } else {
                    skippedCount++;
                }
            } catch (Exception ex) {
                skippedCount++;
                log.warn("Matching skipped order {} for user {} due to processing error", order.getId(), userId, ex);
            }
        }

        MatchingRunView run = new MatchingRunView();
        run.setUserId(userId);
        run.setScanned(openOrders.size());
        run.setFilled(filledCount);
        run.setSkipped(skippedCount);
        run.setTotalAmount(totalAmount);
        run.setCreatedAt(LocalDateTime.now());
        return saveRun(run);
    }

    public Optional<MatchingRunView> last(long userId) {
        return repository.findLatestMatchingRunByUserId(userId);
    }

    @Transactional
    protected MatchingRunView saveRun(MatchingRunView run) {
        return repository.insertMatchingRun(run);
    }
}
