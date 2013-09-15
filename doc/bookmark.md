Bookmark
==

**Low level multithreading APIs, concepts**


Java
--

**Core Java multithreading concepts and APIs**

[The Java Tutorials: Concurrency](http://docs.oracle.com/javase/tutorial/essential/concurrency/)

[Open JDK 7 source files](http://hg.openjdk.java.net/jdk7/jdk7/jdk/file/9b8c96f96a0f/src/share/classes/java/)

**Thread control**

[Javamex: How threads work](http://www.javamex.com/tutorials/threads/how_threads_work.shtml) | [Thread Scheduling](http://www.javamex.com/tutorials/threads/thread_scheduling.shtml) 

[Javamex: API overview](http://www.javamex.com/tutorials/threads/thread_methods.shtml) | [create](http://www.javamex.com/tutorials/threads/thread_runnable_construction.shtml) | [yield](http://www.javamex.com/tutorials/threads/yield.shtml) | [sleep](http://www.javamex.com/tutorials/threads/sleep.shtml) | [interrupt](http://www.javamex.com/tutorials/threads/thread_interruption.shtml)

[stackoverflow: What are the main uses of yield(), and how does it differ from join() and interrupt()?](http://stackoverflow.com/questions/6979796/what-are-the-main-uses-of-yield-and-how-does-it-differ-from-join-and-interr)

**conditons**

[Javamex: wait, notify](http://www.javamex.com/tutorials/synchronization_wait_notify.shtml)

**volatile**

[Javamex: Volatile](http://www.javamex.com/tutorials/synchronization_volatile_java_5.shtml)


**Java Memory Model (JMM)**

[Doug Lea: The JSR-133 Cookbook for Compiler Writers](http://gee.cs.oswego.edu/dl/jmm/cookbook.html) (-2011)

[JSR 133 (Java Memory Model) FAQ](http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html)

[Brian Goetz: Java theory and practice: Fixing the Java Memory Model, Part 1](http://www.ibm.com/developerworks/java/library/j-jtp02244/index.html) | [Part2](http://www.ibm.com/developerworks/library/j-jtp03304/) (2004)

[Java Language Specification: 17.4. Memory Model](http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4)


System
--

**Machine and OS level concepts and C APIs**

[Wikipedia: Thread](http://en.wikipedia.org/wiki/Thread_%28computing%29)

[Wikipedia: CPU cache](http://en.wikipedia.org/wiki/CPU_cache) 

[Intel Guide for Developing Multithreaded Applications](http://software.intel.com/en-us/articles/intel-guide-for-developing-multithreaded-applications/)

### Linux/Unix ###

[M. Tim Jones: Inside the Linux scheduler](http://www.ibm.com/developerworks/library/l-scheduler/) (2006) | [Inside the Linux 2.6 Completely Fair Scheduler](http://www.ibm.com/developerworks/library/l-completely-fair-scheduler/) (2009)

**POSIX threads**

[Wikipedia: POSIX Threads](http://en.wikipedia.org/wiki/POSIX_Threads)

[Daniel Robbins: POSIX threads explained, Part1](http://www.ibm.com/developerworks/library/l-posix1/index.html) | [Part2](http://www.ibm.com/developerworks/library/l-posix2/) (2000)

[Mark Hays: POSIX Threads Tutorial](http://math.arizona.edu/~swig/documentation/pthreads/) (1998)

[Multi-Threaded Programming With POSIX Threads](http://users.actcom.co.il/~choo/lupg/tutorials/multi-thread/multi-thread.html) (2002)

[Alfred Park: Multithreaded Programming (POSIX pthreads Tutorial)](http://randu.org/tutorials/threads/) (2011)

[Blaise Barney: POSIX Threads Programming](https://computing.llnl.gov/tutorials/pthreads/) (2013)

[Linux POSIX Threads tutorial](http://www.yolinux.com/TUTORIALS/LinuxTutorialPosixThreads.html)

[Wei Dong Xie: Avoiding memory leaks in POSIX thread programming](http://www.ibm.com/developerworks/linux/library/l-memory-leaks/index.html) (2010)


### OS X, iOS ###

[Concurrency Programming Guide](https://developer.apple.com/library/ios/DOCUMENTATION/General/Conceptual/ConcurrencyProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40008091)


### Windows ###

[MSDN: Processes and Threads](http://msdn.microsoft.com/en-us/library/windows/desktop/ms684841%28v=vs.85%29.aspx)

[MSDN: Synhcronization](http://msdn.microsoft.com/en-us/library/ms686353%28v=vs.85%29.aspx)


Theory
--

[Wikipedia: Non-blocking algoritm](http://en.wikipedia.org/wiki/Non-blocking_algorithm)

**Performance**

[Wikipedia: Amdahl's law](http://en.wikipedia.org/wiki/Amdahl%27s_law)

[Future Chips: An example of “When Amdahl’s law is inapplicable?](http://www.futurechips.org/thoughts-for-researchers/appendix-amdahls-law-inapplicable.html)

[Wikipedia: Gustafson's law](http://en.wikipedia.org/wiki/Gustafson%27s_law)

**Modelling parallel processes**  

[Wikipedia: I/O automaton](http://en.wikipedia.org/wiki/I/O_automaton)

