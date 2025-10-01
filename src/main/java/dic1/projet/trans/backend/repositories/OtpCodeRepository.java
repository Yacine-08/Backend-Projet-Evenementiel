package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {

    Optional<OtpCode> findByEmailAndVerifiedFalse(String email);
    Optional<OtpCode> findByPhoneNumberAndVerifiedFalse(String phoneNumber);
    
    void deleteByEmail(String email);

    void deleteByPhoneNumber(String phoneNumber);
}