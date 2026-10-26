package cn.xanderye.android.jdck.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 面板请求等后台任务共用的单线程执行器，避免每次点击都新建线程池。
 */
public final class TaskExecutor {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private TaskExecutor() {
    }

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }
}
