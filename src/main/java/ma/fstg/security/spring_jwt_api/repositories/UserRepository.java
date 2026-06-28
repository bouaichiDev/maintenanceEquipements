package ma.fstg.security.spring_jwt_api.repositories;

import ma.fstg.security.spring_jwt_api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByOrganization_Id(Long organizationId);

    Optional<User> findByIdAndOrganization_Id(Long id, Long organizationId);

    long countByOrganization_Id(Long organizationId);
}
