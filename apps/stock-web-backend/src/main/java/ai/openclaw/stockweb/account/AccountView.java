package ai.openclaw.stockweb.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountView {
    private Long id;
    private Long userId;
    private BigDecimal initialCash;
    private BigDecimal cashBalance;
    private LocalDateTime createdAt;

    public AccountView() {}

    public AccountView(Long userId, BigDecimal initialCash, BigDecimal cashBalance) {
        this.userId = userId;
        this.initialCash = initialCash;
        this.cashBalance = cashBalance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getInitialCash() { return initialCash; }
    public void setInitialCash(BigDecimal initialCash) { this.initialCash = initialCash; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
