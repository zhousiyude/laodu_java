1. # 笔记：Java 线程池的创建、程序不退出问题与 shutdown 细节

   ## 一、如何创建 Java 线程池

   线程池的本质是复用线程、控制并发数量，避免频繁创建和销毁线程带来的开销。最推荐的方式是直接 `new ThreadPoolExecutor(...)`，显式指定全部七个参数，而不是依赖 `Executors` 工厂方法隐式使用无界队列或无限线程数。

   ### 七个核心参数

   完整构造器如下：


   ```java
   ThreadPoolExecutor(
       int corePoolSize,          // 核心线程数
       int maximumPoolSize,       // 最大线程数
       long keepAliveTime,        // 非核心线程空闲存活时间
       TimeUnit unit,             // 时间单位
       BlockingQueuerunnable workQueue,  // 任务队列
       ThreadFactory threadFactory,        // 线程工厂
       RejectedExecutionHandler handler    // 拒绝策略
   )
   ```
   任务提交流程遵循固定优先级：核心线程 → 任务队列 → 非核心线程 → 拒绝策略。当新任务到来时，如果当前运行线程数小于 `corePoolSize`，直接创建核心线程执行；核心线程已满则入队；队列已满则创建非核心线程直到 `maximumPoolSize`；仍无法容纳则触发拒绝策略。

   ### 创建示例

   ```java
   ThreadPoolExecutor executor = new ThreadPoolExecutor(
       2,                              // corePoolSize
       4,                              // maximumPoolSize
       60L, TimeUnit.SECONDS,          // keepAliveTime
       new ArrayBlockingQueue<>(10),   // 有界队列
       Executors.defaultThreadFactory(),
       new ThreadPoolExecutor.AbortPolicy()
   );
   executor.submit(() -> System.out.println("task running"));
   // 如果不关闭，程序可能不会退出 —— 见下一节
   ```
   ## 二、main 方法结束了，为什么程序不结束？

   这是使用线程池时最常见也最容易困惑的现象：`main` 方法执行完毕，控制台却没有任何退出的迹象。

   ### 根本原因：默认线程工厂创建的是非守护线程

   `Executors.defaultThreadFactory()` 创建的所有线程，包括线程池中的工作线程，都是非守护线程（用户线程） 。JVM 的退出条件并不是“`main` 方法结束”，而是“所有非守护线程都结束” 。只要线程池中还有存活的非守护工作线程，JVM 就不会终止，即使 `main` 线程早已退出。

   `Executors.newSingleThreadExecutor()`、`newFixedThreadPool`、`newCachedThreadPool` 等工厂方法都使用默认线程工厂，因此它们创建的线程池同样会阻止 JVM 退出。

   ### 关键区别：守护线程 vs 非守护线程


   |                    | 守护线程   | 非守护线程                  |
   | ------------------ | ---------- | --------------------------- |
   | JVM 是否等待其结束 | **不等待** | **等待**                    |
   | 默认线程工厂创建   | 否         | **是**                      |
   | 典型例子           | GC 线程    | `main` 线程、线程池工作线程 |

   **结论**：线程池创建出来的工作线程默认是非守护线程，它们会阻止 JVM 正常退出。如果希望 `main` 结束后 JVM 自动退出，要么在线程工厂中显式设置 `setDaemon(true)`，要么在线程池使用完毕后调用 `shutdown()`。但将线程池线程设为守护线程通常不是推荐做法，因为 JVM 退出时它们会被强制终止，可能造成任务丢失或状态不一致。正确的做法是主动关闭线程池。

   ## 三、shutdown 方法的各类细节

   ### 3.1 线程池的五种状态

   理解 `shutdown` 的行为，首先需要知道线程池的内部状态机：


   | 状态           | 含义                                                      |
   | -------------- | --------------------------------------------------------- |
   | **RUNNING**    | 正常运行，接受新任务，处理队列任务                        |
   | **SHUTDOWN**   | 不接受新任务，但会继续处理队列中已有任务和正在执行的任务  |
   | **STOP**       | 不接受新任务，不处理队列任务，并尝试中断正在执行的任务    |
   | **TIDYING**    | 所有任务结束、活动线程数为 0，即将执行`terminated()` 钩子 |
   | **TERMINATED** | `terminated()` 执行完毕，线程池彻底终止                   |

   状态转换的核心路径：

   - `shutdown()`：`RUNNING → SHUTDOWN → TIDYING → TERMINATED`
   - `shutdownNow()`：`RUNNING → STOP → TIDYING → TERMINATED`

   ### 3.2 shutdown()：优雅关闭

   `shutdown()` 的行为可以概括为：**拒新、做完旧的、不阻塞调用线程**。

   - **不再接受新任务**：之后提交的任务会触发拒绝策略。
   - **队列中已有的任务继续执行**：包括正在执行的和在 `workQueue` 中等待的任务，都会被处理完毕。
   - **中断空闲线程**：`shutdown()` 内部调用 `interruptIdleWorkers()`，只中断那些阻塞在 `getTask()` 上等待任务的空闲工作线程，正在执行任务的线程不会被中断。
   - **不阻塞调用线程**：`shutdown()` 返回时线程池尚未终止，调用方不会被卡住。

   一个容易忽略的细节：`shutdown()` 只中断空闲线程，正在执行的任务线程会继续运行直到任务自然结束；任务结束后，线程在下一次 `getTask()` 时感知到状态为 `SHUTDOWN` 且队列已空，才会自行退出。

   ### 3.3 shutdownNow()：尽力强制关闭

   `shutdownNow()` 与 `shutdown()` 的关键差异如下：

   - **不再接受新任务**，与 `shutdown` 相同。
   - **清空任务队列**，并返回队列中从未开始执行的任务列表（`Listrunnable`）。
   - **尝试中断正在执行的任务**：通过 `Thread.interrupt()` 发出中断信号，但**不保证任务一定停止**。如果任务吞掉了中断信号或根本不对中断做出响应，它们可能永远不会终止。
   - **不阻塞调用线程**。

   `shutdownNow()` 是“尽力而为”的关闭，适合需要尽快释放资源的场景，但前提是任务本身是响应中断的。

   ### 3.4 awaitTermination()：等待真正关闭

   `shutdown()` 和 `shutdownNow()` 都**不阻塞**，调用后线程池可能仍在运行。如果需要等待线程池**彻底终止**，使用 `awaitTermination(long timeout, TimeUnit unit)`：

   - **阻塞调用线程**，直到以下任一条件发生：线程池进入 `TERMINATED`、超时、或当前线程被中断。
   - 返回 `true` 表示线程池已终止；返回 `false` 表示超时仍未终止。
   - **不会改变线程池状态**，只负责等待。

   ### 3.5 三者的对比


   |                    | `shutdown()`         | `shutdownNow()`    | `awaitTermination()` |
   | ------------------ | -------------------- | ------------------ | -------------------- |
   | 接受新任务         | 否                   | 否                 | 不影响               |
   | 处理队列中已有任务 | **是**               | **否，清空并返回** | —                   |
   | 中断正在执行的任务 | 否（仅中断空闲线程） | **尝试中断**       | —                   |
   | 阻塞调用线程       | 否                   | 否                 | **是**               |
   | 返回未执行任务列表 | 否                   | **是**             | —                   |

   ### 3.6 推荐的优雅关闭模板

   生产代码中不建议只调用 `shutdown()` 就结束，因为如果某个任务卡死，线程池可能永远无法终止。推荐的两阶段关闭模式是：

   ```java
   executor.shutdown();                          // 第一阶段：拒新，等旧任务
   try {
       if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
           executor.shutdownNow();               // 超时 → 第二阶段：强制关闭
       }
   } catch (InterruptedException e) {
       executor.shutdownNow();                   // 等待被中断 → 强制关闭
       Thread.currentThread().interrupt();       // 恢复中断标志
   }
   ```
   这个模板同时解决了两个问题：给正常任务一个合理的完成窗口，同时避免因个别不响应中断的任务导致关闭无限阻塞。

   ### 3.7 特殊子类的补充：ScheduledThreadPoolExecutor

   `ScheduledThreadPoolExecutor` 对 `shutdown()` 的行为做了额外策略控制。通过 `setExecuteExistingDelayedTasksAfterShutdownPolicy` 和 `setContinueExistingPeriodicTasksAfterShutdownPolicy` 可以决定延迟任务和周期任务在 `shutdown()` 后是否继续执行。默认策略下，延迟任务可能继续执行，只有 `shutdownNow()` 或显式将策略设为 `false` 才会终止它们。使用定时线程池时需要特别注意这一点，否则可能出现在 `shutdown()` 后程序依然不退出的情况。

   **一句话总结**：线程池默认使用非守护线程，不关闭就会阻止 JVM 退出；`shutdown()` 是“拒新做完旧”的优雅关闭，`shutdownNow()` 是“清队中断”的尽力强制关闭，生产环境应使用 `shutdown() + awaitTermination + 必要时 shutdownNow()` 的两阶段模板来保证可控退出。
