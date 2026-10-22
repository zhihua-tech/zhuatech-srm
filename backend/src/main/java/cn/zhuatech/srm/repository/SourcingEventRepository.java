/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SourcingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SourcingEventRepository extends JpaRepository<SourcingEvent, Long> {
    List<SourcingEvent> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    Optional<SourcingEvent> findByIdAndOrganizationCodeAndDeletedFalse(Long id, String organizationCode);
}
