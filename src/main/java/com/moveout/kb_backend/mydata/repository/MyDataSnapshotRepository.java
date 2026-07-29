package com.moveout.kb_backend.mydata.repository;

import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;
import com.moveout.kb_backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataSnapshotRepository extends JpaRepository<MyDataSnapshot, UUID> {

    Optional<MyDataSnapshot> findByUser(User user);
}
