package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // find user by username, email, or phone number
    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByUsername(String username);

    // check if user exists by email or username
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByUsername(String username);
}