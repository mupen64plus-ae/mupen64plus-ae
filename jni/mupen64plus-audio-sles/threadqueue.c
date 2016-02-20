#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <sys/time.h>
#include "threadqueue.h"
#include <android/log.h>


#define MSGPOOL_SIZE 256

struct msglist {
    struct threadmsg msg;
    struct msglist *next;
};

static inline struct msglist *get_msglist(struct threadqueue *queue)
{
    struct msglist *tmp;

    if(queue->msgpool != NULL) {
        tmp = queue->msgpool;
        queue->msgpool = tmp->next;
        queue->msgpool_length--;
    } else {
        tmp = malloc(sizeof *tmp);
    }

    return tmp;
}

static inline void release_msglist(struct threadqueue *queue,struct msglist *node)
{

    if(queue->msgpool_length > ( queue->length/8 + MSGPOOL_SIZE)) {
        free(node);
    } else {
        node->msg.data = NULL;
        node->msg.msgtype = 0;
        node->next = queue->msgpool;
        queue->msgpool = node;
        queue->msgpool_length++;
    }
    if(queue->msgpool_length > (queue->length/4 + MSGPOOL_SIZE*10)) {
        struct msglist *tmp = queue->msgpool;
        queue->msgpool = tmp->next;
        free(tmp);
        queue->msgpool_length--;
    }
}

int thread_queue_init(struct threadqueue *queue)
{
    int ret = 0;
    if (queue == NULL) {
        return EINVAL;
    }
    memset(queue, 0, sizeof(struct threadqueue));
    ret = pthread_cond_init(&queue->cond, NULL);
    if (ret != 0) {
        return ret;
    }

    ret = pthread_mutex_init(&queue->mutex, NULL);
    if (ret != 0) {
        pthread_cond_destroy(&queue->cond);
        return ret;
    }

    return 0;

}

int thread_queue_add(struct threadqueue *queue, void *data, long msgtype)
{
    struct msglist *newmsg;
    pthread_mutex_lock(&queue->mutex);
    newmsg = get_msglist(queue);
    if (newmsg == NULL) {
        pthread_mutex_unlock(&queue->mutex);
        return ENOMEM;
    }
    newmsg->msg.data = data;
    newmsg->msg.msgtype = msgtype;

    newmsg->next = NULL;
    if (queue->last == NULL) {
        queue->last = newmsg;
        queue->first = newmsg;
    } else {
        queue->last->next = newmsg;
        queue->last = newmsg;
    }

        if(queue->length == 0)
                pthread_cond_broadcast(&queue->cond);
    queue->length++;
    pthread_mutex_unlock(&queue->mutex);

    return 0;

}

int thread_queue_get(struct threadqueue *queue, const struct timespec *timeout, struct threadmsg *msg)
{
    struct msglist *firstrec;
    int ret = 0;
    struct timespec abstimeout;

    if (queue == NULL || msg == NULL) {
        return EINVAL;
    }
    if (timeout) {
        struct timespec lsEndTime;
        clock_gettime(CLOCK_REALTIME, &lsEndTime);
        abstimeout.tv_sec = lsEndTime.tv_sec + timeout->tv_sec;
        abstimeout.tv_nsec = lsEndTime.tv_nsec + timeout->tv_nsec;
        if (abstimeout.tv_nsec >= 1000000000) {
            abstimeout.tv_sec++;
            abstimeout.tv_nsec -= 1000000000;
        }
    }

    pthread_mutex_lock(&queue->mutex);

    /* Will wait until awakened by a signal or broadcast */
    while (queue->first == NULL && ret != ETIMEDOUT) {  //Need to loop to handle spurious wakeups
        if (timeout) {

           struct timespec lsStartTime;
           clock_gettime(CLOCK_REALTIME, &lsStartTime);
            ret = pthread_cond_timedwait(&queue->cond, &queue->mutex, &abstimeout);
        } else {
            pthread_cond_wait(&queue->cond, &queue->mutex);

        }
    }
    if (ret == ETIMEDOUT) {
        pthread_mutex_unlock(&queue->mutex);
        return ret;
    }

    firstrec = queue->first;
    queue->first = queue->first->next;
    queue->length--;

    if (queue->first == NULL) {
        queue->last = NULL;     // we know this since we hold the lock
        queue->length = 0;
    }


    msg->data = firstrec->msg.data;
    msg->msgtype = firstrec->msg.msgtype;
        msg->qlength = queue->length;

    release_msglist(queue,firstrec);
    pthread_mutex_unlock(&queue->mutex);

    return 0;
}

//maybe caller should supply a callback for cleaning the elements ?
int thread_queue_cleanup(struct threadqueue *queue, int freedata)
{
    struct msglist *rec;
    struct msglist *next;
    struct msglist *recs[2];
    int ret,i;
    if (queue == NULL) {
        return EINVAL;
    }

    pthread_mutex_lock(&queue->mutex);
    recs[0] = queue->first;
    recs[1] = queue->msgpool;
    for(i = 0; i < 2 ; i++) {
        rec = recs[i];
        while (rec) {
            next = rec->next;
            if (freedata) {
                free(rec->msg.data);
            }
            free(rec);
            rec = next;
        }
    }

    pthread_mutex_unlock(&queue->mutex);
    ret = pthread_mutex_destroy(&queue->mutex);
    pthread_cond_destroy(&queue->cond);

    return ret;

}

long thread_queue_length(struct threadqueue *queue)
{
    long counter;
    // get the length properly
    pthread_mutex_lock(&queue->mutex);
    counter = queue->length;
    pthread_mutex_unlock(&queue->mutex);
    return counter;

}
