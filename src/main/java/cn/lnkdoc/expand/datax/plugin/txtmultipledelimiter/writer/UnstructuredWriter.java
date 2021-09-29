package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 非结构化数据写入器定义
 *
 * @author langkye
 */
public interface UnstructuredWriter extends Closeable {

    /**
     * 写入一行
     *
     * @param splitedRows 分割的行
     * @throws IOException ex
     */
    public void writeOneRecord(List<String> splitedRows) throws IOException;

    /**
     * flush
     *
     * @throws IOException ex
     */
    public void flush() throws IOException;

    /**
     * close
     *
     * @throws IOException ex
     */
    @Override
    public void close() throws IOException;

}
