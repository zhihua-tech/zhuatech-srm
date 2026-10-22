package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.AsnLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AsnLineRepository extends JpaRepository<AsnLine, Long> {
    List<AsnLine> findByAsnIdAndDeletedFalse(Long asnId);
}
