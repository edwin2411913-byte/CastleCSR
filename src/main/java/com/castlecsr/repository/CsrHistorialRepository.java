package com.castlecsr.repository;

import com.castlecsr.model.CsrHistorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CsrHistorialRepository extends JpaRepository<CsrHistorial, Long> {

    Page<CsrHistorial> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId, Pageable pageable);

    List<CsrHistorial> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    List<CsrHistorial> findByCommonNameContainingAndUsuarioId(String commonName, Long usuarioId);

    Page<CsrHistorial> findByUsuarioIdAndCommonNameContainingIgnoreCaseOrderByCreadoEnDesc(
            Long usuarioId, String commonName, Pageable pageable);

    Optional<CsrHistorial> findByIdAndUsuarioId(Long id, Long usuarioId);
}
