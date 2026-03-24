package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.account.*;
import ai.openclaw.stockweb.trade.TradeView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountMapper {

    AccountSummaryView findSummaryByUserId(@Param("userId") long userId);

    List<AccountPositionView> findPositionsByUserId(@Param("userId") long userId);

    List<OrderView> findOrdersByUserId(@Param("userId") long userId, @Param("limit") int limit);

    List<TradeView> findTradesByUserId(@Param("userId") long userId, @Param("limit") int limit);

    List<StrategyRunView> findStrategyRunsByUserId(@Param("userId") long userId, @Param("limit") int limit);

    List<AccountPositionView> findDailyPlansByUserId(@Param("userId") long userId, @Param("limit") int limit);

    int insertAccount(AccountView account);

    AccountView findAccountByUserId(@Param("userId") long userId);

    int updateAccountCashBalance(@Param("userId") long userId, @Param("delta") BigDecimal delta);

    int upsertPosition(PositionView position);

    int insertOrder(OrderView order);

    int insertTrade(TradeView trade);
}
