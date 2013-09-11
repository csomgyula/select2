`sync2` synchronization primitive
===============================================================================

Protocol
-------------------------------------------------------------------------------


### States ###

States are the ones those are in the pseudo code, ie.:

* `active[i]` - marks whether thread i is selected
* `wait[i]` - marks whether thread i is to wait
* `waker` - shows which thread is the waker
* `waker_change` - marks whether there is a waker change in progress
* `selected[i]` - marks whether thread i is selected

### Pseudo code ###

    # #######################################################################
    # guard stage
    # #######################################################################

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # if the other thread is active then mark this to wait 
        wait[i] = true

        # wait until the other thread is deactived or marks this thread to not wait
        while active[i + 1] and wait[i]:

            # if this is waker either wake up the other thread or acknowledge waker change
            if waker == i:

                # if waker change is not in progress mark the other thread to not wait
                if not waker_change: wait[i + 1] = false
                    
                # otherwise if change is in progress then acknowledge it
                else: waker_change = false

            yield

    # #######################################################################
    # selection stage
    # #######################################################################

    # mark this selected 
    selected[i] = true

    # mark this deselected
    selected[i] = false

    # change waker if this is not the one
    if not waker == i:

       # change waker to point to this thread
       waker = i

       # tell the other thread about the change
       if active[i + 1]:

           # yield the change
           waker_change = true

           # wait until the other thread acknowledges the change
           while waker_change: yield

    # mark this inactive
    active[i] = false

Protocol description
-------------------------------------------------------------------------------

Lets go through the protocol "building it up starting from ground":

### The simplest protocol ###

The simplest, though unsafe protocol is the following:

    # mark this selected 
    selected[i] = true

    # mark this deselected
    selected[i] = false

This is obviously wait-free but not safe.

### The safe one ###

The problem with the simplest protocol is that it does not guard against the case when the two threads enter the protocol at the same time and hence are selected in parallel. So **lets add a guard before the selection which checks whether the other thread is active or not**:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # if the other thread is active then wait until the other thread becomes inactive
        while active[i + 1]: yield

    # mark this selected 
    select[i] = true

    # mark this deselected
    select[i] = false

    # mark this inactive
    active[i] = false

This one is safe, that is it guarantees that threads are never selected in parallel. However it is obviously not wait-free: if both thread enters the guard simultaneously then they will block forever. But before repairing this weaknest, lets see why this protocol is safe. The reason is the following: 

**If two threads enters the protocol in parallel then one of them detects the other as active, hence goes into the wait loop.** Informally speaking the thread that activated itself later will detect the other as active. A bit more formally:

Assume that thread 0 did not detect thread 1 as active. This means that when  thread 0 issued its check `if active[1]`, at that time thread 1 was not yet active (maybe it has not yet invoked the protcol or just started marking itself active). One thing is sure that thread 1 has not yet executed its own guard. Formally the statements has the following historical order:

    active[0] = true < if active[1] < if active[0]

(where `<` means that the statement on the left was issued earlier then the one on the right). From the above history it is obvious that the last guard returns true, since thread 0 activated itself earlier. 

*Note that we must also assume that thread 0 is still active when thread 1 issues its guard, otherwise it may find thread 0 as inactive.*

### The safe and almost wait-free one ###

The problem with the above safe protocol is the case when both thread executes its guard at the same time. (At least to me) this seems to be a problem with symmetry: the system cannot really differentiate between the two threads when staying in the wait loop. So **lets break the symmetry: give thread 0 a special role, the 'waker', whose only task is to wake up thread 0**:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # if the other thread is waiting then in case of thread 1 mark this to wait
        if i == 0: wait1 = true

        # wait until the other thread becomes inactive or thread 0 marks this thread to not wait (in case of thread 1)
        while active[i + 1] and (i == 1 and wait1): 
            if i == 0: wait1 = false
            yield

    # mark this selected 
    select[i] = true

    # mark this deselected
    select[i] = false

    # mark this inactive
    active[i] = false

This protocol is still safe. To put it simply either thread will go into the wait loop and then does not break until (1) the other thread becomes inactive, or (2) in case of thread 1: it is not waken up. However in the latter case, thread 0, the 'waker' will stay in the wait loop until thread 1 becomes inactive.

The problem with this protocol is that it is not wait-free, only almost wait-free. There is a non zero, though low probability that thread 0 blocks forever. This could happen when thread 1 is very busy, ie. a new activation follows a previous deactivation (almost) immediately, think of the following history or such:

    active[1] = false <  active[1] = true < if active[1] < ... < active[1] = false <  active[1] = true

It could happen that thread 0 is unable to detect thread 1's idle period, hence it will see thread 1 as always being active and stay in the wait loop forever. 

### The final one ###

It looks like that by breaking the symmetry we became unfair to thread 0. So lets rebuild the symmetry in a dynamic way and make the waker role dynamic: **different threads will play the 'waker' role in different rounds**. An initial protocol might look like this:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # if the other thread is active then mark this to wait 
        wait[i] = true

        # wait until the other thread is deactived or marks this thread to not wait
        while active[i + 1] and wait[i]:

            # if this is the waker thread then mark the other one to not wait
            if waker == i: wait[i + 1] = false

            yield

    # mark this selected 
    selected[i] = true

    # mark this deselected
    selected[i] = false

    # change waker if this is not the one
    if not waker == i: waker = i

    # mark this inactive
    active[i] = false

This is almost good. The problem is that the role change is not yet safe. Think of the following scenario:

1. Originally thread 0 plays the waker role and it is in the wait loop just before issuing another wakeup, that is: it already issued `if waker == 0` but not yet executed `wait[1] = false`
1. Thread 1 changes the role and becomes the waker, then it 'starts executing statements very busily': before thread 0 would ever execute its next statement (`wait[1] = false`) thread 1 
1. reenters the protocol
1. detects that thread 0 is active hence 
1. enters the wait loop as well - and this is a problem since now there are two 'wakers' in the wait loop: there's thread 1 the real waker and thread 0 the fake waker, who mistakenly thinks that it is the waker - that is:
1.  thread 1 executes `wait[0] = false` which marks thread 0 to not wait, now it might slow down and let thread 0 to work, that is:
1.  thread 0 executes `wait[1] = false` which marks thread 1 to not wait
 
Now both thread is marked to not wait, hence might leave the wait loop in parallel and hence selected in parallel.

Formally a history could look like this:

    thread 0: if waker == 0   # which yields true at this time
    thread 1: if not waker == 1: waker = 1
    thread 1: active[1] = false
    thread 1: active[1] = true
    thread 1: if active[0]
    thread 1: wait[1] = true
    thread 1: while active[0] and wait[1] # now both thread is in the wait loop as a waker 
    thread 1: wait[0] = false # marks thread 0 to not wait 
    thread 0: wait[1] = false # marks thread 1 to not wait

In order to make the role change safe, the protocol adds a synchronization between the two threads: 

1. the thread who initiated the change yields that a role change is in progress, then it waits for an acknowledgement 
1. the thread who was previously the waker acknowledges the role change, when it detects it

*yielding role change*:

-- if the previous waker is active, then the changer tells it that a role change is in progress, technically it sets the corresponding flag to true:

     # if the other thread is active then tell it about the change
     if active[i + 1]:

         # yield the change
         waker_change = true

-- then the new waker goes into a wait loop until the previous one acknowledges the change:

         # wait until the other thread acknowledges the change
         while waker_change: yield

*acknowledging role change*

-- when the previous waker (staying in the guard) detects the role change, acknowledges it, hence breaks the other thread's wait loop:

    # if waker change is not in progress then mark the other thread to not wait
    if not waker_change: wait[i + 1] = false
                    
    # otherwise acknowledge the change
    else: waker_change = false

That's all:-)


Protocol properties
-------------------------------------------------------------------------------

The above chapter described the protocol in details, now we try to prove its features that is to say: the protocol is safe and wait-free. The proof will go like this:

1. first we need a preliminary assumption related to states
1. prove some internal properties through a series of lemmas wich will be helpful in the proof
1. finally prove first safety then
1. wait-freeness 

### Assumption ###

The proof builds upon the assumption that the read-write operations of any state provide [`linearizibility`](http://en.wikipedia.org/wiki/Linearizability) (ie. setting and getting `active`, `wait` etc. is consistent):

**Assumption: If a write operation setting a state to a new value finishes before a read invocation, then the read operation must yield the this new value.** Formally:

if a write operation setting a new value (`v`) initiated by thread i finishes before the read operation initiated by (possibly another) thread j:

    invoke_set(i, state, v) < return_set(i, state) < invoke_get(j, state) < return_get(j, state, w)

then the read operation must yield the previously set value:

    v = w

Here I used the following formalism:

* `invoke_set(i, state, v)` means that thread `i` invokes a write operation on the given `state` indicating to set the state to `v`
* `return_set(i, state)` means that the previous write operation finished
* `invoke_get(j, state)` means that thread `j` invokes a read operation on the given `state`
* `return_get(j, state, w)` means that the previous read operation finished and returned `w`

Note that obviously we assume that there was no other write operation between them.

### Internal properties ###

**Lemma 1: If the threads run in parallel, then one of them detects the other as active, hence enters the wait loop of the guard**.

Proof (draft): The thread (say `thread i`) that issued its guard statement later (`if active[i+1]`) will detect the other as active. Because: 

For this thread the following will be true:

     (1) activate[i] < if active[i] <= if active[i+1] < deactivate[i]

From (1) and the Assumption it follows that `active[i+1]` is true, hence `thread i` enters its wait loop.

**Lemma 2: Exit criterias of the guard's wait loop**:

1. A thread might leave the guard's wait loop 
   1. if the other thread is/becomes inactive or 
   2. if the thread is marked to not wait (which should be issued after the thread marked itself to wait)
1. A thread can be marked to not wait
   1. only by the other thread
   1. only when the other thread is in the wait loop
   1. only if the other thread is the waker

Proof: Obvious from the protocol definition.

**Definition: Guard stage, wait-loop, selection stage**

**Lemma 3:**

1. **Waker is safe**
1. **If the protocol is safe (according to Theorem 1) then the waker change is wait-free. That is the wait loop of the waker change does not block forever.**

Proof:

1. Safe because it waits until the other thread acknowledges.
1. When the (non-waker) thread enters the waker-change loop then the other (waker) thread must be active as well. If the protocol is safe then both thread cannot stay at the selection stage. Hence the waker thread must be in the guard stage. Since the non-waker thread starts waiting in every case the waker thread must get into its wait loop and then acknowledge the waker-change.

*TODO: formalize safety, informally it means: if both thread is in the wait loop then only one thread can mark the other to not wait.*

**Corollary: If both thread is in the wait loop at the same time, then the waker thread won't leave the wait loop until the other is deactivated**

*Proof*: According to the previous lemma the waker role is safe. Hence only the waker thread will mark the non-waker thread to not wait, the non-waker can't do that. That is: the non-waker thread will leave its wait loop and at that time  the waker thread's wait flag remains true. Hence the waker thread can't leave its wait loop until either the non-waker thread is deactivated or it reenters the protocol. In both case the non-waker thread is deactivated before the waker thread leaves its wait loop.

### Safe ###

**Theorem 1: The synch2 protocol is safe that is threads are never selected simultaneously. In fact a bit more is true: threads cannot stay at the selection stage simultaneously.** 

Proof: Indirectly assume that both thread entered the selection stage and are still active at some point in time. 

(1) Take that thread (say `thread i`) which left the guard stage later (or at least not sooner) than the other one. The last statement it issued in the guard stage could be one of the following ones:

*Case 1 - In case it did not enter the wait loop then the last guard statement was:*

    if active[i+1]

and it evaluated to false.

*Case 2 - In case it broke the wait loop when the other thread became inactive then the last guard statement was:*

    if active[i+1]: break

*Case 3 - In case it broke the wait loop after the other thread marked it to not wait:*

    if not wait[i]: break

The first two case is impossible, because `thread i+1` left the guard stage sooner (or at least not later), hence it was still active when `thread i` left the guard stage. 

Hence `thread i` which left the guard stage later (or at least not sooner) was in its wait loop just before leaving the guard stage and left the wait loop because its wait flag was set to false.

(2) Due to (1) previously `thread i+1` must have marked `thread i` to not wait  when it was already in its wait loop (more precisely when it already marked itself to wait) and `thread i` left the wait loop after it was marked to not wait. Formally:

    wait[i] = true < wait[i] = false < if not wait[i]: break

(3) Obviously at the time when `thread i+1` marked the other thread to not wait it must have been in a wait loop as well.

Due to (1, 3) both threads were in a wait loop, hence according to Lemma the waker thread could not leave its wait loop until the non-waker became inactive. Due to (2) this case `thread i+1` was the waker, hence it did not leave its wait loop until `thread i` was deactivated. Contradiction.

It immediately follows from Lemma and Theorem 1 that:

**Corollary: Waker change is wait-free**


### Wait-free ###


**Theorem 2: The synch2 protocol is wait-free: if a thread enters the protocol it will finish in finite steps.** 

Proof (draft):

The waker change is wait free. Enough to prove that it does not block in the guard.

Indirectly if a thread blocks in the guard then the other thread must block as well or reenter infinite many times - otherwise this thread wakes up on the idle state of the other thread. If the other thread blocks as well that is a contradiction since one is waker who wakes up the other. It the other thread reenters infinitely then once it becomes a waker and wakes up the thread, which is a contradiction again.