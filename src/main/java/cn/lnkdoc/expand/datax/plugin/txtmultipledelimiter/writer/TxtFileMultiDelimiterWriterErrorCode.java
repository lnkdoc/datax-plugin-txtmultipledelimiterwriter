package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * 文件写入错误码
 *
 * @author langkye
 */
public enum TxtFileMultiDelimiterWriterErrorCode implements ErrorCode {
    /**定义错误码*/
    CONFIG_INVALID_EXCEPTION("TxtFileMultiDelimiterWriter-00", "您的参数配置错误."),
    REQUIRED_VALUE("TxtFileMultiDelimiterWriter-01", "您缺失了必须填写的参数值."),
    ILLEGAL_VALUE("TxtFileMultiDelimiterWriter-02", "您填写的参数值不合法."),
    Write_FILE_ERROR("TxtFileMultiDelimiterWriter-03", "您配置的目标文件在写入时异常."),
    Write_FILE_IO_ERROR("TxtFileMultiDelimiterWriter-04", "您配置的文件在写入时出现IO异常."),
    SECURITY_NOT_ENOUGH("TxtFileMultiDelimiterWriter-05", "您缺少权限执行相应的文件写入操作."),
    NULL_POINTER_ERROR("TxtFileMultiDelimiterWriter-06", "空指针异常，请检查您的参数."),
    UNKNOWN_ERROR("TxtFileMultiDelimiterWriter-999", "遭遇到了未知错误."),
    ;

    /**错误码*/
    private final String code;
    /**错误描述*/
    private final String description;

    /**私有构造*/
    private TxtFileMultiDelimiterWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    @Override
    public String getCode() {
        return this.code;
    }

    /**
     * 获取错误描述
     *
     * @return 错误描述
     */
    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * override from Object's class
     *
     * @return current object string
     */
    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code, this.description);
    }

}
