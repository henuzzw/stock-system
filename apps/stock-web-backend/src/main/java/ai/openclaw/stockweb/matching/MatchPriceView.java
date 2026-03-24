package ai.openclaw.stockweb.matching;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MatchPriceView {
    private BigDecimal price;
    private LocalDateTime matchedAt;
    private String source;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
