package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

/**
 * 常量配置类
 *
 * @author langkye
 */
public class Constant {
	/**默认编码*/
	public static final String DEFAULT_ENCODING = "UTF-8";
	/**默认分隔符*/
	public static final char DEFAULT_FIELD_DELIMITER = ',';
	/**默认空分隔符*/
	public static final String DEFAULT_NULL_FORMAT = "\\N";
	/**csv文件格式*/
	public static final String FILE_FORMAT_CSV = "csv";
	/**text文件格式*/
	public static final String FILE_FORMAT_TEXT = "text";
	/**每个分块10MB，最大10000个分块*/
	public static final Long MAX_FILE_SIZE = 1024 * 1024 * 10 * 10000L;
	/**默认后缀*/
	public static final String DEFAULT_SUFFIX = "";
}
