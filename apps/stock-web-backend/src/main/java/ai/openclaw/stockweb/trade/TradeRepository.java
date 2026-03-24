package ai.openclaw.stockweb.trade;

import ai.openclaw.stockweb.mapper.TradeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TradeRepository {
    private final TradeMapper mapper;

    public TradeRepository(TradeMapper mapper) {
        this.mapper = mapper;
    }

    public List<TradeView> findTradesByUserId(long userId, int limit) {
        return mapper.findTradesByUserId(userId, limit);
    }

    public Optional<TradeView> findTradeByIdAndUserId(long id, long userId) {
        return mapper.findTradeByIdAndUserId(id, userId);
    }
}
