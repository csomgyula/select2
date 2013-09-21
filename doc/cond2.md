`cond2` synchronization template
==


states
--

`condY: Object` - the associtated monitor with condition y

`waitY` - the wait flag for condition y

`x` - value of the y flag

methods
--

`getX()` - get the value of the `x` flag

`setX(v: boolean)` - set the value of the `x` flag, pseudo code

    # set the value
    x = v

    # repeat for all y

        # if there's a wait Y on x and the condition Y became false
        if waitY and not isY():

            # unset the wait Y flag
            waitY = false

            # notification is synchronized
            synchronized(condY):

                # notify the waiting thread
                condY.notify

`isY()` - the condition expression, could be any boolean expression

`waitWhileY()` - wait while condition y is true

    # wait only if the condition is true
    if isY():

        # mark waiting
        waitY = true

        # recheck the condition
        if isY():

            # wait while the condition is true
            while isY():

                # wait is synchronized
                synchronize(condY):

                    # recheck whether to wait
                    if isY():

                        # wait
                        condY.wait

        # if the condition so far became true
        else:

            # not waiting
            waitY = false
