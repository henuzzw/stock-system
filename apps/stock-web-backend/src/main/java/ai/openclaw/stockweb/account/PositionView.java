package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionView {
    private Long id;
    private Long userId;
    private Long symbolId;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal avgCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PositionView() {}

    public PositionView(Long userId, Long symbolId, BigDecimal quantity, BigDecimal availableQuantity, BigDecimal avgCost, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.symbolId = symbolId;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
        this.avgCost = avgCost;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSymbolId() { return symbolId; }
    public void setSymbolId(Long symbolId) { this.symbolId = symbolId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
