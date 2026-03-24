package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.account.OrderView;
import ai.openclaw.stockweb.order.OrderSymbolView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderMapper {

    Optional<OrderSymbolView> findSymbolById(@Param("symbolId") long symbolId);

    Optional<OrderSymbolView> findSymbolByCode(@Param("code") String code);

    Optional<BigDecimal> findAvailableQuantity(@Param("userId") long userId, @Param("symbolId") long symbolId);

    List<OrderView> findOrdersByUserId(@Param("userId") long userId, @Param("limit") int limit);

    Optional<OrderView> findOrderByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    int insertOrder(OrderView order);
}
