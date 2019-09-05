package com.yueny.scanner;

import com.yueny.scanner.api.IScanner;
import com.yueny.scanner.config.ScanConfig;
import com.yueny.scanner.factory.ExecutorsFactory;
import com.yueny.scanner.util.PackageUtil;
import com.yueny.scanner.util.ScannerUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * 类扫描器实现类
 *
 * @Author yueny09 <deep_blue_yang@126.com>
 * @Date 2019-09-04 20:39
 */
public class Scanner implements IScanner {
    @Override
    public List<Class<?>> scan(List<String> basePackages) {
        return scan(ScanConfig.builder()
                .basePackages(basePackages)
                .build());
    }

    @Override
    public List<Class<?>> scan(List<String> basePackages, Class<? extends Annotation> annotation) {
        List<Class<?>> classList = scan(ScanConfig.builder()
                .basePackages(basePackages)
                .annotation(annotation)
                .build());

        return classList;

//        //根据 Annotation 过滤并返回
//        return classList.parallelStream()
//                .filter(clz -> {
//                    try {
//                        if (clz.getAnnotation(annotation) == null) {
//                            return false;
//                        }
//                    } catch (Throwable e) {
////                        log.debug(e.getMessage());
//                        return false;
//                    }
//                    return true;
//                })
//                .collect(Collectors.toList());
    }

    @Override
    public List<Class<?>> scan(Set<String> basePackages, Class<?> clazz) {
        return null;
    }

    private List<Class<?>> scan(ScanConfig scanConfig) {
        StopWatch stopWatch = StopWatch.createStarted();

        //没有需要扫描的包，返回空列表
        if (scanConfig == null || CollectionUtils.isEmpty(scanConfig.getBasePackages())) {
            return Collections.emptyList();
        }

        List<Class<?>> classList = new LinkedList<>();
        //去除重复包和父子包
        List<String> realScanBasePackages = PackageUtil.distinct(scanConfig.getBasePackages());

        //创建异步线程
        List<FutureTask<List<Class<?>>>> tasks = new LinkedList<>();
        realScanBasePackages
                .forEach(pkg -> {
                    ScannerCallable call = new ScannerCallable(pkg, scanConfig);

                    FutureTask<List<Class<?>>> task = new FutureTask(call);
                    ExecutorsFactory.submit(new Thread(task));

                    tasks.add(task);
                });

        //等待返回结果
        tasks.parallelStream().forEach(task -> {
            try {
                classList.addAll(task.get());
            } catch (InterruptedException | ExecutionException e) {
//                log.error(e.getMessage(), e);
            }
        });

        stopWatch.stop();
        Long millTime = stopWatch.getTime();

        return classList;
    }

    /**
     * 扫描器线程类
     */
    @AllArgsConstructor
    class ScannerCallable implements Callable<List<Class<?>>> {
        /**
         * 扫描的包名称, 扫描路径的去重去父子包后的真实路径
         */
        private String pkg;

        /**
         * 扫描条件项
         */
        private ScanConfig scanConfig;

        @Override
        public List<Class<?>> call() {
            return ScannerUtils.scan(pkg, scanConfig);
        }
    }
}
