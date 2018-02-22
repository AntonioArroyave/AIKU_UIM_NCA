<?xml version="1.0" encoding="UTF-8"?>
<com:modelEntity xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:com="http://www.mslv.com/studio/core/model/common" xmlns:data="http://www.oracle.com/communications/studio/core/model/common/data" xmlns:inv="http://www.mslv.com/studio/inventory/model/specification" xmlns="http://www.mslv.com/studio/inventory/model/specification" xmlns:layout="http://xmlns.oracle.com/communications/sce/poms/model/layout" xmlns:poms="http://xmlns.oracle.com/communications/sce/poms/model/poms" xsi:type="inv:BusinessInteractionSpecificationType" name="Aprovisionamiento_BA_HFC">
  <com:baseEntityRef>
    <com:entity>BusinessInteraction</com:entity>
    <com:entityType>inventoryEntity</com:entityType>
    <com:relationship>unknown</com:relationship>
  </com:baseEntityRef>
  <com:id>YTMV+NPISaWP2q5u6aViwA</com:id>
  <data:dataElementNode virtual="true">
    <com:id>YTMV+NPISaWP2q5u6aViwA</com:id>
    <com:elementType>oracle.communications.studio.model.data.StudioModelDataElement</com:elementType>
    <data:name>Aprovisionamiento_BA_HFC</data:name>
    <data:displayName lang="[default]">Aprovisionamiento_BA_HFC</data:displayName>
    <data:primitiveType>none</data:primitiveType>
  </data:dataElementNode>
  <data:dataElementDetails xsi:type="data:dataElementCommonDetail">
    <com:id>yHWxRqF5QaOcBpMHIMizdQ</com:id>
    <com:elementType>oracle.communications.studio.model.data.StudioModelDataElementCommonDetails</com:elementType>
    <data:dataElementId>YTMV+NPISaWP2q5u6aViwA</data:dataElementId>
    <data:defaultValue></data:defaultValue>
    <data:key></data:key>
    <data:deprecated>false</data:deprecated>
    <data:sensitive>false</data:sensitive>
    <data:minLength>0</data:minLength>
    <data:maxLength>40</data:maxLength>
    <data:minMultiplicity>0</data:minMultiplicity>
    <data:maxMultiplicity>-1</data:maxMultiplicity>
  </data:dataElementDetails>
  <data:dataElementDetails xsi:type="layout:uiConfigurationType">
    <com:id>DmEITx1jRtysKPdioEyjZg</com:id>
    <com:elementType>oracle.communications.sce.poms.model.data.LayoutDetailType</com:elementType>
    <data:dataElementId>YTMV+NPISaWP2q5u6aViwA</data:dataElementId>
  </data:dataElementDetails>
  <inv:relatedSpecification>
    <com:entity>BA_CFS</com:entity>
    <com:entityType>configuration</com:entityType>
    <com:relationship>Aprovisionamiento_BA_HFC To BA_CFS</com:relationship>
    <inv:data xsi:type="inv:SpecificationRelationshipReferenceDataType">
      <inv:relationship>CHILD</inv:relationship>
      <inv:status>ACTIVE</inv:status>
      <inv:parentsToChildMin>1</inv:parentsToChildMin>
      <inv:parentsToChildMax>1</inv:parentsToChildMax>
    </inv:data>
  </inv:relatedSpecification>
  <inv:entityType>BusinessInteraction</inv:entityType>
</com:modelEntity>