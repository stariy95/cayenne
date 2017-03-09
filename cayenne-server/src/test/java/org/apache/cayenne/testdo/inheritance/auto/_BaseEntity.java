package org.apache.cayenne.testdo.inheritance.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.inheritance.DirectToSubEntity;
import org.apache.cayenne.testdo.inheritance.RelatedEntity;
import org.apache.cayenne.testdo.inheritance.ToManyRelatedEntity;

/**
 * Class _BaseEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _BaseEntity extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String BASE_ENTITY_ID_PK_COLUMN = "BASE_ENTITY_ID";

    public static final Property<String> ENTITY_TYPE = Property.create("entityType", String.class);
    public static final Property<DirectToSubEntity> TO_DIRECT_TO_SUB_ENTITY = Property.create("toDirectToSubEntity", DirectToSubEntity.class);
    public static final Property<List<ToManyRelatedEntity>> TO_MANY_RELATED_ENTITIES = Property.create("toManyRelatedEntities", List.class);
    public static final Property<RelatedEntity> TO_RELATED_ENTITY = Property.create("toRelatedEntity", RelatedEntity.class);

    public void setEntityType(String entityType) {
        writeProperty("entityType", entityType);
    }
    public String getEntityType() {
        return (String)readProperty("entityType");
    }

    public void setToDirectToSubEntity(DirectToSubEntity toDirectToSubEntity) {
        setToOneTarget("toDirectToSubEntity", toDirectToSubEntity, true);
    }

    public DirectToSubEntity getToDirectToSubEntity() {
        return (DirectToSubEntity)readProperty("toDirectToSubEntity");
    }


    public void addToToManyRelatedEntities(ToManyRelatedEntity obj) {
        addToManyTarget("toManyRelatedEntities", obj, true);
    }
    public void removeFromToManyRelatedEntities(ToManyRelatedEntity obj) {
        removeToManyTarget("toManyRelatedEntities", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<ToManyRelatedEntity> getToManyRelatedEntities() {
        return (List<ToManyRelatedEntity>)readProperty("toManyRelatedEntities");
    }


    public void setToRelatedEntity(RelatedEntity toRelatedEntity) {
        setToOneTarget("toRelatedEntity", toRelatedEntity, true);
    }

    public RelatedEntity getToRelatedEntity() {
        return (RelatedEntity)readProperty("toRelatedEntity");
    }


}
