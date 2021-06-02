package de.tgx03;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizedList<E> implements List<E> {

    private final List<E> list;
    private final AtomicInteger readers = new AtomicInteger(0);
    private final AtomicBoolean writing = new AtomicBoolean(false);
    private final AtomicInteger waitingWriters = new AtomicInteger(0);

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
    public boolean add(E e) {
        writerWait();
        boolean result = list.add(e);
        writing.lazySet(false);
        return result;
    }

    @Override
    public boolean remove(Object o) {
        writerWait();
        boolean result = list.remove(o);
        writing.lazySet(false);
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
    public boolean addAll(Collection<? extends E> c) {
        writerWait();
        boolean result = list.addAll(c);
        writing.lazySet(false);
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        writerWait();
        boolean result = list.addAll(index, c);
        writing.lazySet(false);
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        writerWait();
        boolean result = list.removeAll(c);
        writing.lazySet(false);
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        writerWait();
        boolean result = list.removeAll(c);
        writing.lazySet(false);
        return result;
    }

    @Override
    public void clear() {
        writerWait();
        list.clear();
        writing.lazySet(false);
    }

    @Override
    public E get(int index) {
        readerWait();
        E result = list.get(index);
        readers.decrementAndGet();
        return result;
    }

    @Override
    public E set(int index, E element) {
        writerWait();
        E result = list.set(index, element);
        writing.lazySet(false);
        return result;
    }

    @Override
    public void add(int index, E element) {
        writerWait();
        list.add(index, element);
        writing.lazySet(false);
    }

    @Override
    public E remove(int index) {
        writerWait();
        E result = list.remove(index);
        writing.lazySet(false);
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

    private void readerWait() {
        while (waitingWriters.get() != 0 || writing.get()) {
            Thread.yield();
        }
        readers.incrementAndGet();
    }

    private void writerWait() {
        waitingWriters.incrementAndGet();
        while (!writing.compareAndSet(false, true)) {
            Thread.yield();
        }
        waitingWriters.decrementAndGet();
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
        public void remove() {
            writerWait();
            list.remove(current.get());
            writing.lazySet(false);
        }

        @Override
        public void set(E e) {
            writerWait();
            list.set(current.get(), e);
            writing.lazySet(false);
        }

        @Override
        public void add(E e) {
            writerWait();
            list.add(current.get() + 1, e);
            writing.lazySet(false);
        }
    }
}
