package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.google.common.collect.Sets;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 非结构化存储写入工具类
 *
 * @author langkye
 */
public class UnstructuredStorageWriterUtil {
    private static final Logger logger = LoggerFactory.getLogger(UnstructuredStorageWriterUtil.class);
    public static final String GZIP = "gzip";
    public static final String BZIP2 = "bzip2";

    private UnstructuredStorageWriterUtil() {}

    /**
     * 检查参数：writeMode、编码、压缩、字段分隔符
     *
     * @param writerConfiguration 配置信息
     */
    public static void validateParameter(Configuration writerConfiguration) {
        /*必要参数检查*/
        String writeMode = writerConfiguration.getNecessaryValue(Key.WRITE_MODE, UnstructuredStorageWriterErrorCode.REQUIRED_VALUE);
        writeMode = writeMode.trim();
        //支持的处理模式
        Set<String> supportedWriteModes = Sets.newHashSet("truncate", "append", "nonConflict");

        /*校验处理模式*/
        if (!supportedWriteModes.contains(writeMode)) {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format("仅支持 truncate, append, nonConflict 三种模式, 不支持您配置的 writeMode 模式 : [%s]", writeMode));
        }

        //更新处理模式
        writerConfiguration.set(Key.WRITE_MODE, writeMode);

        /*校验编码*/
        String encoding = writerConfiguration.getString(Key.ENCODING);
        //未配置编码，使用默认编码：UTF-8
        if (StringUtils.isBlank(encoding)) {
            logger.warn(String.format("您的encoding配置为空, 将使用默认值[%s]", Constant.DEFAULT_ENCODING));
            writerConfiguration.set(Key.ENCODING, Constant.DEFAULT_ENCODING);
        } else {
            //尝试解析编码参数
            try {
                encoding = encoding.trim();
                writerConfiguration.set(Key.ENCODING, encoding);
                Charsets.toCharset(encoding);
            } catch (Exception ex) {
                throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format("不支持您配置的编码格式:[%s]", encoding), ex);
            }
        }

        /*校验压缩格式*/
        // only support compress types
        //获取压缩格式参数
        String compress = writerConfiguration.getString(Key.COMPRESS);
        //为配置压缩参数
        if (StringUtils.isBlank(compress)) {
            writerConfiguration.set(Key.COMPRESS, null);
        }
        //已经配置压缩参数
        else {
            //当前支持的雅俗格式
            Set<String> supportedCompress = Sets.newHashSet("gzip", "bzip2");
            //输入了不支持的压缩格式
            if (!supportedCompress.contains(compress.toLowerCase().trim())) {
                String message = String.format("仅支持 [%s] 文件压缩格式 , 不支持您配置的文件压缩格式: [%s]", StringUtils.join(supportedCompress, ","), compress);
                throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format(message, compress));
            }
        }

        /*字段分割符号校验*/
        //获取配置的"字段分隔符"
        String delimiterInStr = writerConfiguration.getString(Key.FIELD_DELIMITER);
        //多字段分隔符
        if (null != delimiterInStr && 1 != delimiterInStr.length()) {
            logger.warn(String.format("非单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
        }
        //为配置"字段分隔符"使用默认配置','
        if (null == delimiterInStr) {
            logger.warn(String.format("您没有配置列分隔符, 使用默认值[%s]", Constant.DEFAULT_FIELD_DELIMITER));
            writerConfiguration.set(Key.FIELD_DELIMITER, Constant.DEFAULT_FIELD_DELIMITER);
        }

        /*文件类型校验*/
        //构建支持的文件类型
        String fileFormat = writerConfiguration.getString(Key.FILE_FORMAT, Constant.FILE_FORMAT_TEXT);
        //未配置或配置的文件类型暂不支持
        if (!Constant.FILE_FORMAT_CSV.equals(fileFormat) && !Constant.FILE_FORMAT_TEXT.equals(fileFormat)) {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format("您配置的fileFormat [%s]错误, 支持csv, text两种.", fileFormat));
        }
    }

    /**
     * 文明名处理：处理同名文件，存在同名则重命名
     *
     * @param writerSliceConfig 预处理的配置信息
     * @param originAllFileExists 目录中的文件列表
     * @param mandatoryNumber 处理次数
     * @return 处理过的配置信息
     */
    @SuppressWarnings(value = {"unused"})
    public static List<Configuration> handlerFileNames(Configuration writerSliceConfig, Set<String> originAllFileExists, int mandatoryNumber) {
        logger.info("<###分割·开始###>");
        //文件列表
        Set<String> allFileExists = new HashSet<>(originAllFileExists);
        //分割配置信息
        List<Configuration> writerSplitConfigs = new ArrayList<>();
        //文件名
        String filePrefix = writerSliceConfig.getString(Key.FILE_NAME);

        for (int i = 0; i < mandatoryNumber; i++) {
            /*处理同名文件*/
            //获取配置信息
            Configuration splitedTaskConfig = writerSliceConfig.clone();
            //实际文件名
            String fullFileName = filePrefix;
            //检测已经存在的文件中是否包含"配置的文件名"，若包含，则在末尾拼接uuid字符串
            while (allFileExists.contains(fullFileName)) {
                String uuid = UUID.randomUUID().toString().replace('-', '_');
                fullFileName = String.format("%s__%s", filePrefix, uuid);
            }
            //将实际的文件名添加的文件列表
            allFileExists.add(fullFileName);
            //更新文件名
            splitedTaskConfig.set(Key.FILE_NAME, fullFileName);
            logger.info(String.format("目录[%s]中已存在文件[%s]，已重命名为:[%s]", writerSliceConfig.getString(Key.PATH), filePrefix, fullFileName));
            writerSplitConfigs.add(splitedTaskConfig);
        }
        logger.info("end do split.");
        return writerSplitConfigs;
    }

    /**
     * 构建文件绝对路径
     *
     * @param path 路径
     * @param fileName 文件名
     * @param suffix 文件后缀
     * @return 文件绝对路径
     */
    @SuppressWarnings(value = {"unused"})
    public static String buildFilePath(String path, String fileName, String suffix) {
        //路径是否以"文件分隔符"结尾
        boolean isEndWithSeparator = false;
        //获取当前操作系统的文件分隔符
        switch (IOUtils.DIR_SEPARATOR) {
            //Unix系统分隔符
            case IOUtils.DIR_SEPARATOR_UNIX:
                isEndWithSeparator = path.endsWith(String.valueOf(IOUtils.DIR_SEPARATOR));
                break;
            //Windows系统文件分隔符
            case IOUtils.DIR_SEPARATOR_WINDOWS:
                isEndWithSeparator = path.endsWith(String.valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                break;
            //默认false
            default:
                break;
        }

        //若目录不是以文件分隔符结束，则拼接上当前系统的文件分隔符
        if (!isEndWithSeparator) {
            path = path + IOUtils.DIR_SEPARATOR;
        }

        //检查文件后缀，默认空""
        if (null == suffix) {
            suffix = "";
        } else {
            suffix = suffix.trim();
        }

        //将路径+文件名+文件后置拼接返回
        return String.format("%s%s%s", path, fileName, suffix);
    }

    /**
     * 内容写入流预处理
     *
     * @param lineReceiver 接收的行
     * @param outputStream 输出流
     * @param config 配置信息
     * @param context ctx：配置文件
     * @param taskPluginCollector 任务处理器
     */
    public static void writeToStream(RecordReceiver lineReceiver, OutputStream outputStream, Configuration config, String context, TaskPluginCollector taskPluginCollector) {
        //获取编码参数
        String encoding = config.getString(Key.ENCODING, Constant.DEFAULT_ENCODING);
        //编码格式解析
        if (StringUtils.isBlank(encoding)) {
            logger.warn(String.format("您配置的encoding为[%s], 使用默认值[%s]", encoding, Constant.DEFAULT_ENCODING));
            encoding = Constant.DEFAULT_ENCODING;
        }

        //获取压缩参数
        String compress = config.getString(Key.COMPRESS);

        BufferedWriter writer = null;
        //压缩处理逻辑
        try {
            //不压缩
            if (null == compress) {
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
            }
            //压缩处理
            else {
                //gzip
                if (GZIP.equalsIgnoreCase(compress)) {
                    CompressorOutputStream compressorOutputStream = new GzipCompressorOutputStream(outputStream);
                    writer = new BufferedWriter(new OutputStreamWriter(compressorOutputStream, encoding));
                }
                //bzip2
                else if (BZIP2.equalsIgnoreCase(compress)) {
                    CompressorOutputStream compressorOutputStream = new BZip2CompressorOutputStream(outputStream);
                    writer = new BufferedWriter(new OutputStreamWriter(compressorOutputStream, encoding));
                }
                //不支持的文件压缩格式
                else {
                    throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format("仅支持 gzip, bzip2 文件压缩格式 , 不支持您配置的文件压缩格式: [%s]",compress));
                }
            }
            //执行写处理
            UnstructuredStorageWriterUtil.doWriteToStream(lineReceiver, writer, context, config, taskPluginCollector);
        } catch (UnsupportedEncodingException uee) {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.Write_FILE_WITH_CHARSET_ERROR, String.format("不支持的编码格式 : [%s]", encoding), uee);
        } catch (NullPointerException e) {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.RUNTIME_EXCEPTION, "运行时错误, 请联系我们", e);
        } catch (IOException e) {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.Write_FILE_IO_ERROR, String.format("流写入错误 : [%s]", context), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * 实际[将内容写入流]写入处理
     *
     * @param lineReceiver 接收的行
     * @param writer 写操作缓冲对象
     * @param context ctx：配置文件
     * @param config 配置信息
     * @param taskPluginCollector 任务处理器
     * @throws IOException ex
     */
    private static void doWriteToStream(RecordReceiver lineReceiver, BufferedWriter writer, String context, Configuration config, TaskPluginCollector taskPluginCollector) throws IOException {
        //获取空处理格式参数
        String nullFormat = config.getString(Key.NULL_FORMAT);

        // 获取日期格式：兼容format & dataFormat
        String dateFormat = config.getString(Key.DATE_FORMAT);
        // 获取旧的日期格式，warn: 可能不兼容
        DateFormat dateParse = null;
        if (StringUtils.isNotBlank(dateFormat)) {
            dateParse = new SimpleDateFormat(dateFormat);
        }

        //获取文件格式
        String fileFormat = config.getString(Key.FILE_FORMAT, Constant.FILE_FORMAT_TEXT);

        //获取"字段分隔符"
        String fieldMultipleDelimiter = config.getString(Key.FIELD_DELIMITER, Constant.DEFAULT_FIELD_DELIMITER + "");

        //根据字段分隔符，选择写入模式：单分隔符使用csv处理；多字段分隔符使用自定义流处理
        UnstructuredWriter unstructuredWriter;
        //单字段分隔符
        if (fieldMultipleDelimiter.length() == 1) {
            char fieldDelimiter = config.getChar(Key.FIELD_DELIMITER, Constant.DEFAULT_FIELD_DELIMITER);
            unstructuredWriter = TextCsvWriterManager.produceUnstructuredWriter(fileFormat, fieldDelimiter, writer);
        }
        //多字段分隔符
        else {
            unstructuredWriter = TextCsvWriterManager.produceUnstructuredWriter(fileFormat, fieldMultipleDelimiter, writer);
        }

        //是否跳过首行
        List<String> headers = config.getList(Key.HEADER, String.class);
        if (null != headers && !headers.isEmpty()) {
            unstructuredWriter.writeOneRecord(headers);
        }

        //读取一条"记录",对每一条记录进行处理
        Record record;
        while ((record = lineReceiver.getFromReader()) != null) {
            //对当前读取到的"记录"进行处理
            UnstructuredStorageWriterUtil.transportOneRecord(record, nullFormat, dateParse, taskPluginCollector, unstructuredWriter);
        }

        // warn:由调用方控制流的关闭（框架）
        // IOUtils.closeQuietly(unstructuredWriter);
    }

    /**
     * 异常表示脏数据
     *
     * @param record 记录
     * @param nullFormat 空处理格式
     * @param dateParse 日期格式化对象
     * @param taskPluginCollector 任务处理器
     * @param unstructuredWriter 非结构化写入器
     */
    public static void transportOneRecord(Record record, String nullFormat, DateFormat dateParse, TaskPluginCollector taskPluginCollector, UnstructuredWriter unstructuredWriter) {
        //默认空处理为"null"
        if (null == nullFormat) {
            nullFormat = "null";
        }
        //处理一行的列
        try {
            //所有列
            List<String> splitedRows = new ArrayList<>();
            //获取列的数量
            int recordLength = record.getColumnNumber();
            if (0 != recordLength) {
                Column column;
                //遍历处理每一列
                for (int i = 0; i < recordLength; i++) {
                    column = record.getColumn(i);
                    //当前列不为空
                    if (null != column.getRawData()) {
                        //检测是否是日期格式
                        boolean isDateColumn = column instanceof DateColumn;
                        //非日期格式
                        if (!isDateColumn) {
                            splitedRows.add(column.asString());
                        }
                        //日期格式处理
                        else {
                            //格式化为日期
                            if (null != dateParse) {
                                splitedRows.add(dateParse.format(column.asDate()));
                            }
                            //转为字符串
                            else {
                                splitedRows.add(column.asString());
                            }
                        }
                    }
                    //数据为空处理
                    else {
                        // warn: it's all ok if nullFormat is null
                        splitedRows.add(nullFormat);
                    }
                }
            }
            unstructuredWriter.writeOneRecord(splitedRows);
        } catch (Exception e) {
            // warn: dirty data
            taskPluginCollector.collectDirtyRecord(record, e);
        }
    }
}
