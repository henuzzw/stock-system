package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.trade.TradeView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TradeMapper {

    List<TradeView> findTradesByUserId(@Param("userId") long userId, @Param("limit") int limit);

    Optional<TradeView> findTradeByIdAndUserId(@Param("id") long id, @Param("userId") long userId);
}
