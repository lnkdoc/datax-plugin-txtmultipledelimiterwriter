package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import com.alibaba.datax.common.exception.DataXException;
import com.csvreader.CsvWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * csv文本处理器
 *
 * @author langkye
 */
public class TextCsvWriterManager {
    /**
     * 构建非结构化写入器: 但字符分割
     *
     * @param fileFormat 文件类型
     * @param fieldDelimiter 字段分隔符
     * @param writer Writer
     * @return UnstructuredWriter
     */
    public static UnstructuredWriter produceUnstructuredWriter(String fileFormat, char fieldDelimiter, Writer writer) {
        //test文件格式
        if (Constant.FILE_FORMAT_TEXT.equals(fileFormat)) {
            return new TextWriterImpl(writer, fieldDelimiter);
        }
        //csv文件格式
        else {
            return new CsvWriterImpl(writer, fieldDelimiter);
        }
    }

    /**
     * 构建非结构化写入器: 多字符分割
     *
     * @param fileFormat 文件类型
     * @param fieldMultipleDelimiter 字段分隔符
     * @param writer Writer
     * @return UnstructuredWriter
     */
    public static UnstructuredWriter produceUnstructuredWriter(String fileFormat, String fieldMultipleDelimiter, Writer writer) {
        //test文件格式
        if (Constant.FILE_FORMAT_TEXT.equals(fileFormat)) {
            return new TextWriterImpl(writer, fieldMultipleDelimiter);
        }
        //csv文件格式
        else {
            throw DataXException.asDataXException(UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String.format("字段多分割符仅支持[.txt]文件格式, 您配置的切分为 : [%s],当前配置的文件格式为:[%s]", fieldMultipleDelimiter, fileFormat));
        }
    }
}

/**
 * 非结构化数据写入器--Csv实:
 * </span>csv 严格符合csv语法, 有标准的转义等处理</span>
 */
class CsvWriterImpl implements UnstructuredWriter {
    private static final Logger logger = LoggerFactory.getLogger(CsvWriterImpl.class);

    /**csv写入器*/
    private final CsvWriter csvWriter;

    /**
     * 通过写入器对象、字段分隔符构建csv写入器对象
     *
     * @param writer Writer
     * @param fieldDelimiter 字段分隔符
     */
    public CsvWriterImpl(Writer writer, char fieldDelimiter) {
        //初始化csv写入器
        this.csvWriter = new CsvWriter(writer, fieldDelimiter);
        //设置文本限定符: "
        this.csvWriter.setTextQualifier('"');
        //使用文本限定符
        this.csvWriter.setUseTextQualifier(true);
        //换行符：linux[\n], windows[\r\n]
        this.csvWriter.setRecordDelimiter(IOUtils.LINE_SEPARATOR.charAt(0));
    }

    /**
     * 写入一行
     *
     * @param splitedRows 分割的行
     * @throws IOException ex
     */
    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            logger.info("Found one record line which is empty.");
        }
        this.csvWriter.writeRecord(splitedRows.toArray(new String[0]));
    }

    /**
     * flush
     *
     * @throws IOException ex
     */
    @Override
    @SuppressWarnings(value = {""})
    public void flush() throws IOException {
        this.csvWriter.flush();
    }

    /**
     * close
     *
     * @throws IOException ex
     */
    @Override
    public void close() throws IOException {
        this.csvWriter.close();
    }

}

/**
 * 非结构化数据写入器--text实现:
 * <span>StringUtils的join方式, 简单的字符串拼接</span>
 */
class TextWriterImpl implements UnstructuredWriter {
    private static final Logger logger = LoggerFactory.getLogger(TextWriterImpl.class);
    /**单段分隔符*/
    private char fieldDelimiter;
    /**多字段分隔符*/
    private String fieldMultipleDelimiter;
    /**Writer*/
    private final Writer textWriter;

    /**
     * 通过Writer、字段分隔符构建写入器对象
     *
     * @param writer Writer
     * @param fieldDelimiter 单字段分隔符
     */
    public TextWriterImpl(Writer writer, char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
        this.textWriter = writer;
    }

    /**
     * 通过Writer、字段分隔符构建写入器对象
     *
     * @param writer Writer
     * @param fieldMultipleDelimiter 多字段分隔符
     */
    public TextWriterImpl(Writer writer, String fieldMultipleDelimiter) {
        this.fieldMultipleDelimiter = fieldMultipleDelimiter;
        this.textWriter = writer;
    }

    /**
     * 对每一行的处理
     *
     * @param splitedRows 分割的行
     * @throws IOException ex
     */
    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        //空行
        if (splitedRows.isEmpty()) {
            logger.info("Found one record line which is empty.");
        }
        //多字段分隔符处理
        if (this.fieldMultipleDelimiter != null) {
            this.textWriter.write(String.format("%s%s", StringUtils.join(splitedRows, this.fieldMultipleDelimiter), IOUtils.LINE_SEPARATOR));
        }
        //但字段分割符处理
        else {
            this.textWriter.write(String.format("%s%s", StringUtils.join(splitedRows, this.fieldDelimiter), IOUtils.LINE_SEPARATOR));
        }
    }

    /**
     * flush
     *
     * @throws IOException ex
     */
    @Override
    public void flush() throws IOException {
        this.textWriter.flush();
    }

    /**
     * close
     *
     * @throws IOException ex
     */
    @Override
    public void close() throws IOException {
        this.textWriter.close();
    }

}
