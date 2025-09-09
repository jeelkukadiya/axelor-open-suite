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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ImportChecks {

  private ImportChecks() {}

  public static boolean present(Object value) {
    if (value == null) return false;
    String s = value.toString().trim();
    return !s.isEmpty();
  }

  public static boolean inSelection(
      String entityFqn, String fieldName, Object value, boolean isRequired) {

    if (value == null || value.toString().trim().isEmpty()) {
      return !isRequired;
    }

    try {
      Class<?> entity = Class.forName(entityFqn);
      Property p = Mapper.of(entity).getProperty(fieldName);

      if (p == null || p.getSelection() == null) {
        return true;
      }

      String selectionName = p.getSelection();

      List<MetaSelectItem> items =
          JPA.all(MetaSelectItem.class)
              .filter("self.select.name = :name")
              .bind("name", selectionName)
              .fetch();

      if (items == null || items.isEmpty()) {
        System.err.println("Warning: No MetaSelectItem found for selection: " + selectionName);
        return false;
      }

      String val = value.toString().trim();

      boolean isValid = items.stream().map(MetaSelectItem::getValue).anyMatch(val::equals);

      if (!isValid) {
        System.err.println(
            "Invalid selection value '"
                + val
                + "' for "
                + entityFqn
                + "."
                + fieldName
                + ". Allowed values: "
                + items.stream().map(MetaSelectItem::getValue).collect(Collectors.joining(", ")));
      }

      return isValid;

    } catch (Throwable t) {
      System.err.println(
          "Error checking selection for " + entityFqn + "." + fieldName + ": " + t.getMessage());
      return true;
    }
  }

  public static boolean isBooleanText(Object value) {
    if (value == null) return true;
    String s = value.toString().trim().toLowerCase();
    return "true".equals(s) || "false".equals(s);
  }

  public static boolean isXorEmpty(Object value) {
    if (value == null) return true;
    String s = value.toString().trim().toLowerCase();
    return "x".equals(s) || s.isEmpty();
  }

  public static boolean isUnique(
      String entityFqn, String[] fieldNames, Object[] fieldValues, Object csvImportId) {
    try {
      if (fieldNames == null
          || fieldValues == null
          || fieldNames.length == 0
          || fieldNames.length != fieldValues.length) {
        return true;
      }
      Class<?> raw = Class.forName(entityFqn);
      if (!Model.class.isAssignableFrom(raw)) {
        return true;
      }
      @SuppressWarnings("unchecked")
      Class<? extends Model> entity = (Class<? extends Model>) raw.asSubclass(Model.class);

      Object currentRecordDbId = null;
      if (csvImportId != null && present(csvImportId)) {
        Query<? extends Model> existingRecordQuery =
            JPA.all(entity).filter("self.importId = :importId").bind("importId", csvImportId);
        Model existingRecord = existingRecordQuery.fetchOne();

        if (existingRecord != null) {
          currentRecordDbId = existingRecord.getId();
        }
      }

      List<String> whereClauses = new ArrayList<>();
      List<Object[]> binds = new ArrayList<>();

      for (int i = 0; i < fieldNames.length; i++) {
        String fieldName = fieldNames[i];
        Object fieldValue = fieldValues[i];
        String paramName = "p" + i;

        if (fieldValue == null
            || (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty())) {
          whereClauses.add("self." + fieldName + " IS NULL");
        } else if (fieldName.contains(".")) {
          whereClauses.add("self." + fieldName + " = :" + paramName);
          binds.add(new Object[] {paramName, fieldValue});
        } else if (fieldValue instanceof Model) {
          Model model = (Model) fieldValue;
          if (model.getId() != null) {
            whereClauses.add("self." + fieldName + ".id = :" + paramName);
            binds.add(new Object[] {paramName, model.getId()});
          } else {
            whereClauses.add("self." + fieldName + " IS NULL");
          }
        } else {
          whereClauses.add("self." + fieldName + " = :" + paramName);
          binds.add(new Object[] {paramName, fieldValue});
        }
      }

      if (currentRecordDbId != null) {
        String excludeIdParamName = "currentRecordDbIdParam";
        whereClauses.add("self.id != :" + excludeIdParamName);
        binds.add(new Object[] {excludeIdParamName, currentRecordDbId});
      }

      String where = String.join(" AND ", whereClauses);
      Query<? extends Model> query = JPA.all(entity).filter(where);

      for (Object[] bind : binds) {
        query = query.bind((String) bind[0], bind[1]);
      }

      return query.count() == 0;
    } catch (Throwable t) {
      System.err.println("Error checking uniqueness for " + entityFqn + ": " + t.getMessage());
      return true;
    }
  }

  public static boolean entityExistsByField(
	      String targetEntityFqn, String fieldName, Object fieldValue) {
	    if (fieldValue == null
	        || (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty())) {
	      return true;
	    }

	    try {
	      Class<?> raw = Class.forName(targetEntityFqn);
	      if (!Model.class.isAssignableFrom(raw)) {
	        System.err.println(
	            "Error: " + targetEntityFqn + " is not an Axelor Model. Cannot check existence.");
	        return false;
	      }
	      @SuppressWarnings("unchecked")
	      Class<? extends Model> targetEntity = (Class<? extends Model>) raw.asSubclass(Model.class);

	      String whereClause = "self." + fieldName + " = :value";
	      Query<? extends Model> query =
	          JPA.all(targetEntity).filter(whereClause).bind("value", fieldValue);

	      return query.count() > 0;
	    } catch (Throwable t) {
	      System.err.println(
	          "Error checking existence for "
	              + targetEntityFqn
	              + "."
	              + fieldName
	              + ": "
	              + t.getMessage());
	      return false;
	    }
	  }

  public static boolean isResolvedIfProvided(Object csvRawValue, Object relationBean) {
    if (csvRawValue == null) return true;
    String s = csvRawValue.toString().trim();
    if (s.isEmpty()) return true;
    return relationBean != null;
  }
}
