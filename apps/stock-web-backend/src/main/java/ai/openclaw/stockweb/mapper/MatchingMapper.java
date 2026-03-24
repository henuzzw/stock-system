package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.account.AccountView;
import ai.openclaw.stockweb.account.PositionView;
import ai.openclaw.stockweb.matching.MatchPriceView;
import ai.openclaw.stockweb.matching.MatchableOrderView;
import ai.openclaw.stockweb.matching.MatchingRunView;
import ai.openclaw.stockweb.trade.TradeView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface MatchingMapper {

    List<MatchableOrderView> findOpenOrdersByUserId(@Param("userId") long userId);

    Optional<MatchableOrderView> lockOrderByIdAndUserId(@Param("orderId") long orderId, @Param("userId") long userId);

    Optional<AccountView> lockAccountByUserId(@Param("userId") long userId);

    Optional<PositionView> lockPositionByUserIdAndSymbolId(@Param("userId") long userId, @Param("symbolId") long symbolId);

    Optional<MatchPriceView> findLatestMinutePrice(
            @Param("symbolId") long symbolId,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("asOf") LocalDateTime asOf
    );

    Optional<MatchPriceView> findLatestDailyPrice(@Param("symbolId") long symbolId);

    int updateOrderAsFilled(
            @Param("orderId") long orderId,
            @Param("filledQuantity") BigDecimal filledQuantity,
            @Param("avgFillPrice") BigDecimal avgFillPrice,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    int insertTrade(TradeView trade);

    int updateAccountCashBalance(@Param("userId") long userId, @Param("delta") BigDecimal delta);

    int insertPosition(PositionView position);

    int updatePosition(PositionView position);

    int deletePositionById(@Param("id") long id);

    int insertMatchingRun(MatchingRunView run);

    Optional<MatchingRunView> findLatestMatchingRunByUserId(@Param("userId") long userId);
}
