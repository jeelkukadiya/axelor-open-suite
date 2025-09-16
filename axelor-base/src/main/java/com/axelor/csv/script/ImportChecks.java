/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.csv.script;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSelectItem;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

public final class ImportChecks {

  private ImportChecks() {}

  public static boolean isValuePresent(Object value) {
    return value != null && !value.toString().trim().isEmpty();
  }

  public static boolean checkSelection(String entityFqn, String fieldName, Object value) {
    try {
      Class<?> entity = Class.forName(entityFqn);
      Property prop = Mapper.of(entity).getProperty(fieldName);

      if (prop == null) {
        return false;
      }

      boolean isRequired = prop.isRequired();
      if (!isValuePresent(value)) {
        return !isRequired;
      }

      if (prop.getSelection() == null) {
        return false;
      }

      String selectionName = prop.getSelection();
      List<MetaSelectItem> items =
          JPA.all(MetaSelectItem.class)
              .filter("self.select.name = :name")
              .bind("name", selectionName)
              .fetch();

      if (CollectionUtils.isEmpty(items)) {
        return false;
      }

      Set<String> allowedValues =
          items.stream()
              .map(MetaSelectItem::getValue)
              .map(String::trim)
              .collect(Collectors.toSet());

      List<String> valueList;
      if (value instanceof String) {
        valueList =
            Arrays.stream(((String) value).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
      } else {
        valueList = Collections.singletonList(String.valueOf(value).trim());
      }

      return valueList.stream().allMatch(allowedValues::contains);

    } catch (Exception e) {
      return false;
    }
  }

  public static boolean checkUnique(
      String entityFqn,
      String[] fieldNames,
      Object[] fieldValues,
      String exclusionFieldName,
      Object exclusionFieldValue) {
    if (fieldNames == null
        || fieldValues == null
        || fieldNames.length == 0
        || fieldNames.length != fieldValues.length) {
      return true;
    }

    try {
      Class<?> raw = Class.forName(entityFqn);
      if (!Model.class.isAssignableFrom(raw)) return true;

      @SuppressWarnings("unchecked")
      Class<? extends Model> entity = (Class<? extends Model>) raw;

      Long excludeId = getIdForExclusion(entity, exclusionFieldName, exclusionFieldValue);

      StringBuilder where = new StringBuilder();
      Map<String, Object> binds = new HashMap<>();

      for (int i = 0; i < fieldNames.length; i++) {
        String field = fieldNames[i];
        Object val = fieldValues[i];
        String param = "p" + i;

        if (i > 0) where.append(" AND ");
        where.append(buildFieldClause(field, val, param, binds));
      }

      if (excludeId != null) {
        where.append(" AND self.id != :excludeId");
        binds.put("excludeId", excludeId);
      }

      Query<? extends Model> query = JPA.all(entity).filter(where.toString());
      for (Map.Entry<String, Object> entry : binds.entrySet()) {
        query = query.bind(entry.getKey(), entry.getValue());
      }

      return query.count() == 0;

    } catch (Exception e) {
      return false;
    }
  }


  private static String buildFieldClause(
      String fieldName, Object fieldValue, String paramName, Map<String, Object> binds) {
    if (!isValuePresent(fieldValue)) {
      return "self." + fieldName + " IS NULL";
    } else if (fieldValue instanceof Model) {
      Model m = (Model) fieldValue;
      if (m.getId() != null) {
        binds.put(paramName, m.getId());
        return "self." + fieldName + ".id = :" + paramName;
      } else {
        return "self." + fieldName + " IS NULL"; 
      }
    } else if (fieldValue instanceof List) { 
      List<?> valueList = (List<?>) fieldValue;
      if (CollectionUtils.isEmpty(valueList)) {
        return "self." + fieldName + " IS NULL";
      }
      binds.put(paramName, fieldValue);
      return "self." + fieldName + " IN :" + paramName;
    } else {
      binds.put(paramName, fieldValue);
      return "self." + fieldName + " = :" + paramName;
    }
  }

  private static Long getIdForExclusion(
      Class<? extends Model> entity, String exclusionFieldName, Object exclusionFieldValue) {
    if (!isValuePresent(exclusionFieldName) || !isValuePresent(exclusionFieldValue))
      return null; 
    try {
      String filterClause = "self." + exclusionFieldName + " = :val";
     
      Query<? extends Model> q =
          JPA.all(entity).filter(filterClause).bind("val", exclusionFieldValue);
      Model record = q.fetchOne();
      return record != null ? record.getId() : null;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean checkEntityExistsByField(
      String entityFqn, String fieldName, Object fieldValue) {
    if (!isValuePresent(fieldValue)) return true;
    try {
      Class<?> raw = Class.forName(entityFqn);
      if (!Model.class.isAssignableFrom(raw)) return false;
      @SuppressWarnings("unchecked")
      Class<? extends Model> targetEntity = (Class<? extends Model>) raw;
      Query<? extends Model> q =
          JPA.all(targetEntity).filter("self." + fieldName + " = :val").bind("val", fieldValue);
      return q.fetchOne() != null;
    } catch (Exception e) {
      return false;
    }
  }
}
