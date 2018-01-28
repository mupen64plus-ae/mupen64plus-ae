#ifndef _THREADQUEUE_H_
#define _THREADQUEUE_H_ 1

#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif
/**
 * @defgroup ThreadQueue ThreadQueue
 *
 * Little API for waitable queues, typically used for passing messages
 * between threads.
 *
 */

/**
 * @mainpage
   */

/**
 * A thread message.
 *
 * @ingroup ThreadQueue
 *
 * This is used for passing to #thread_queue_get for retreive messages.
 * the date is stored in the data member, the message type in the  #msgtype.
 *
 * Typical:
 * @code
 * struct threadmsg;
 * struct myfoo *foo;
 * while(1)
 *      ret = thread_queue_get(&queue,NULL,&message);
 *      ..
 *      foo = msg.data;
 *      switch(msg.msgtype){
 *              ...
 *      }
 * }
 * @endcode
 *
 */
struct threadmsg{
        /**
         * Holds the data.
         */
        void *data;
        /**
         * Holds the messagetype
         */
        long msgtype;
        /**
        * Holds the current queue lenght. Might not be meaningful if there's several readers
        */
        long qlength;

};


/**
 * A TthreadQueue
 *
 * @ingroup ThreadQueue
 *
 * You should threat this struct as opaque, never ever set/get any
 * of the variables. You have been warned.
 */
struct threadqueue {
/**
 * Length of the queue, never set this, never read this.
 * Use #threadqueue_length to read it.
 */
        long length;
/**
 * Mutex for the queue, never touch.
 */
        pthread_mutex_t mutex;
/**
 * Condition variable for the queue, never touch.
 */
        pthread_cond_t cond;
/**
 * Internal pointers for the queue, never touch.
 */
        struct msglist *first,*last;
/**
 * Internal cache of msglists
 */
    struct msglist *msgpool;
/**
 * No. of elements in the msgpool
 */
    long msgpool_length;
};

/**
 * Initializes a queue.
 *
 * @ingroup ThreadQueue
 *
 * thread_queue_init initializes a new threadqueue. A new queue must always
 * be initialized before it is used.
 *
 * @param queue Pointer to the queue that should be initialized
 * @return 0 on success see pthread_mutex_init
 */
int thread_queue_init(struct threadqueue *queue);

/**
 * Adds a message to a queue
 *
 * @ingroup ThreadQueue
 *
 * thread_queue_add adds a "message" to the specified queue, a message
 * is just a pointer to a anything of the users choice. Nothing is copied
 * so the user must keep track on (de)allocation of the data.
 * A message type is also specified, it is not used for anything else than
 * given back when a message is retreived from the queue.
 *
 * @param queue Pointer to the queue on where the message should be added.
 * @param data the "message".
 * @param msgtype a long specifying the message type, choice of the user.
 * @return 0 on succes ENOMEM if out of memory EINVAL if queue is NULL
 */
int thread_queue_add(struct threadqueue *queue, void *data, long msgtype);

/**
 * Gets a message from a queue
 *
 * @ingroup ThreadQueue
 *
 * thread_queue_get gets a message from the specified queue, it will block
 * the caling thread untill a message arrives, or the (optional) timeout occurs.
 * If timeout is NULL, there will be no timeout, and thread_queue_get will wait
 * untill a message arrives.
 *
 * struct timespec is defined as:
 * @code
 *      struct timespec {
 *                 long    tv_sec;         // seconds
 *                 long    tv_nsec;        // nanoseconds
 *             };
 * @endcode
 *
 * @param queue Pointer to the queue to wait on for a message.
 * @param timeout timeout on how long to wait on a message
 * @param msg pointer that is filled in with mesagetype and data
 *
 * @return 0 on success EINVAL if queue is NULL ETIMEDOUT if timeout occurs
 */
int thread_queue_get(struct threadqueue *queue, const struct timespec *timeout, struct threadmsg *msg);


/**
 * Gets the length of a queue
 *
 * @ingroup ThreadQueue
 *
 * threadqueue_length returns the number of messages waiting in the queue
 *
 * @param queue Pointer to the queue for which to get the length
 * @return the length(number of pending messages) in the queue
 */
long thread_queue_length( struct threadqueue *queue );

/**
 * @ingroup ThreadQueue
 * Cleans up the queue.
 *
 * threadqueue_cleanup cleans up and destroys the queue.
 * This will remove all messages from a queue, and reset it. If
 * freedata is != 0 free(3) will be called on all pending messages in the queue
 * You cannot call this if there are someone currently adding or getting messages
 * from the queue.
 * After a queue have been cleaned, it cannot be used again untill #thread_queue_init
 * has been called on the queue.
 *
 * @param queue Pointer to the queue that should be cleaned
 * @param freedata set to nonzero if free(3) should be called on remaining
 * messages
 * @return 0 on success EINVAL if queue is NULL EBUSY if someone is holding any locks on the queue
 */
int thread_queue_cleanup(struct threadqueue *queue, int freedata);

#ifdef __cplusplus
}
#endif

#endif
