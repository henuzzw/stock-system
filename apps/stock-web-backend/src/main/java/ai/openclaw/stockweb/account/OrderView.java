package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderView {
    private Long id;
    private Long userId;
    private Long strategyRunId;
    private Long symbolId;
    private String side;
    private String orderType;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String code;
    private String name;

    public OrderView() {
    }

    public OrderView(
            Long id,
            Long userId,
            Long strategyRunId,
            Long symbolId,
            String side,
            String orderType,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal filledQuantity,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String code,
            String name
    ) {
        this.id = id;
        this.userId = userId;
        this.strategyRunId = strategyRunId;
        this.symbolId = symbolId;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStrategyRunId() {
        return strategyRunId;
    }

    public void setStrategyRunId(Long strategyRunId) {
        this.strategyRunId = strategyRunId;
    }

    public Long getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(Long symbolId) {
        this.symbolId = symbolId;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(BigDecimal filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
