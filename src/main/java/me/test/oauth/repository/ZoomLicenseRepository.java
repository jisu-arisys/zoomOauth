package me.test.oauth.repository;

import me.test.oauth.entity.ZoomLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZoomLicenseRepository extends JpaRepository<ZoomLicense, String> {
    Optional<ZoomLicense> findByType(Integer type);

    Optional<ZoomLicense> findByName(String name);

    ZoomLicense findTypeByName(String name);
}