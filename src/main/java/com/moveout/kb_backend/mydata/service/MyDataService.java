package com.moveout.kb_backend.mydata.service;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.mydata.dto.MyDataSnapshotResponse;
import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;
import com.moveout.kb_backend.mydata.mock.MyDataMock;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MyDataService {

    private static final String MOCK_FILE_PATH = "mock/mydata-mock.json";

    private final UserRepository userRepository;
    private final MyDataSnapshotRepository myDataSnapshotRepository;
    private final ObjectMapper objectMapper;

    private MyDataMock mock;

    @PostConstruct
    private void init() {
        mock = loadMock();
    }

    @Transactional
    public MyDataSnapshotResponse sync(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("MYDATA_001", "존재하지 않는 사용자입니다."));

        MyDataSnapshot snapshot = myDataSnapshotRepository
                .findByUser(user)
                .orElseGet(() -> myDataSnapshotRepository.save(new MyDataSnapshot(user)));

        MyDataMock.MonthlyConsumption consumption = mock.monthlyConsumption();
        MyDataMock.FixedCost fixedCost = consumption.fixedCost();
        snapshot.updateConsumption(
                consumption.food(),
                consumption.culture(),
                consumption.shopping(),
                consumption.etc(),
                fixedCost.transport(),
                fixedCost.telecom(),
                fixedCost.insurance(),
                fixedCost.subscription(),
                fixedCost.loanInterest(),
                fixedCost.housing());

        MyDataMock.Asset asset = mock.asset();
        snapshot.updateAsset(
                asset.deposit(), asset.saving(), asset.investment(), asset.loan(),
                asset.remainingRepayment(), asset.monthlyRepayment());

        return MyDataSnapshotResponse.from(snapshot);
    }

    @Transactional(readOnly = true)
    public MyDataSnapshotResponse getSnapshot(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("MYDATA_001", "존재하지 않는 사용자입니다."));

        MyDataSnapshot snapshot = myDataSnapshotRepository
                .findByUser(user)
                .orElseThrow(() -> new BusinessException("MYDATA_002", "마이데이터 연동 내역이 없습니다."));

        return MyDataSnapshotResponse.from(snapshot);
    }

    private MyDataMock loadMock() {
        try {
            return objectMapper.readValue(new ClassPathResource(MOCK_FILE_PATH).getInputStream(), MyDataMock.class);
        } catch (IOException e) {
            throw new IllegalStateException("마이데이터 mock 파일을 읽을 수 없습니다: " + MOCK_FILE_PATH, e);
        }
    }
}
