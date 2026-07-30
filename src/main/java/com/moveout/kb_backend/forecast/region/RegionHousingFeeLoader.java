package com.moveout.kb_backend.forecast.region;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class RegionHousingFeeLoader {

    private static final String CSV_PATH = "seoul_housingfee_pergu.csv";
    private static final long MANWON_TO_WON = 10_000L;

    private final Map<String, RegionHousingFee> feesByRegion = new HashMap<>();

    @PostConstruct
    void load() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // 헤더 스킵
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(",");
                String region = columns[0].trim();
                long depositWon = Long.parseLong(columns[1].trim()) * MANWON_TO_WON;
                long monthlyRentWon = Long.parseLong(columns[2].trim()) * MANWON_TO_WON;
                feesByRegion.put(region, new RegionHousingFee(depositWon, monthlyRentWon));
            }
        } catch (IOException e) {
            throw new IllegalStateException("지역 시세 CSV를 읽을 수 없습니다: " + CSV_PATH, e);
        }
    }

    public Optional<RegionHousingFee> find(String region) {
        return Optional.ofNullable(feesByRegion.get(region));
    }
}
