package com.ahs.cvm.persistence.alert;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertDispatchRepository extends JpaRepository<AlertDispatch, UUID> {
}
