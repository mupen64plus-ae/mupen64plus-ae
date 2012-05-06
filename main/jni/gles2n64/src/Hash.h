#ifndef __HASH_H__
#define __HASH_H__

#include <stdlib.h>

template<typename T>
class HashMap
{
public:
    void init(unsigned power2)
    {
        _mask = (1 << power2) - 1;
        _hashmap = (T**)malloc((_mask+1) * sizeof(T*));
        reset();
    }

    void destroy()
    {
        free(_hashmap);
    }

    void reset()
    {
        memset(_hashmap, 0, (_mask+1) * sizeof(T*));
    }

    void insert(unsigned hash, T* data)
    {
        _hashmap[hash & _mask] = data;
    }

    T* find(unsigned hash)
    {
        return _hashmap[hash & _mask];
    }

protected:
    T **_hashmap;
    unsigned _mask;
};

#endif
