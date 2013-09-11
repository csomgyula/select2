`sync2` synchronization primitive
===============================================================================

Protocol
-------------------------------------------------------------------------------

Threads are numbered as `0` and `1` in the protocol.


_Note that `i + 1` means `(i+1) % 2` in the following sections._

### States ###

states:

* `active[i]: boolean` - marks whether `thread i` is selected
* `wait[i]: boolean` - marks whether `thread i` is to wait
* `waker: 0..1` - shows which thread is the waker
* `waker_change: boolean` - marks whether there is a waker change in progress
* `selected[i]: boolean` - marks whether `thread i` is selected (useful for debugging purposes)

initial states:

* `active[i] = {false, false}`
* `wait[i] = {false, false}`
* `waker = 0`
* `waker_change = false`
* `selected[i] = {false, false}`

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

            # if this is waker wake up the other thread
            if waker == i: wait[i + 1] = false
                    
            # if waker change is in progress then acknowledge it
            if waker_change: waker_change = false

            yield

    # #######################################################################
    # selection stage
    # #######################################################################

    # mark this selected 
    selected[i] = true

    # mark this unselected
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

    # mark this unselected
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

    # mark this unselected
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

    # mark this unselected
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
1. finally prove the safety feature and then
1. wait-freeness 

### Assumption ###

The proof builds upon the assumption that the read-write operations of states are safe and wait-free, that is setting and getting `active`, `wait`, etc. states provide [`linearizibility`](http://en.wikipedia.org/wiki/Linearizability):

**Assumption: If a write operation yielding a new value for a state finishes before a read invocation, then the read operation must return this new value.** Formally:

if a write operation setting a new value (`v`) initiated by `thread i` finishes before the read operation initiated by (possibly another) `thread j`:

    invoke_set(i, state, v) < return_set(i, state) < invoke_get(j, state) < return_get(j, state, w)

then the read operation must return the previously set value:

    v = w

*Note that obviously we assume that there was no other write operation between them.*

Here I used the following formalism:

* `invoke_set(i, state, v)` means that thread `i` invokes a write operation on the given `state` indicating to set the state to `v`
* `return_set(i, state)` means that the previous write operation finished
* `invoke_get(j, state)` means that thread `j` invokes a read operation on the given `state`
* `return_get(j, state, w)` means that the previous read operation finished and returned `w`


### Internal properties ###

**Definition 1: Entering and exiting the guard's wait loop:** 

*Entering the guard wait loop* means that the guard check evaluated to true: `active[i+1]` is true and the associated thread marked itself to wait: `wait[i] = true`. Formally it is written as:

    (1) entered_guard_wait(i) := wait[i] = true finished

*Leaving the guard's wait loop* means that the condition evaluated to false: `not active[i+1] or not wait[i]`. Formally it is written as:

    (2) exited_guard_wait(i) := active[i+1] and wait[i] evaluated to false


**Definition 2: Guard and selection stage:** 

*Guard stage* means that the thread already activated itself (or at least invoked the corresponding write operation on `active[]`) but has not yet finished the last guard statement. Here the last guard statement for `thread i` could be either:

Case 1 - In case it did not enter the wait loop then the last guard statement is: `if active[i+1]` and this statement must evaluate to false.

Case 2 - In case it broke the wait loop when either the other thread became inactive or the wait flag is set to false then the last guard statement is the evaluation of the wait condition: `active[i+1] and wait[i]` and the condition must evaluate to false that is either `not active[i+1]` or `not wait[i]`.

*Selection stage* means that the thread is active and is not in the guard stage. In other words it already finished its last guard statement but not yet deactivated itself (at least the write operation has not yet finished).


**Definition 3: ordering of statements and states**: 

* `statement1 < statement2` means that `statement2` was invoked later
* `statement1 <= statement2` means that `statement2` was invoked not sooner (later or at the same time)
* `statement1 << statement2` means that `statement2` was invoked later then `statement1` finished 
* The ordering is extended to cover states as well. In this case by 'statement' I mean the corresponding write operation. For instance `state << statement` means that the `statement` was invoked later then the write operation of `state` finished.

From the definition it follows that `<` and `<<` provides a partial ordering of states and statements, and `<=` provides a total ordering (except for the fact that the latter is reflexive).


**Lemma 1: If the threads run in parallel, then one of them detects the other as active, hence enters the wait loop of the guard**. Formally:

if threads executes their guard checks before the other thread deactivates itself:

    (1) if active[i+1] << activate[i+1] = false : i = 0, 1

then one of the threads detects the other thread active:

    (2) active[i+1] is true for i = 0 or 1

hence enters the guard wait loop:

    (3) if active[i+1] < wait[i] = true

Proof: The thread (say `thread i`) that issued its guard statement later (`if active[i+1]`) will detect the other as active. Details: 

Take that thread (say `thread i+1`) that issued its guard check later (or at least not sooner) than the other one. Formally:

    (4) if active[i+1] <= if active[i]

Due to (1, 4) and since guard check is executed after thread activation:

    (5) active[i] = true << if active[i+1] <= if active[i] << active[i]=false

From (5) it follows that:

    (6) active[i] = true << if active[i]

In words: the activation of `thread i` finished before `thread i+1` invoked its guard check. 

From (6) and the Assumption it follows that `active[i]` was already set to true when `thread i+1` evaluated its guard check and not yet set to false, hence it detected the other thread as active and entered its wait loop.


**Lemma 2: Exit criterias of the guard's wait loop**:

1. A thread might leave the guard's wait loop 
   1. if the other thread is/becomes inactive or 
   2. if the thread is marked to not wait (which should be issued after the thread marked itself to wait)
1. A thread can be marked to not wait
   1. only by the other thread
   1. only when the other thread is in the wait loop
   1. only if the other thread is the waker

Proof: Obvious from the protocol definition.


**Lemma 3:**

(i) **Waker is safe**. Formally:
 
If both thread is in its guard wait loop at some point in time and assuming that `thread i` is the waker

    (1) waker == i and wait[i] = true and wait[i+1] = true

then after that the non-waker thread cannot execute a wakeup statement until it leaves the wait loop:

    (2) entered_guard_wait(j) < wait[j] = false < exited_guard_wait(j) => i == j


(ii) **If the protocol is safe (according to Theorem 1) then the waker change is wait-free. That is the wait loop of the waker change cannot not block forever.**

Proof:

**(i) Safe**:

To put it simply the waker-change protocol is safe because the thread who changes waits until the other thread acknowledges the change. Details:

(1) Indirectly assume that both thread is in its guard wait loop at some point in time (lets call this time the *origo*) and after that the non-waker (say `thread i`) executes a wakeup statement within the same loop. Hence the indirect assumption can be formally written as:

    entered_guard_wait(i) <= origo < wait[i+1] = false < exited_guard_wait(i)

(2) In order to execute a wakeup statement `thread i` should have previously evaluated itself as the waker. Since at the *origo* point this condition was already false, the evaluation must have happened before. Formally:

    if waker == i <= origo < wait[i+1] = false

(3) At the time of the above check, the `waker` value was `i`. Hence the waker value must have been changed to `i+1` after the above waker-check and before the *origo* point (when it was already `i+1`). Formally:

    if waker == i <= waker = i+1 <= origo

(4) The above waker-change was issued by `thread i+1`. At that time it was after its *guard stage*, hence the waker-change must have happened before it reentered the protocol again and went into the guard wait loop. 

Hence the above waker-change happened before the *origo* point:

    waker = i+1 << origo

(5) Due to the indirect assumption `thread i+1` left its *selection stage* (did not block in the waker-change loop) and reentered the protocol again. It follows that  after the waker-change and before the *origo* point `thread i+1` deactivated itself :

    waker = i+1 << active[i+1] = false << origo

(6) It follows from (3, 5) that 

    if waker == i <= waker = i+1 << active[i+1] = false << origo < wait[i+1] = false

(7) During this time period (between waker change and when `thread i+1` reentered the protocol) `thread i` was and remained active since at the *origo* point both thread was in its wait loop (or take this: the first and the last statement above was issued by `thread i` within the same wait loop). 

It follows that when changing the waker value, then `thread i+1` should have detected the other thread active. Hence it went into the waker-change wait loop and stayed there until `thread i` acknowledged the change.

However `thread i` executed its wake up statement before `thread i+1` was acknowledged, hence before the origo point. Formally:

    wait[i+1] == false << waker_change = false < active[i+1] = false << origo

This contradicts to the indirect assumption that the wake up statement happened after the origo point. Formally both `wait[i+1] == false << origo` and `origo < wait[i+1] == false` holds which is contradiction.

**(ii) Wait-free if protocol is safe**: Indirectly assume that a thread (say `thread i`) is blocked in the waker-change wait loop.

Since `thread i` entered the wait loop, hence just before that the other thread was active (because before entering the loop the thread checks whether the other one is active). If the protocol is safe then according to Theorem 1 both thread cannot stay at the selection stage, hence the other thread must have been in the guard stage when `thread i` issued its `if active[i+1]` check.

Since according to the indirect asumption `thread i` is blocked in the waker-change wait loop, according to Theorem 1 the other thread must be blocked in the guard wait loop. Otherwise if it left the *guard stage* then it would contradict Theorem 1 (*both thread cannot stay at the selection stage*). 

Hence `thread i+1` is blocked in its guard wait loop. Then and there it will detect that waker-change is in progress, hence acknowledge it which then breaks the wait loop of `thread i`. Contradiction.

From the above lemma it follows that:

**Corollary 1: If both thread is in the wait loop at the same time, then the waker thread won't leave the wait loop until the other is deactivated**

*Proof*: According to the previous lemma the waker role is safe, hence only the waker thread will mark the non-waker thread to not wait, the non-waker can't do that. Hence the non-waker thread will leave its wait loop and at that time  the waker thread's wait flag remains true. Hence the waker thread can't leave its wait loop until either the non-waker thread is deactivated or its wait flag is set to false. In both case the non-waker thread is deactivated before the waker thread wakes up.


### Safe ###

**Theorem 1: The synch2 protocol is safe that is threads are never selected simultaneously. In fact a bit more is true: threads cannot stay at the selection stage simultaneously.** 

Proof: Indirectly assume that both thread entered the *selection stage* and are still active at some point in time. 

(1) Take that thread (say `thread i`) which left the guard stage later (or at least not sooner) than the other one. The last statement it issued in the *guard stage* could be one of the following ones:

*Case 1 - In case it did not enter the wait loop* then the last guard statement was

    if active[i+1]

and it evaluated to false.

*Case 2 - In case it broke the wait loop when either the other thread became inactive or the wait flag is set to false* then the last guard statement was the evaluation of the wait condition: `active[i+1] and wait[i]` and the condition evaluated to false. There are two possible subcases:

*Case 2.1 - In case it broke the wait loop after the other thread became inactive, then:*

    active[i+1]

evaluated to false.

*Case 2.2 - In case it broke the wait loop when the other thread was still active and after the other thread marked it to not wait, then:*

    wait[i]

evaluated to false.

The first two cases (Case 1 and Case 2.1.) is impossible. `thread i+1` left the *guard stage* sooner (or at least not later), hence due to the indirect assumption `thread i+1` was in the *selection stage* when `thread i` left the *guard stage*. 

Hence `thread i` which left the guard stage later (or at least not sooner) was in its wait loop just before leaving the *guard stage* and left the wait loop because its wait flag was set to false.

(2) Due to (1) previously `thread i+1` must have marked `thread i` to not wait. This must have been happened after `thread i` already entered its wait loop (ie. after it already marked itself to wait) and before `thread i` left the wait loop. Formally:

    entered_guard_wait(i) < wait[i] = false < exited_guard_wait(i)

(3) Obviously at the time when `thread i+1` marked the other thread to not wait it must have been in a wait loop as well.

Due to (1, 3) both threads were in a wait loop, hence according to Corollary 1 the waker thread could not leave its wait loop until the non-waker became inactive. Due to (2) this case `thread i+1` was the waker, hence it did not leave its wait loop until `thread i` was deactivated. Contradiction, since `thread i+1` left the guard stage earlier then `thread i`.


It immediately follows from Lemma 3 and Theorem 1 that:

**Corollary 2: Waker change is wait-free**


### Wait-free ###

**Theorem 2: The synch2 protocol is wait-free: if a thread enters the protocol it will finish in finite steps.** 

Proof (draft):

Due to the above Corollary 2 the waker-change is wait free. Hence it is enough to prove that a thread cannot block in the guard wait loop forever.

Indirectly assume that this thread (say `thread i`) blocks in the guard wait loop.

It follows then that the other thread must either block as well or reenter the protocol infinite many times. Otherwise this thread wakes up on the idle state of the other thread. 

*Case 1 - If the other thread blocks for ever as well*, then it must block in the guard wait loop as well, since the waker-change is wait-free. This is a contradiction since this means that both thread blocks in the guard wait loop. However one thread is the waker who will wake up the other. 

*Case 2 - If the other thread reenters the protocol infinite many times* then eventually it enters the protcol as a waker. Since `thread i` is blocked in its loop, the other thread will detect it active and go into its wait loop as well. However since the other thread is the waker it will wake up `thread i`, which is a contradiction again.

