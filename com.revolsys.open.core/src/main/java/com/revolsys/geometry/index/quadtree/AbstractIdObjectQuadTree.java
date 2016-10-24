package com.revolsys.geometry.index.quadtree;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.geometry.index.IdObjectIndex;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.visitor.CreateListVisitor;

public abstract class AbstractIdObjectQuadTree<T> implements IdObjectIndex<T> {

  private final QuadTree<Integer> index;

  public AbstractIdObjectQuadTree(final GeometryFactory geometryFactory) {
    this.index = new QuadTree<>(geometryFactory);
  }

  public void add(final Collection<Integer> ids) {
    for (final Integer id : ids) {
      final T object = getObject(id);
      add(object);
    }
  }

  @Override
  public T add(final T object) {
    final BoundingBox envelope = getEnvelope(object);
    final int id = getId(object);
    this.index.insertItem(envelope, id);
    return object;
  }

  @Override
  public void forEach(final BoundingBox boundingBox, final Consumer<? super T> action) {
    this.index.forEach(boundingBox, (id) -> {
      final T object = getObject(id);
      final BoundingBox e = getEnvelope(object);
      if (e.intersects(boundingBox)) {
        action.accept(object);
      }
    });
  }

  @Override
  public void forEach(final BoundingBox boundingBox, final Predicate<? super T> filter,
    final Consumer<? super T> action) {
    this.index.forEach(boundingBox, (id) -> {
      final T object = getObject(id);
      final BoundingBox e = getEnvelope(object);
      if (e.intersects(boundingBox) && filter.test(object)) {
        action.accept(object);
      }
    });
  }

  @Override
  public Iterator<T> iterator() {
    return queryAll().iterator();
  }

  @Override
  public List<T> query(final BoundingBox envelope) {
    final CreateListVisitor<T> visitor = new CreateListVisitor<>();
    forEach(envelope, visitor);
    return visitor.getList();
  }

  public List<T> queryAll() {
    final List<Integer> ids = this.index.getAll();
    return getObjects(ids);
  }

  @Override
  public boolean remove(final T object) {
    final BoundingBox envelope = getEnvelope(object);
    final int id = getId(object);
    return this.index.removeItem(envelope, id);
  }

  public void removeAll(final Collection<T> objects) {
    for (final T object : objects) {
      remove(object);
    }
  }

}
