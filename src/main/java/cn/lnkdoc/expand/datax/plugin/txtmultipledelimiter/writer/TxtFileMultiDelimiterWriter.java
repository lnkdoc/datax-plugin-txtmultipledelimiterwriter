package cn.lnkdoc.expand.datax.plugin.txtmultipledelimiter.writer;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 多分隔符文本文件读取器
 *
 * @author langkye
 */
@SuppressWarnings(value = {"ALL"})
public class TxtFileMultiDelimiterWriter extends Writer {
    /**
     * 读取器作业类
     */
    public static class Job extends Writer.Job {
        private static final Logger logger = LoggerFactory.getLogger(Job.class);

        /**插件配置信息*/
        private Configuration writerSliceConfig = null;

        /**
         * 初始化读取器作业
         */
        @Override
        public void init() {
            //获取配置信息，从"plugin.json"
            this.writerSliceConfig = this.getPluginJobConf();
            //参数校验
            this.validateParameter();

            /*获取日期格式*/
            String dateFormatOld = this.writerSliceConfig.getString(Key.FORMAT);
            String dateFormatNew = this.writerSliceConfig.getString(Key.DATE_FORMAT);
            if (null == dateFormatNew) {
                this.writerSliceConfig.set(Key.DATE_FORMAT, dateFormatOld);
            }
            if (null != dateFormatOld) {
                logger.warn("您使用format配置日期格式化, 这是不推荐的行为, 请优先使用dateFormat配置项, 两项同时存在则使用dateFormat.");
            }

            //非结构化参数校验
            UnstructuredStorageWriterUtil.validateParameter(this.writerSliceConfig);
        }

        /**
         * 校验参数
         */
        private void validateParameter() {
            //必要参数校验：文件名
            this.writerSliceConfig.getNecessaryValue(Key.FILE_NAME, TxtFileMultiDelimiterWriterErrorCode.REQUIRED_VALUE);
            //必要参数校验：文件路径
            String path = this.writerSliceConfig.getNecessaryValue(Key.PATH, TxtFileMultiDelimiterWriterErrorCode.REQUIRED_VALUE);

            try {
                // warn: 这里用户需要配一个目录
                File dir = new File(path);

                //得到一个文件类型
                if (dir.isFile()) {
                    //期待一个文件目录，但得到一个文件类型
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.ILLEGAL_VALUE, String.format("您配置的path: [%s] 不是一个合法的目录, 请您注意文件重名, 不合法目录名等情况.", path));
                }
                //文件或目录不存在
                if (!dir.exists()) {
                    //创建目录
                    boolean createdOk = dir.mkdirs();
                    //创建目录失败
                    if (!createdOk) {
                        throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.CONFIG_INVALID_EXCEPTION, String.format("您指定的文件路径 : [%s] 创建失败.", path));
                    }
                }
            } catch (SecurityException se) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您没有权限创建文件路径 : [%s] ", path), se);
            } catch (Exception ex) {
                logger.error("遭遇未知异常：{}", ex.getMessage());
                throw ex;
            }
        }

        /**
         * 全局准备工作
         */
        @Override
        public void prepare() {
            //获取目标路径
            String path = this.writerSliceConfig.getString(Key.PATH);
            //获取文件名
            String fileName = this.writerSliceConfig.getString(Key.FILE_NAME);
            //获取切片配置[写模式]
            String writeMode = this.writerSliceConfig.getString(Key.WRITE_MODE);

            /*处理模式*/
            //截断操作处理（清理）-模式
            String truncateWriteMode = "truncate";
            //追加-模式
            String appendWriteMode = "append";
            //无冲突-模式
            String nonConflictWriteMode = "nonConflict";

            //--截断操作处理（清理）-模式
            if (truncateWriteMode.equals(writeMode)) {
                logger.info(String.format("由于您配置了writeMode truncate, 开始清理 [%s] 下面以 [%s] 开头的内容", path, fileName));
                File dir = new File(path);
                // warn:需要判断文件是否存在，不存在时，不能删除
                try {
                    if (dir.exists()) {
                        // warn:不要使用FileUtils.deleteQuietly(dir);
                        FilenameFilter filter = new PrefixFileFilter(fileName);
                        File[] filesWithFileNamePrefix = dir.listFiles(filter);
                        //File[] filesWithFileNamePrefix = Optional.ofNullable(dir.listFiles(filter)).orElseGet(() -> new File[]{});
                        for (File eachFile : filesWithFileNamePrefix) {
                            logger.info(String.format("开始删除文件： [%s].", eachFile.getName()));
                            FileUtils.forceDelete(eachFile);
                        }
                        // FileUtils.cleanDirectory(dir);
                    }
                } catch (NullPointerException npe) {
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.Write_FILE_ERROR, String.format("您配置的目录清空时出现空指针异常 : [%s]", path), npe);
                } catch (IllegalArgumentException iae) {
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您配置的目录参数异常 : [%s]", path));
                } catch (SecurityException se) {
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您没有权限查看目录 : [%s]", path));
                } catch (IOException e) {
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.Write_FILE_ERROR, String.format("无法清空目录 : [%s]", path), e);
                }
            }
            //--追加-模式
            else if (appendWriteMode.equals(writeMode)) {
                logger.info(String.format("由于您配置了writeMode append, 写入前不做清理工作, [%s] 目录下写入相应文件名前缀  [%s] 的文件", path, fileName));
            }
            //--无冲突-模式
            else if (nonConflictWriteMode.equals(writeMode)) {
                logger.info(String.format("由于您配置了writeMode nonConflict, 开始检查 [%s] 下面的内容", path));
                // warn: check two times about exists, mkdirs
                //获取目录
                File dir = new File(path);
                try {
                    //目录存在
                    if (dir.exists()) {
                        //期待目录，但得到类文件类型
                        if (dir.isFile()) {
                            throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.ILLEGAL_VALUE, String.format("您配置的path: [%s] 不是一个合法的目录, 请您注意文件重名, 不合法目录名等情况.", path));
                        }
                        //检查该文件名前缀的文件是否存在，存在则冲突
                        FilenameFilter filter = new PrefixFileFilter(fileName);
                        File[] filesWithFileNamePrefix = dir.listFiles(filter);
                        if (filesWithFileNamePrefix != null && filesWithFileNamePrefix.length > 0) {
                            List<String> allFiles = new ArrayList<String>();
                            for (File eachFile : filesWithFileNamePrefix) {
                                allFiles.add(eachFile.getName());
                            }
                            logger.error(String.format("冲突文件列表为: [%s]", StringUtils.join(allFiles, ",")));
                            throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.ILLEGAL_VALUE, String.format("您配置的path: [%s] 目录不为空, 下面存在其他文件或文件夹.", path));
                        }
                    }
                    //目录不存在
                    else {
                        //创建目录
                        boolean createdOk = dir.mkdirs();
                        if (!createdOk) {
                            throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.CONFIG_INVALID_EXCEPTION, String.format("您指定的文件路径 : [%s] 创建失败.", path));
                        }
                    }
                } catch (SecurityException se) {
                    throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您没有权限查看目录 : [%s]", path));
                }
            }
            //不支持的模式
            else {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.ILLEGAL_VALUE, String.format("仅支持 truncate, append, nonConflict 三种模式, 不支持您配置的 writeMode 模式 : [%s]", writeMode));
            }
        }

        /**
         * 后置处理：作业（JOB）处理完成后被调用
         */
        @Override
        public void post() {

        }

        /**
         * 销毁处理：作业（JOB）销毁时被调用
         */
        @Override
        public void destroy() {

        }

        /**
         * 拆分Task。参数adviceNumber框架建议的拆分数，一般是运行时所配置的并发度。值返回的是Task的配置列表。
         *
         * @param mandatoryNumber 为了做到Reader、Writer任务数对等，这里要求Writer插件必须按照源端的切分数进行切分。否则框架报错！
         * @return 配置信息
         */
        @Override
        public List<Configuration> split(int mandatoryNumber) {
            logger.info("<###拆分[TxtFileMultiDelimiterWriter.Job]任务·开始###>");
            //配置信息
            List<Configuration> writerSplitConfigs = new ArrayList<Configuration>();
            //获取文件名（文件名前缀）
            String filePrefix = this.writerSliceConfig.getString(Key.FILE_NAME);

            //文件列表
            Set<String> allFiles = new HashSet<String>();
            //文件目录
            String path = null;
            try {
                //获取文件目录
                path = this.writerSliceConfig.getString(Key.PATH);
                //获取目录对象
                File dir = new File(path);
                //获取目录下文件列表
                String[] list = dir.list();
                //添加该目录下的文件到文件列表
                allFiles.addAll(Arrays.asList(list));
            } catch (NullPointerException ex) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.NULL_POINTER_ERROR, String.format("处理目录: [%s]时发生了异常，该目录不合法。", path));
            } catch (SecurityException ex) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您没有权限查看目录 : [%s]", path));
            } catch (Exception ex) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.UNKNOWN_ERROR, String.format("您没有权限查看目录 : [%s]", path));
            }

            for (int i = 0; i < mandatoryNumber; i++) {
                /*处理同名文件*/
                //获取配置信息
                Configuration splitedTaskConfig = this.writerSliceConfig.clone();
                //文件名
                String fullFileName = filePrefix;
                //文件列表中是否包含同名文件，如果包含则以uuid进行拼接，组成新的文件名
                while (allFiles.contains(fullFileName)) {
                    //获取uuid字符串
                    String uuid = UUID.randomUUID().toString().replace('-', '_');
                    fullFileName = String.format("%s__%s", filePrefix, uuid);
                    logger.warn("文件名[{}]已存在，正在重命名为：[{}]", filePrefix, fullFileName);
                }
                //将新的文件名添加到文件列表
                allFiles.add(fullFileName);

                //更新文件名
                splitedTaskConfig.set(Key.FILE_NAME, fullFileName);

                logger.info(String.format("拆分写入文件名:[%s]", fullFileName));

                writerSplitConfigs.add(splitedTaskConfig);
            }
            logger.info("<###拆分[TxtFileMultiDelimiterWriter.Job]任务·完毕###>");
            return writerSplitConfigs;
        }

    }

    /**
     * 读取器任务类
     */
    public static class Task extends Writer.Task {
        private static final Logger logger = LoggerFactory.getLogger(Task.class);

        /**插件配置信息*/
        private Configuration writerSliceConfig;
        /**目录*/
        private String path;
        /**文件名*/
        private String fileName;

        /**
         * 初始化读取器任务
         */
        @Override
        public void init() {
            this.writerSliceConfig = this.getPluginJobConf();
            this.path = this.writerSliceConfig.getString(Key.PATH);
            this.fileName = this.writerSliceConfig.getString(Key.FILE_NAME);
        }

        /**
         * 全局准备工作
         */
        @Override
        public void prepare() {

        }

        /**
         * 开始写处理
         * @param lineReceiver 接收一行数据
         */
        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            logger.info("<###[TxtFileMultiDelimiterWriter.Task]写任务·开始###>");
            //获取文件绝对路径
            String fileFullPath = this.buildFilePath();
            logger.info(String.format("###即将写入的文件 : [%s]", fileFullPath));

            //输出流
            OutputStream outputStream = null;
            try {
                //创建文件对象
                File newFile = new File(fileFullPath);
                final boolean createNewFile = newFile.createNewFile();
                logger.info("###文件[{}]创建结果: [{}].", fileFullPath, createNewFile);

                //获取新文件输出流
                outputStream = new FileOutputStream(newFile);
                //开始写入
                UnstructuredStorageWriterUtil.writeToStream(lineReceiver, outputStream, this.writerSliceConfig, this.fileName, this.getTaskPluginCollector());
            } catch (SecurityException se) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.SECURITY_NOT_ENOUGH, String.format("您没有权限创建文件  : [%s]", this.fileName));
            } catch (IOException ioe) {
                throw DataXException.asDataXException(TxtFileMultiDelimiterWriterErrorCode.Write_FILE_IO_ERROR, String.format("无法创建待写文件 : [%s]", this.fileName), ioe);
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
            logger.info("<###[TxtFileMultiDelimiterWriter.Task]写任务·结束###>");
        }

        /**
         * 构建文件绝对路径
         *
         * @return 文件绝对路径
         */
        private String buildFilePath() {
            //文件系统分隔符
            boolean isEndWithSeparator = false;
            switch (IOUtils.DIR_SEPARATOR) {
                //Unix分隔符
                case IOUtils.DIR_SEPARATOR_UNIX:
                    isEndWithSeparator = this.path.endsWith(String.valueOf(IOUtils.DIR_SEPARATOR));
                    break;
                //Windows系统分隔符
                case IOUtils.DIR_SEPARATOR_WINDOWS:
                    isEndWithSeparator = this.path.endsWith(String.valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                    break;
                //默认不处理，false
                default:
                    break;
            }
            //若路径没有以分隔符结尾
            if (!isEndWithSeparator) {
                //添加分隔符
                this.path = this.path + IOUtils.DIR_SEPARATOR;
            }
            //将合法文件路径+文件名进行拼接
            return String.format("%s%s", this.path, this.fileName);
        }

        /**
         * 后置处理：任务（Task）处理完成后被调用
         */
        @Override
        public void post() {

        }

        /**
         * 销毁处理：任务（Task）销毁时被调用
         */
        @Override
        public void destroy() {

        }
    }
}
