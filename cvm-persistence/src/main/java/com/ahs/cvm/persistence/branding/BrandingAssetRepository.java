package com.ahs.cvm.persistence.branding;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandingAssetRepository extends JpaRepository<BrandingAsset, UUID> {}
