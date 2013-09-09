`synch2` synchronization primitive
===============================================================================

Protocol
-------------------------------------------------------------------------------

    # #########################################################################
    # start
    # #########################################################################

    # mark this thread as active
    active[i] = true

    # #########################################################################
    # guard
    # #########################################################################
    # check whether the other thread is active
    if active[i + 1]:

        # mark this to be in wait 
        wait[i] = true

        # wait until the other thread is deactived or wakes this up
        while active[i + 1] and wait[i]:
            # if this is waker either wake up the other thread or acknowledge waker change
            if waker == i:
                # if waker change is not in progress wakeup other thread
                if not waker_change: wait[i + 1] = false
                    
                # otherwise acknowledge it
                else: waker_change = false

            yield

    
    # #########################################################################
    # finish
    # #########################################################################

    # mark this selected 
    selected[i] = true

    # mark this deselected
    selected[i] = false

    # change waker if this is not the one
    if not waker == i:
       # change waker to this thread
       waker = i

       # acknowledge other thread about change
       if active[i + 1]:
           # acknowledge
           waker_change = true

           # wait until other thread is either deactivated or acknowledges 
           while waker_change: yield

           # for the case when the other thread became inactive
           waker_change = false

    # mark this inactive
    active[i] = false

The evolution of the protocol
-------------------------------------------------------------------------------

### The simplest ###

The simplest, though unsafe protocol is the following:

    # mark this selected 
    selected[i] = true

    # mark this deselected
    selected[i] = false

Thi is obviously wait-free but not safe.

### The safe ###

The problem with the simplest protocol is that it does not guard against the case when the two threads enter the protocol and hence are selected in parallel. So **lets add a guard before selection which checks whether the other thread is active or not**:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # if yes, then wait until the other thread becomes inactive
        while active[i + 1]: yield

    # mark this selected 
    select[i] = true

    # mark this deselected
    select[i] = false

    # mark this inactive
    active[i] = false

This one is safe, that is it guarantees that threads are never selected in parallel. However it is obviously not wait-free: if both thread enters the guard simultaneously then they will block forever. But before repairing this weaknest, lets see why this protocol is safe. The reason is the following: 

**If two threads enters the protocol in parallel then one of them detects the other as active, hence goes into the wait loop.** Informally speaking the thread that activated itself later will detect the other as active. A bit more formally:

Assume that thread 0 did not detect thread 1 as active. This means that when  thread 0 issued its check `if active[i + 1]`, at that time thread 1 was not yet active (maybe it has not yet invoked the protcol or just started marking itself active). One thing is sure that it has not yet executed its own guard. Formally the statements has the following historical order:

    active[0] = true < if active[1] < if active[0]

(where `<` means that the statement on the left was issued earlier then the one on the right). From the above history it is obvious that the last guard returns true, since thread 0 activated itself earlier. 

*Note that we must also assume that thread 0 is still active when thread 1 issues its guard, otherwise it may find thread 0 as inactive.*

### The safe and almost wait-free ###

The problem with the above safe protocol is the case when both thread executes its guard at the same time. (At least to me) this seems to be a problem with symmetry: the system cannot really differentiate between the two threads when staying in the wait loop. So **lets break the symmetry: give thread 0 a special role, the 'waker', whose only task is to wake up thread 0**:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # in case of thread 1 mark this as waiting
        if i == 0: wait1 = true

        # if yes, then wait until the other thread becomes inactive or 
        # in case of thread 1: until thread 0 wakes this up
        while active[i + 1] and (i == 0 or wait1): 
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

### The safe and wait-free ###

It looks like that by breaking the symmetry we became unfair to thread 0. So lets rebuild the symmetry in a dynamic way. We make the waker role dynamic: different thread will play the role in different rounds. An initial protocol might look like this:

    # mark this thread as active
    active[i] = true

    # check whether the other thread is active
    if active[i + 1]:

        # mark this to be in wait 
        wait[i] = true

        # wait until the other thread is deactived or wakes this up
        while active[i + 1] and wait[i]:
            # if this is the waker thread then wake up the other one
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

This is almost good. The only problem is that the role change is not safe. Think of the following scenario:

1. Originally thread 0 plays the waker role and it is in the wait loop just before issuing another wakeup, that is: it already issued `if waker == 0` but not yet executed `wait[1] = false`
1. Thread 1 changes the role and becomes the waker, then it 'becomes very busy': before thread 0 could execute its next statement (`wait[1] = false`): 
1. thread 1 reenters the protocol
1. detects that thread 0 is active hence 
1. enter the wait loop as well - and this is a problem since now there are two 'wakers' in the wait loop: there's thread 1 the real waker and thread 0 the fake waker, who mistakenly thinks that it is the waker - that is:
1.  thread 1 might `yield`, then
1.  thread 0 executes `wait[1] = false` which marks thread 1 to not wait, then 
1.  thread 0 might `yield` and
1.  thread 1 execute `wait[0] = false` which marks thread 0 to not wait

Hence both thread might leave the wait loop in parallel and hence selected in parallel.

In order to make the role change safe, the protocol adds a synchronization between the two threads: 

1. the thread who initiated the change yields that a role change is in progress, then it waits for an acknowledgement 
1. the thread who was previously the waker acknowledges the role change, when it detects it

*yielding role change*:

-- if the previous waker is active, then the changer tells it that a role change is in progress, technically it sets the corresponding flag to true:

     # acknowledge other thread about change
     if active[i + 1]:

         # acknowledge
         waker_change = true

-- then the new waker goes into a wait loop until the previous one acknowledges the change:

         # wait until other thread acknowledges 
         while active[i + 1] and waker_change: yield

*acknowledging role change*

-- when the previous waker (staying in the guard) detects the role change, acknowledges it, hence breaks the other thread's wait loop:

    # if waker change is not in progress wakeup other thread
    if not waker_change: wait[i + 1] = false
                    
    # otherwise acknowledge it
    else: waker_change = false

That's all.

Properties
-------------------------------------------------------------------------------

### Internal properties ###

#### enter guard ####

1. **either thread enters**
 
#### exit guard ####

1. **how exits?** other thread is either deactivated or this is waken up by setting wait[i] to false
1. **waker and wake up by setting wait[i] to false**
   1. only the other thread can set this wait flag to false
   1. wakeup can only happen in the guard
   1. only the waker thread can wakeup
   1. the waker role is exclusive
   1. if a thread was not the waker when entered the guard then it becomes the waker just before deactivating
   1. if a thread exits the guard when the other thread is still active, then this thread was not the waker and left the other thread in the guard as a waker

#### general ####

1. if a thread exits or avoids the guard **it did not wake the other thread up**
1. if this thread was still active when the other thread deactivated then either (1) this thread will deactivate before the other reenters or **(2) the other thread enters the guard**

### Wait-free ###

### Safe ###

