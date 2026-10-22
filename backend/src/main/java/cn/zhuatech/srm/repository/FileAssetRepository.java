package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
    Optional<FileAsset> findByFileKeyAndDeletedFalse(String fileKey);
}
