package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

/**
 * 常量Key
 *
 * @author langkye
 */
public class Key {
    /**路径：必要参数[是],默认值[无]*/
    public static final String PATH = "path";
    /**文件名：必要参数[是],默认值[无]*/
    public static final String FILE_NAME = "fileName";
    /**写模式：必要参数[是],默认值[无]*/
    public static final String WRITE_MODE = "writeMode";
    /**字段分割符：必要参数[非],默认值[无]*/
    public static final String FIELD_DELIMITER = "fieldDelimiter";
    /**编码：必要参数[非],默认值[UTF-8]*/
    public static final String ENCODING = "encoding";
    /**压缩：必要参数[非],默认值[不压缩]*/
    public static final String COMPRESS = "compress";
    /**空格式：必要参数[非],默认值[无]*/
    public static final String NULL_FORMAT = "nullFormat";
    /**日期格式·旧的：必要参数[非],默认值[无]；该参数仅仅为了兼容旧的参数，请尽量避免使用它，而使用[DATE_FORMAT]*/
    public static final String FORMAT = "format";
    /**日期格式：必要参数[非],默认值[无]*/
    public static final String DATE_FORMAT = "dateFormat";
    /**文件格式（类型）：必要参数[非],默认值[无],可选值[csv｜txt]（其他将不支持的类型将抛异常）*/
    public static final String FILE_FORMAT = "fileFormat";
    /**是否跳过首行：必要参数[非],默认值[无]*/
    public static final String HEADER = "header";
    /**最大处理文件数量：必要参数[非],默认值[无]*/
    public static final String MAX_FILE_SIZE = "maxFileSize";
    /**写入的文件类型(如：.txt|.cav)：必要参数[非],默认值[无]*/
    public static final String SUFFIX = "suffix";
}
