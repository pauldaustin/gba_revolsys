package com.revolsys.swing.map.layer.dataobject;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectFactory;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectState;
import com.revolsys.gis.data.query.Query;
import com.revolsys.swing.SwingWorkerManager;
import com.revolsys.swing.listener.InvokeMethodListener;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.dataobject.renderer.AbstractDataObjectLayerRenderer;
import com.revolsys.swing.map.layer.dataobject.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.overlay.EditGeometryOverlay;
import com.revolsys.swing.map.util.LayerUtil;
import com.vividsolutions.jts.geom.Geometry;

public abstract class AbstractDataObjectLayer extends AbstractLayer implements
  DataObjectLayer, DataObjectFactory {

  public static void actionCompleteAddNewRecord(
    final EditGeometryOverlay overlay) {
    synchronized (overlay) {
      final DataObjectLayer layer = overlay.getLayer();
      final DataObject object = overlay.getObject();
      if (object != null) {
        LayerUtil.showForm(layer, object);
      }
    }
  }

  private DataObjectMetaData metaData;

  private Set<DataObject> selectedObjects = new LinkedHashSet<DataObject>();

  private Set<DataObject> editingObjects = new LinkedHashSet<DataObject>();

  private Set<DataObject> deletedObjects = new LinkedHashSet<DataObject>();

  private Set<DataObject> newObjects = new LinkedHashSet<DataObject>();

  private Set<DataObject> modifiedObjects = new LinkedHashSet<DataObject>();

  private boolean canAddObjects = true;

  private boolean canEditObjects = true;

  private boolean canDeleteObjects = true;

  private BoundingBox boundingBox = new BoundingBox();

  protected Query query;

  private final Object editSync = new Object();

  public AbstractDataObjectLayer() {
    this("");
  }

  @Override
  public int getNewObjectCount() {
    return newObjects.size();
  }

  public AbstractDataObjectLayer(final DataObjectMetaData metaData) {
    this(metaData.getTypeName());
    setMetaData(metaData);
  }

  public AbstractDataObjectLayer(final String name) {
    this(name, GeometryFactory.getFactory(4326));
    setReadOnly(false);
    setSelectSupported(true);
    setQuerySupported(true);
    setRenderer(new GeometryStyleRenderer(this));
  }

  public AbstractDataObjectLayer(final String name,
    final GeometryFactory geometryFactory) {
    super(name);
    setGeometryFactory(geometryFactory);
  }

  public void addEditingObject(final DataObject object) {
    editingObjects.add(object);
  }

  protected void addModifiedObject(final DataObject object) {
    synchronized (modifiedObjects) {
      modifiedObjects.add(object);
    }
  }

  protected void removeModifiedObject(final DataObject object) {
    synchronized (modifiedObjects) {
      modifiedObjects.remove(object);
    }
  }

  @Override
  public void addNewRecord() {
    final DataObjectMetaData metaData = getMetaData();
    final Attribute geometryAttribute = metaData.getGeometryAttribute();
    if (geometryAttribute == null) {
      final DataObject object = this.createObject();
      if (object != null) {
        LayerUtil.showForm(this, object);
      }
    } else {
      final MapPanel map = MapPanel.get(this);
      if (map != null) {
        final EditGeometryOverlay addGeometryOverlay = map.getMapOverlay(EditGeometryOverlay.class);
        synchronized (addGeometryOverlay) {
          // TODO what if there is another feature being edited?
          addGeometryOverlay.addObject(this, new InvokeMethodListener(this,
            "actionCompleteAddNewRecord", addGeometryOverlay));
          // TODO cancel action
        }
      }
    }
  }

  protected void addSelectedObject(final DataObject object) {
    if (isLayerObject(object)) {
      selectedObjects.add(object);
    }
  }

  @Override
  public void addSelectedObjects(final Collection<? extends DataObject> objects) {
    for (final DataObject object : objects) {
      addSelectedObject(object);
    }
    fireSelected();
  }

  @Override
  public void addSelectedObjects(final DataObject... objects) {
    addSelectedObjects(Arrays.asList(objects));
  }

  public void cancelChanges() {
    synchronized (editSync) {
      internalCancelChanges();
      fireObjectsChanged();
    }
  }

  protected void clearChanges() {
    newObjects = new LinkedHashSet<DataObject>();
    modifiedObjects = new LinkedHashSet<DataObject>();
    deletedObjects = new LinkedHashSet<DataObject>();
    editingObjects.clear();
  }

  @Override
  public void clearEditingObjects() {
    this.editingObjects.clear();
  }

  @Override
  public void clearSelectedObjects() {
    selectedObjects = new LinkedHashSet<DataObject>();
    getPropertyChangeSupport().firePropertyChange("selected", true, false);
  }

  @Override
  public DataObject createDataObject(final DataObjectMetaData metaData) {
    if (metaData.equals(getMetaData())) {
      return new LayerDataObject(this);
    } else {
      throw new IllegalArgumentException("Cannot create objects for "
        + metaData);
    }
  }

  @Override
  public DataObject createObject() {
    if (!isReadOnly() && isEditable() && isCanAddObjects()) {
      final DataObject object = new LayerDataObject(this);
      newObjects.add(object);
      return object;
    } else {
      return null;
    }
  }

  @Override
  public void deleteObjects(final Collection<? extends DataObject> objects) {
    synchronized (editSync) {
      unselectObjects(objects);
      for (final DataObject object : objects) {
        deleteObject(object);
      }
    }
    fireObjectsChanged();
  }

  protected void deleteObject(final DataObject object) {
    if (isLayerObject(object)) {
      if (!newObjects.remove(object)) {
        modifiedObjects.remove(object);
        deletedObjects.add(object);
        selectedObjects.remove(object);
      }
      object.setState(DataObjectState.Deleted);
    }
  }

  @Override
  public void deleteObjects(final DataObject... objects) {
    deleteObjects(Arrays.asList(objects));
  }

  protected boolean doSaveChanges() {
    return true;
  }

  protected void fireObjectsChanged() {
    getPropertyChangeSupport().firePropertyChange("objectsChanged", false, true);
  }

  protected void fireSelected() {
    final boolean selected = !selectedObjects.isEmpty();
    getPropertyChangeSupport().firePropertyChange("selected", !selected,
      selected);
  }

  @Override
  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  public CoordinateSystem getCoordinateSystem() {
    return getGeometryFactory().getCoordinateSystem();
  }

  @Override
  public List<DataObject> getDataObjects(final BoundingBox boundingBox) {
    return Collections.emptyList();
  }

  @Override
  public DataObjectStore getDataStore() {
    return getMetaData().getDataObjectStore();
  }

  public Set<DataObject> getDeletedObjects() {
    return deletedObjects;
  }

  @Override
  public Set<DataObject> getEditingObjects() {
    return editingObjects;
  }

  @Override
  public DataObjectMetaData getMetaData() {
    return metaData;
  }

  public Set<DataObject> getModifiedObjects() {
    return new LinkedHashSet<DataObject>(modifiedObjects);
  }

  public List<DataObject> getNewObjects() {
    return new ArrayList<DataObject>(newObjects);
  }

  @Override
  public DataObject getObject(final int row) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<DataObject> getObjects() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<DataObject> getObjects(final Geometry geometry,
    final double distance) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query getQuery() {
    if (query == null) {
      return null;
    } else {
      return query.clone();
    }
  }

  @Override
  public int getRowCount() {
    final DataObjectMetaData metaData = getMetaData();
    final Query query = new Query(metaData);
    return getRowCount(query);
  }

  @Override
  public int getRowCount(final Query query) {
    LoggerFactory.getLogger(getClass()).error("Get row count not implemented");
    return 0;
  }

  @Override
  public BoundingBox getSelectedBoundingBox() {
    BoundingBox boundingBox = super.getSelectedBoundingBox();
    for (final DataObject object : getSelectedObjects()) {
      final Geometry geometry = object.getGeometryValue();
      boundingBox = boundingBox.expandToInclude(geometry);
    }
    return boundingBox;
  }

  @Override
  public List<DataObject> getSelectedObjects() {
    return new ArrayList<DataObject>(selectedObjects);
  }

  @Override
  public int getSelectionCount() {
    return selectedObjects.size();
  }

  protected void internalCancelChanges() {
    clearChanges();
  }

  @Override
  public boolean isCanAddObjects() {
    return !isReadOnly() && isEditable() && canAddObjects;
  }

  @Override
  public boolean isCanDeleteObjects() {
    return !isReadOnly() && isEditable() && canDeleteObjects;
  }

  @Override
  public boolean isCanEditObjects() {
    return !isReadOnly() && isEditable() && canEditObjects;
  }

  @Override
  public boolean isDeleted(final DataObject object) {
    return deletedObjects.contains(object);
  }

  public boolean isEditing(final DataObject object) {
    return editingObjects.contains(object);
  }

  public boolean isNew(final DataObject object) {
    return newObjects.contains(object);
  }

  public int getChangeCount() {
    synchronized (editSync) {
      int changeCount = 0;
      changeCount += newObjects.size();
      changeCount += modifiedObjects.size();
      changeCount += deletedObjects.size();
      return changeCount;
    }
  }

  public List<DataObject> getChanges() {
    synchronized (editSync) {
      List<DataObject> objects = new ArrayList<DataObject>();
      objects.addAll(newObjects);
      objects.addAll(modifiedObjects);
      objects.addAll(deletedObjects);
      return objects;
    }
  }

  @Override
  public boolean isHasChanges() {
    if (isEditable()) {
      synchronized (editSync) {
        if (!newObjects.isEmpty()) {
          return true;
        } else if (!modifiedObjects.isEmpty()) {
          return true;
        } else if (!deletedObjects.isEmpty()) {
          return true;
        } else {
          return false;
        }
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean isHidden(final DataObject object) {
    if (isDeleted(object)) {
      return true;
    } else if (isSelected(object)) {
      return true;
    } else if (isEditing(object)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isLayerObject(final DataObject object) {
    if (object.getMetaData() == getMetaData()) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean isModified(final DataObject object) {
    return modifiedObjects.contains(object);
  }

  @Override
  public boolean isSelected(final DataObject object) {
    if (object == null) {
      return false;
    } else {
      return selectedObjects.contains(object);
    }
  }

  @Override
  public boolean isVisible(final DataObject object) {
    if (isVisible()) {
      final AbstractDataObjectLayerRenderer renderer = getRenderer();
      if (renderer.isVisible(object)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    super.propertyChange(event);
    final Object source = event.getSource();
    if (source instanceof LayerDataObject) {
      final LayerDataObject dataObject = (LayerDataObject)source;
      if (dataObject.getLayer() == this) {
        DataObjectState state = dataObject.getState();
        if (state == DataObjectState.Modified) {
          addModifiedObject(dataObject);
        } else if (state == DataObjectState.Persisted) {
          removeModifiedObject(dataObject);
        }
      }
    }
  }

  @Override
  public List<DataObject> query(final Query query) {
    throw new UnsupportedOperationException("Query not currently supported");
  }

  public boolean saveChanges() {
    synchronized (editSync) {
      final boolean saved = doSaveChanges();
      if (saved) {
        clearChanges();
      }
      fireObjectsChanged();
      return saved;
    }
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setCanAddObjects(final boolean canAddObjects) {
    this.canAddObjects = canAddObjects;
    getPropertyChangeSupport().firePropertyChange("canAddObjects",
      !isCanAddObjects(), isCanAddObjects());
  }

  public void setCanDeleteObjects(final boolean canDeleteObjects) {
    this.canDeleteObjects = canDeleteObjects;
    getPropertyChangeSupport().firePropertyChange("canDeleteObjects",
      !isCanDeleteObjects(), isCanDeleteObjects());
  }

  public void setCanEditObjects(final boolean canEditObjects) {
    this.canEditObjects = canEditObjects;
    getPropertyChangeSupport().firePropertyChange("canEditObjects",
      !isCanEditObjects(), isCanEditObjects());
  }

  @Override
  public void setEditable(final boolean editable) {
    if (SwingUtilities.isEventDispatchThread()) {
      SwingWorkerManager.execute("Set editable", this, "setEditable", editable);
    } else {
      synchronized (editSync) {
        if (editable == false) {
          if (isHasChanges()) {
            final Integer result = InvokeMethodCallable.invokeAndWait(
              JOptionPane.class,
              "showConfirmDialog",
              JOptionPane.getRootFrame(),
              "The layer has unsaved changes. Click Yes to save changes. Click No to discard changes. Click Cancel to continue editing.",
              "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION);

            if (result == JOptionPane.YES_OPTION) {
              if (!saveChanges()) {
                return;
              }
            } else if (result == JOptionPane.NO_OPTION) {
              cancelChanges();
            } else {
              // Don't allow state change if cancelled
              return;
            }

          }
        }
        super.setEditable(editable);
        setCanAddObjects(canAddObjects);
        setCanDeleteObjects(canDeleteObjects);
        setCanEditObjects(canEditObjects);
      }
    }
  }

  @Override
  public void setEditingObjects(
    final Collection<? extends DataObject> editingObjects) {
    this.editingObjects = new LinkedHashSet<DataObject>();
    for (final DataObject object : editingObjects) {
      addEditingObject(object);
    }
  }

  @Override
  protected void setGeometryFactory(final GeometryFactory geometryFactory) {
    super.setGeometryFactory(geometryFactory);
    if (geometryFactory != null && boundingBox.isNull()) {
      boundingBox = geometryFactory.getCoordinateSystem().getAreaBoundingBox();
    }
  }

  protected void setMetaData(final DataObjectMetaData metaData) {
    this.metaData = metaData;
    setGeometryFactory(metaData.getGeometryFactory());
    if (metaData.getGeometryAttributeIndex() == -1) {
      setSelectSupported(false);
    }
  }

  @Override
  public void setProperty(final String name, final Object value) {
    if ("style".equals(name)) {
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> style = (Map<String, Object>)value;
        final LayerRenderer<DataObjectLayer> renderer = AbstractDataObjectLayerRenderer.getRenderer(
          this, style);
        if (renderer != null) {
          setRenderer(renderer);
        }
      }
    } else {
      super.setProperty(name, value);
    }
  }

  public void setQuery(final Query query) {
    final Query oldValue = this.query;
    this.query = query;
    getPropertyChangeSupport().firePropertyChange("query", oldValue, query);
  }

  @Override
  public void setRenderer(final LayerRenderer<? extends Layer> renderer) {
    super.setRenderer(renderer);
  }

  @Override
  public void setSelectedObjects(final BoundingBox boundingBox) {
    final List<DataObject> objects = getDataObjects(boundingBox);
    setSelectedObjects(objects);
  }

  @Override
  public void setSelectedObjects(final Collection<DataObject> selectedObjects) {
    this.selectedObjects = new LinkedHashSet<DataObject>(selectedObjects);
    fireSelected();

  }

  @Override
  public void setSelectedObjects(final DataObject... selectedObjects) {
    setSelectedObjects(Arrays.asList(selectedObjects));
  }

  @Override
  public void setSelectedObjectsById(final Object id) {
    final DataObjectMetaData metaData = getMetaData();
    final String idAttributeName = metaData.getIdAttributeName();
    if (idAttributeName == null) {
      clearSelectedObjects();
    } else {
      final Query query = new Query(metaData);
      query.addFilter(idAttributeName, id);
      final List<DataObject> objects = query(query);
      setSelectedObjects(objects);
    }
  }

  @Override
  public int setSelectedWithinDistance(final boolean selected,
    final Geometry geometry, final int distance) {
    final List<DataObject> objects = getObjects(geometry, distance);
    if (selected) {
      selectedObjects.addAll(objects);
    } else {
      selectedObjects.removeAll(objects);
    }
    return objects.size();
  }

  @Override
  public void unselectObjects(final Collection<? extends DataObject> objects) {
    selectedObjects.removeAll(objects);
    fireSelected();
  }

  @Override
  public void unselectObjects(final DataObject... objects) {
    unselectObjects(Arrays.asList(objects));
  }
}
