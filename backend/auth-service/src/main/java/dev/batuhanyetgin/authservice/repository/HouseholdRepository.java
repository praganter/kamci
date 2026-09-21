package dev.batuhanyetgin.authservice.repository;

import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.commonmodule.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HouseholdRepository extends BaseRepository<HouseholdDocument, String> {

    List<HouseholdDocument> findByOwnerId(String ownerId);

    List<HouseholdDocument> findByPartnerId(String partnerId);
}
