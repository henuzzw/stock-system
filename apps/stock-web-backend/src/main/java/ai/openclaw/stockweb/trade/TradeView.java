package ai.openclaw.stockweb.trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeView {
    private Long id;
    private Long userId;
    private Long orderId;
    private Long strategyRunId;
    private Long symbolId;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private String code;
    private String name;

    public TradeView() {}

    public TradeView(
            Long userId,
            Long orderId,
            Long strategyRunId,
            Long symbolId,
            String side,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.strategyRunId = strategyRunId;
        this.symbolId = symbolId;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long id() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public Long userId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrderId() { return orderId; }
    public Long orderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getStrategyRunId() { return strategyRunId; }
    public Long strategyRunId() { return strategyRunId; }
    public void setStrategyRunId(Long strategyRunId) { this.strategyRunId = strategyRunId; }
    public Long getSymbolId() { return symbolId; }
    public Long symbolId() { return symbolId; }
    public void setSymbolId(Long symbolId) { this.symbolId = symbolId; }
    public String getSide() { return side; }
    public String side() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal quantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal price() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal amount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCode() { return code; }
    public String code() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
}
