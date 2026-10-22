package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrganizationCodeOrderByOccurredAtDesc(String organizationCode, Pageable pageable);
    List<AuditLog> findByOrganizationCodeAndObjectTypeAndObjectIdOrderByOccurredAtDesc(
        String organizationCode, String objectType, String objectId);

    @Query("""
        select value from AuditLog value
        where value.organizationCode = :organizationCode
          and (:actor is null or lower(value.actor) like lower(concat('%', :actor, '%')))
          and (:module is null or value.module = :module)
          and (:fromTime is null or value.occurredAt >= :fromTime)
          and (:toTime is null or value.occurredAt <= :toTime)
        order by value.occurredAt desc
        """)
    Page<AuditLog> search(@Param("organizationCode") String organizationCode,
                          @Param("actor") String actor,
                          @Param("module") String module,
                          @Param("fromTime") LocalDateTime fromTime,
                          @Param("toTime") LocalDateTime toTime,
                          Pageable pageable);
}
