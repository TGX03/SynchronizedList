package de.tgx03;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A list wrapper supposed to enable a list to be read from in parallel,
 * but still ensures only one write-operation occurs at the same time and also no reads are executed during writing.
 * @param <E> The type of elements in this list
 */
public class SynchronizedList<E> implements List<E> {

    private final List<E> list;
    private final AtomicInteger readers = new AtomicInteger(0);
    private boolean writer = false;

    public SynchronizedList(List<E> list) {
        this.list = list;
    }

    @Override
    public int size() {
        readerWait();
        int result = list.size();
        readers.decrementAndGet();
        return result;
    }

    @Override
    public boolean isEmpty() {
        readerWait();
        boolean result = list.isEmpty();
        readers.decrementAndGet();
        return result;
    }

    @Override
    public boolean contains(Object o) {
        readerWait();
        boolean result = list.contains(o);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public java.util.Iterator<E> iterator() {
        return new Iterator();
    }

    @Override
    public Object[] toArray() {
        readerWait();
        Object[] result = list.toArray();
        readers.decrementAndGet();
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        readerWait();
        T[] result = list.toArray(a);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public synchronized boolean add(E e) {
        writerWait();
        boolean result = list.add(e);
        writer = false;
        return result;
    }

    @Override
    public synchronized boolean remove(Object o) {
        writerWait();
        boolean result = list.remove(o);
        writer = false;
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        readerWait();
        boolean result = list.contains(c);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        writerWait();
        boolean result = list.addAll(c);
        writer = false;
        return result;
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        writerWait();
        boolean result = list.addAll(index, c);
        writer = false;
        return result;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        writerWait();
        boolean result = list.removeAll(c);
        writer = false;
        return result;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        writerWait();
        boolean result = list.removeAll(c);
       writer = false;
        return result;
    }

    @Override
    public synchronized void clear() {
        writerWait();
        list.clear();
        writer = false;
    }

    @Override
    public E get(int index) {
        readerWait();
        E result = list.get(index);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public synchronized E set(int index, E element) {
        writerWait();
        E result = list.set(index, element);
        writer = false;
        return result;
    }

    @Override
    public synchronized void add(int index, E element) {
        writerWait();
        list.add(index, element);
        writer = false;
    }

    @Override
    public synchronized E remove(int index) {
        writerWait();
        E result = list.remove(index);
        writer = false;
        return result;
    }

    @Override
    public int indexOf(Object o) {
        readerWait();
        int result = list.indexOf(o);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public int lastIndexOf(Object o) {
        readerWait();
        int result = list.lastIndexOf(o);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public ListIterator<E> listIterator() {
        return new Iterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Iterator iterator = new Iterator();
        iterator.current.lazySet(index);
        return iterator;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * This method makes this thread wait if another thread is currently writing to this list
     * or other threads are waiting to write
     */
    private void readerWait() {
        while (writer) {
            Thread.yield(); // I'm still thinking about a better way as yielding still wastes lots of CPU
        }
        readers.incrementAndGet();
    }

    private synchronized void writerWait() {
        writer = true;
        while (readers.get() > 0) {
            Thread.yield();
        }
    }

    private class Iterator implements ListIterator<E> {

        private final AtomicInteger current = new AtomicInteger(0);

        @Override
        public boolean hasNext() {
            readerWait();
            boolean result = list.size() > current.get();
            readers.decrementAndGet();
            return result;
        }

        @Override
        public E next() {
            readerWait();
            int position = current.getAndIncrement();
            if (list.size() > position) {
                E result = list.get(position);
                readers.decrementAndGet();
                return result;
            } else {
                readers.decrementAndGet();
                throw new NoSuchElementException("No next element");
            }
        }

        @Override
        public boolean hasPrevious() {
            readerWait();
            boolean result = list.size() > 0 && current.get() > 0;
            readers.decrementAndGet();
            return result;
        }

        @Override
        public E previous() {
            readerWait();
            int position = current.getAndDecrement();
            E result;
            if (list.size() > 0 && position >= 0 && position < list.size()) {
                result = list.get(position);
            } else if (list.size() == 0 || position < 0) {
                readers.decrementAndGet();
                throw new NoSuchElementException("No previous element");
            } else {
                int last = list.size() - 1;
                result = list.get(last);
            }
            readers.decrementAndGet();
            return result;
        }

        @Override
        public int nextIndex() {
            return current.get() + 1;
        }

        @Override
        public int previousIndex() {
            return current.get() - 1;
        }

        @Override
        public synchronized void remove() {
            writerWait();
            list.remove(current.get());
            writer = false;
        }

        @Override
        public synchronized void set(E e) {
            writerWait();
            list.set(current.get(), e);
            writer = false;
        }

        @Override
        public synchronized void add(E e) {
            writerWait();
            list.add(current.get() + 1, e);
            writer = false;
        }
    }
}
