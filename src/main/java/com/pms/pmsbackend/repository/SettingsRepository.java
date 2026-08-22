package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Integer> {
}
