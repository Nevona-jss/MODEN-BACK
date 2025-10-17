package com.moden.modenapi.modules.auth.repository;
import com.moden.modenapi.modules.auth.model.AuthLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuthLocalRepository extends JpaRepository<AuthLocal, UUID> {

}
