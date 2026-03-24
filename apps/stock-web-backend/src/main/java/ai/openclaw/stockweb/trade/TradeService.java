package ai.openclaw.stockweb.trade;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    private static final int DEFAULT_LIMIT = 100;

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public List<TradeView> listTrades(long userId) {
        return tradeRepository.findTradesByUserId(userId, DEFAULT_LIMIT);
    }

    public TradeView getTrade(long userId, long tradeId) {
        return tradeRepository.findTradeByIdAndUserId(tradeId, userId)
                .orElseThrow(() -> new TradeNotFoundException("Trade not found"));
    }
}
