package ai.openclaw.stockweb.matching;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MatchingRunView {
    private Long id;
    private Long userId;
    private Integer scanned;
    private Integer filled;
    private Integer skipped;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<MatchingOrderResult> results = List.of();

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

    public Integer getScanned() {
        return scanned;
    }

    public void setScanned(Integer scanned) {
        this.scanned = scanned;
    }

    public Integer getFilled() {
        return filled;
    }

    public void setFilled(Integer filled) {
        this.filled = filled;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(Integer skipped) {
        this.skipped = skipped;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MatchingOrderResult> getResults() {
        return results;
    }

    public void setResults(List<MatchingOrderResult> results) {
        this.results = results == null ? List.of() : results;
    }
}
