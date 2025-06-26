/*
 * Filament工具插件 - 用于编译Filament材质、网格和IBL资源
 * 
 * 该插件接受以下参数：
 *
 * com.google.android.filament.tools-dir
 *     Filament桌面版分发/安装目录的路径
 *     该目录必须包含bin/matc工具
 *
 * com.google.android.filament.exclude-vulkan
 *     设置后，将排除对Vulkan的支持
 *
 * com.google.android.filament.include-webgpu
 *     设置后，将包含对WebGPU的支持
 *
 * com.google.android.filament.matnopt
 *     设置后，将禁用材质优化
 *
 * 使用示例：
 *     ./gradlew -Pcom.google.android.filament.tools-dir=../../dist-release assembleDebug
 */

// 导入Gradle API相关类


import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel

// 默认任务基类

import org.gradle.api.logging.Logger

// Gradle异常类

import org.gradle.api.model.ObjectFactory

// 插件接口

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.InputFileDetails
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

// 项目对象

// 提供者工厂

// 目录属性

// 文件系统操作

// 文件类型枚举

// 常规文件属性

// 日志级别

// 日志记录器

import javax.inject.Inject

// 属性接口

// 输入注解

// 输入目录注解

// 输入文件注解

// 可选注解

// 输出目录注解

import java.nio.file.Paths

// 任务动作注解

// 输入文件详情

// 对象工厂

// 操作系统工具类

// 执行操作接口

// 变更类型枚举

// 增量构建注解

// 输入变更接口

// Java NIO路径工具

// 依赖注入注解

/**
 * 带有二进制工具的任务基类
 * 该抽象类为需要调用Filament工具（如matc、cmgen、filamesh）的任务提供基础功能
 */
abstract class TaskWithBinary extends DefaultTask {
    private final String binaryName    // 二进制工具名称
    private Property<String> binaryPath = null  // 二进制工具路径属性

    /**
     * 构造函数
     * @param name 二进制工具名称（如"matc"、"cmgen"、"filamesh"）
     */
    TaskWithBinary(String name) {
        binaryName = name
    }

    // 注入对象工厂，用于创建Gradle对象
    @Inject
    abstract ObjectFactory getObjects()
    // 注入提供者工厂，用于获取项目属性
    @Inject
    abstract ProviderFactory getProviders()

    /**
     * 获取二进制工具的完整路径
     * 根据操作系统类型选择合适的可执行文件（Windows使用.exe后缀）
     * @return 二进制工具路径的属性对象
     */
    @Input
    Property<String> getBinary() {
        if (binaryPath == null) {
            // 定义工具路径模板：Windows版本和Unix版本
            def tool = ["/bin/${binaryName}.exe", "/bin/${binaryName}"]
            def fullPath = tool.collect { path ->
                // 从Gradle属性中获取Filament工具目录路径
                def filamentToolsPath = providers
                        .gradleProperty("com.google.android.filament.tools-dir")
                        .forUseAtConfigurationTime().get()
                // 构建完整的工具文件路径
                def directory = objects.fileProperty()
                        .fileValue(new File(filamentToolsPath)).getAsFile().get()
                Paths.get(directory.absolutePath, path).toFile()
            }

            // 创建字符串属性并根据操作系统选择合适的路径
            binaryPath = objects.property(String.class)
            binaryPath.set(
                    (OperatingSystem.current().isWindows() ? fullPath[0] : fullPath[1]).toString())
        }
        return binaryPath
    }
}

/**
 * 日志输出流类
 * 将字节数组输出流的内容重定向到Gradle日志系统
 * 用于捕获外部工具的标准输出和错误输出
 */
class LogOutputStream extends ByteArrayOutputStream {
    private final Logger logger    // Gradle日志记录器
    private final LogLevel level   // 日志级别

    /**
     * 构造函数
     * @param logger Gradle日志记录器实例
     * @param level 日志级别（如LIFECYCLE、ERROR等）
     */
    LogOutputStream(Logger logger, LogLevel level) {
        this.logger = logger
        this.level = level
    }

    /**
     * 刷新输出流
     * 将缓冲区内容输出到Gradle日志，然后清空缓冲区
     */
    @Override
    void flush() {
        logger.log(level, toString())  // 将内容记录到日志
        reset()                        // 清空缓冲区
    }
}

/**
 * 材质编译器任务
 * 使用matc工具编译Filament材质文件（.mat -> .filamat）
 * 支持增量构建，只编译发生变化的材质文件
 */
abstract class MaterialCompiler extends TaskWithBinary {
    // 输入目录：包含.mat材质源文件的目录
    @Incremental
    @InputDirectory
    abstract DirectoryProperty getInputDir()

    // 输出目录：编译后的.filamat文件存放目录
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    // 注入文件系统操作接口，用于文件删除等操作
    @Inject
    abstract FileSystemOperations getFs()
    // 注入执行操作接口，用于调用外部命令
    @Inject
    abstract ExecOperations getExec()
    // 注入对象工厂
    @Inject
    abstract ObjectFactory getObjects()
    // 注入提供者工厂
    @Inject
    abstract ProviderFactory getProviders()

    /**
     * 构造函数
     * 调用父类构造函数，指定使用"matc"工具
     */
    MaterialCompiler() {
        super("matc")
    }

    /**
     * 任务执行方法
     * 处理材质文件的增量编译
     * @param inputs 输入变更信息，包含文件变化详情
     */
    @TaskAction
    void execute(InputChanges inputs) {
        // 如果不是增量构建，清理所有输出文件
        if (!inputs.incremental) {
            fs.delete({
                delete(objects.fileTree().from(outputDir).matching { include '*.filamat' })
            })
        }

        // 遍历输入目录中的文件变更
        inputs.getFileChanges(inputDir).each { InputFileDetails change ->
            // 跳过目录，只处理文件
            if (change.fileType == FileType.DIRECTORY) return

            def file = change.file

            // 如果文件被删除，删除对应的输出文件
            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                // 创建日志输出流，用于捕获matc工具的输出
                def out = new LogOutputStream(logger, LogLevel.LIFECYCLE)
                def err = new LogOutputStream(logger, LogLevel.ERROR)

                // 输出编译开始信息
                def header = ("Compiling material " + file + "\n").getBytes()
                out.write(header)
                out.flush()

                // 检查matc工具是否存在
                if (!new File(binary.get()).exists()) {
                    throw new GradleException("Could not find ${binary.get()}." +
                            " Ensure Filament has been built/installed before building this app.")
                }

                // 构建matc命令参数
                def matcArgs = []

                // 检查是否排除Vulkan支持
                def exclude_vulkan = providers
                        .gradleProperty("com.google.android.filament.exclude-vulkan")
                        .forUseAtConfigurationTime().present
                if (!exclude_vulkan) {
                    matcArgs += ['-a', 'vulkan']  // 添加Vulkan API支持
                }

                // 检查是否包含WebGPU支持
                def include_webgpu = providers
                        .gradleProperty("com.google.android.filament.include-webgpu")
                        .forUseAtConfigurationTime().present
                if (include_webgpu) {
                    matcArgs += ['-a', 'webgpu', '--variant-filter=skinning,stereo']
                    // 添加WebGPU API支持
                }

                // 检查是否禁用材质优化
                def mat_no_opt = providers
                        .gradleProperty("com.google.android.filament.matnopt")
                        .forUseAtConfigurationTime().present
                if (mat_no_opt) {
                    matcArgs += ['-g']  // 禁用优化，保留调试信息
                }

                // 添加基本参数：OpenGL API、移动平台、输出文件、输入文件
                matcArgs += ['-a', 'opengl', '-p', 'mobile', '-o', getOutputFile(file), file]

                // 执行matc命令
                exec.exec {
                    standardOutput out   // 标准输出重定向到日志
                    errorOutput err      // 错误输出重定向到日志
                    executable "${binary.get()}"  // 可执行文件路径
                    args matcArgs        // 命令参数
                }
            }
        }
    }

    /**
     * 根据输入文件生成对应的输出文件路径
     * 将.mat文件扩展名替换为.filamat
     * @param file 输入的材质源文件
     * @return 对应的编译后文件
     */
    File getOutputFile(final File file) {
        return outputDir.file(file.name[0..file.name.lastIndexOf('.')] + 'filamat').get().asFile
    }
}

/**
 * IBL（基于图像的光照）生成器任务
 * 使用cmgen工具处理HDR环境贴图，生成IBL资源
 * 支持增量构建
 */
abstract class IblGenerator extends TaskWithBinary {
    // 可选的cmgen命令参数，用于自定义IBL生成选项
    @Input
    @Optional
    abstract Property<String> getCmgenArgs()

    // 输入文件：HDR环境贴图文件（如.hdr、.exr格式）
    @Incremental
    @InputFile
    abstract RegularFileProperty getInputFile()

    // 输出目录：生成的IBL资源存放目录
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    // 注入文件系统操作接口
    @Inject
    abstract FileSystemOperations getFs()
    // 注入执行操作接口
    @Inject
    abstract ExecOperations getExec()
    // 注入对象工厂
    @Inject
    abstract ObjectFactory getObjects()

    /**
     * 构造函数
     * 调用父类构造函数，指定使用"cmgen"工具
     */
    IblGenerator() {
        super("cmgen")
    }

    /**
     * 任务执行方法
     * 处理IBL文件的增量生成
     * @param inputs 输入变更信息
     */
    @TaskAction
    void execute(InputChanges inputs) {
        // 如果不是增量构建，清理所有输出文件
        if (!inputs.incremental) {
            fs.delete({
                delete(objects.fileTree().from(outputDir).matching { include '*' })
            })
        }

        // 遍历输入文件的变更
        inputs.getFileChanges(inputFile).each { InputFileDetails change ->
            // 跳过目录，只处理文件
            if (change.fileType == FileType.DIRECTORY) return

            def file = change.file

            // 如果文件被删除，删除对应的输出文件
            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                // 创建日志输出流
                def out = new LogOutputStream(logger, LogLevel.LIFECYCLE)
                def err = new LogOutputStream(logger, LogLevel.ERROR)

                // 输出IBL生成开始信息
                def header = ("Generating IBL " + file + "\n").getBytes()
                out.write(header)
                out.flush()

                // 检查cmgen工具是否存在
                if (!new File(binary.get()).exists()) {
                    throw new GradleException("Could not find ${binary.get()}." +
                            " Ensure Filament has been built/installed before building this app.")
                }

                // 构建cmgen命令参数
                def outputPath = outputDir.get().asFile
                def commandArgs = cmgenArgs.getOrNull()
                if (commandArgs == null) {
                    // 使用默认参数：安静模式、交叉过滤、RGB32F格式、模糊提取
                    commandArgs =
                            '-q -x ' + outputPath + ' --format=rgb32f ' +
                                    '--extract-blur=0.08 --extract=' + outputPath.absolutePath
                }
                commandArgs = commandArgs + " " + file

                // 执行cmgen命令
                exec.exec {
                    standardOutput out   // 标准输出重定向到日志
                    errorOutput err      // 错误输出重定向到日志
                    executable "${binary.get()}"  // 可执行文件路径
                    args(commandArgs.split())     // 命令参数（分割字符串为数组）
                }
            }
        }
    }

    /**
     * 根据输入文件生成对应的输出目录
     * 移除文件扩展名，使用文件名作为输出目录名
     * @param file 输入的HDR环境贴图文件
     * @return 对应的输出目录
     */
    File getOutputFile(final File file) {
        return outputDir.file(file.name[0..file.name.lastIndexOf('.') - 1]).get().asFile
    }
}

/**
 * 网格编译器任务
 * 使用filamesh工具编译3D网格文件（如.obj -> .filamesh）
 * 支持增量构建
 */
abstract class MeshCompiler extends TaskWithBinary {
    // 输入文件：3D网格源文件（如.obj、.fbx等格式）
    @Incremental
    @InputFile
    abstract RegularFileProperty getInputFile()

    // 输出目录：编译后的.filamesh文件存放目录
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    // 注入文件系统操作接口
    @Inject
    abstract FileSystemOperations getFs()
    // 注入执行操作接口
    @Inject
    abstract ExecOperations getExec()

    /**
     * 构造函数
     * 调用父类构造函数，指定使用"filamesh"工具
     */
    MeshCompiler() {
        super("filamesh")
    }

    /**
     * 任务执行方法
     * 处理3D网格文件的增量编译
     * @param inputs 输入变更信息
     */
    @TaskAction
    void execute(InputChanges inputs) {
        // 如果不是增量构建，清理所有输出文件
        if (!inputs.incremental) {
            fs.delete({
                delete(objects.fileTree().from(outputDir).matching { include '*.filamesh' })
            })
        }

        // 遍历输入文件的变更
        inputs.getFileChanges(inputFile).each { InputFileDetails change ->
            // 跳过目录，只处理文件
            if (change.fileType == FileType.DIRECTORY) return

            def file = change.file

            // 如果文件被删除，删除对应的输出文件
            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                // 创建日志输出流
                def out = new LogOutputStream(logger, LogLevel.LIFECYCLE)
                def err = new LogOutputStream(logger, LogLevel.ERROR)

                // 输出网格编译开始信息
                def header = ("Compiling mesh " + file + "\n").getBytes()
                out.write(header)
                out.flush()

                // 检查filamesh工具是否存在
                if (!new File(binary.get()).exists()) {
                    throw new GradleException("Could not find ${binary.get()}." +
                            " Ensure Filament has been built/installed before building this app.")
                }

                // 执行filamesh命令
                exec.exec {
                    standardOutput out   // 标准输出重定向到日志
                    errorOutput err      // 错误输出重定向到日志
                    executable "${binary.get()}"  // 可执行文件路径
                    args(file, getOutputFile(file))  // 参数：输入文件和输出文件
                }
            }
        }
    }

    /**
     * 根据输入文件生成对应的输出文件
     * 将输入文件扩展名替换为.filamesh
     * @param file 输入的3D网格文件
     * @return 对应的.filamesh输出文件
     */
    File getOutputFile(final File file) {
        return outputDir.file(file.name[0..file.name.lastIndexOf('.')] + 'filamesh').get().asFile
    }
}

/**
 * Filament工具插件扩展配置类
 * 用于配置插件的各种参数
 */
class FilamentToolsPluginExtension {
    /**
     * 材质文件输入目录
     */
    DirectoryProperty materialInputDir
    /**
     * 材质文件输出目录
     */
    DirectoryProperty materialOutputDir

    /**
     * cmgen工具的自定义参数
     */
    String cmgenArgs
    /**
     * IBL环境贴图输入文件
     */
    RegularFileProperty iblInputFile
    /**
     * IBL环境贴图输出目录
     */
    DirectoryProperty iblOutputDir

    /**
     * 3D网格输入文件
     */
    RegularFileProperty meshInputFile
    /**
     * 3D网格文件输出目录
     */
    DirectoryProperty meshOutputDir
}
/**
 * Filament工具插件实现类
 * 实现Plugin接口，为Android项目提供Filament相关的构建任务
 */
class FilamentToolsPlugin implements Plugin<Project> {
    /**
     * 插件应用方法
     * 在项目中注册Filament相关的构建任务
     * @param project 目标项目
     */
    void apply(Project project) {
        // 创建插件扩展配置
        def extension = project.extensions.create('filamentTools', FilamentToolsPluginExtension)
        // 初始化扩展属性的默认值
        extension.materialInputDir = project.objects.directoryProperty()
        extension.materialOutputDir = project.objects.directoryProperty()
        extension.iblInputFile = project.objects.fileProperty()
        extension.iblOutputDir = project.objects.directoryProperty()
        extension.meshInputFile = project.objects.fileProperty()
        extension.meshOutputDir = project.objects.directoryProperty()

        project.tasks.register("filamentCompileMaterials", MaterialCompiler) {
            enabled = extension.materialInputDir.isPresent() && extension.materialOutputDir.isPresent()
            inputDir.set(extension.materialInputDir.getOrNull())
            outputDir.set(extension.materialOutputDir.getOrNull())
        }

        project.preBuild.dependsOn "filamentCompileMaterials"

        project.tasks.register("filamentGenerateIbl", IblGenerator) {
            enabled = extension.iblInputFile.isPresent() && extension.iblOutputDir.isPresent()
            cmgenArgs = extension.cmgenArgs
            inputFile = extension.iblInputFile.getOrNull()
            outputDir = extension.iblOutputDir.getOrNull()
        }

        project.preBuild.dependsOn "filamentGenerateIbl"

        project.tasks.register("filamentCompileMesh", MeshCompiler) {
            enabled = extension.meshInputFile.isPresent() && extension.meshOutputDir.isPresent()
            inputFile = extension.meshInputFile.getOrNull()
            outputDir = extension.meshOutputDir.getOrNull()
        }

        project.preBuild.dependsOn "filamentCompileMesh"
    }
}
