/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.common;

import java.util.List;

/** 批量导入全部校验完成后的行级错误集合；抛出时事务整体回滚。 */
public class ImportValidationException extends RuntimeException {
    private final List<RowError> errors;
    public ImportValidationException(List<RowError> errors) { super("导入校验失败，共 " + errors.size() + " 项错误"); this.errors = List.copyOf(errors); }
    public List<RowError> getErrors() { return errors; }
    public record RowError(int row, String field, String message) {}
}
