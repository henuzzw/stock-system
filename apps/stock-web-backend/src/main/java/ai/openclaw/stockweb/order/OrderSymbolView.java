package ai.openclaw.stockweb.order;

public record OrderSymbolView(
        long id,
        String code,
        String name
) {
}
