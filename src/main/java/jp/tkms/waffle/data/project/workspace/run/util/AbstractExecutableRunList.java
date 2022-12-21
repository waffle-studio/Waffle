package jp.tkms.waffle.data.project.workspace.run.util;

import jp.tkms.utils.value.Values;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract public class AbstractExecutableRunList implements List<ExecutableRun> {
  abstract protected void initProcess();
  abstract protected AbstractExecutableRunList createEmptyList();
  abstract public FilteredExecutableRunList filterByTag(String tag);

  protected Workspace workspace;
  protected ArrayList<ExecutableRun> list;

  AbstractExecutableRunList(Workspace workspace) {
    this.workspace = workspace;
    this.list = null;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  protected void init() {
    if (list == null) {
      initProcess();
    }
  }

  public AbstractExecutableRunList sort(Function<ExecutableRun, Object> keyExtractor) {
    init();
    AbstractExecutableRunList res = createEmptyList();
    res.list = cloneList();
    Collections.sort(res.list, (r1, r2)->{
      Object v1 = keyExtractor.apply(r1);
      Object v2 = keyExtractor.apply(r2);
      if (v1 == v2) {
        return 0;
      } else if (v1 == null && v2 != null) {
        return 1;
      } else if (v1 != null && v2 == null) {
        return -1;
      }
      if (v1 instanceof Number) {
        return Double.valueOf(v1.toString()).compareTo(Double.valueOf(v2.toString()));
      }
      return v1.toString().compareTo(v2.toString());
    });
    return res;
  }

  public AbstractExecutableRunList sortWithComparator(BiFunction<ExecutableRun, ExecutableRun, Double> comparator) {
    init();
    AbstractExecutableRunList res = createEmptyList();
    res.list = cloneList();
    Collections.sort(res.list, (r1, r2)->{
      Double v = comparator.apply(r1, r2);
      if (v == null || v == 0.0) {
        return 0;
      } else if (v > 0) {
        return 1;
      } else {
        return -1;
      }
    });
    return res;
  }

  public AbstractExecutableRunList reverse(){
    init();
    AbstractExecutableRunList res = createEmptyList();
    res.list = cloneList();
    Collections.reverse(res.list);
    return res;
  }

  protected ArrayList<ExecutableRun> cloneList() {
    init();
    return new ArrayList<>(list);
  }

  @Override
  public int size() {
    init();
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    init();
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    init();
    return list.contains(o);
  }

  @NotNull
  @Override
  public Iterator<ExecutableRun> iterator() {
    init();
    return list.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    init();
    return list.toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] ts) {
    init();
    return list.toArray(ts);
  }

  @Override
  public boolean add(ExecutableRun executableRun) {
    init();
    return list.add(executableRun);
  }

  @Override
  public boolean remove(Object o) {
    init();
    return remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> collection) {
    init();
    return list.containsAll(collection);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends ExecutableRun> collection) {
    init();
    return list.addAll(collection);
  }

  @Override
  public boolean addAll(int i, @NotNull Collection<? extends ExecutableRun> collection) {
    init();
    return list.addAll(i, collection);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> collection) {
    init();
    return list.removeAll(collection);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> collection) {
    init();
    return list.retainAll(collection);
  }

  @Override
  public void clear() {
    init();
    list.clear();
  }

  @Override
  public ExecutableRun get(int i) {
    init();
    return list.get(i);
  }

  @Override
  public ExecutableRun set(int i, ExecutableRun executableRun) {
    init();
    return list.set(i, executableRun);
  }

  @Override
  public void add(int i, ExecutableRun executableRun) {
    init();
    list.add(i, executableRun);
  }

  @Override
  public ExecutableRun remove(int i) {
    init();
    return list.remove(i);
  }

  @Override
  public int indexOf(Object o) {
    init();
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    init();
    return list.lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<ExecutableRun> listIterator() {
    init();
    return list.listIterator();
  }

  @NotNull
  @Override
  public ListIterator<ExecutableRun> listIterator(int i) {
    init();
    return list.listIterator(i);
  }

  @NotNull
  @Override
  public List<ExecutableRun> subList(int i, int i1) {
    init();
    AbstractExecutableRunList res = createEmptyList();
    res.list = new ArrayList<>(cloneList().subList(i, i1));
    return res;
  }
}
