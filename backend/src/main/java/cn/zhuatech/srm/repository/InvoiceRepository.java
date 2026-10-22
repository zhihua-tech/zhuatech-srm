package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.InvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InvoiceRepository extends JpaRepository<InvoiceRecord, Long> {
    List<InvoiceRecord> findByStatementIdAndDeletedFalse(Long statementId);
    boolean existsByStatementIdAndInvoiceNoAndDeletedFalse(Long statementId, String invoiceNo);
}
