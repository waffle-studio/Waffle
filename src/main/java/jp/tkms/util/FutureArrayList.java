package jp.tkms.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FutureArrayList<E> implements List<E> {
  private static final Object objectLocker = new Object();
  private static ExecutorService staticExecutorService = null;

  private transient int modCount = 0;
  private ExecutorService executorService;
  private ArrayList<Future<E>> list;

  public FutureArrayList() {
    executorService = initializeExecutorService();
    list = new ArrayList<>();
  }

  public FutureArrayList(ExecutorService executorService) {
    this.executorService = executorService;
    list = new ArrayList<>();
  }

  private static ExecutorService initializeExecutorService() {
    synchronized (objectLocker) {
      if (staticExecutorService == null) {
        staticExecutorService = Executors.newWorkStealingPool();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> staticExecutorService.shutdown()));
      }
      return staticExecutorService;
    }
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    for (E entity : this) {
      if (entity.equals(o)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    Object[] array = new Object[size()];
    for (int i = 0; i < size(); i += 1) {
      array[i] = get(i);
    }
    return array;
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] ts) {
    for (int i = 0; i < size(); i += 1) {
      ts[i] = (T)get(i);
    }
    return ts;
  }

  @Override
  public boolean add(E e) {
    return list.add(executorService.submit(() -> e));
  }

  public boolean add(Callable<E> c) {
    return list.add(executorService.submit(c));
  }

  public boolean add(Future<E> f) {
    return list.add(f);
  }

  @Override
  public boolean remove(Object o) {
    boolean result = false;
    for (int i = size() -1; i >= 0; i -= 1) {
      if (get(i).equals(o)) {
        result = true;
        list.remove(i);
      }
    }
    return result;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> collection) {
    for (Object o : collection) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends E> collection) {
    boolean result = true;
    for (E o : collection) {
      if (!add(o)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public boolean addAll(int i, @NotNull Collection<? extends E> collection) {
    boolean result = false;
    for (E o : collection) {
      result = true;
      add(i, o);
    }
    return result;
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> collection) {
    boolean result = false;
    for (Object o : collection) {
      if (remove(o)) {
        result = true;
      }
    }
    return result;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> collection) {
    boolean result = false;
    for (int i = size() -1; i >= 0; i -= 1) {
      if (!collection.equals(get(i))) {
        result = true;
        list.remove(i);
      }
    }
    return result;
  }

  @Override
  public void clear() {
    for (Future<E> future : list) {
      future.cancel(false);
    }
    list.clear();
  }

  @Override
  public E get(int i) {
    try {
      return list.get(i).get();
    } catch (InterruptedException | ExecutionException e) {
      return null;
    }
  }

  @Override
  public E set(int i, E e) {
    list.set(i, executorService.submit(() -> e));
    return e;
  }

  @Override
  public void add(int i, E e) {
    list.add(i, executorService.submit(() -> e));
  }

  @Override
  public E remove(int i) {
    return null;
  }

  @Override
  public int indexOf(Object o) {
    for (int i = 0; i < size(); i += 1) {
      if (get(i).equals(o)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    for (int i = size() -1; i >= 0; i -= 1) {
      if (get(i).equals(o)) {
        return i;
      }
    }
    return -1;
  }

  @NotNull
  @Override
  public ListIterator<E> listIterator() {
    return new ListItr(0);
  }

  @NotNull
  @Override
  public ListIterator<E> listIterator(int i) {
    return new ListItr(i);
  }

  @NotNull
  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    subListRangeCheck(fromIndex, toIndex, list.size());
    FutureArrayList<E> subList = new FutureArrayList<>();
    for (int i = fromIndex; i <= toIndex; i += 1) {
      subList.add(list.get(i));
    }
    return subList;
  }

  private static void subListRangeCheck(int fromIndex, int toIndex, int size) {
    if (fromIndex < 0) {
      throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
    } else if (toIndex > size) {
      throw new IndexOutOfBoundsException("toIndex = " + toIndex);
    } else if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
  }

  private class ListItr extends Itr implements ListIterator<E> {
    ListItr(int index) {
      super();
      this.cursor = index;
    }

    public boolean hasPrevious() {
      return this.cursor != 0;
    }

    public int nextIndex() {
      return this.cursor;
    }

    public int previousIndex() {
      return this.cursor - 1;
    }

    public E previous() {
      this.checkForComodification();
      int i = this.cursor - 1;
      if (i < 0) {
        throw new NoSuchElementException();
      } else {
        if (i >= FutureArrayList.this.size()) {
          throw new ConcurrentModificationException();
        } else {
          this.cursor = i;
          return FutureArrayList.this.get(this.lastRet = i);
        }
      }
    }

    public void set(E e) {
      if (this.lastRet < 0) {
        throw new IllegalStateException();
      } else {
        this.checkForComodification();

        try {
          FutureArrayList.this.set(this.lastRet, e);
        } catch (IndexOutOfBoundsException var3) {
          throw new ConcurrentModificationException();
        }
      }
    }

    public void add(E e) {
      this.checkForComodification();

      try {
        int i = this.cursor;
        FutureArrayList.this.add(i, e);
        this.cursor = i + 1;
        this.lastRet = -1;
        this.expectedModCount = FutureArrayList.this.modCount;
      } catch (IndexOutOfBoundsException var3) {
        throw new ConcurrentModificationException();
      }
    }
  }

  private class Itr implements Iterator<E> {
    int cursor;
    int lastRet = -1;
    int expectedModCount;

    Itr() {
      this.expectedModCount = FutureArrayList.this.modCount;
    }

    public boolean hasNext() {
      return this.cursor != FutureArrayList.this.size();
    }

    public E next() {
      this.checkForComodification();
      int i = this.cursor;
      if (i >= FutureArrayList.this.size()) {
        throw new NoSuchElementException();
      } else {
        if (i >= FutureArrayList.this.size()) {
          throw new ConcurrentModificationException();
        } else {
          this.cursor = i + 1;
          return FutureArrayList.this.get(this.lastRet = i);
        }
      }
    }

    public void remove() {
      if (this.lastRet < 0) {
        throw new IllegalStateException();
      } else {
        this.checkForComodification();

        try {
          FutureArrayList.this.remove(this.lastRet);
          this.cursor = this.lastRet;
          this.lastRet = -1;
          this.expectedModCount = FutureArrayList.this.modCount;
        } catch (IndexOutOfBoundsException var2) {
          throw new ConcurrentModificationException();
        }
      }
    }

    public void forEachRemaining(Consumer<? super E> action) {
      Objects.requireNonNull(action);
      int size = FutureArrayList.this.size();
      int i = this.cursor;
      if (i < size) {
        if (i >= FutureArrayList.this.size()) {
          throw new ConcurrentModificationException();
        }

        while(i < size && FutureArrayList.this.modCount == this.expectedModCount) {
          action.accept(FutureArrayList.this.get(i));
          ++i;
        }

        this.cursor = i;
        this.lastRet = i - 1;
        this.checkForComodification();
      }

    }

    final void checkForComodification() {
      if (FutureArrayList.this.modCount != this.expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }
  }
}
