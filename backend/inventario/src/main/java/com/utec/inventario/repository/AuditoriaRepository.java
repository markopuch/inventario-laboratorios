package com.utec.inventario.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.utec.inventario.entity.AuditoriaEntity;

@Repository
public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, Integer>,
        JpaSpecificationExecutor<AuditoriaEntity> {
}

