package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import com.alibaba.datax.common.spi.ErrorCode;


/**
 * 非结构化存储写入器错误代码
 *
 * @author langkye
 */
public enum UnstructuredStorageWriterErrorCode implements ErrorCode {
	/**错误码定义*/
	ILLEGAL_VALUE("UnstructuredStorageWriter-00", "您填写的参数值不合法."),
	Write_FILE_WITH_CHARSET_ERROR("UnstructuredStorageWriter-01", "您配置的编码未能正常写入."),
	Write_FILE_IO_ERROR("UnstructuredStorageWriter-02", "您配置的文件在写入时出现IO异常."),
	RUNTIME_EXCEPTION("UnstructuredStorageWriter-03", "出现运行时异常, 请联系我们"),
	REQUIRED_VALUE("UnstructuredStorageWriter-04", "您缺失了必须填写的参数值."),;

	/**错误码*/
	private final String code;
	/**描述*/
	private final String description;

	/**私有构造，避免传入未知类型*/
	private UnstructuredStorageWriterErrorCode(String code, String description) {
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
	 * 获取错误码描述
	 *
	 * @return 描述信息
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/**
	 * override from Object
	 *
	 * @return current object string
	 */
	@Override
	public String toString() {
		return String.format("Code:[%s], Description:[%s].", this.code, this.description);
	}
}
