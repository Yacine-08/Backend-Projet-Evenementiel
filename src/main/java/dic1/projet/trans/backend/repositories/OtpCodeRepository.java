package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {

    Optional<OtpCode> findByEmailAndCodeAndVerifiedFalse(String email, String code);
    Optional<OtpCode> findByEmailAndVerifiedFalse(String email);
    Optional<OtpCode> findByPhoneNumberAndCodeAndVerifiedFalse(String phoneNumber, String code);
    Optional<OtpCode> findByPhoneNumberAndVerifiedFalse(String phoneNumber);
    
    void deleteByEmail(String email);
    void deleteByExpiryDateBefore(LocalDateTime date);

    void deleteByPhoneNumber(String phoneNumber);
}