package me.test.oauth.repository;

import me.test.oauth.entity.ZoomLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoomLicenseRepository extends JpaRepository<ZoomLicense, String> {
    static ZoomLicense findByType(Integer type) {
        return ZoomLicenseRepository.findByType(type);
    }
}