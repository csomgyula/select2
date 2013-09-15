Bookmark
==

**Low level multithreading APIs, concepts**:

* thread concepts, Java and C APIs
* memory consistency
* some background theory on parallel computing

Threads
--

### Java ###

*Core Java multithreading concepts and APIs*

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


### System ###

*Machine and OS level concepts and C APIs*


**Linux scheduler**

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


**OS X, iOS**

[Concurrency Programming Guide](https://developer.apple.com/library/ios/DOCUMENTATION/General/Conceptual/ConcurrencyProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40008091)


**Windows**

[MSDN: Processes and Threads](http://msdn.microsoft.com/en-us/library/windows/desktop/ms684841%28v=vs.85%29.aspx)

[MSDN: Synhcronization](http://msdn.microsoft.com/en-us/library/ms686353%28v=vs.85%29.aspx)

**Common**

[Wikipedia: Thread](http://en.wikipedia.org/wiki/Thread_%28computing%29)

[Intel Guide for Developing Multithreaded Applications](http://software.intel.com/en-us/articles/intel-guide-for-developing-multithreaded-applications/)


Memory consistency
--

*Memory models and consistency with multiple processes*

### Java Memory Model (JMM) ###

[JSR 133 (Java Memory Model) FAQ](http://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html)

[William Pugh: The Java Memory Model](http://www.cs.umd.edu/~pugh/java/memoryModel/)

[Doug Lea: The JSR-133 Cookbook for Compiler Writers](http://gee.cs.oswego.edu/dl/jmm/cookbook.html) (- 2011)

[Brian Goetz: Java theory and practice: Fixing the Java Memory Model, Part 1](http://www.ibm.com/developerworks/java/library/j-jtp02244/index.html) | [Part2](http://www.ibm.com/developerworks/library/j-jtp03304/) (2004)

[Java Language Specification: 17.4. Memory Model](http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4)

[Jaroslav Sevcik and David Aspinal: On Validity of Program Transformations in the Java Memory Model](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.112.1790&rep=rep1&type=pdf) (2008)  
Quote from the abstract:

> On the otherhand, we demonstrate that some other cases of reordering, which claims to be valid, are not generally valid transformations. For example, swapping a normal memory access with a consequent lock can introduce new behaviours, and thus is not a valid transformation. Another example of an invalid transformation is reusing a value of a read for a subsequent read, or an introduction of an irrelevant read. With this analysis, we establish that the JMM is still flawed, because these transformations are performed by hardware and compilers. Even Sun’s Hotspot JVM [20] performs transformations that are not compliant with the JMM.


### System ###

[Wikipedia: CPU cache](http://en.wikipedia.org/wiki/CPU_cache) 

[Relaxed-Memory Concurrency](http://www.cl.cam.ac.uk/~pes20/weakmemory/index.html)

[x86-TSO: A Rigorous and Usable Programmer's Model for x86 Multiprocessors](http://www.cl.cam.ac.uk/~pes20/weakmemory/cacm.pdf) (2010) Peter Sewell, Susmit Sarkar, Scott Owens, Francesco Zappa Nardelli, Magnus O. Myreen  
Abstract:

> Exploiting the multiprocessors that have recently become ubiquitous requires high-performance and reliable concurrent systems code, for concurrent data structures, operating system kernels, synchronisation libraries, compilers, and so on. However, concurrent programming, which is always challenging, is made much more so by two problems. First, real multiprocessors typically do not provide the sequentially consistent memory that is assumed by most work on semantics and verification. Instead, they have relaxed memory models, varying in subtle ways between processor families, in which different hardware threads may have only loosely consistent views of a shared memory. Second, the public vendor architectures, supposedly specifying what programmers can rely on, are often in ambiguous informal prose (a particularly poor medium for loose specifications), leading to widespread confusion.
> 
> In this paper we focus on x86 processors. We review several recent Intel and AMD specifications, showing that all contain serious ambiguities, some are arguably too weak to program above, and some are simply unsound with respect to actual hardware. We present a new x86-TSO programmer’s model that, to the best of our knowledge, suffers from none of these problems. It is mathematically precise (rigorously defined in HOL4) but can be presented as an intuitive abstract machine which should be widely accessible to working programmers. We illustrate how this can be used to reason about the correctness of a Linux spinlock implementation and describe a general theory of data-race-freedom for x86-TSO. This should put x86 multiprocessor system building on a more solid foundation; it should also provide a basis for future work on verification of such systems.


Parallel programming
--

[Wikipedia: Non-blocking algoritm](http://en.wikipedia.org/wiki/Non-blocking_algorithm)

**Performance**

[Wikipedia: Amdahl's law](http://en.wikipedia.org/wiki/Amdahl%27s_law)

[Future Chips: An example of “When Amdahl’s law is inapplicable?](http://www.futurechips.org/thoughts-for-researchers/appendix-amdahls-law-inapplicable.html)

[Wikipedia: Gustafson's law](http://en.wikipedia.org/wiki/Gustafson%27s_law)

**Modelling parallel processes**

[Wikipedia: I/O automaton](http://en.wikipedia.org/wiki/I/O_automaton)