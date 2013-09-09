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
    if active[i + 1]:

        # mark this to be in wait 
        wait[i] = true

        # if waker change is in progress acknowledge it since the initiator might be waiting on it
        if waker_change: waker_change = false

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
    select[i] = true

    # mark this deselected
    select[i] = false

    # change waker if this is not the one
    if not waker == i:
       # change waker to this thread
       waker = i

       # acknowledge other thread about change
       if active[i + 1]:
           # acknowledge
           waker_change = true

           # wait until other thread is either deactivated or acknowledges 
           while active[i + 1] and waker_change: yield

           # for the case when the other thread became inactive
           waker_change = false

    # mark this inactive
    active[i] = false

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

