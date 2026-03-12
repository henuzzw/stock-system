package ai.openclaw.stockweb.api;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SymbolInitializationService {
    private static final String PIPELINE_BIN = "/home/openclaw/projects/stock-pipeline/.venv/bin/stock-pipeline";
    private static final String PIPELINE_DIR = "/home/openclaw/projects/stock-pipeline";

    public Map<String, Object> initialize(String code, String market) {
        List<Map<String, Object>> steps = new ArrayList<>();
        boolean ok = true;

        steps.add(runStep("ingest_market_data", List.of(
                PIPELINE_BIN, "ingest", "--codes", code, "--skip-news"
        )));

        if ("CN".equalsIgnoreCase(market)) {
            steps.add(runStep("minute_prices", List.of(
                    PIPELINE_BIN, "minute", "--codes", code, "--date", LocalDate.now().toString()
            )));
        }

        for (Map<String, Object> step : steps) {
            Object success = step.get("success");
            if (!(success instanceof Boolean) || !((Boolean) success)) {
                ok = false;
            }
        }

        return Map.of(
                "success", ok,
                "steps", steps
        );
    }

    private Map<String, Object> runStep(String name, List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new java.io.File(PIPELINE_DIR));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() < 8000) {
                        output.append(line).append('\n');
                    }
                }
            }

            int exitCode = process.waitFor();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("step", name);
            result.put("success", exitCode == 0);
            result.put("exitCode", exitCode);
            result.put("output", output.toString().trim());
            return result;
        } catch (Exception ex) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("step", name);
            result.put("success", false);
            result.put("exitCode", -1);
            result.put("output", ex.getMessage());
            return result;
        }
    }
}
